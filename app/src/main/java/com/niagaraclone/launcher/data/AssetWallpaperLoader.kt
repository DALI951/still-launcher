package com.niagaraclone.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, memory-safe asset wallpaper loader.
 * Loads curated high-resolution photography wallpapers from the app assets/wallpapers/ directory
 * with thread-safe caching and memory-efficient thumbnail decoding.
 */
object AssetWallpaperLoader {
    private const val TAG = "AssetWallpaperLoader"
    private const val ASSETS_WALLPAPERS_DIR = "wallpapers"

    // Thread-safe in-memory bitmap cache
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    private val thumbnailCache = ConcurrentHashMap<String, Bitmap>()

    /**
     * Normalizes the asset path so either "wallpaper_black_desert.jpg"
     * or "wallpapers/wallpaper_black_desert.jpg" works seamlessly.
     */
    private fun resolveAssetPath(fileName: String): String {
        return if (fileName.startsWith("$ASSETS_WALLPAPERS_DIR/")) {
            fileName
        } else {
            "$ASSETS_WALLPAPERS_DIR/$fileName"
        }
    }

    /**
     * Decodes and returns the full-resolution wallpaper bitmap from assets.
     * Results are cached in memory to avoid repeated decodes across recompositions.
     */
    fun loadBitmap(context: Context, fileName: String): Bitmap? {
        val path = resolveAssetPath(fileName)
        bitmapCache[path]?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        return try {
            context.assets.open(path).use { stream: InputStream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    bitmapCache[path] = bitmap
                }
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load asset wallpaper: $path", e)
            null
        }
    }

    /**
     * Decodes a memory-efficient downsampled thumbnail from assets for preview cards.
     * Uses inSampleSize calculation to minimize heap usage.
     */
    fun loadThumbnail(
        context: Context,
        fileName: String,
        reqWidth: Int = 120,
        reqHeight: Int = 160
    ): Bitmap? {
        val path = resolveAssetPath(fileName)
        val cacheKey = "$path:$reqWidth:$reqHeight"

        thumbnailCache[cacheKey]?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        return try {
            // Step 1: Decode image dimensions only
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            // Step 2: Compute optimal inSampleSize
            var inSampleSize = 1
            val outHeight = boundsOptions.outHeight
            val outWidth = boundsOptions.outWidth

            if (outHeight > reqHeight || outWidth > reqWidth) {
                val halfHeight = outHeight / 2
                val halfWidth = outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            // Step 3: Decode downsampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // More memory-efficient for thumbnails
            }

            context.assets.open(path).use { stream ->
                val thumb = BitmapFactory.decodeStream(stream, null, decodeOptions)
                if (thumb != null) {
                    thumbnailCache[cacheKey] = thumb
                }
                thumb
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load asset thumbnail: $path", e)
            null
        }
    }

    /**
     * Clears all cached bitmaps.
     */
    fun clearCache() {
        bitmapCache.clear()
        thumbnailCache.clear()
    }
}
