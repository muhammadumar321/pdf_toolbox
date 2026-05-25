package com.pdftoolbox.app.ui.viewer

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.graphics.pdf.PdfRenderer
import com.pdftoolbox.app.R
import com.pdftoolbox.app.databinding.FragmentViewerBinding
import com.pdftoolbox.app.databinding.ItemPdfPageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewerFragment : Fragment() {

    private var _binding: FragmentViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUriString: String? = null
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pageCount: Int = 0
    private lateinit var adapter: PdfPageAdapter
    private val bitmapCache = object : LruCache<Int, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fileUriString = it.getString("fileUri")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbarViewer.setNavigationIcon(R.drawable.ic_home)
        binding.toolbarViewer.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.toolbarViewer.setOnMenuItemClickListener { false }

        adapter = PdfPageAdapter(viewLifecycleOwner.lifecycleScope, bitmapCache) { index, targetWidth ->
            renderPage(index, targetWidth)
        }
        binding.recyclerPages.layoutManager = LinearLayoutManager(context)
        binding.recyclerPages.adapter = adapter

        val uriStr = fileUriString
        if (uriStr != null) {
            openPdfInApp(Uri.parse(uriStr))
        }
    }

    private fun openPdfInApp(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        bitmapCache.evictAll()
        viewLifecycleOwner.lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) { openRenderer(uri) }
            pageCount = count
            val pages = (0 until pageCount).toList()
            adapter.submitList(pages)
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun openRenderer(uri: Uri): Int {
        closeRenderer()
        val pfd = requireContext().contentResolver.openFileDescriptor(uri, "r") ?: return 0
        parcelFileDescriptor = pfd
        pdfRenderer = PdfRenderer(pfd)
        return pdfRenderer?.pageCount ?: 0
    }

    private fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap? {
        val renderer = pdfRenderer ?: return null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
        val page = renderer.openPage(pageIndex)
        val width = if (targetWidth > 0) targetWidth else page.width
        val scale = width.toFloat() / page.width.toFloat()
        val height = (page.height * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val matrix = Matrix().apply {
            postScale(scale, scale)
        }
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    private fun closeRenderer() {
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
        pdfRenderer = null
        parcelFileDescriptor = null
    }

    // Removed viewer-specific helpers in external viewer fallback

    override fun onDestroyView() {
        super.onDestroyView()
        closeRenderer()
        _binding = null
    }

    private class PdfPageAdapter(
        private val scope: CoroutineScope,
        private val cache: LruCache<Int, Bitmap>,
        private val renderer: suspend (Int, Int) -> Bitmap?
    ) : ListAdapter<Int, PdfPageAdapter.PageViewHolder>(DiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val binding = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PageViewHolder(binding, scope, cache, renderer)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class PageViewHolder(
            private val binding: ItemPdfPageBinding,
            private val scope: CoroutineScope,
            private val cache: LruCache<Int, Bitmap>,
            private val renderer: suspend (Int, Int) -> Bitmap?
        ) : RecyclerView.ViewHolder(binding.root) {

            private var job: Job? = null

            fun bind(pageIndex: Int) {
                job?.cancel()
                binding.textPageNumber.text = "Page ${pageIndex + 1}"
                val cached = cache.get(pageIndex)
                if (cached != null) {
                    binding.imagePage.setImageBitmap(cached)
                    return
                }
                binding.imagePage.setImageBitmap(null)
                binding.imagePage.doOnLayout {
                    val targetWidth = it.width
                    job = scope.launch {
                        val bitmap = withContext(Dispatchers.IO) { renderer(pageIndex, targetWidth) }
                        if (bitmap != null) {
                            cache.put(pageIndex, bitmap)
                            binding.imagePage.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }

        class DiffCallback : DiffUtil.ItemCallback<Int>() {
            override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean = oldItem == newItem
            override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean = oldItem == newItem
        }
    }
}
