package net.kdt.pojavlaunch.utils;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.concurrent.Callable;

import net.kdt.pojavlaunch.*;
import org.apache.commons.io.*;

@SuppressWarnings("IOStreamConstructor")
public class DownloadUtils {
    public static final String USER_AGENT = Tools.APP_NAME;
    private static final int TIME_OUT = 10000;

    public static void download(String url, OutputStream os) throws IOException {
        download(new URL(url), os);
    }

    public static void download(URL url, OutputStream os) throws IOException {
        InputStream is = null;
        try {
            // System.out.println("Connecting: " + url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(TIME_OUT);
            conn.setReadTimeout(TIME_OUT);
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + conn.getResponseCode()
                        + ": " + conn.getResponseMessage());
            }
            is = conn.getInputStream();
            IOUtils.copy(is, os);
        } catch (IOException e) {
            throw new IOException("Unable to download from " + url, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String downloadString(String url) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        download(url, bos);
        bos.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    public static void downloadFile(String url, File out) throws IOException {
        FileUtils.ensureParentDirectory(out);
        try (FileOutputStream fileOutputStream = new FileOutputStream(out)) {
            download(url, fileOutputStream);
        }
    }

    private static final int MONITORED_MAX_ATTEMPTS = 3;
    private static final int MAX_REDIRECTS = 5;

    /** Non-2xx answer from a server. Kept as its own type so callers/logs can see the real status. */
    public static class HttpStatusException extends IOException {
        public final int statusCode;
        public HttpStatusException(int statusCode, String url, String message) {
            super("Server returned HTTP " + statusCode + (message != null ? " (" + message + ")" : "") + " for " + url);
            this.statusCode = statusCode;
        }
        /** 5xx, 408 and 429 usually go away on their own; other 4xx never will. */
        public boolean isTransient() {
            return statusCode >= 500 || statusCode == 408 || statusCode == 429;
        }
    }

    /**
     * Download a file with progress reporting.
     *
     * Compared to the old version this:
     *  - checks the HTTP status (a 404 still throws FileNotFoundException so mirrors can fall back),
     *  - follows redirects itself, including http->https ones that HttpURLConnection refuses to follow
     *    (the redirect body used to be saved as the "file", which then failed SHA-1 verification),
     *  - writes to "<name>.part" and only moves it into place when complete, so an interrupted
     *    download never leaves a truncated file under the real name,
     *  - verifies the received size against Content-Length,
     *  - retries transient failures (timeouts, resets, 5xx/429) with a short back-off,
     *  - always closes the connection and removes the partial file.
     */
    public static void downloadFileMonitored(String urlInput, File outputFile, @Nullable byte[] buffer,
                                             Tools.DownloaderFeedback monitor) throws IOException {
        FileUtils.ensureParentDirectory(outputFile);
        File partFile = new File(outputFile.getPath() + ".part");
        if (buffer == null) buffer = new byte[65535];

        IOException lastError = null;
        for (int attempt = 1; attempt <= MONITORED_MAX_ATTEMPTS; attempt++) {
            try {
                downloadOnce(urlInput, outputFile, partFile, buffer, monitor);
                return;
            } catch (FileNotFoundException e) {
                partFile.delete();
                throw e; // 404/410: not transient, DownloadMirror relies on this type for its fallback
            } catch (IOException e) {
                partFile.delete();
                lastError = e;
                boolean retry = isTransientFailure(e) && attempt < MONITORED_MAX_ATTEMPTS
                        && !Thread.currentThread().isInterrupted();
                Log.w("DownloadUtils", "Download attempt " + attempt + "/" + MONITORED_MAX_ATTEMPTS
                        + " failed for " + urlInput + ": " + e + (retry ? " - retrying" : ""));
                if (!retry) break;
                try {
                    Thread.sleep(400L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IOException("Unable to download from " + urlInput + " : "
                + (lastError != null ? lastError.getMessage() : "unknown error"), lastError);
    }

    private static boolean isTransientFailure(IOException e) {
        if (e instanceof HttpStatusException) return ((HttpStatusException) e).isTransient();
        if (e instanceof javax.net.ssl.SSLException && !(e instanceof javax.net.ssl.SSLHandshakeException)) return true; // reset mid-stream
        if (e instanceof javax.net.ssl.SSLHandshakeException) return false; // certificate/protocol problem, will not fix itself
        return true; // timeouts, resets, truncated streams, DNS hiccups
    }

    private static void downloadOnce(String urlInput, File outputFile, File partFile, byte[] buffer,
                                     Tools.DownloaderFeedback monitor) throws IOException {
        HttpURLConnection conn = openFollowingRedirects(urlInput);
        try {
            long length = conn.getContentLengthLong();
            long overall = 0;
            try (InputStream in = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(partFile)) {
                int current;
                while ((current = in.read(buffer)) != -1) {
                    overall += current;
                    fos.write(buffer, 0, current);
                    monitor.updateProgress((int) overall, (int) length);
                }
            }
            if (length >= 0 && overall != length) {
                throw new IOException("Truncated download: received " + overall + " of " + length + " bytes");
            }
            if (outputFile.exists() && !outputFile.delete()) {
                throw new IOException("Cannot replace the existing file " + outputFile);
            }
            if (!partFile.renameTo(outputFile)) {
                throw new IOException("Cannot move the downloaded file to " + outputFile);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static HttpURLConnection openFollowingRedirects(String urlInput) throws IOException {
        URL url = new URL(urlInput);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            // Plain-http links are refused by Android's cleartext policy on newer targets; every host
            // this launcher talks to serves https as well.
            if ("http".equalsIgnoreCase(url.getProtocol())) url = new URL("https" + url.toString().substring(4));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept-Encoding", "identity"); // keep Content-Length trustworthy
            conn.setConnectTimeout(TIME_OUT);
            conn.setReadTimeout(TIME_OUT);
            conn.setInstanceFollowRedirects(false);
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new IOException("Redirect (HTTP " + code + ") without a Location from " + url);
                }
                url = new URL(url, location);
                continue;
            }
            if (code == 404 || code == 410) {
                conn.disconnect();
                throw new FileNotFoundException("HTTP " + code + " for " + url);
            }
            if (code != HttpURLConnection.HTTP_OK) {
                String msg = conn.getResponseMessage();
                conn.disconnect();
                throw new HttpStatusException(code, url.toString(), msg);
            }
            return conn;
        }
        throw new IOException("Too many redirects for " + urlInput);
    }

    public static <T> T downloadStringCached(String url, String cacheName, ParseCallback<T> parseCallback) throws IOException, ParseException{
        File cacheDestination = new File(Tools.DIR_CACHE, "string_cache/"+cacheName);
        if(cacheDestination.isFile() &&
                cacheDestination.canRead() &&
                System.currentTimeMillis() < (cacheDestination.lastModified() + 86400000)) {
            try {
                String cachedString = Tools.read(new FileInputStream(cacheDestination));
                return parseCallback.process(cachedString);
            }catch(IOException e) {
                Log.i("DownloadUtils", "Failed to read the cached file", e);
            }catch (ParseException e) {
                Log.i("DownloadUtils", "Failed to parse the cached file", e);
            }
        }
        String urlContent = DownloadUtils.downloadString(url);
        // if we download the file and fail parsing it, we will yeet outta there
        // and not cache the unparseable sting. We will return this after trying to save the downloaded
        // string into cache
        T parseResult = parseCallback.process(urlContent);

        boolean tryWriteCache;
        if(cacheDestination.exists()) {
            tryWriteCache = cacheDestination.canWrite();
        } else {
            tryWriteCache = FileUtils.ensureParentDirectorySilently(cacheDestination);
        }

        if(tryWriteCache) try {
            Tools.write(cacheDestination.getAbsolutePath(), urlContent);
        }catch(IOException e) {
            Log.i("DownloadUtils", "Failed to cache the string", e);
        }
        return parseResult;
    }

    private static <T> T downloadFile(Callable<T> downloadFunction) throws IOException{
        try {
            return downloadFunction.call();
        } catch (IOException e){
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean verifyFile(File file, String sha1) {
        return file.exists() && Tools.compareSHA1(file, sha1);
    }

    public static <T> T ensureSha1(File outputFile, @Nullable String sha1, Callable<T> downloadFunction) throws IOException {
        // Skip if needed
        if(sha1 == null) {
            // If the file exists and we don't know it's SHA1, don't try to redownload it.
            if(outputFile.exists()) return null;
            else return downloadFile(downloadFunction);
        }

        int attempts = 0;
        boolean fileOkay = verifyFile(outputFile, sha1);
        T result = null;
        String previousBadHash = null;
        while (attempts < 5 && !fileOkay){
            attempts++;
            downloadFile(downloadFunction);
            fileOkay = verifyFile(outputFile, sha1);
            if (!fileOkay) {
                String actual = Tools.getSHA1(outputFile);
                Log.w("DownloadUtils", "SHA1 mismatch for " + outputFile.getName() + " (attempt " + attempts
                        + "): expected " + sha1 + " got " + actual);
                // The same wrong content twice in a row is not a network glitch: the server (or mirror)
                // is serving something different from what the metadata describes. Stop early.
                if (actual != null && actual.equals(previousBadHash)) {
                    outputFile.delete();
                    throw new SHA1VerificationException("SHA1 verification failed for " + outputFile.getName()
                            + ": the server keeps returning a file with hash " + actual + " instead of " + sha1
                            + " (outdated/tampered mirror or wrong metadata)");
                }
                previousBadHash = actual;
            }
        }
        if(!fileOkay) {
            outputFile.delete(); // never leave a file that failed verification behind
            throw new SHA1VerificationException("SHA1 verification failed for " + outputFile.getName()
                    + " after " + attempts + " download attempts");
        }
        return result;
    }

    /**
     * Get the content length for a given URL.
     * @param url the URL to get the length for
     * @return the length in bytes or -1 if not available
     * @throws IOException if an I/O error occurs.
     */
    public static long getContentLength(String url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        urlConnection.setRequestMethod("HEAD");
        urlConnection.setDoInput(false);
        urlConnection.setDoOutput(false);
        urlConnection.connect();
        int responseCode = urlConnection.getResponseCode();
        if(responseCode >= 200 && responseCode <= 299) return urlConnection.getContentLength();
        return -1;
    }

    public interface ParseCallback<T> {
        T process(String input) throws ParseException;
    }
    public static class ParseException extends Exception {
        public ParseException(Exception e) {
            super(e);
        }
    }

    public static class SHA1VerificationException extends IOException {
        public SHA1VerificationException(String message) {
            super(message);
        }
    }
}

