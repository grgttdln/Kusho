package com.example.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Manages image storage for Word Bank words.
 *
 * Images are stored in the app's internal storage under a dedicated "word_images" directory.
 * This ensures images are private to the app and are automatically cleaned up when the app is uninstalled.
 */
class ImageStorageManager(private val context: Context) {

    companion object {
        private const val IMAGES_DIR = "word_images"
        private const val MAX_IMAGE_SIZE = 1024 // Max width/height in pixels
        private const val JPEG_QUALITY = 85

        // Supported image MIME types
        val SUPPORTED_MIME_TYPES = listOf(
            "image/jpeg",
            "image/png",
            "image/webp"
        )
    }

    /**
     * Result class for image save operations.
     */
    sealed class SaveResult {
        data class Success(val imagePath: String) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }

    /**
     * Get the directory for storing word images.
     * Creates the directory if it doesn't exist.
     */
    private fun getImagesDirectory(): File {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Save an image from a URI to internal storage.
     *
     * The image is:
     * 1. Loaded from the URI
     * 2. Resized if too large (to save storage space)
     * 3. Compressed as JPEG
     * 4. Saved with a unique filename
     *
     * @param uri The source image URI (from gallery or camera)
     * @return SaveResult indicating success with the file path, or an error message
     */
    suspend fun saveImageFromUri(uri: Uri): SaveResult = withContext(Dispatchers.IO) {
        try {
            // Open input stream from URI
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext SaveResult.Error("Unable to open image")

            // Decode bitmap with options to get dimensions first
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate sample size for downsampling large images
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, MAX_IMAGE_SIZE)

            // Reopen stream and decode with sample size
            val inputStream2 = context.contentResolver.openInputStream(uri)
                ?: return@withContext SaveResult.Error("Unable to open image")

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()

            if (bitmap == null) {
                return@withContext SaveResult.Error("Unable to decode image")
            }

            // Resize if still too large
            val resizedBitmap = resizeBitmapIfNeeded(bitmap, MAX_IMAGE_SIZE)

            // Generate unique filename
            val filename = "word_${UUID.randomUUID()}.jpg"
            val imageFile = File(getImagesDirectory(), filename)

            // Save to file
            FileOutputStream(imageFile).use { outputStream ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            }

            // Clean up bitmaps
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            bitmap.recycle()

            SaveResult.Success(imageFile.absolutePath)
        } catch (e: IOException) {
            SaveResult.Error("Failed to save image: ${e.message}")
        } catch (e: SecurityException) {
            SaveResult.Error("Permission denied to access image")
        } catch (e: OutOfMemoryError) {
            SaveResult.Error("Image is too large to process")
        } catch (e: Exception) {
            SaveResult.Error("An unexpected error occurred: ${e.message}")
        }
    }

    /**
     * Delete an image file from storage.
     *
     * @param imagePath The absolute path to the image file
     * @return true if the file was deleted successfully, false otherwise
     */
    suspend fun deleteImage(imagePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(imagePath)
            if (file.exists() && file.parentFile?.name == IMAGES_DIR) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if an image file exists.
     *
     * @param imagePath The absolute path to the image file
     * @return true if the file exists, false otherwise
     */
    fun imageExists(imagePath: String?): Boolean {
        if (imagePath.isNullOrBlank()) return false
        return File(imagePath).exists()
    }

    /**
     * Validate if the URI points to a supported image type.
     *
     * @param uri The URI to validate
     * @return true if the URI is a supported image type, false otherwise
     */
    fun isValidImageUri(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType in SUPPORTED_MIME_TYPES
    }

    /**
     * Calculate the sample size for downsampling large images.
     */
    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        if (width > maxSize || height > maxSize) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / sampleSize) >= maxSize && (halfHeight / sampleSize) >= maxSize) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    /**
     * Resize a bitmap if it exceeds the maximum dimensions.
     */
    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Clean up orphaned images that are no longer referenced by any word.
     * This should be called periodically or when the app starts.
     *
     * @param referencedPaths List of image paths that are still in use
     */
    suspend fun cleanupOrphanedImages(referencedPaths: Set<String>) = withContext(Dispatchers.IO) {
        try {
            val imagesDir = getImagesDirectory()
            imagesDir.listFiles()?.forEach { file ->
                if (file.absolutePath !in referencedPaths) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Silently fail - cleanup is not critical
        }
    }
}

