package com.pdfstudio.app.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfMergeUtil {

    fun mergePdfFiles(context: Context, pdfUris: List<Uri>): File {
        val outputDir = File(context.filesDir, "merged_pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "Merged_${System.currentTimeMillis()}.pdf")

        val merger = PDFMergerUtility().apply {
            destinationFileName = outputFile.absolutePath
        }

        val tempFiles = mutableListOf<File>()
        try {
            for ((index, uri) in pdfUris.withIndex()) {
                val tempFile = File(context.cacheDir, "temp_merge_${index}.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFiles.add(tempFile)
                merger.addSource(tempFile)
            }

            merger.mergeDocuments(null)
        } finally {
            for (temp in tempFiles) {
                temp.delete()
            }
        }

        return outputFile
    }
}
