package com.pdfstudio.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.pdfstudio.app.data.RecentFileEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CatalogExportUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportToCsv(context: Context, files: List<RecentFileEntity>): File {
        val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val csvFile = File(exportDir, "PDFStudio_Catalog_${System.currentTimeMillis()}.csv")

        val sb = StringBuilder()
        sb.append("ID,File Name,Tool / Operation,File Size,Creation Date,Watermark\n")

        for (file in files) {
            val dateStr = dateFormat.format(Date(file.timestamp))
            val cleanName = file.fileName.replace(",", ";")
            val cleanTool = file.actionType.replace(",", ";")
            val watermarkStr = if (file.hasWatermark) "Yes" else "No"
            sb.append("${file.id},\"${cleanName}\",\"${cleanTool}\",${file.fileSize},\"${dateStr}\",${watermarkStr}\n")
        }

        FileOutputStream(csvFile).use { out ->
            out.write(sb.toString().toByteArray())
        }
        return csvFile
    }

    fun exportToSummaryPdf(context: Context, files: List<RecentFileEntity>): File {
        val totalFiles = files.size
        val dateHeader = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())

        val contentBuilder = StringBuilder()
        contentBuilder.append("SUMMARY METRICS:\n")
        contentBuilder.append("Total Documents Generated: \$totalFiles\n")
        contentBuilder.append("Storage Engine: Room SQLite (100% Offline)\n")
        contentBuilder.append("Export Timestamp: \$dateHeader\n\n")
        contentBuilder.append("CATALOG INVENTORY:\n")

        for ((index, file) in files.withIndex()) {
            val dateStr = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(file.timestamp))
            contentBuilder.append("${index + 1}. ${file.fileName}\n")
            contentBuilder.append("   Tool: ${file.actionType} | Size: ${file.fileSize} | Created: ${dateStr}\n\n")
        }

        return PdfCreatorUtil.createCustomPdf(
            context = context,
            title = "PDF Studio - Document Catalog Report",
            content = contentBuilder.toString(),
            includeTimestamp = true,
            hasWatermark = false
        )
    }

    fun shareExportedFile(context: Context, file: File, mimeType: String) {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "com.pdfstudio.app.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Exported Catalog - ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Exported Catalog"))
    }
}
