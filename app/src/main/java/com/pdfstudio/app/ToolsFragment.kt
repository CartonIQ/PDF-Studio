package com.pdfstudio.app

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pdfstudio.app.data.RecentFileEntity
import com.pdfstudio.app.databinding.DialogCreatePdfBinding
import com.pdfstudio.app.databinding.DialogPasswordBinding
import com.pdfstudio.app.databinding.DialogSignatureBinding
import com.pdfstudio.app.databinding.FragmentToolsBinding
import com.pdfstudio.app.util.ImageCompressUtil
import com.pdfstudio.app.util.ImageToPdfUtil
import com.pdfstudio.app.util.PdfCompressUtil
import com.pdfstudio.app.util.PdfCreatorUtil
import com.pdfstudio.app.util.PdfMergeUtil
import com.pdfstudio.app.util.PdfProtectUtil
import com.pdfstudio.app.util.PdfSignUtil
import com.pdfstudio.app.util.PdfSplitUtil
import com.pdfstudio.app.util.PdfToImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ToolsFragment : Fragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FilesViewModel by viewModels()

    private var pendingTool: String? = null

    // Multi-Image picker for Image to PDF
    private val pickMultipleImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            executeImageToPdf(uris)
        }
    }

    // Single/Multi PDF picker for Merge
    private val pickMultiplePdfs = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.size >= 2) {
            executeMergePdfs(uris)
        } else if (uris.isNotEmpty()) {
            Toast.makeText(requireContext(), "Please select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
        }
    }

    // Single PDF picker for Split, Sign, Protect, PDF to Image, Compress PDF
    private val pickSinglePdf = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSinglePdfSelected(it) }
    }

    // Single image picker for Compress Image
    private val pickSingleImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { executeCompressImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBentoClickListeners()
    }

    private fun setupBentoClickListeners() {
        // 1. Image to PDF
        binding.cardImageToPdf.setOnClickListener {
            vibrateFeedback()
            pickMultipleImages.launch("image/*")
        }

        // 2. PDF Creator (Create rich formatted documents)
        binding.cardPdfCreator.setOnClickListener {
            vibrateFeedback()
            showCreatePdfDialog()
        }

        // 3. Merge PDFs
        binding.cardMergePdf.setOnClickListener {
            vibrateFeedback()
            pickMultiplePdfs.launch("application/pdf")
        }

        // 4. Split PDF
        binding.cardSplitPdf.setOnClickListener {
            vibrateFeedback()
            pendingTool = "SPLIT"
            pickSinglePdf.launch("application/pdf")
        }

        // 5. Compress Image
        binding.cardCompressImage.setOnClickListener {
            vibrateFeedback()
            pickSingleImage.launch("image/*")
        }

        // 6. Sign PDF
        binding.cardSignPdf.setOnClickListener {
            vibrateFeedback()
            pendingTool = "SIGN"
            pickSinglePdf.launch("application/pdf")
        }

        // 7. Protect PDF
        binding.cardProtectPdf.setOnClickListener {
            vibrateFeedback()
            pendingTool = "PROTECT"
            pickSinglePdf.launch("application/pdf")
        }

        // 8. PDF to Image
        binding.cardPdfToImage.setOnClickListener {
            vibrateFeedback()
            pendingTool = "PDF_TO_IMAGE"
            pickSinglePdf.launch("application/pdf")
        }

        // 9. Compress PDF
        binding.cardCompressPdf.setOnClickListener {
            vibrateFeedback()
            pendingTool = "COMPRESS_PDF"
            pickSinglePdf.launch("application/pdf")
        }
    }

    private fun handleSinglePdfSelected(uri: Uri) {
        when (pendingTool) {
            "SPLIT" -> executeSplitPdf(uri)
            "SIGN" -> showSignatureDialog(uri)
            "PROTECT" -> showPasswordDialog(uri)
            "PDF_TO_IMAGE" -> executePdfToImage(uri)
            "COMPRESS_PDF" -> executeCompressPdf(uri)
        }
        pendingTool = null
    }

    private fun showCreatePdfDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogCreatePdfBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.btnCancelCreate.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnGeneratePdf.setOnClickListener {
            val title = dialogBinding.etDocTitle.text?.toString()?.trim() ?: ""
            val content = dialogBinding.etDocContent.text?.toString()?.trim() ?: ""
            val addTimestamp = dialogBinding.cbAddTimestamp.isChecked

            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter document content", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            executeCreatePdf(title, content, addTimestamp)
        }

        dialog.show()
    }

    private fun executeCreatePdf(title: String, content: String, addTimestamp: Boolean) {
        showLoading("Creating and formatting PDF document...")
        lifecycleScope.launch(Dispatchers.IO) {
            val isWatermarkEnabled = isWatermarkDefaultEnabled()
            val outputFile = PdfCreatorUtil.createCustomPdf(
                requireContext(),
                title,
                content,
                addTimestamp,
                isWatermarkEnabled
            )
            onOperationSuccess("PDF Creator", outputFile, isWatermarkEnabled)
        }
    }

    private fun showSignatureDialog(pdfUri: Uri) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogSignatureBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.btnClearSignature.setOnClickListener {
            dialogBinding.signaturePad.clear()
        }

        dialogBinding.btnApplySignature.setOnClickListener {
            val signatureBitmap = dialogBinding.signaturePad.getSignatureBitmap()
            if (signatureBitmap == null) {
                Toast.makeText(requireContext(), "Please draw signature first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            executeSignPdf(pdfUri, signatureBitmap)
        }

        dialog.show()
    }

    private fun showPasswordDialog(pdfUri: Uri) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogPasswordBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.btnCancelPassword.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmPassword.setOnClickListener {
            val password = dialogBinding.etPassword.text?.toString()?.trim() ?: ""
            if (password.length < 4) {
                Toast.makeText(requireContext(), "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            executeProtectPdf(pdfUri, password)
        }

        dialog.show()
    }

    // Tool 1: Image to PDF
    private fun executeImageToPdf(uris: List<Uri>) {
        showLoading("Converting images to PDF...")
        lifecycleScope.launch(Dispatchers.IO) {
            val isWatermarkEnabled = isWatermarkDefaultEnabled()
            val outputFile = ImageToPdfUtil.convertImagesToPdf(
                requireContext(),
                uris,
                isWatermarkEnabled
            )
            onOperationSuccess("Image to PDF", outputFile, isWatermarkEnabled)
        }
    }

    // Tool 2: Merge PDFs
    private fun executeMergePdfs(uris: List<Uri>) {
        showLoading("Merging documents...")
        lifecycleScope.launch(Dispatchers.IO) {
            val outputFile = PdfMergeUtil.mergePdfFiles(requireContext(), uris)
            onOperationSuccess("Merge PDFs", outputFile, false)
        }
    }

    // Tool 3: Split PDF
    private fun executeSplitPdf(uri: Uri) {
        showLoading("Splitting PDF document...")
        lifecycleScope.launch(Dispatchers.IO) {
            val outputFile = PdfSplitUtil.splitFirstPages(requireContext(), uri, 1)
            onOperationSuccess("Split PDF", outputFile, false)
        }
    }

    // Tool 4: Compress Image
    private fun executeCompressImage(uri: Uri) {
        showLoading("Compressing image size...")
        lifecycleScope.launch(Dispatchers.IO) {
            val outputFile = ImageCompressUtil.compressImage(requireContext(), uri, 65)
            onOperationSuccess("Compress Image", outputFile, false)
        }
    }

    // Tool 5: Sign PDF
    private fun executeSignPdf(uri: Uri, signature: Bitmap) {
        showLoading("Applying signature to PDF...")
        lifecycleScope.launch(Dispatchers.IO) {
            val outputFile = PdfSignUtil.applySignatureToPdf(requireContext(), uri, signature)
            onOperationSuccess("Sign PDF", outputFile, false)
        }
    }

    // Tool 6: Protect PDF
    private fun executeProtectPdf(uri: Uri, password: String) {
        showLoading("Encrypting PDF with password...")
        lifecycleScope.launch(Dispatchers.IO) {
            val outputFile = PdfProtectUtil.protectPdfWithPassword(requireContext(), uri, password)
            onOperationSuccess("Protect PDF", outputFile, false)
        }
    }

    // Tool 7: PDF to Image
    private fun executePdfToImage(uri: Uri) {
        showLoading("Extracting pages as JPG images...")
        lifecycleScope.launch(Dispatchers.IO) {
            val outputFile = PdfToImageUtil.convertFirstPageToJpg(requireContext(), uri)
            onOperationSuccess("PDF to Image", outputFile, false)
        }
    }

    // Tool 8: Compress PDF
    private fun executeCompressPdf(uri: Uri) {
        showLoading("Optimizing and compressing PDF...")
        lifecycleScope.launch(Dispatchers.IO) {
            val outputFile = PdfCompressUtil.compressPdfDocument(requireContext(), uri)
            onOperationSuccess("Compress PDF", outputFile, false)
        }
    }

    private suspend fun onOperationSuccess(
        toolName: String,
        outputFile: File,
        hasWatermark: Boolean
    ) {
        // Save action and file to Room DB
        val entity = RecentFileEntity(
            fileName = outputFile.name,
            filePath = outputFile.absolutePath,
            fileSize = formatFileSize(outputFile.length()),
            actionType = toolName,
            timestamp = System.currentTimeMillis(),
            hasWatermark = hasWatermark
        )
        viewModel.insertFile(entity)

        withContext(Dispatchers.Main) {
            hideLoading()
            val bundle = Bundle().apply {
                putString("filePath", outputFile.absolutePath)
                putString("toolName", toolName)
                putBoolean("hasWatermark", hasWatermark)
            }
            findNavController().navigate(R.id.action_tools_to_preview, bundle)
        }
    }

    private fun showLoading(message: String) {
        binding.tvProgressText.text = message
        binding.layoutProgressOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.layoutProgressOverlay.visibility = View.GONE
    }

    private fun isWatermarkDefaultEnabled(): Boolean {
        val prefs = requireContext().getSharedPreferences("pdf_studio_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("watermark_enabled", true)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }

    private fun vibrateFeedback() {
        view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
