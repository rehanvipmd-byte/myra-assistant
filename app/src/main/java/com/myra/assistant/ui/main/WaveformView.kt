package com.myra.assistant.ui.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 20
    private val barHeights = FloatArray(barCount) { 0.2f }
    private val targetHeights = FloatArray(barCount) { 0.2f }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lerpFactor = 0.3f

    private var isAnimating = false
    private var lastAmplitude = 0f

    init {
        barPaint.color = 0xFFFF1744.toInt()
        barPaint.style = Paint.Style.FILL
    }

    fun setAmplitude(amplitude: Float) {
        lastAmplitude = amplitude
        if (!isAnimating) return

        // Update target heights based on amplitude
        for (i in barHeights.indices) {
            // Vary heights around the amplitude
            val randomVariation = 0.5f + (i.toFloat() / barCount) * 1.5f
            targetHeights[i] = (0.2f + amplitude * randomVariation * 0.8f).coerceIn(0.1f, 1f)
        }
        invalidate()
    }

    fun startAnimation() {
        isAnimating = true
        post { updateBars() }
    }

    fun stopAnimation() {
        isAnimating = false
        // Gradually return to idle
        for (i in barHeights.indices) {
            targetHeights[i] = 0.2f
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val barWidth = width.toFloat() / barCount
        val centerY = height / 2f
        val maxHeight = height / 2f * 0.9f

        updateBars()

        for (i in barHeights.indices) {
            val barHeight = barHeights[i] * maxHeight
            val x = i * barWidth + barWidth / 2f

            // Color based on height
            val alpha = (150 + barHeights[i] * 105).toInt().coerceIn(150, 255)
            barPaint.alpha = alpha

            canvas.drawRect(
                x - barWidth / 3f,
                centerY - barHeight,
                x + barWidth / 3f,
                centerY + barHeight,
                barPaint
            )
        }
    }

    private fun updateBars() {
        var changed = false
        for (i in barHeights.indices) {
            val diff = targetHeights[i] - barHeights[i]
            if (abs(diff) > 0.01f) {
                barHeights[i] += diff * lerpFactor
                changed = true
            }
        }

        if (changed && isAnimating) {
            invalidate()
            post { updateBars() }
        }
    }
}
