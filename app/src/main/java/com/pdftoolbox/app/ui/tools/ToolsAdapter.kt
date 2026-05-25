package com.pdftoolbox.app.ui.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pdftoolbox.app.databinding.ItemToolBinding

data class ToolItem(val id: Int, val name: String, val desc: String, val iconRes: Int, val bgRes: Int)

class ToolsAdapter(
    private val tools: List<ToolItem>,
    private val onToolClick: (ToolItem) -> Unit
) : RecyclerView.Adapter<ToolsAdapter.ToolViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        holder.bind(tools[position])
    }

    override fun getItemCount(): Int = tools.size

    inner class ToolViewHolder(private val binding: ItemToolBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tool: ToolItem) {
            binding.textToolName.text = tool.name
            binding.textToolDesc.text = tool.desc
            binding.imgToolIcon.setImageResource(tool.iconRes)
            binding.iconContainer.setBackgroundResource(tool.bgRes)
            binding.root.setOnClickListener { onToolClick(tool) }
        }
    }
}
