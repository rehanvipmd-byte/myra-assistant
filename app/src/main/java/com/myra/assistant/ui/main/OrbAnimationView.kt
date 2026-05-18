package com.myra.assistant.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class OrbAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class OrbState {
        IDLE,
        LISTENING,
        SPEAKING,
        THINKING,
        ACTIVE
    }

    private var state = OrbState.IDLE
    private var amplitude = 0f

    // Paint objects
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 4f), 0f)
    }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Animation values
    private var pulseScale = 1f
    private var rotationAngle = 0f
    private var waveOffset = 0f
    private var thinkingArc = 0f
    private var glowAlpha = 120
    private var orbColor = 0xFFB71C1C.toInt() // Red
    private var orbColorSecondary = 0xFF880E4F.toInt() // Purple

    // Animators
    private var pulseAnimator: ValueAnimator? = null
    private var rotationAnimator: ValueAnimator? = null
    private var waveAnimator: ValueAnimator? = null
    private var thinkingAnimator: ValueAnimator? = null
    private var glowAnimator: ValueAnimator? = null

    init {
        startIdleAnimation()
    }

    fun setState(newState: OrbState) {
        if (state == newState) return

        stopAllAnimations()
        state = newState
        glowAlpha = 120

        when (state) {
            OrbState.IDLE -> {
                orbColor = 0xFFB71C1C.toInt()
                orbColorSecondary = 0xFF880E4F.toInt()
                startIdleAnimation()
            }
            OrbState.LISTENING -> {
                orbColor = 0xFFFF1744.toInt()
                orbColorSecondary = 0xFFD500F9.toInt()
                startListeningAnimation()
            }
            OrbState.SPEAKING -> {
                orbColor = 0xFFE040FB.toInt()
                orbColorSecondary = 0xFFFF1744.toInt()
                startSpeakingAnimation()
            }
            OrbState.THINKING -> {
                orbColor = 0xFF40C4FF.toInt()
                orbColorSecondary = 0xFF00B0FF.toInt()
                startThinkingAnimation()
            }
            OrbState.ACTIVE -> {
                orbColor = 0xFFFF1744.toInt()
                orbColorSecondary = 0xFFD500F9.toInt()
                startActiveAnimation()
            }
        }
    }

    fun setAmplitude(amp: Float) {
        amplitude = amp
    }

    private fun startIdleAnimation() {
        // Pulse animation
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                pulseScale = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Glow animation
        glowAnimator = ValueAnimator.ofInt(120, 220, 120).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                glowAlpha = animation.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun startListeningAnimation() {
        // Rotation
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                rotationAngle = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Wave
        waveAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                waveOffset = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Glow pulse
        glowAnimator = ValueAnimator.ofInt(150, 255, 150).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                glowAlpha = animation.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun startSpeakingAnimation() {
        // Fast rotation
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                rotationAngle = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Wave with amplitude
        waveAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                waveOffset = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Steady glow
        glowAnimator = ValueAnimator.ofInt(200, 255, 200).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                glowAlpha = animation.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun startThinkingAnimation() {
        // Thinking arc
        thinkingAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                thinkingArc = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Glow pulse
        glowAnimator = ValueAnimator.ofInt(150, 255, 150).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                glowAlpha = animation.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun startActiveAnimation() {
        // Rotation
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                rotationAngle = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Wave
        waveAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                waveOffset = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Pulse
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                pulseScale = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAllAnimations() {
        pulseAnimator?.cancel()
        rotationAnimator?.cancel()
        waveAnimator?.cancel()
        thinkingAnimator?.cancel()
        glowAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = minOf(width, height) / 2f * 0.3f
        val scaledRadius = baseRadius * pulseScale

        // Draw glow
        drawGlow(canvas, centerX, centerY, scaledRadius)

        // Draw core orb
        drawOrb(canvas, centerX, centerY, scaledRadius)

        // Draw rings
        drawRings(canvas, centerX, centerY, scaledRadius)

        // Draw waves
        drawWaves(canvas, centerX, centerY, scaledRadius)

        // Draw thinking arc
        if (state == OrbState.THINKING) {
            drawThinkingArc(canvas, centerX, centerY, scaledRadius)
        }

        // Draw particles
        if (state == OrbState.SPEAKING || state == OrbState.ACTIVE) {
            drawParticles(canvas, centerX, centerY, scaledRadius)
        }

        // Draw inner highlight
        drawHighlight(canvas, centerX, centerY, scaledRadius)
    }

    private fun drawGlow(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val glowRadius = radius * 1.6f
        val shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(orbColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = shader
        glowPaint.alpha = glowAlpha
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)
    }

    private fun drawOrb(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val shader = RadialGradient(
            cx - radius * 0.3f, cy - radius * 0.3f, radius * 1.5f,
            intArrayOf(Color.WHITE, orbColor, orbColorSecondary),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        orbPaint.shader = shader
        canvas.drawCircle(cx, cy, radius, orbPaint)
    }

    private fun drawRings(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        ringPaint.color = orbColor
        ringPaint.alpha = 200

        // 3 rings at different radii
        val rings = listOf(radius * 1.5f, radius * 2f, radius * 2.5f)
        for ((index, ringRadius) in rings.withIndex()) {
            canvas.save()
            canvas.rotate(rotationAngle + index * 120f, cx, cy)
            canvas.drawCircle(cx, cy, ringRadius, ringPaint)
            canvas.restore()
        }
    }

    private fun drawWaves(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        wavePaint.color = orbColor
        wavePaint.alpha = 150

        val waveRadius = radius * 1.8f
        val waveCount = 3
        val amplitudeVariation = if (state == OrbState.SPEAKING) amplitude * 0.3f else 0.1f

        for (i in 0 until waveCount) {
            val angle = (waveOffset + i * 120f) % 360f
            val x = cx + waveRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = cy + waveRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val waveSize = radius * 0.3f * (1f + amplitudeVariation)
            canvas.drawCircle(x, y, waveSize, wavePaint)
        }
    }

    private fun drawThinkingArc(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        arcPaint.color = orbColor
        arcPaint.alpha = 220

        val arcRadius = radius * 2f
        val rect = android.graphics.RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)

        // Two arcs spinning
        canvas.drawArc(rect, thinkingArc, 45f, false, arcPaint)
        canvas.drawArc(rect, thinkingArc + 180f, 45f, false, arcPaint)
    }

    private fun drawParticles(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        particlePaint.color = orbColor
        particlePaint.alpha = 200

        val particleCount = 12
        val orbitRadius = radius * 1.5f

        for (i in 0 until particleCount) {
            val angle = (rotationAngle + i * (360f / particleCount)) % 360f
            val x = cx + orbitRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = cy + orbitRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            canvas.drawCircle(x, y, radius * 0.15f, particlePaint)
        }
    }

    private fun drawHighlight(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val highlightShader = RadialGradient(
            cx - radius * 0.4f, cy - radius * 0.4f, radius * 0.5f,
            intArrayOf(Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = highlightShader
            alpha = 100
        }
        canvas.drawCircle(cx, cy, radius, highlightPaint)
    }
}
