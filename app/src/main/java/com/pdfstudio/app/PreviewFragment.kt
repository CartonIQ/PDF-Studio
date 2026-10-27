package com.pdfstudio.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pdfstudio.app.ads.AdManager
import com.pdfstudio.app.databinding.FragmentPreviewBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FilesViewModel by viewModels()

    private var filePath: String = ""
    private var toolName: String = ""
    private var hasWatermark: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filePath = it.getString("filePath", "")
            toolName = it.getString("toolName", "")
            hasWatermark = it.getBoolean("hasWatermark", true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val file = File(filePath)
        binding.tvFilename.text = file.name
        binding.tvFilesize.text = "${formatFileSize(file.length())} • 100% Offline"
        updateWatermarkBadge()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            saveFileToPublicStorage(file)
        }

        binding.btnShare.setOnClickListener {
            shareFile(file)
        }

        binding.btnRemoveWatermark.setOnClickListener {
            triggerRemoveWatermarkFlow()
        }

        // Initialize and load test banner ad
        activity?.let { act ->
            AdManager.loadBanner(act, binding.bannerAdContainer)
            AdManager.loadRewardedAd(act)
        }
    }

    private fun updateWatermarkBadge() {
        if (hasWatermark) {
            binding.chipWatermarkStatus.text = "Watermark: Active"
            binding.chipWatermarkStatus.setTextColor(resources.getColor(R.color.electric_cyan, null))
            binding.btnRemoveWatermark.visibility = View.VISIBLE
        } else {
            binding.chipWatermarkStatus.text = "Watermark: Removed"
            binding.chipWatermarkStatus.setTextColor(resources.getColor(R.color.neon_green, null))
            binding.btnRemoveWatermark.visibility = View.GONE
        }
    }

    private fun triggerRemoveWatermarkFlow() {
        activity?.let { act ->
            AdManager.showRewardedAd(act) {
                // User completed watching test ad!
                hasWatermark = false
                updateWatermarkBadge()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.watermark_removed_msg),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun shareFile(file: File) {
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "com.pdfstudio.app.fileprovider",
                file
            )

            val mimeType = if (file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true)) {
                "image/jpeg"
            } else {
                "application/pdf"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFileToPublicStorage(file: File) {
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mimeType = if (file.name.endsWith(".jpg", true)) "image/jpeg" else "application/pdf"
                val collection = if (mimeType.startsWith("image")) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PDFStudio")
                }

                val uri = requireContext().contentResolver.insert(collection, values)
                if (uri != null) {
                    requireContext().contentResolver.openOutputStream(uri).use { out ->
                        FileInputStream(file).use { input ->
                            input.copyTo(out!!)
                        }
                    }
                    Toast.makeText(requireContext(), getString(R.string.file_saved_msg), Toast.LENGTH_SHORT).show()
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "PDFStudio")
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(targetFile).use { out ->
                        input.copyTo(out)
                    }
                }
                Toast.makeText(requireContext(), getString(R.string.file_saved_msg), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Save error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
