package com.pdftoolbox.app.ui.tools.extract

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pdftoolbox.app.databinding.FragmentExtractImagesBinding

class ExtractImagesFragment : Fragment() {

    private var _binding: FragmentExtractImagesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExtractImagesViewModel by viewModels()

    private var selectedFileUri: Uri? = null

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                updateSelectedFileUI(uri)
                binding.btnExtract.isEnabled = true
            }
        }
    }

    private val selectFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri?.let { fileUri ->
                    // Persist permissions for the folder
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                    
                    viewModel.extractImages(fileUri, uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtractImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectFile.setOnClickListener {
            pickPdfFile()
        }

        binding.btnExtract.setOnClickListener {
            pickOutputFolder()
        }

        viewModel.extractionStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is ExtractImagesViewModel.ExtractionStatus.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnExtract.isEnabled = false
                }
                is ExtractImagesViewModel.ExtractionStatus.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnExtract.isEnabled = true
                    Toast.makeText(context, "Extracted ${status.count} images successfully", Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }
                is ExtractImagesViewModel.ExtractionStatus.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnExtract.isEnabled = true
                    Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                }
                is ExtractImagesViewModel.ExtractionStatus.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun pickPdfFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        selectFileLauncher.launch(intent)
    }

    private fun pickOutputFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        selectFolderLauncher.launch(intent)
    }

    private fun updateSelectedFileUI(uri: Uri) {
        val fileName = getFileName(uri)
        binding.tvSelectedFile.text = fileName ?: uri.lastPathSegment
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
