package com.pdftoolbox.app.ui.tools.edit

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pdftoolbox.app.R
import com.pdftoolbox.app.databinding.FragmentEditPdfBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditPdfFragment : Fragment() {

    private var _binding: FragmentEditPdfBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditPdfViewModel by viewModels()

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pageCount = 0
    private var currentPage = 0
    private var renderJob: Job? = null
    private var currentRenderedBitmap: Bitmap? = null

    private var isDrawMode = true
    private var currentColor = Color.BLACK

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.selectFile(uri)
                openPdf(uri)
            }
        }
    }

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                binding.progressBar.visibility = View.VISIBLE
                val overlayBitmap = captureDrawingOverlay()
                viewModel.setCurrentPage(currentPage)
                viewModel.applyEdits(overlayBitmap, uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditPdfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarEdit.setNavigationIcon(R.drawable.ic_home)
        binding.toolbarEdit.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbarEdit.inflateMenu(R.menu.menu_edit_pdf)
        binding.toolbarEdit.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open -> {
                    openFilePicker()
                    true
                }
                else -> false
            }
        }

        setupToolbar()

        binding.btnSaveEdits.setOnClickListener {
            if (viewModel.selectedFile.value != null) {
                createOutputFile()
            } else {
                Toast.makeText(requireContext(), "Please open a PDF first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPrevPage.setOnClickListener { navigatePage(-1) }
        binding.btnNextPage.setOnClickListener { navigatePage(1) }

        viewModel.editState.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(context, "Edits saved successfully", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        if (viewModel.selectedFile.value == null) {
            openFilePicker()
        }
    }

    private fun openPdf(uri: Uri) {
        currentPage = 0
        renderCurrentPage(uri)
    }

    private fun navigatePage(delta: Int) {
        val newPage = currentPage + delta
        if (newPage in 0 until pageCount) {
            currentPage = newPage
            viewModel.selectedFile.value?.let { renderCurrentPage(it) }
        }
    }

    private fun renderCurrentPage(uri: Uri) {
        renderJob?.cancel()
        binding.progressBar.visibility = View.VISIBLE

        val lp = binding.canvasContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        lp.width = 0
        lp.height = 0
        binding.canvasContainer.layoutParams = lp

        binding.canvasContainer.post {
            val maxW = binding.canvasContainer.width.coerceAtLeast(1)
            val maxH = binding.canvasContainer.height.coerceAtLeast(1)

            renderJob = lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) { renderPageBitmap(uri, maxW, maxH, currentPage) }
                currentRenderedBitmap?.recycle()
                currentRenderedBitmap = bitmap
                if (bitmap != null) {
                    binding.pdfPageImage.setImageBitmap(bitmap)
                }
                binding.drawingView.clear()
                removeTextOverlays()
                binding.progressBar.visibility = View.GONE
                updatePageIndicator()
            }
        }
    }

    private fun renderPageBitmap(uri: Uri, maxW: Int, maxH: Int, pageIndex: Int): Bitmap? {
        closeRenderer()
        return try {
            val pfd = requireContext().contentResolver.openFileDescriptor(uri, "r") ?: return null
            parcelFileDescriptor = pfd
            pdfRenderer = PdfRenderer(pfd)
            val renderer = pdfRenderer ?: return null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            pageCount = renderer.pageCount

            val page = renderer.openPage(pageIndex)
            val pdfWidth = page.width.toFloat()
            val pdfHeight = page.height.toFloat()
            if (pdfWidth <= 0f || pdfHeight <= 0f) { page.close(); return null }

            val scaleX = maxW / pdfWidth
            val scaleY = maxH / pdfHeight
            val scale = minOf(scaleX, scaleY, 1.5f)

            val layoutWidth = (pdfWidth * scale).toInt().coerceAtLeast(1)
            val layoutHeight = (pdfHeight * scale).toInt().coerceAtLeast(1)

            lifecycleScope.launch {
                val clp = binding.canvasContainer.layoutParams
                clp.width = layoutWidth
                clp.height = layoutHeight
                binding.canvasContainer.layoutParams = clp
            }

            val maxRenderDim = 2048
            val renderScale = minOf(scale * 2f, maxRenderDim / pdfWidth, maxRenderDim / pdfHeight)
            val bmpWidth = (pdfWidth * renderScale).toInt().coerceIn(1, maxRenderDim)
            val bmpHeight = (pdfHeight * renderScale).toInt().coerceIn(1, maxRenderDim)

            val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
            val matrix = Matrix().apply { postScale(renderScale, renderScale) }
            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun captureDrawingOverlay(): Bitmap {
        val view = binding.canvasContainer
        binding.pdfPageImage.visibility = View.INVISIBLE
        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        binding.pdfPageImage.visibility = View.VISIBLE
        return bitmap
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
            putExtra(Intent.EXTRA_TITLE, "edited_output.pdf")
        }
        createFileLauncher.launch(intent)
    }

    private fun setupToolbar() {
        binding.btnUndo.setOnClickListener { binding.drawingView.undo() }

        binding.btnModeDraw.setOnClickListener {
            isDrawMode = true
            binding.drawingView.isDrawingMode = true
            updateModeUI()
        }
        binding.btnModeText.setOnClickListener {
            isDrawMode = false
            binding.drawingView.isDrawingMode = false
            updateModeUI()
        }

        binding.colorBlack.setOnClickListener { setColor(Color.BLACK, binding.colorBlack) }
        binding.colorRed.setOnClickListener { setColor(Color.parseColor("#BA1A1A"), binding.colorRed) }
        binding.colorBlue.setOnClickListener { setColor(Color.parseColor("#5A31E1"), binding.colorBlue) }

        binding.canvasContainer.setOnTouchListener { _, event ->
            if (!isDrawMode && event.action == android.view.MotionEvent.ACTION_UP) {
                addTextAt(event.x, event.y)
                true
            } else {
                false
            }
        }
        updateModeUI()
    }

    private fun updateModeUI() {
        val activeColor = requireContext().getColor(R.color.md_theme_light_primary)
        val inactiveColor = requireContext().getColor(R.color.md_theme_light_onSurfaceVariant)
        binding.btnModeDraw.setColorFilter(if (isDrawMode) activeColor else inactiveColor)
        binding.btnModeText.setColorFilter(if (!isDrawMode) activeColor else inactiveColor)
        binding.btnUndo.isEnabled = isDrawMode
    }

    private fun setColor(color: Int, selectedView: View) {
        currentColor = color
        binding.drawingView.setStrokeColor(color)
        binding.colorBlack.strokeWidth = 0
        binding.colorRed.strokeWidth = 0
        binding.colorBlue.strokeWidth = 0
        (selectedView as com.google.android.material.card.MaterialCardView).strokeWidth = 6
    }

    private fun addTextAt(x: Float, y: Float) {
        val editText = EditText(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = x.toInt()
                topMargin = y.toInt()
            }
            background = null
            setTextColor(currentColor)
            textSize = 24f
            hint = "Text"
            setHintTextColor(Color.GRAY)
            requestFocus()
        }
        binding.canvasContainer.addView(editText)
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun removeTextOverlays() {
        for (i in binding.canvasContainer.childCount - 1 downTo 0) {
            val child = binding.canvasContainer.getChildAt(i)
            if (child is EditText) {
                binding.canvasContainer.removeView(child)
            }
        }
    }

    private fun updatePageIndicator() {
        if (pageCount > 0) {
            binding.tvPageIndicator.text = "Page ${currentPage + 1} of $pageCount"
            binding.tvPageIndicator.visibility = View.VISIBLE
            binding.btnPrevPage.visibility = View.VISIBLE
            binding.btnNextPage.visibility = View.VISIBLE
            binding.btnPrevPage.isEnabled = currentPage > 0
            binding.btnNextPage.isEnabled = currentPage < pageCount - 1
        } else {
            binding.tvPageIndicator.visibility = View.GONE
            binding.btnPrevPage.visibility = View.GONE
            binding.btnNextPage.visibility = View.GONE
        }
    }

    private fun closeRenderer() {
        pdfRenderer?.close()
        try { parcelFileDescriptor?.close() } catch (_: Exception) {}
        pdfRenderer = null
        parcelFileDescriptor = null
    }

    override fun onDestroyView() {
        renderJob?.cancel()
        renderJob = null
        closeRenderer()
        currentRenderedBitmap?.recycle()
        currentRenderedBitmap = null
        _binding = null
        super.onDestroyView()
    }
}