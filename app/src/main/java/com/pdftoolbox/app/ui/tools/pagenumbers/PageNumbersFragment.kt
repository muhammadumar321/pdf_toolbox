package com.pdftoolbox.app.ui.tools.pagenumbers

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.PdfToolboxApp
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import com.pdftoolbox.app.databinding.FragmentPageNumbersBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PageNumbersViewModel : ViewModel() {
    private val repository = PdfToolsRepository(PdfToolboxApp.instance)
    val selectedFile = MutableStateFlow<Uri?>(null)
    val pageNumbersState = MutableStateFlow<Result<Unit>?>(null)

    fun selectFile(uri: Uri) {
        selectedFile.value = uri
    }

    fun applyPageNumbers(outputUri: Uri) {
        val uri = selectedFile.value ?: return
        viewModelScope.launch {
            val success = repository.addPageNumbers(uri, outputUri)
            if (success) pageNumbersState.value = Result.success(Unit)
            else pageNumbersState.value = Result.failure(Exception("Failed to add page numbers"))
        }
    }
}

class PageNumbersFragment : Fragment() {
    private var _binding: FragmentPageNumbersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PageNumbersViewModel by viewModels()

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { viewModel.selectFile(it) }
        }
    }

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.applyPageNumbers(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPageNumbersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSelectFile.setOnClickListener { openFilePicker() }
        binding.btnAction.setOnClickListener {
            if (viewModel.selectedFile.value == null) {
                Toast.makeText(context, "Select a file", Toast.LENGTH_SHORT).show()
            } else {
                createOutputFile()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedFile.collect { uri ->
                binding.tvSelectedFile.text = uri?.lastPathSegment ?: "No file selected"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pageNumbersState.collect { result ->
                result?.let {
                    binding.progressBar.visibility = View.GONE
                    it.onSuccess { Toast.makeText(context, "Page numbers added successfully", Toast.LENGTH_SHORT).show() }
                    it.onFailure { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
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
            putExtra(Intent.EXTRA_TITLE, "with_page_numbers.pdf")
        }
        createFileLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
