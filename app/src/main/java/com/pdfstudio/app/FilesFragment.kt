package com.pdfstudio.app

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pdfstudio.app.data.RecentFileEntity
import com.pdfstudio.app.databinding.DialogExportFilesBinding
import com.pdfstudio.app.databinding.FragmentFilesBinding
import com.pdfstudio.app.databinding.ItemFileBinding
import com.pdfstudio.app.util.CatalogExportUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FilesViewModel by viewModels()
    private lateinit var adapter: FilesAdapter
    private var currentFiles: List<RecentFileEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = FilesAdapter { fileEntity ->
            val bundle = Bundle().apply {
                putString("filePath", fileEntity.filePath)
                putString("toolName", fileEntity.actionType)
                putBoolean("hasWatermark", fileEntity.hasWatermark)
            }
            findNavController().navigate(R.id.action_files_to_preview, bundle)
        }

        binding.recyclerFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFiles.adapter = adapter

        viewModel.allFiles.observe(viewLifecycleOwner) { files ->
            currentFiles = files
            if (files.isEmpty()) {
                binding.layoutEmptyFiles.visibility = View.VISIBLE
                binding.recyclerFiles.visibility = View.GONE
                binding.fabExportCatalog.visibility = View.GONE
                binding.btnExportHeader.visibility = View.GONE
            } else {
                binding.layoutEmptyFiles.visibility = View.GONE
                binding.recyclerFiles.visibility = View.VISIBLE
                binding.fabExportCatalog.visibility = View.VISIBLE
                binding.btnExportHeader.visibility = View.VISIBLE
                adapter.submitList(files)
            }
        }

        // Floating Action Button & Header Export Action
        binding.fabExportCatalog.setOnClickListener {
            showExportCatalogDialog()
        }
        binding.btnExportHeader.setOnClickListener {
            showExportCatalogDialog()
        }
    }

    private fun showExportCatalogDialog() {
        if (currentFiles.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.export_empty_msg), Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogExportFilesBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // 1. Export as CSV
        dialogBinding.cardExportCsv.setOnClickListener {
            dialog.dismiss()
            try {
                val csvFile = CatalogExportUtil.exportToCsv(requireContext(), currentFiles)
                CatalogExportUtil.shareExportedFile(requireContext(), csvFile, "text/csv")
                Toast.makeText(requireContext(), getString(R.string.export_success_msg), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "CSV export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Export as PDF Summary Report
        dialogBinding.cardExportPdf.setOnClickListener {
            dialog.dismiss()
            try {
                val pdfReport = CatalogExportUtil.exportToSummaryPdf(requireContext(), currentFiles)
                CatalogExportUtil.shareExportedFile(requireContext(), pdfReport, "application/pdf")
                Toast.makeText(requireContext(), getString(R.string.export_success_msg), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "PDF report failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancelExport.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class FilesAdapter(
        private val onItemClick: (RecentFileEntity) -> Unit
    ) : RecyclerView.Adapter<FilesAdapter.FileViewHolder>() {

        private var items = listOf<RecentFileEntity>()

        fun submitList(newItems: List<RecentFileEntity>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val binding = ItemFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return FileViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            holder.bind(items[position], onItemClick)
        }

        override fun getItemCount(): Int = items.size

        class FileViewHolder(private val itemBinding: ItemFileBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            private val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

            fun bind(file: RecentFileEntity, onItemClick: (RecentFileEntity) -> Unit) {
                itemBinding.tvItemTitle.text = file.fileName
                val dateStr = dateFormat.format(Date(file.timestamp))
                itemBinding.tvItemSubtitle.text = "${file.actionType} • ${file.fileSize} • \$dateStr"
                itemBinding.root.setOnClickListener { onItemClick(file) }
            }
        }
    }
}
