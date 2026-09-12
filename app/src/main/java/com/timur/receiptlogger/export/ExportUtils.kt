package com.timur.receiptlogger.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.timur.receiptlogger.data.ReceiptEntry
import com.timur.receiptlogger.data.Trip
import java.io.File

object ExportUtils {

    /** Writes the trip's report to a file under the app's cache dir and returns that file. */
    fun exportTripReport(context: Context, trip: Trip, receipts: List<ReceiptEntry>): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = trip.name
            .ifBlank { "trip" }
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .trim()
            .ifBlank { "trip" }
        val file = File(exportsDir, "$safeName-report.xlsx")
        XlsxReportWriter.writeTripReport(file, trip, receipts)
        return file
    }

    /** Opens the system share sheet so the user can save/send the exported file. */
    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export expense report"))
    }
}
