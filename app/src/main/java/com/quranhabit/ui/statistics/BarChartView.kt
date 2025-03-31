package com.quranhabit.ui.statistics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.quranhabit.R
import kotlin.math.max
import kotlin.math.min
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Properties
    private var barWidth: Float = 0f
    private var goal: Int = StatisticsFragment.PAGES_PER_DAY_GOAL
    private var data: List<Int> = emptyList()
    private var labels: List<String> = emptyList()

    // Paints
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.text_primary)
        strokeWidth = 3f
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.text_primary)
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isSubpixelText = true // For smoother text rendering
    }

    private val goalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 32f
        textAlign = Paint.Align.RIGHT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 40f
        val axisPadding = 60f
        val availableWidth = width - padding * 2 - axisPadding - 10f
        val availableHeight = height - padding * 3 - labelPaint.textSize * 2
        barWidth = min(
            60f,
            (availableWidth - (padding * (data.size - 1))) / data.size
        )

        drawAxes(canvas, width, height, padding, axisPadding)
        drawGoalLine(canvas, width, height, padding, axisPadding, availableHeight)
        drawBarsAndLabels(canvas, width, height, padding, axisPadding, availableHeight)
    }

    private fun drawAxes(canvas: Canvas, width: Float, height: Float, padding: Float, axisPadding: Float) {
        // Y-axis (pages)
        canvas.drawLine(
            padding + axisPadding,
            padding,
            padding + axisPadding,
            height - padding - labelPaint.textSize * 1.5f,
            axisPaint
        )

        // X-axis (days)
        canvas.drawLine(
            padding + axisPadding,
            height - padding - labelPaint.textSize * 1.5f,
            width - padding,
            height - padding - labelPaint.textSize * 1.5f,
            axisPaint
        )
    }

    private fun drawGoalLine(canvas: Canvas, width: Float, height: Float, padding: Float,
                             axisPadding: Float, availableHeight: Float) {
        val goalY = height - padding - labelPaint.textSize * 1.5f -
                (availableHeight * goal / max(goal, data.maxOrNull() ?: goal))

        canvas.drawLine(
            padding + axisPadding,
            goalY,
            width - padding,
            goalY,
            goalPaint
        )

        canvas.drawText("Goal", width - padding - 30f, goalY - 15f, goalLabelPaint)
    }

    private fun drawBarsAndLabels(canvas: Canvas, width: Float, height: Float, padding: Float,
                                  axisPadding: Float, availableHeight: Float) {
        val cornerRadius = 20f // Radius for rounded tops

        data.forEachIndexed { index, value ->
            val left = padding + axisPadding + index * (barWidth + padding)
            val barHeight = (availableHeight * value / max(goal, data.maxOrNull() ?: goal))
            val top = height - padding - labelPaint.textSize * 1.5f - barHeight
            val right = left + barWidth
            val bottom = height - padding - labelPaint.textSize * 1.5f

            // Draw bar with rounded top only
            val barPath = Path().apply {
                moveTo(left, bottom)
                lineTo(left, top + cornerRadius)
                quadTo(left, top, left + cornerRadius, top)
                lineTo(right - cornerRadius, top)
                quadTo(right, top, right, top + cornerRadius)
                lineTo(right, bottom)
                close()
            }
            canvas.drawPath(barPath, barPaint)

            // Draw value label if > 0
            if (value > 0) {
                canvas.drawText(
                    value.toString(),
                    left + barWidth / 2,
                    top - 10,
                    labelPaint
                )
            }

            // Draw day label with perfect centering
            val dayLabel = labels.getOrNull(index)?.take(3) ?: ""
            val labelX = left + barWidth / 2
            val labelY = height - padding - 10f

            // Snap to pixel for crisp rendering
            val snappedX = (labelX + 0.5f).toInt() - 0.5f
            val snappedY = (labelY + 0.5f).toInt() - 0.5f

            canvas.drawText(
                dayLabel,
                snappedX,
                snappedY,
                labelPaint
            )
        }

        // Draw Y-axis labels
        val maxValue = max(goal, data.maxOrNull() ?: goal)
        val yStep = if (maxValue > 0) maxValue / 4 else 1
        for (i in 0..4) {
            val yValue = i * yStep
            val yPos = height - padding - labelPaint.textSize * 1.5f -
                    (availableHeight * yValue / maxValue)

            canvas.drawText(
                yValue.toString(),
                padding + axisPadding - 15f,
                yPos + labelPaint.textSize / 3,
                labelPaint
            )
        }
    }

    fun setData(values: List<Int>, labels: List<String>) {
        require(values.size == labels.size) { "Values and labels must have same size" }
        this.data = values
        this.labels = labels
        invalidate()
    }
}