package com.pennywize.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * A lightweight custom View that draws a pie chart using Canvas.
 * No third-party chart library required.
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Slice(val label: String, val value: Double, val color: Int)

    private val slices      = mutableListOf<Slice>()
    private var isEmpty     = true

    private val paint       = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val emptyPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2937")
        style = Paint.Style.FILL
    }
    private val emptyText   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.parseColor("#94A3B8")
        textAlign = Paint.Align.CENTER
    }

    // Hole in the middle for a donut look
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A0F1F")
        style = Paint.Style.FILL
    }

    fun setData(data: List<Pair<String, Double>>, colors: List<String>) {
        slices.clear()
        isEmpty = false
        val total = data.sumOf { it.second }
        data.forEachIndexed { i, (name, value) ->
            slices.add(Slice(name, value / total, Color.parseColor(colors[i % colors.size])))
        }
        invalidate()
    }

    fun setEmpty() {
        slices.clear()
        isEmpty = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx      = width  / 2f
        val cy      = height / 2f
        val radius  = (minOf(width, height) / 2f) * 0.88f
        val holeR   = radius * 0.52f          // donut hole size
        val oval    = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        if (isEmpty || slices.isEmpty()) {
            canvas.drawCircle(cx, cy, radius, emptyPaint)
            canvas.drawCircle(cx, cy, holeR, holePaint)
            emptyText.textSize = radius * 0.18f
            canvas.drawText("No data", cx, cy + emptyText.textSize / 3, emptyText)
            return
        }

        var startAngle = -90f          // start at 12 o'clock

        slices.forEach { slice ->
            val sweep = (slice.value * 360f).toFloat()

            // Slice fill
            paint.style = Paint.Style.FILL
            paint.color = slice.color
            canvas.drawArc(oval, startAngle, sweep, true, paint)

            // Thin separator line
            paint.style       = Paint.Style.STROKE
            paint.color       = Color.parseColor("#0A0F1F")
            paint.strokeWidth = 2f
            canvas.drawArc(oval, startAngle, sweep, true, paint)

            // Percentage label — only when slice is big enough to fit text
            if (sweep > 18f) {
                val midAngle  = Math.toRadians((startAngle + sweep / 2).toDouble())
                val labelR    = radius * 0.72f
                val tx        = (cx + labelR * Math.cos(midAngle)).toFloat()
                val ty        = (cy + labelR * Math.sin(midAngle)).toFloat()
                textPaint.textSize = radius * 0.13f
                val pct = "%.0f%%".format(slice.value * 100)
                canvas.drawText(pct, tx, ty + textPaint.textSize / 3, textPaint)
            }

            startAngle += sweep
        }

        // Draw donut hole on top
        canvas.drawCircle(cx, cy, holeR, holePaint)

        // Centre label
        val centreLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = Color.parseColor("#94A3B8")
            textAlign = Paint.Align.CENTER
            textSize  = holeR * 0.28f
        }
        canvas.drawText("Spending", cx, cy - centreLabel.textSize * 0.4f, centreLabel)
        canvas.drawText("breakdown", cx, cy + centreLabel.textSize * 1.1f, centreLabel)
    }
}