package com.pdftoolbox.app.ui.tools.convert

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
import com.pdftoolbox.app.databinding.FragmentConvertBinding

class ConvertFragment : Fragment() {

    private var _binding: FragmentConvertBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ConvertViewModel by viewModels()

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<android.net.Uri>()
            result.data?.let { data ->
                if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        uris.add(data.clipData!!.getItemAt(i).uri)
                    }
                } else {
                    data.data?.let { uris.add(it) }
                }
            }
            if (uris.isNotEmpty()) {
                viewModel.addImages(uris)
            }
        }
    }

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                binding.progressBar.visibility = View.VISIBLE
                viewModel.convertToPdf(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConvertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectImages.setOnClickListener {
            openImagePicker()
        }

        binding.btnConvert.setOnClickListener {
            createOutputFile()
        }

        viewModel.selectedImages.observe(viewLifecycleOwner) { images ->
            binding.tvSelectedCount.text = "${images.size} images selected"
            binding.btnConvert.isEnabled = images.isNotEmpty()
        }

        viewModel.convertState.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(context, "Images converted successfully", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImagesLauncher.launch(intent)
    }

    private fun createOutputFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "converted_images.pdf")
        }
        createFileLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
