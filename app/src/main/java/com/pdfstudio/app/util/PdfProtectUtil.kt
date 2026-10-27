package com.pdfstudio.app.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.File
import java.io.FileOutputStream

object PdfProtectUtil {

    fun protectPdfWithPassword(
        context: Context,
        pdfUri: Uri,
        userPassword: String
    ): File {
        val tempInput = File(context.cacheDir, "temp_protect_input.pdf")
        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(tempInput).use { output ->
                input.copyTo(output)
            }
        }

        val outputDir = File(context.filesDir, "protected_pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "Protected_${System.currentTimeMillis()}.pdf")

        PDDocument.load(tempInput).use { document ->
            val accessPermission = AccessPermission()
            val protectionPolicy = StandardProtectionPolicy(
                userPassword,
                userPassword,
                accessPermission
            ).apply {
                encryptionKeyLength = 128
                permissions = accessPermission
            }

            document.protect(protectionPolicy)
            document.save(outputFile)
        }

        tempInput.delete()
        return outputFile
    }
}
