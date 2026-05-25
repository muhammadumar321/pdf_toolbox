package com.pdftoolbox.app.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pdftoolbox.app.R
import com.pdftoolbox.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private val openPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.addRecentFile(uri)
                openPdfViewer(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val adapter = RecentFilesAdapter { recentFile ->
            openPdfViewer(Uri.parse(recentFile.uri))
        }

        binding.rvRecentFiles.layoutManager = LinearLayoutManager(context)
        binding.rvRecentFiles.adapter = adapter

        binding.btnOpenPdf.setOnClickListener {
            pickPdfFile()
        }
        binding.btnGoTools.setOnClickListener {
            findNavController().navigate(R.id.navigation_tools)
        }

        viewModel.recentFiles.observe(viewLifecycleOwner) { files ->
            adapter.submitList(files)
            binding.tvEmptyRecent.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            val latest = files.firstOrNull()
            if (latest == null) {
                binding.tvLastOpened.visibility = View.GONE
            } else {
                binding.tvLastOpened.text = "Last opened: ${latest.name}"
                binding.tvLastOpened.visibility = View.VISIBLE
            }
        }
    }

    private fun pickPdfFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        openPdfLauncher.launch(intent)
    }

    private fun openPdfViewer(uri: Uri) {
        // Navigate to ViewerFragment with the URI
        val action = HomeFragmentDirections.actionHomeToViewer(uri.toString())
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRecentFiles()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
