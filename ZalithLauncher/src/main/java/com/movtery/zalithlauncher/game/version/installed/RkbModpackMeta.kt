/*
 * RKB LAUNCHER - marks an instance as "installed from a modpack".
 * Written by the modpack installer only after every download and the final copy succeeded, stored next to
 * version.config inside the instance's launcher folder. Instances without this file are normal instances.
 */
package com.movtery.zalithlauncher.game.version.installed

import org.json.JSONObject
import java.io.File

class RkbModpackMeta(
    val name: String,
    val platform: String,
    val minecraft: String,
    val loaders: String
)

object RkbModpackMetaStore {
    private const val FILE_NAME = "modpack.json"

    fun write(versionFolder: File, name: String, platform: String, minecraft: String, loaders: String) {
        runCatching {
            val dir = getZalithVersionPath(versionFolder)
            dir.mkdirs()
            val json = JSONObject()
                .put("name", name)
                .put("platform", platform)
                .put("minecraft", minecraft)
                .put("loaders", loaders)
                .put("installedAt", System.currentTimeMillis())
            File(dir, FILE_NAME).writeText(json.toString())
        }
    }

    /** The modpack info of this instance, or null for a normal instance. */
    fun read(version: Version): RkbModpackMeta? = runCatching {
        val f = File(version.getZalithVersionPath(), FILE_NAME)
        if (!f.exists()) return null
        val o = JSONObject(f.readText())
        RkbModpackMeta(
            name = o.optString("name"),
            platform = o.optString("platform"),
            minecraft = o.optString("minecraft"),
            loaders = o.optString("loaders")
        )
    }.getOrNull()
}
