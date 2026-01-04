package com.example.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utility object for handling image file operations.
 */
object ImageUtil {
    
    /**
     * Copy an image from a content URI to internal storage.
     * 
     * @param context Application context
     * @param uri The content URI of the image to copy
     * @param type The type of image ("banner" or "profile")
     * @return The file path to the copied image, or null if copy failed
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri, type: String): String? {
        return try {
            // Create a unique filename
            val filename = "${type}_${UUID.randomUUID()}.jpg"
            
            // Create directory if it doesn't exist
            val directory = File(context.filesDir, "images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            // Create the destination file
            val destinationFile = File(directory, filename)
            
            // Copy the image
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Return the absolute path
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Delete an image file from internal storage.
     * 
     * @param filePath The absolute path to the file to delete
     * @return true if deletion was successful, false otherwise
     */
    fun deleteImage(filePath: String?): Boolean {
        if (filePath == null) return false
        
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
