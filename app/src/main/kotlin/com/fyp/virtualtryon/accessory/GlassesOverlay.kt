package com.fyp.virtualtryon.accessory

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.fyp.virtualtryon.pose.BodyKeypoints
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Aligns glasses onto the face using the eye keypoints.
 * The glasses bitmap should be a transparent-background PNG
 * wider than it is tall, with the bridge centred horizontally.
 */
object GlassesOverlay {

    fun apply(frame: Bitmap, glassesBitmap: Bitmap, keypoints: BodyKeypoints): Bitmap {
        val le = keypoints.leftEye ?: return frame
        val re = keypoints.rightEye ?: return frame

        val output = frame.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val frameW = frame.width.toFloat()
        val frameH = frame.height.toFloat()

        val leX = le.x() * frameW
        val leY = le.y() * frameH
        val reX = re.x() * frameW
        val reY = re.y() * frameH

        // Eye separation in pixels → target width
        val eyeDist = sqrt((reX - leX).pow(2) + (reY - leY).pow(2))
        val targetWidth = eyeDist * 2.6f
        val targetHeight = targetWidth * (glassesBitmap.height.toFloat() / glassesBitmap.width)

        val midX = (leX + reX) / 2f
        val midY = (leY + reY) / 2f

        val angle = Math.toDegrees(atan2((reY - leY).toDouble(), (reX - leX).toDouble())).toFloat()

        val scaleX = targetWidth / glassesBitmap.width
        val scaleY = targetHeight / glassesBitmap.height

        val matrix = Matrix().apply {
            postScale(scaleX, scaleY)
            postRotate(angle, (glassesBitmap.width * scaleX) / 2f, (glassesBitmap.height * scaleY) / 2f)
            postTranslate(
                midX - (glassesBitmap.width * scaleX) / 2f,
                midY - (glassesBitmap.height * scaleY) / 2f
            )
        }

        canvas.drawBitmap(glassesBitmap, matrix, paint)
        return output
    }
}
