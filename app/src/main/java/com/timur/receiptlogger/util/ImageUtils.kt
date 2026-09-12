package com.timur.receiptlogger.util

import android.content.Context
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
}
