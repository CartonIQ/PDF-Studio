package com.pdfstudio.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pdfstudio.app.data.RecentFileEntity
import com.pdfstudio.app.databinding.FragmentHistoryBinding
import com.pdfstudio.app.databinding.ItemFileBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FilesViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = HistoryAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        viewModel.allHistory.observe(viewLifecycleOwner) { historyList ->
            if (historyList.isEmpty()) {
                binding.layoutEmptyHistory.visibility = View.VISIBLE
                binding.recyclerHistory.visibility = View.GONE
            } else {
                binding.layoutEmptyHistory.visibility = View.GONE
                binding.recyclerHistory.visibility = View.VISIBLE
                adapter.submitList(historyList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        private var items = listOf<RecentFileEntity>()

        fun submitList(newItems: List<RecentFileEntity>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val binding = ItemFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return HistoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class HistoryViewHolder(private val itemBinding: ItemFileBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            private val dateFormat = SimpleDateFormat("EEE, MMM dd • hh:mm:ss a", Locale.getDefault())

            fun bind(file: RecentFileEntity) {
                itemBinding.tvItemTitle.text = file.actionType
                val dateStr = dateFormat.format(Date(file.timestamp))
                itemBinding.tvItemSubtitle.text = "Target: ${file.fileName} (${file.fileSize})\nExecuted: \$dateStr"
            }
        }
    }
}
