package com.pdftoolbox.app.ui.tools.rotate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pdftoolbox.app.databinding.FragmentRotateBinding

class RotateFragment : Fragment() {

    private var _binding: FragmentRotateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RotateViewModel by viewModels()

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
                val angle = when (binding.rgRotation.checkedRadioButtonId) {
                    binding.rb90.id -> 90
                    binding.rb180.id -> 180
                    binding.rb270.id -> 270
                    else -> 90
                }
                binding.progressBar.visibility = View.VISIBLE
                viewModel.rotatePdf(angle, uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRotateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnRotatePdf.setOnClickListener {
            createOutputFile()
        }

        binding.rgRotation.setOnCheckedChangeListener { _, _ ->
            updateRotateButtonState()
        }

        viewModel.selectedFile.observe(viewLifecycleOwner) { uri ->
            binding.tvSelectedFile.text = uri?.lastPathSegment ?: "No file selected"
            updateRotateButtonState()
        }

        viewModel.rotateState.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(context, "PDF Rotated successfully", Toast.LENGTH_SHORT).show()
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
            putExtra(Intent.EXTRA_TITLE, "rotated_output.pdf")
        }
        createFileLauncher.launch(intent)
    }

    private fun updateRotateButtonState() {
        val hasFile = viewModel.selectedFile.value != null
        val hasRotation = binding.rgRotation.checkedRadioButtonId != -1
        binding.btnRotatePdf.isEnabled = hasFile && hasRotation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
