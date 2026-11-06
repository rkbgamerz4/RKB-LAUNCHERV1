#!/usr/bin/env python3
"""Build a minimal Minecraft Java directory for ONE version straight from Mojang's official endpoints.

Layout produced (what horizon-prepare reads from ~/.minecraft):
  versions/<v>/<v>.json, versions/<v>/<v>.jar, libraries/**, assets/indexes/<id>.json, assets/objects/**
plus the server jar at --server-jar.

Every file is verified against the SHA-1 published by Mojang; a mismatch aborts. Nothing is cached,
patched or modified. The files are the user's own copy fetched from Mojang's public CDN.
"""
import argparse
import concurrent.futures as cf
import hashlib
import json
import os
import sys
import time
import urllib.request

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
ASSETS_BASE = "https://resources.download.minecraft.net"
UA = "rkb-horizon-prep/1.0 (+personal use)"


def http_get(url, tries=5):
    last = None
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(1 + i * 2)
    raise RuntimeError(f"GET failed after {tries} tries: {url}: {last}")


def sha1_of(data):
    return hashlib.sha1(data).hexdigest()


def fetch_verified(url, sha1, dest, label):
    """Download url to dest (skip if already valid), verifying SHA-1."""
    if sha1 and os.path.exists(dest):
        with open(dest, "rb") as f:
            if sha1_of(f.read()) == sha1:
                return "kept"
    data = http_get(url)
    if sha1 and sha1_of(data) != sha1:
        raise RuntimeError(f"SHA-1 mismatch for {label}: expected {sha1}, got {sha1_of(data)}")
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    tmp = dest + ".part"
    with open(tmp, "wb") as f:
        f.write(data)
    os.replace(tmp, dest)
    return "downloaded"


def rules_allow(rules, os_name="linux"):
    """Mojang rule semantics: with rules present the default is deny; the last matching rule wins."""
    if not rules:
        return True
    result = False
    for r in rules:
        match = True
        osr = r.get("os")
        if osr and osr.get("name") and osr["name"] != os_name:
            match = False
        if r.get("features"):
            match = False  # feature-gated entries (demo, quick play...) are not needed
        if match:
            result = r.get("action") == "allow"
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", required=True)
    ap.add_argument("--mc-dir", required=True)
    ap.add_argument("--server-jar", required=True)
    ap.add_argument("--manifest-url", default=MANIFEST)
    ap.add_argument("--assets-base", default=ASSETS_BASE)
    ap.add_argument("--workers", type=int, default=16)
    a = ap.parse_args()

    manifest = json.loads(http_get(a.manifest_url))
    entry = next((v for v in manifest["versions"] if v["id"] == a.version), None)
    if not entry:
        sys.exit(f"version {a.version} not found in Mojang's manifest")
    vjson_bytes = http_get(entry["url"])
    if entry.get("sha1") and sha1_of(vjson_bytes) != entry["sha1"]:
        sys.exit("version JSON SHA-1 mismatch")
    vjson = json.loads(vjson_bytes)

    vdir = os.path.join(a.mc_dir, "versions", a.version)
    os.makedirs(vdir, exist_ok=True)
    with open(os.path.join(vdir, a.version + ".json"), "wb") as f:
        f.write(vjson_bytes)

    dl = vjson["downloads"]
    jobs = []  # (url, sha1, dest, label)
    jobs.append((dl["client"]["url"], dl["client"]["sha1"], os.path.join(vdir, a.version + ".jar"), "client jar"))
    jobs.append((dl["server"]["url"], dl["server"]["sha1"], a.server_jar, "server jar"))

    libs = 0
    for lib in vjson.get("libraries", []):
        if not rules_allow(lib.get("rules")):
            continue
        art = (lib.get("downloads") or {}).get("artifact")
        if not art or not art.get("path") or not art.get("url"):
            continue
        jobs.append((art["url"], art.get("sha1"), os.path.join(a.mc_dir, "libraries", art["path"]), "lib " + art["path"]))
        libs += 1

    ai = vjson["assetIndex"]
    idx_dest = os.path.join(a.mc_dir, "assets", "indexes", ai["id"] + ".json")
    fetch_verified(ai["url"], ai["sha1"], idx_dest, "asset index")
    with open(idx_dest, "rb") as f:
        index = json.loads(f.read())
    objs = {}
    for name, o in index.get("objects", {}).items():
        objs[o["hash"]] = o
    for h in objs:
        jobs.append((f"{a.assets_base}/{h[:2]}/{h}", h, os.path.join(a.mc_dir, "assets", "objects", h[:2], h), "asset " + h))

    print(f"version {a.version}: {libs} libraries, {len(objs)} asset objects, 2 jars")
    done = kept = 0
    with cf.ThreadPoolExecutor(max_workers=a.workers) as ex:
        futs = [ex.submit(fetch_verified, *j) for j in jobs]
        for fu in cf.as_completed(futs):
            r = fu.result()  # raises on any failure
            done += r == "downloaded"
            kept += r == "kept"
    print(f"OK: {done} downloaded, {kept} already valid, all SHA-1 verified")
    print("client_sha1=" + dl["client"]["sha1"])
    print("server_sha1=" + dl["server"]["sha1"])


if __name__ == "__main__":
    main()
