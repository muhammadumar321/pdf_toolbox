package com.pdftoolbox.app.ui.tools.merge

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pdftoolbox.app.databinding.ItemFileSelectedBinding
import java.util.Collections

class SelectedFilesAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : RecyclerView.Adapter<SelectedFilesAdapter.FileViewHolder>() {

    private val files = mutableListOf<Uri>()

    fun submitList(newFiles: List<Uri>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }

    fun getItems(): List<Uri> {
        return files.toList()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(files, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(files, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class FileViewHolder(private val binding: ItemFileSelectedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            binding.textFileName.text = uri.lastPathSegment ?: "Unknown File"
            binding.btnOpen.visibility = android.view.View.GONE
            binding.btnRemove.visibility = android.view.View.VISIBLE
            binding.btnRemove.setOnClickListener { onRemoveClick(uri) }
        }
    }
}
