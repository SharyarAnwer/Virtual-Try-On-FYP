package com.fyp.virtualtryon.shoes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Debug overlay — draws the foot keypoints detected by FootDetectorCV directly on screen.
 * Heel = red circle, Toe = green circle, connecting line = white.
 * Confirms detection is working before 3D overlay is re-enabled.
 */
class FootKeypointOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var keypoints: FootKeypoints? = null

    private val heelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val toePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        textSize = 36f
    }

    fun update(fk: FootKeypoints?) {
        keypoints = fk
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fk = keypoints ?: return
        val w = width.toFloat()
        val h = height.toFloat()

        if (fk.hasLeftFoot()) {
            drawFoot(canvas, fk.leftHeel!!, fk.leftToe!!, w, h, "L")
        }
        if (fk.hasRightFoot()) {
            drawFoot(canvas, fk.rightHeel!!, fk.rightToe!!, w, h, "R")
        }
    }

    private fun drawFoot(
        canvas: Canvas,
        heel: android.graphics.PointF,
        toe: android.graphics.PointF,
        w: Float, h: Float,
        label: String,
    ) {
        val hx = heel.x * w;  val hy = heel.y * h
        val tx = toe.x  * w;  val ty = toe.y  * h

        // Connecting line
        canvas.drawLine(hx, hy, tx, ty, linePaint)

        // Heel dot (red) — larger
        canvas.drawCircle(hx, hy, DOT_RADIUS_LARGE, heelPaint)
        canvas.drawText("heel $label", hx + 10, hy - 10, labelPaint)

        // Toe dot (green)
        canvas.drawCircle(tx, ty, DOT_RADIUS_SMALL, toePaint)
        canvas.drawText("toe $label", tx + 10, ty - 10, labelPaint)
    }

    companion object {
        private const val DOT_RADIUS_LARGE = 20f
        private const val DOT_RADIUS_SMALL = 16f
    }
}
