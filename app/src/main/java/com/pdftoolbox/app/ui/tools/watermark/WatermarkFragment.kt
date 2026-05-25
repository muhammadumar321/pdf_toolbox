package com.pdftoolbox.app.ui.tools.watermark

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
import com.pdftoolbox.app.R
import com.pdftoolbox.app.databinding.FragmentWatermarkBinding

class WatermarkFragment : Fragment() {

    private var _binding: FragmentWatermarkBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WatermarkViewModel by viewModels()

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.selectFile(uri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.selectImage(uri)
            }
        }
    }

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                binding.progressBar.visibility = View.VISIBLE
                if (binding.rbText.isChecked) {
                    val text = binding.etWatermarkText.text.toString()
                    viewModel.applyWatermark(text, uri)
                } else {
                    viewModel.applyImageWatermark(uri)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWatermarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        binding.btnApplyWatermark.setOnClickListener {
            createOutputFile()
        }
        
        binding.etWatermarkText.doAfterTextChanged {
            updateApplyButtonState()
        }

        binding.rgWatermarkType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_text) {
                binding.tilWatermarkText.visibility = View.VISIBLE
                binding.layoutImageSelection.visibility = View.GONE
            } else {
                binding.tilWatermarkText.visibility = View.GONE
                binding.layoutImageSelection.visibility = View.VISIBLE
            }
            updateApplyButtonState()
        }

        viewModel.selectedFile.observe(viewLifecycleOwner) { uri ->
            binding.tvSelectedFile.text = uri?.lastPathSegment ?: "No file selected"
            updateApplyButtonState()
        }

        viewModel.selectedImage.observe(viewLifecycleOwner) { uri ->
            binding.tvSelectedImage.text = uri?.lastPathSegment ?: "No image selected"
            updateApplyButtonState()
        }

        viewModel.watermarkState.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(context, "Watermark applied successfully", Toast.LENGTH_SHORT).show()
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun createOutputFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "watermarked_output.pdf")
        }
        createFileLauncher.launch(intent)
    }
    
    private fun updateApplyButtonState() {
        val hasFile = viewModel.selectedFile.value != null
        val isText = binding.rbText.isChecked
        val hasText = !binding.etWatermarkText.text.isNullOrBlank()
        val hasImage = viewModel.selectedImage.value != null
        
        binding.btnApplyWatermark.isEnabled = hasFile && ((isText && hasText) || (!isText && hasImage))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
