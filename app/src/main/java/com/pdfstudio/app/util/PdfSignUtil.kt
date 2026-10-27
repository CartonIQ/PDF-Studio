package com.pdfstudio.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.File
import java.io.FileOutputStream

object PdfSignUtil {

    fun applySignatureToPdf(
        context: Context,
        pdfUri: Uri,
        signatureBitmap: Bitmap
    ): File {
        val tempInput = File(context.cacheDir, "temp_sign_input.pdf")
        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(tempInput).use { output ->
                input.copyTo(output)
            }
        }

        val outputDir = File(context.filesDir, "signed_pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "Signed_${System.currentTimeMillis()}.pdf")

        PDDocument.load(tempInput).use { document ->
            if (document.numberOfPages > 0) {
                // Apply signature to the last page (signature line area)
                val targetPage = document.getPage(document.numberOfPages - 1)
                val pdImage = LosslessFactory.createFromImage(document, signatureBitmap)

                PDPageContentStream(
                    document,
                    targetPage,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { contentStream ->
                    val signWidth = 140f
                    val signHeight = (signWidth / signatureBitmap.width) * signatureBitmap.height
                    val pageWidth = targetPage.mediaBox.width
                    val x = pageWidth - signWidth - 40f
                    val y = 60f

                    contentStream.drawImage(pdImage, x, y, signWidth, signHeight)
                }
            }
            document.save(outputFile)
        }

        tempInput.delete()
        return outputFile
    }
}
