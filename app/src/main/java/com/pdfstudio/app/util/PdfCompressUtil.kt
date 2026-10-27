package com.pdfstudio.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.FileOutputStream

object PdfCompressUtil {

    fun compressPdfDocument(context: Context, pdfUri: Uri): File {
        val tempInput = File(context.cacheDir, "temp_compress_input.pdf")
        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(tempInput).use { output ->
                input.copyTo(output)
            }
        }

        val outputDir = File(context.filesDir, "compressed_pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "Compressed_${System.currentTimeMillis()}.pdf")

        PDDocument.load(tempInput).use { document ->
            // Optimize image streams across all pages
            for (page in document.pages) {
                val resources = page.resources
                for (name in resources.xObjectNames) {
                    val xobject = resources.getXObject(name)
                    if (xobject is PDImageXObject) {
                        try {
                            val bitmap = xobject.image
                            val compressedImage = JPEGFactory.createFromImage(document, bitmap, 0.60f)
                            resources.put(name, compressedImage)
                        } catch (_: Exception) {
                            // If re-encoding fails on special color space, keep original stream
                        }
                    }
                }
            }
            document.save(outputFile)
        }

        tempInput.delete()
        return outputFile
    }
}
