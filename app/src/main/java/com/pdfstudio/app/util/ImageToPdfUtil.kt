package com.pdfstudio.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageToPdfUtil {

    // Standard A4 dimensions at 72 DPI
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun convertImagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        hasWatermark: Boolean
    ): File {
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        for ((index, uri) in imageUris.withIndex()) {
            val bitmap = loadOrientedBitmap(context, uri) ?: continue
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Scale image proportionally to fit page margins
            val margin = 36f
            val maxW = PAGE_WIDTH - (margin * 2)
            val maxH = PAGE_HEIGHT - (margin * 2) - if (hasWatermark) 30f else 0f

            val scale = (maxW / bitmap.width).coerceAtMost(maxH / bitmap.height)
            val scaledW = bitmap.width * scale
            val scaledH = bitmap.height * scale
            val left = (PAGE_WIDTH - scaledW) / 2f
            val top = (PAGE_HEIGHT - scaledH) / 2f

            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(left, top)
            }
            canvas.drawBitmap(bitmap, matrix, paint)

            if (hasWatermark) {
                WatermarkUtil.drawWatermarkOnCanvas(canvas, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat())
            }

            pdfDocument.finishPage(page)
            bitmap.recycle()
        }

        val outputDir = File(context.filesDir, "generated_pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "PDF_Export_${System.currentTimeMillis()}.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }

    private fun loadOrientedBitmap(context: Context, uri: Uri): Bitmap? {
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
            BitmapFactory.decodeStream(input, null, options)
        } catch (e: Exception) {
            null
        } finally {
            input?.close()
        }
    }
}
