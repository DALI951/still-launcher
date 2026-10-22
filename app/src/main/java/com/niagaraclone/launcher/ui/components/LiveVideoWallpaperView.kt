package com.niagaraclone.launcher.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import android.view.TextureView
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private const val TAG = "LiveVideoWallpaper"

/**
 * Authentic, hardware-accelerated looping video wallpaper player.
 * Plays seamless WebM/MP4 video wallpapers directly from app assets using Android's
 * native TextureView and MediaPlayer pipeline with zero distortion and center-crop scaling.
 */
@Composable
fun LiveVideoWallpaperView(
    videoAssetFileName: String,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0f
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var activeTextureView by remember { mutableStateOf<TextureView?>(null) }

    // Lifecycle observer to pause video when app is backgrounded or screen is off
    DisposableEffect(lifecycleOwner, mediaPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.pause()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error pausing live video: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        mediaPlayer?.start()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error resuming live video: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing live video: ${e.message}")
                    }
                    mediaPlayer = null
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing live video on dispose: ${e.message}")
            }
            mediaPlayer = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    activeTextureView = this
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            initAndStartMediaPlayer(ctx, this@apply, surface, videoAssetFileName) { mp ->
                                mediaPlayer = mp
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            val mp = mediaPlayer ?: return
                            try {
                                val vw = mp.videoWidth
                                val vh = mp.videoHeight
                                if (vw > 0 && vh > 0) {
                                    applyCenterCropTransform(this@apply, vw, vh, width, height)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error on size changed transform: ${e.message}")
                            }
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            try {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                            } catch (e: Exception) {
                                Log.w(TAG, "Error destroying surface player: ${e.message}")
                            }
                            mediaPlayer = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            update = { textureView ->
                activeTextureView = textureView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Customizable darkness scrim on top of the live video
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha.coerceIn(0f, 0.95f)))
            )
        }
    }
}

private fun initAndStartMediaPlayer(
    context: Context,
    textureView: TextureView,
    surfaceTexture: SurfaceTexture,
    fileName: String,
    onPlayerReady: (MediaPlayer) -> Unit
) {
    try {
        val cleanName = if (fileName.startsWith("wallpapers/")) fileName.removePrefix("wallpapers/") else fileName
        val cacheDir = File(context.cacheDir, "wallpapers").apply { mkdirs() }
        val videoFile = File(cacheDir, cleanName)

        if (!videoFile.exists() || videoFile.length() == 0L) {
            context.assets.open("wallpapers/$cleanName").use { input ->
                videoFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val mp = MediaPlayer().apply {
            setDataSource(videoFile.absolutePath)

            setSurface(Surface(surfaceTexture))
            isLooping = true
            setVolume(0f, 0f) // Ambient live wallpaper is silent

            setOnVideoSizeChangedListener { _, width, height ->
                if (width > 0 && height > 0) {
                    applyCenterCropTransform(textureView, width, height, textureView.width, textureView.height)
                }
            }

            setOnPreparedListener { player ->
                player.start()
                if (player.videoWidth > 0 && player.videoHeight > 0) {
                    applyCenterCropTransform(textureView, player.videoWidth, player.videoHeight, textureView.width, textureView.height)
                }
            }

            prepareAsync()
        }

        onPlayerReady(mp)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize MediaPlayer for asset $fileName", e)
    }
}

/**
 * Applies a Center-Crop Matrix transform to TextureView so video fills the screen
 * with authentic aspect ratio and zero black letterboxing or stretching.
 */
private fun applyCenterCropTransform(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int,
    viewWidth: Int,
    viewHeight: Int
) {
    if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return

    val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()

    val scaleX: Float
    val scaleY: Float

    if (videoAspect > viewAspect) {
        // Video is wider than view -> scale height to fill, crop sides
        scaleX = videoAspect / viewAspect
        scaleY = 1.0f
    } else {
        // Video is taller than view -> scale width to fill, crop top/bottom
        scaleX = 1.0f
        scaleY = viewAspect / videoAspect
    }

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}
