package com.pdftoolbox.app.ui.tools.merge

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.pdftoolbox.app.databinding.FragmentMergeBinding

class MergeFragment : Fragment() {

    private var _binding: FragmentMergeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MergeViewModel by viewModels()
    private lateinit var adapter: SelectedFilesAdapter

    private val pickFilesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val uris = mutableListOf<Uri>()
                if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        uris.add(data.clipData!!.getItemAt(i).uri)
                    }
                } else {
                    data.data?.let { uris.add(it) }
                }
                viewModel.addFiles(uris)
            }
        }
    }

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.mergeFiles(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMergeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SelectedFilesAdapter { uri ->
            viewModel.removeFile(uri)
        }
        
        binding.recyclerFiles.layoutManager = LinearLayoutManager(context)
        binding.recyclerFiles.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                adapter.onItemMove(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe to dismiss
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewModel.setFiles(adapter.getItems())
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerFiles)

        binding.btnAddFiles.setOnClickListener {
            pickPdfFiles()
        }

        binding.btnMerge.setOnClickListener {
            createOutputFile()
        }

        viewModel.selectedFiles.observe(viewLifecycleOwner) { files ->
            adapter.submitList(files.toList())
            binding.btnMerge.isEnabled = files.size >= 2
        }

        viewModel.mergeState.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Merge Successful!", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun pickPdfFiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickFilesLauncher.launch(intent)
    }

    private fun createOutputFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "merged_output.pdf")
        }
        createDocumentLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
