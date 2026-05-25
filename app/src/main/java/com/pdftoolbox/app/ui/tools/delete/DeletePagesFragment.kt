package com.pdftoolbox.app.ui.tools.delete

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pdftoolbox.app.databinding.FragmentDeletePagesBinding

class DeletePagesFragment : Fragment() {

    private var _binding: FragmentDeletePagesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DeletePagesViewModel by viewModels()

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.selectFile(uri)
            }
        }
    }

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val pageRange = binding.etPageRange.text.toString()
                binding.progressBar.visibility = View.VISIBLE
                viewModel.deletePages(pageRange, uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDeletePagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnDeletePages.setOnClickListener {
            createOutputFile()
        }
        
        binding.etPageRange.doAfterTextChanged {
            updateDeleteButtonState()
        }

        viewModel.selectedFile.observe(viewLifecycleOwner) { uri ->
            binding.tvSelectedFile.text = uri?.lastPathSegment ?: "No file selected"
            updateDeleteButtonState()
        }

        viewModel.deleteState.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(context, "Pages deleted successfully", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFilePicker() {
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
            putExtra(Intent.EXTRA_TITLE, "output_deleted_pages.pdf")
        }
        createFileLauncher.launch(intent)
    }
    
    private fun updateDeleteButtonState() {
        val hasFile = viewModel.selectedFile.value != null
        val hasText = !binding.etPageRange.text.isNullOrBlank()
        binding.btnDeletePages.isEnabled = hasFile && hasText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
