package com.pdfstudio.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfCreatorUtil {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    fun createCustomPdf(
        context: Context,
        title: String,
        content: String,
        includeTimestamp: Boolean,
        hasWatermark: Boolean
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Clean background
        canvas.drawColor(Color.WHITE)

        var currentY = MARGIN + 24f

        // Document Title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val safeTitle = if (title.isBlank()) "Untitled Document" else title
        canvas.drawText(safeTitle, MARGIN, currentY, titlePaint)
        currentY += 28f

        // Timestamp
        if (includeTimestamp) {
            val datePaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 10f
                isAntiAlias = true
            }
            val dateStr = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Created: $dateStr", MARGIN, currentY, datePaint)
            currentY += 16f
        }

        // Horizontal accent line
        val linePaint = Paint().apply {
            color = Color.parseColor("#00BCD4")
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
        currentY += 24f

        // Content Paint & Wrapping
        val contentPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val maxWidth = PAGE_WIDTH - (MARGIN * 2)
        val paragraphs = content.split("\n")

        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                currentY += 14f
                continue
            }

            val words = paragraph.split(" ")
            var line = ""

            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                val width = contentPaint.measureText(testLine)
                if (width <= maxWidth) {
                    line = testLine
                } else {
                    canvas.drawText(line, MARGIN, currentY, contentPaint)
                    currentY += 18f
                    line = word
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, MARGIN, currentY, contentPaint)
                currentY += 22f
            }

            if (currentY > PAGE_HEIGHT - 60f) {
                break
            }
        }

        if (hasWatermark) {
            WatermarkUtil.drawWatermarkOnCanvas(canvas, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat())
        }

        pdfDocument.finishPage(page)

        val outputDir = File(context.filesDir, "created_pdfs").apply { if (!exists()) mkdirs() }
        val sanitized = safeTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val outputFile = File(outputDir, "${sanitized}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }
}
