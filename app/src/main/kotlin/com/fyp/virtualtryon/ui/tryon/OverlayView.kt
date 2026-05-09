package com.fyp.virtualtryon.ui.tryon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.fyp.virtualtryon.pose.BodyKeypoints

/**
 * Transparent overlay drawn on top of the camera preview.
 *
 * Currently draws every pose landmark as a green dot — used to verify
 * MediaPipe is producing valid keypoints. Phase 1 step 3 will extend
 * this to also draw the warped garment bitmap.
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var keypoints: BodyKeypoints? = null

    /** Set true to mirror x-coordinate (front camera). */
    var mirrorHorizontal: Boolean = true

    /** Set true to draw debug skeleton; false to hide for production. */
    var showSkeleton: Boolean = true

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 255, 0)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    fun updateKeypoints(kp: BodyKeypoints?) {
        keypoints = kp
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val kp = keypoints ?: return
        if (!kp.isValid()) return

        if (showSkeleton) drawSkeleton(canvas, kp)
    }

    private fun drawSkeleton(canvas: Canvas, kp: BodyKeypoints) {
        val w = width.toFloat()
        val h = height.toFloat()

        fun mapX(nx: Float) = if (mirrorHorizontal) (1f - nx) * w else nx * w
        fun mapY(ny: Float) = ny * h

        // Bones (pairs of landmark indices we want connected)
        val bones = listOf(
            11 to 12,  // shoulder line
            11 to 13, 13 to 15,  // left arm
            12 to 14, 14 to 16,  // right arm
            11 to 23, 12 to 24,  // torso sides
            23 to 24,            // hip line
            23 to 25, 25 to 27, 27 to 31,  // left leg + foot
            24 to 26, 26 to 28, 28 to 32,  // right leg + foot
        )

        for ((aIdx, bIdx) in bones) {
            val a = kp.landmarks.getOrNull(aIdx) ?: continue
            val b = kp.landmarks.getOrNull(bIdx) ?: continue
            canvas.drawLine(mapX(a.x()), mapY(a.y()), mapX(b.x()), mapY(b.y()), linePaint)
        }

        // Dots for every landmark
        for (lm in kp.landmarks) {
            canvas.drawCircle(mapX(lm.x()), mapY(lm.y()), 8f, dotPaint)
        }
    }
}
