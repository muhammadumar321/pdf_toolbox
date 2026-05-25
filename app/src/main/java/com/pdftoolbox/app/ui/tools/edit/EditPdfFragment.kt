package com.pdftoolbox.app.ui.tools.edit

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pdftoolbox.app.databinding.FragmentEditPdfBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.widget.EditText
import android.widget.FrameLayout

class EditPdfFragment : Fragment() {

    private var _binding: FragmentEditPdfBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditPdfViewModel by viewModels()

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private var isDrawMode = true
    private var currentColor = Color.BLACK

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.selectFile(uri)
                renderFirstPage(uri)
            }
        }
    }

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                binding.progressBar.visibility = View.VISIBLE
                val overlayBitmap = getDrawingBitmap()
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

        binding.toolbarEdit.setNavigationIcon(com.pdftoolbox.app.R.drawable.ic_home)
        binding.toolbarEdit.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbarEdit.inflateMenu(com.pdftoolbox.app.R.menu.menu_edit_pdf)
        binding.toolbarEdit.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                com.pdftoolbox.app.R.id.action_open -> {
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

        viewModel.editState.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(context, "Edits saved successfully", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Start by picking a file if arguments are empty (arguments aren't strictly passed in MVP from Tools yet)
        if (viewModel.selectedFile.value == null) {
            openFilePicker()
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
            putExtra(Intent.EXTRA_TITLE, "edited_output.pdf")
        }
        createFileLauncher.launch(intent)
    }

    private fun renderFirstPage(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        
        // Reset layout margins/constraints to get max available space
        val lp = binding.canvasContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        lp.width = 0
        lp.height = 0
        binding.canvasContainer.layoutParams = lp
        
        binding.canvasContainer.post {
            val maxW = binding.canvasContainer.width
            val maxH = binding.canvasContainer.height

            CoroutineScope(Dispatchers.Main).launch {
                val bitmap = withContext(Dispatchers.IO) { getPageBitmap(uri, maxW, maxH) }
                binding.pdfPageImage.setImageBitmap(bitmap)
                binding.drawingView.clear()
                
                // Clear any added textviews
                val childCount = binding.canvasContainer.childCount
                for (i in childCount - 1 downTo 0) {
                    val child = binding.canvasContainer.getChildAt(i)
                    if (child is EditText) {
                        binding.canvasContainer.removeView(child)
                    }
                }
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun getPageBitmap(uri: Uri, maxW: Int, maxH: Int): Bitmap? {
        closeRenderer()
        try {
            val pfd = requireContext().contentResolver.openFileDescriptor(uri, "r") ?: return null
            parcelFileDescriptor = pfd
            pdfRenderer = PdfRenderer(pfd)
            
            val renderer = pdfRenderer ?: return null
            if (renderer.pageCount == 0) return null
            
            val page = renderer.openPage(0)
            
            val pdfWidth = page.width.toFloat()
            val pdfHeight = page.height.toFloat()
            if (pdfWidth == 0f || pdfHeight == 0f) return null

            val scaleX = maxW / pdfWidth
            val scaleY = maxH / pdfHeight
            val scale = minOf(scaleX, scaleY)

            val layoutWidth = (pdfWidth * scale).toInt()
            val layoutHeight = (pdfHeight * scale).toInt()
            
            // Adjust container dimensions to tightly fit the PDF page
            CoroutineScope(Dispatchers.Main).launch {
                val clp = binding.canvasContainer.layoutParams
                clp.width = layoutWidth
                clp.height = layoutHeight
                binding.canvasContainer.layoutParams = clp
            }

            // High-res render scale for crispness
            val renderScale = scale * 2f
            val bmpWidth = (pdfWidth * renderScale).toInt()
            val bmpHeight = (pdfHeight * renderScale).toInt()
            
            val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
            val matrix = Matrix().apply { postScale(renderScale, renderScale) }
            
            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    // Captures the drawingView as a Bitmap to overlay onto the pdf
    private fun getDrawingBitmap(): Bitmap {
        val view = binding.canvasContainer
        
        // Hide pdf_page_image temporarily to only capture the drawings and text
        binding.pdfPageImage.visibility = View.INVISIBLE
        
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        
        binding.pdfPageImage.visibility = View.VISIBLE
        return bitmap
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
        val activeColor = requireContext().getColor(com.pdftoolbox.app.R.color.md_theme_light_primary)
        val inactiveColor = requireContext().getColor(com.pdftoolbox.app.R.color.md_theme_light_onSurfaceVariant)
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
        
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeRenderer() {
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
        pdfRenderer = null
        parcelFileDescriptor = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        closeRenderer()
        _binding = null
    }
}
