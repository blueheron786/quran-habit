package com.quranhabit.ui.statistics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.quranhabit.R
import kotlin.math.max

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
        color = ContextCompat.getColor(context, R.color.green)
    }

    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.text_primary)
        strokeWidth = 3f
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.DKGRAY
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.CENTER
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
        val availableWidth = width - padding * 2 - axisPadding
        val availableHeight = height - padding * 3 - labelPaint.textSize * 2
        barWidth = (availableWidth) / data.size - padding

        // Draw axes
        drawAxes(canvas, width, height, padding, axisPadding)

        // Draw goal line
        drawGoalLine(canvas, width, height, padding, axisPadding, availableHeight)

        // Draw bars and labels
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

        canvas.drawText(
            "Goal: $goal",
            padding + axisPadding - 10f,
            goalY - 10f,
            goalLabelPaint
        )
    }

    private fun drawBarsAndLabels(canvas: Canvas, width: Float, height: Float, padding: Float,
                                  axisPadding: Float, availableHeight: Float) {
        data.forEachIndexed { index, value ->
            val left = padding + axisPadding + index * (barWidth + padding)
            val barHeight = (availableHeight * value / max(goal, data.maxOrNull() ?: goal))
            val top = height - padding - labelPaint.textSize * 1.5f - barHeight
            val right = left + barWidth
            val bottom = height - padding - labelPaint.textSize * 1.5f

            // Draw bar
            canvas.drawRoundRect(
                left, top, right, bottom,
                8f, 8f, barPaint
            )

            // Draw value label if > 0
            if (value > 0) {
                canvas.drawText(
                    value.toString(),
                    left + barWidth / 2,
                    top - 10,
                    labelPaint
                )
            }

            // Draw day label (3-letter abbreviation)
            canvas.drawText(
                labels.getOrNull(index)?.take(3) ?: "",
                left + barWidth / 2,
                height - padding - labelPaint.textSize / 2,
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
                padding + axisPadding - 10f,
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