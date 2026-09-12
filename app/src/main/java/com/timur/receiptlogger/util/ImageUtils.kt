package com.timur.receiptlogger.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    /** Creates a new, empty jpg file inside the app's private files/images directory. */
    fun createImageFile(context: Context): File {
        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(imagesDir, "RECEIPT_$timeStamp.jpg")
    }

    /** Wraps a private file in a content:// Uri the system camera app is allowed to write to. */
    fun getUriForFile(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Copies a gallery-picked image (a content:// Uri, only readable for as long as the
     * picker grants temporary access) into a new file in the app's private images directory,
     * byte-for-byte, so it's stored the same way a camera photo is and stays readable later.
     * Returns null if the source couldn't be opened or copied.
     */
    fun copyToImageFile(context: Context, sourceUri: Uri): File? {
        return try {
            val destFile = createImageFile(context)
            val copied = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) destFile else null
        } catch (e: Exception) {
            null
        }
    }
}
