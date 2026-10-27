package com.pdfstudio.app.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream

object PdfSplitUtil {

    fun splitFirstPages(context: Context, pdfUri: Uri, splitInterval: Int = 1): File {
        val tempInput = File(context.cacheDir, "temp_split_input.pdf")
        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(tempInput).use { output ->
                input.copyTo(output)
            }
        }

        val outputDir = File(context.filesDir, "split_pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "Split_Page1_${System.currentTimeMillis()}.pdf")

        PDDocument.load(tempInput).use { document ->
            val splitter = Splitter().apply {
                setSplitAtPage(splitInterval)
            }
            val pages: List<PDDocument> = splitter.split(document)
            if (pages.isNotEmpty()) {
                pages[0].save(outputFile)
                for (doc in pages) {
                    doc.close()
                }
            } else {
                document.save(outputFile)
            }
        }

        tempInput.delete()
        return outputFile
    }
}
