package com.pdftoolbox.app.ui.tools.extract

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pdftoolbox.app.databinding.FragmentExtractTextBinding

class ExtractTextFragment : Fragment() {

    private var _binding: FragmentExtractTextBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExtractTextViewModel by viewModels()

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.selectFile(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExtractTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnExtractText.setOnClickListener {
            viewModel.extractText()
        }

        binding.btnCopyText.setOnClickListener {
            copyToClipboard()
        }

        viewModel.selectedFile.observe(viewLifecycleOwner) { uri ->
            binding.tvSelectedFile.text = uri?.lastPathSegment ?: "No file selected"
            binding.btnExtractText.isEnabled = uri != null
        }

        viewModel.extractedText.observe(viewLifecycleOwner) { text ->
            binding.tvExtractedText.text = text
            binding.btnCopyText.isEnabled = text.isNotEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        pickFileLauncher.launch(intent)
    }

    private fun copyToClipboard() {
        val text = binding.tvExtractedText.text.toString()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Extracted Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
