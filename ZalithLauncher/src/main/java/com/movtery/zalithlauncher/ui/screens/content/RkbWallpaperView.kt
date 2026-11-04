/*
 * RKB LAUNCHER - Home wallpaper renderer: still image, animated GIF/WebP (Android 9+) or looping muted video.
 */
package com.movtery.zalithlauncher.ui.screens.content

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.graphics.drawable.AnimatedImageDrawable
import android.media.MediaPlayer
import android.os.Build
import android.view.Surface
import android.view.View
import android.view.TextureView
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import java.io.File

private val VIDEO_EXT = setOf("mp4", "webm", "mkv", "3gp", "mov")
private val ANIMATED_EXT = setOf("gif", "webp")

@Composable
fun RkbWallpaperView(file: File, modifier: Modifier = Modifier) {
    val ext = file.extension.lowercase()
    when {
        ext in VIDEO_EXT -> AndroidView(
            modifier = modifier,
            factory = { ctx -> VideoWallpaperView(ctx).also { it.path = file.absolutePath } },
            update = { v -> if (v.path != file.absolutePath) { v.path = file.absolutePath; v.restart() } }
        )
        ext in ANIMATED_EXT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> AndroidView(
            modifier = modifier,
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    runCatching {
                        val d = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file))
                        setImageDrawable(d)
                        (d as? AnimatedImageDrawable)?.apply { repeatCount = AnimatedImageDrawable.REPEAT_INFINITE; start() }
                    }
                }
            }
        )
        else -> AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

/** TextureView + MediaPlayer, scaled to fill (center-crop), muted and looping. */
private class VideoWallpaperView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {
    var path: String? = null
    private var player: MediaPlayer? = null
    private var surface: Surface? = null

    init {
        surfaceTextureListener = this
        isOpaque = false
    }

    fun restart() {
        surface?.let { start(it) }
    }

    private fun start(target: Surface) {
        release()
        val p = path ?: return
        runCatching {
            val mp = MediaPlayer()
            mp.setSurface(target)
            mp.setDataSource(p)
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            mp.setOnVideoSizeChangedListener { _, vw, vh -> applyCenterCrop(vw, vh) }
            mp.setOnPreparedListener { it.start() }
            mp.setOnErrorListener { _, _, _ -> true }
            mp.prepareAsync()
            player = mp
        }
    }

    private fun applyCenterCrop(vw: Int, vh: Int) {
        if (vw <= 0 || vh <= 0 || width <= 0 || height <= 0) return
        val scale = maxOf(width / vw.toFloat(), height / vh.toFloat())
        val m = Matrix()
        m.setScale(vw * scale / width, vh * scale / height, width / 2f, height / 2f)
        setTransform(m)
    }

    private fun release() {
        runCatching { player?.release() }
        player = null
    }

    /** Free the decoder as soon as the launcher is not visible (e.g. while the game runs) so it never competes for CPU/GPU. */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility != View.VISIBLE) {
            release()
        } else if (player == null) {
            surface?.let { start(it) }
        }
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
        val s = Surface(st)
        surface = s
        start(s)
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        release()
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
}
