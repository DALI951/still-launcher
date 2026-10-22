package com.niagaraclone.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ExtractedPalette(
    val imagePath: String,
    val accentHex: Long,
    val accentLightHex: Long
)

object ColorExtractor {

    suspend fun extractAndSavePalette(context: Context, imageUri: Uri): ExtractedPalette? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val fullBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (fullBitmap == null) return@withContext null

            // Save full wallpaper file to app files dir
            val destFile = File(context.filesDir, "custom_wallpaper.jpg")
            FileOutputStream(destFile).use { out ->
                fullBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Downsample bitmap for fast color analysis
            val sampleSize = 100
            val scaled = Bitmap.createScaledBitmap(fullBitmap, sampleSize, sampleSize, true)

            val width = scaled.width
            val height = scaled.height
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)

            var bestAccentColor = 0xFF4E8CFF.toInt()
            var highestVibrancy = -1f

            val hsl = FloatArray(3)
            var totalLuminance = 0f
            var pixelCount = 0

            for (pixel in pixels) {
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 128) continue

                ColorUtils.colorToHSL(pixel, hsl)
                val s = hsl[1] // 0..1
                val l = hsl[2] // 0..1

                totalLuminance += l
                pixelCount++

                // Vibrancy metric: reward good saturation and pleasant mid-luminance
                if (s > 0.15f && l in 0.25f..0.85f) {
                    val vibrancy = s * (1f - abs(l - 0.55f))
                    if (vibrancy > highestVibrancy) {
                        highestVibrancy = vibrancy
                        bestAccentColor = pixel
                    }
                }
            }

            // If the image is largely monochrome / low saturation
            if (highestVibrancy < 0.1f) {
                val avgLum = if (pixelCount > 0) totalLuminance / pixelCount else 0.5f
                bestAccentColor = if (avgLum < 0.5f) {
                    0xFFECEFF1.toInt() // Bright silver for dark images
                } else {
                    0xFF37474F.toInt() // Dark charcoal for bright images
                }
            }

            // Derive accent light hex for high legibility
            ColorUtils.colorToHSL(bestAccentColor, hsl)
            val accentHex = (0xFF000000 or (bestAccentColor.toLong() and 0xFFFFFFL))

            // Light accent: increase lightness towards 0.85f and moderate saturation
            hsl[2] = min(0.85f, max(0.70f, hsl[2] + 0.25f))
            hsl[1] = min(hsl[1], 0.75f)
            val lightColorInt = ColorUtils.HSLToColor(hsl)
            val accentLightHex = (0xFF000000 or (lightColorInt.toLong() and 0xFFFFFFL))

            ExtractedPalette(
                imagePath = destFile.absolutePath,
                accentHex = accentHex,
                accentLightHex = accentLightHex
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
