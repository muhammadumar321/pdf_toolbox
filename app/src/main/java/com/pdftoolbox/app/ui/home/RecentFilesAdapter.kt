package com.pdftoolbox.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pdftoolbox.app.R
import com.pdftoolbox.app.data.models.RecentFile
import com.pdftoolbox.app.databinding.ItemFileSelectedBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentFilesAdapter(private val onFileClick: (RecentFile) -> Unit) :
    ListAdapter<RecentFile, RecentFilesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFileSelectedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: RecentFile) {
            binding.textFileName.text = file.name
            val date = Date(file.timestamp)
            val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            binding.textFileDetails.text = format.format(date)
            binding.textFileDetails.visibility = android.view.View.VISIBLE
            binding.btnRemove.visibility = android.view.View.GONE
            binding.btnOpen.visibility = android.view.View.VISIBLE
            binding.btnOpen.setOnClickListener {
                onFileClick(file)
            }
            
            binding.root.setOnClickListener {
                onFileClick(file)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RecentFile>() {
        override fun areItemsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
            return oldItem == newItem
        }
    }
}
