package com.fyp.virtualtryon.accessory

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.fyp.virtualtryon.pose.BodyKeypoints

/**
 * Places shoe bitmaps over each foot using heel + foot-index keypoints.
 * Requires a full-body standing pose with feet visible.
 */
object ShoesOverlay {

    fun apply(frame: Bitmap, shoeBitmap: Bitmap, keypoints: BodyKeypoints): Bitmap {
        val output = frame.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val frameW = frame.width.toFloat()
        val frameH = frame.height.toFloat()

        // Draw a shoe for each foot
        drawShoe(canvas, shoeBitmap, paint, frameW, frameH, keypoints, left = true)
        drawShoe(canvas, shoeBitmap, paint, frameW, frameH, keypoints, left = false)

        return output
    }

    private fun drawShoe(
        canvas: Canvas,
        shoeBitmap: Bitmap,
        paint: Paint,
        frameW: Float,
        frameH: Float,
        keypoints: BodyKeypoints,
        left: Boolean,
    ) {
        val heel = if (left) keypoints.leftHeel else keypoints.rightHeel
        val toe  = if (left) keypoints.leftFootIndex else keypoints.rightFootIndex

        if (heel == null || toe == null) return

        val heelX = heel.x() * frameW
        val heelY = heel.y() * frameH
        val toeX  = toe.x() * frameW
        val toeY  = toe.y() * frameH

        val footLen = Math.hypot((toeX - heelX).toDouble(), (toeY - heelY).toDouble()).toFloat()
        val targetWidth = footLen * 1.5f
        val scaleX = targetWidth / shoeBitmap.width
        val scaleY = scaleX // preserve aspect

        val angle = Math.toDegrees(Math.atan2((toeY - heelY).toDouble(), (toeX - heelX).toDouble())).toFloat()

        // Mirror the right shoe horizontally
        val matrix = Matrix().apply {
            if (!left) postScale(-1f, 1f, shoeBitmap.width / 2f, 0f)
            postScale(scaleX, scaleY)
            postRotate(angle, 0f, (shoeBitmap.height * scaleY) / 2f)
            postTranslate(heelX, heelY - (shoeBitmap.height * scaleY) / 2f)
        }

        canvas.drawBitmap(shoeBitmap, matrix, paint)
    }
}
