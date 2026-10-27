package com.pdfstudio.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File
import java.io.FileOutputStream

object PdfToImageUtil {

    fun convertFirstPageToJpg(context: Context, pdfUri: Uri): File {
        val tempInput = File(context.cacheDir, "temp_pdf2img_input.pdf")
        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(tempInput).use { output ->
                input.copyTo(output)
            }
        }

        val outputDir = File(context.filesDir, "extracted_images").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "Page_1_${System.currentTimeMillis()}.jpg")

        PDDocument.load(tempInput).use { document ->
            val renderer = PDFRenderer(document)
            val bitmap = renderer.renderImageWithDPI(0, 150f, ImageType.RGB)

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()
        }

        tempInput.delete()
        return outputFile
    }
}
