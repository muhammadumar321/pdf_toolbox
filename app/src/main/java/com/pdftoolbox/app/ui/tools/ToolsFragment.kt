package com.pdftoolbox.app.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.pdftoolbox.app.R
import com.pdftoolbox.app.databinding.FragmentToolsBinding

class ToolsFragment : Fragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!

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
        
        val tools = listOf(
            ToolItem(1, "Merge PDF", "Combine multiple PDFs into one", R.drawable.ic_dashboard, R.drawable.bg_gradient_blue_purple),
            ToolItem(2, "Split PDF", "Extract pages into new files", R.drawable.ic_dashboard, R.drawable.bg_gradient_pink_orange),
            ToolItem(3, "Compress", "Reduce file size maintaining quality", R.drawable.ic_dashboard, R.drawable.bg_gradient_teal_green),
            ToolItem(4, "Convert", "Change format to/from PDF", R.drawable.ic_dashboard, R.drawable.bg_gradient_blue_purple),
            ToolItem(5, "Rotate", "Fix page orientation", R.drawable.ic_dashboard, R.drawable.bg_gradient_pink_orange),
            ToolItem(6, "Watermark", "Add text or image overlay", R.drawable.ic_dashboard, R.drawable.bg_gradient_teal_green),
            ToolItem(7, "Delete Pages", "Remove unwanted pages", R.drawable.ic_dashboard, R.drawable.bg_gradient_blue_purple),
            ToolItem(8, "Extract Text", "Pull text content from PDF", R.drawable.ic_dashboard, R.drawable.bg_gradient_pink_orange),
            ToolItem(9, "Extract Images", "Save all images from PDF", R.drawable.ic_dashboard, R.drawable.bg_gradient_teal_green),
            ToolItem(10, "Edit PDF", "Draw and annotate on your PDF", R.drawable.ic_dashboard, R.drawable.bg_gradient_blue_purple),
            ToolItem(11, "Protect PDF", "Add password protection", R.drawable.ic_dashboard, R.drawable.bg_gradient_pink_orange),
            ToolItem(12, "Unlock PDF", "Remove PDF password", R.drawable.ic_dashboard, R.drawable.bg_gradient_teal_green),
            ToolItem(13, "Page Numbers", "Add numbers to all pages", R.drawable.ic_dashboard, R.drawable.bg_gradient_blue_purple),
            ToolItem(14, "Rearrange", "Change page sequence", R.drawable.ic_dashboard, R.drawable.bg_gradient_pink_orange)
        )

        val adapter = ToolsAdapter(tools) { tool ->
            when (tool.id) {
                1 -> findNavController().navigate(R.id.navigation_merge)
                2 -> findNavController().navigate(R.id.navigation_split)
                3 -> findNavController().navigate(R.id.navigation_compress)
                4 -> findNavController().navigate(R.id.navigation_convert)
                5 -> findNavController().navigate(R.id.navigation_rotate)
                6 -> findNavController().navigate(R.id.navigation_watermark)
                7 -> findNavController().navigate(R.id.navigation_delete_pages)
                8 -> findNavController().navigate(R.id.navigation_extract_text)
                9 -> findNavController().navigate(R.id.navigation_extract_images)
                10 -> findNavController().navigate(R.id.navigation_edit_pdf)
                11 -> findNavController().navigate(R.id.navigation_protect)
                12 -> findNavController().navigate(R.id.navigation_unlock)
                13 -> findNavController().navigate(R.id.navigation_page_numbers)
                14 -> findNavController().navigate(R.id.navigation_rearrange)
                else -> {
                    Toast.makeText(requireContext(), "Clicked: ${tool.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.recyclerTools.layoutManager = LinearLayoutManager(context)
        binding.recyclerTools.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
