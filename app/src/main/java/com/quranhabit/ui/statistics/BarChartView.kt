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
    private var goal: Int = StatisticsFragment.PAGES_PER_DAY_GOAL

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.text_primary)
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private var barWidth = 0f
    private var maxValue = 20 // Default goal
    private var data: List<Int> = emptyList()
    private var labels: List<String> = emptyList()

    fun setData(values: List<Int>, labels: List<String>, goal: Int = 20) {
        this.data = values
        this.labels = labels
        this.maxValue = max(goal, values.maxOrNull() ?: goal)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 20f
        val availableHeight = height - padding * 3 - textPaint.textSize
        barWidth = (width - padding * 2) / data.size - padding

        // Draw goal line
        val goalY = height - padding - (availableHeight * goal / maxValue)
        canvas.drawLine(padding, goalY, width - padding, goalY, goalPaint)

        // Draw bars
        data.forEachIndexed { index, value ->
            val left = padding + index * (barWidth + padding)
            val barHeight = (availableHeight * value / maxValue)
            val top = height - padding - barHeight - textPaint.textSize
            val right = left + barWidth
            val bottom = height - padding - textPaint.textSize

            // Draw bar
            canvas.drawRoundRect(
                left, top, right, bottom,
                20f, 20f, barPaint
            )

            // Draw value text
            canvas.drawText(
                value.toString(),
                left + barWidth / 2,
                top - 10,
                textPaint
            )

            // Draw day label
            canvas.drawText(
                labels[index],
                left + barWidth / 2,
                height - 10,
                textPaint
            )
        }
    }
}