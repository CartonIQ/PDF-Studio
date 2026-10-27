package com.pdfstudio.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File

object WatermarkUtil {

    private const val WATERMARK_TEXT = "Created with PDF Studio • 100% Offline"

    fun drawWatermarkOnCanvas(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.argb(120, 160, 170, 191) // 47% translucent cool grey
            textSize = 14f
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val bounds = Rect()
        paint.getTextBounds(WATERMARK_TEXT, 0, WATERMARK_TEXT.length, bounds)
        val x = (width - bounds.width()) / 2f
        val y = height - 24f

        canvas.drawText(WATERMARK_TEXT, x, y, paint)
    }

    fun applyWatermarkToPdf(inputFile: File, outputFile: File) {
        PDDocument.load(inputFile).use { document ->
            for (page in document.pages) {
                PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { cs ->
                    cs.beginText()
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12f)
                    cs.setNonStrokingColor(160, 170, 191)
                    val pageWidth = page.mediaBox.width
                    val textWidth = (PDType1Font.HELVETICA_BOLD.getStringWidth(WATERMARK_TEXT) / 1000f) * 12f
                    val x = (pageWidth - textWidth) / 2f
                    cs.newLineAtOffset(x, 24f)
                    cs.showText(WATERMARK_TEXT)
                    cs.endText()
                }
            }
            document.save(outputFile)
        }
    }
}
