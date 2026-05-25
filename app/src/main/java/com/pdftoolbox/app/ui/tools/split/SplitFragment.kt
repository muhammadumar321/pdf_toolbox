package com.pdftoolbox.app.ui.tools.split

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pdftoolbox.app.databinding.FragmentSplitBinding

class SplitFragment : Fragment() {

    private var _binding: FragmentSplitBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SplitViewModel by viewModels()
    private var pendingRange: String = ""

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.selectFile(uri)
            }
        }
    }

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.splitPdf(pendingRange, uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectFile.setOnClickListener {
            pickPdfFile()
        }

        binding.btnSplit.setOnClickListener {
            val range = binding.inputPages.text.toString()
            if (range.isNotBlank()) {
                pendingRange = range
                createOutputFile()
            } else {
                Toast.makeText(context, "Please enter page range", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.selectedFile.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.textSelectedFile.text = uri.lastPathSegment ?: "Selected File"
                binding.btnSplit.isEnabled = true
            } else {
                binding.textSelectedFile.text = "No file selected"
                binding.btnSplit.isEnabled = false
            }
        }

        viewModel.splitState.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Split Successful!", Toast.LENGTH_SHORT).show()
                    binding.inputPages.text = null
                },
                onFailure = {
                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun pickPdfFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        pickFileLauncher.launch(intent)
    }

    private fun createOutputFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "split_output.pdf")
        }
        createDocumentLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
