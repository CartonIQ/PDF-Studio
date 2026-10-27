package com.pdfstudio.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageCompressUtil {

    fun compressImage(
        context: Context,
        imageUri: Uri,
        qualityPercent: Int = 65
    ): File {
        val outputDir = File(context.filesDir, "compressed_images").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "Compressed_${System.currentTimeMillis()}.jpg")

        // 1. Decode bounds to avoid OutOfMemoryError on high-res camera shots
        var input: InputStream? = context.contentResolver.openInputStream(imageUri)
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(input, null, boundsOptions)
        input?.close()

        val sampleSize = calculateInSampleSize(boundsOptions, 1920, 1920)

        // 2. Decode sampled bitmap
        input = context.contentResolver.openInputStream(imageUri)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val sampledBitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
        input?.close()

        // 3. Compress to JPEG with target quality
        if (sampledBitmap != null) {
            FileOutputStream(outputFile).use { out ->
                sampledBitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent, out)
            }
            sampledBitmap.recycle()
        }

        return outputFile
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
