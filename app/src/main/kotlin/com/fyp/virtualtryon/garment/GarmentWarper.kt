package com.fyp.virtualtryon.garment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.fyp.virtualtryon.pose.BodyKeypoints

/**
 * Scales, rotates, and positions a garment image over a camera frame
 * based on the detected body keypoints.
 *
 * Coordinate system: normalised 0..1 → pixel by multiplying by image dimensions.
 */
object GarmentWarper {

    /**
     * Overlays [garmentBitmap] onto [frameBitmap] aligned to the upper body
     * (shoulders → hips region). Returns a new composite bitmap.
     */
    fun overlayUpperBody(
        frameBitmap: Bitmap,
        garmentBitmap: Bitmap,
        keypoints: BodyKeypoints,
    ): Bitmap {
        val output = frameBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val ls = keypoints.leftShoulder ?: return output
        val rs = keypoints.rightShoulder ?: return output
        val lh = keypoints.leftHip ?: return output
        val rh = keypoints.rightHip ?: return output

        val frameW = frameBitmap.width.toFloat()
        val frameH = frameBitmap.height.toFloat()

        // Shoulder midpoint in pixels
        val shoulderMidX = ((ls.x() + rs.x()) / 2f) * frameW
        val shoulderMidY = ((ls.y() + rs.y()) / 2f) * frameH

        // Shoulder width in pixels → target garment width (add 20% padding each side)
        val shoulderWidthPx = (rs.x() - ls.x()).coerceAtLeast(0f) * frameW
        val targetWidth = shoulderWidthPx * 1.4f

        // Torso height in pixels
        val hipMidY = ((lh.y() + rh.y()) / 2f) * frameH
        val torsoHeightPx = (hipMidY - shoulderMidY).coerceAtLeast(1f)
        val targetHeight = torsoHeightPx * 1.15f

        // Rotation angle from left→right shoulder vector
        val angle = Math.toDegrees(
            Math.atan2(
                (rs.y() - ls.y()).toDouble(),
                (rs.x() - ls.x()).toDouble()
            )
        ).toFloat()

        val scaleX = targetWidth / garmentBitmap.width
        val scaleY = targetHeight / garmentBitmap.height

        val matrix = Matrix().apply {
            // Scale around garment center
            postScale(scaleX, scaleY)
            // Rotate to match shoulder tilt
            postRotate(angle, (garmentBitmap.width * scaleX) / 2f, 0f)
            // Translate so garment top-center aligns with shoulder midpoint
            postTranslate(
                shoulderMidX - (garmentBitmap.width * scaleX) / 2f,
                shoulderMidY
            )
        }

        canvas.drawBitmap(garmentBitmap, matrix, paint)
        return output
    }

    /**
     * Overlays [garmentBitmap] onto [frameBitmap] aligned to the lower body
     * (hips → ankles). Requires a full-body standing pose.
     */
    fun overlayLowerBody(
        frameBitmap: Bitmap,
        garmentBitmap: Bitmap,
        keypoints: BodyKeypoints,
    ): Bitmap {
        val output = frameBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val lh = keypoints.leftHip ?: return output
        val rh = keypoints.rightHip ?: return output
        val la = keypoints.leftAnkle ?: return output
        val ra = keypoints.rightAnkle ?: return output

        val frameW = frameBitmap.width.toFloat()
        val frameH = frameBitmap.height.toFloat()

        val hipMidX = ((lh.x() + rh.x()) / 2f) * frameW
        val hipMidY = ((lh.y() + rh.y()) / 2f) * frameH
        val ankleMidY = ((la.y() + ra.y()) / 2f) * frameH

        val hipWidthPx = (rh.x() - lh.x()).coerceAtLeast(0f) * frameW
        val targetWidth = hipWidthPx * 1.3f
        val targetHeight = (ankleMidY - hipMidY).coerceAtLeast(1f) * 1.05f

        val scaleX = targetWidth / garmentBitmap.width
        val scaleY = targetHeight / garmentBitmap.height

        val matrix = Matrix().apply {
            postScale(scaleX, scaleY)
            postTranslate(
                hipMidX - (garmentBitmap.width * scaleX) / 2f,
                hipMidY
            )
        }

        canvas.drawBitmap(garmentBitmap, matrix, paint)
        return output
    }
}
