package com.pdftoolbox.app.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class PdfDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Stroke(val path: Path, val color: Int, val width: Float)

    private val strokes = mutableListOf<Stroke>()
    private var currentPath = Path()
    private var currentColor = Color.RED
    private var currentStrokeWidth = 8f

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    init {
        paint.color = currentColor
        paint.strokeWidth = currentStrokeWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw existing strokes
        for (stroke in strokes) {
            paint.color = stroke.color
            paint.strokeWidth = stroke.width
            canvas.drawPath(stroke.path, paint)
        }
        // Draw current stroke
        paint.color = currentColor
        paint.strokeWidth = currentStrokeWidth
        canvas.drawPath(currentPath, paint)
    }

    var isDrawingMode = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingMode) return false
        
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.moveTo(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                strokes.add(Stroke(Path(currentPath), currentColor, currentStrokeWidth))
                currentPath.reset()
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setStrokeColor(color: Int) {
        currentColor = color
        paint.color = color
    }
    
    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
        paint.strokeWidth = width
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
            invalidate()
        }
    }

    fun clear() {
        strokes.clear()
        invalidate()
    }

    fun getStrokes(): List<Stroke> = strokes
}
