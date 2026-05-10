package com.fyp.virtualtryon.shoes

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

private const val TAG = "FootDetectorCV"

/**
 * Option C: detects feet using OpenCV skin-colour segmentation.
 *
 * Works when only the feet are visible — no torso/hips required.
 *
 * Algorithm:
 *  1. Convert frame to YCrCb colour space.
 *  2. Threshold on known skin-colour ranges (works for most skin tones under typical lighting).
 *  3. Morphological open+close to remove noise and fill gaps.
 *  4. Find external contours; keep those large enough to be a foot.
 *  5. For each candidate contour, derive heel and toe from the bounding-rectangle extremes
 *     along the contour's long axis.
 *  6. Assign left/right by horizontal position.
 *
 * Limitations (acceptable for FYP scope):
 *  - Fails with socks / shoes that cover skin.
 *  - May produce false positives if background contains skin-like colours.
 *  - Heel/toe assignment assumes foot points roughly up/down or left/right.
 */
class FootDetectorCV(
    private val onResult: (FootKeypoints?) -> Unit,
) {
    private var lastKeypoints: FootKeypoints? = null
    private var missedFrames = 0

    fun detectAsync(imageProxy: ImageProxy, @Suppress("UNUSED_PARAMETER") timestampMs: Long) {
        val bitmap = imageProxy.toBitmap()
            .rotateBitmap(imageProxy.imageInfo.rotationDegrees.toFloat())
        imageProxy.close()

        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val result = detectFeet(mat, w, h)
        handleResult(result)
    }

    private fun handleResult(keypoints: FootKeypoints?) {
        if (keypoints != null && keypoints.isValid()) {
            lastKeypoints = keypoints
            missedFrames  = 0
            onResult(keypoints)
        } else {
            missedFrames++
            // Hold the last valid frame for up to MAX_MISSED frames before hiding the shoe
            onResult(if (missedFrames <= MAX_MISSED) lastKeypoints else null)
            if (missedFrames > MAX_MISSED) lastKeypoints = null
        }
    }

    // ── Core detection ────────────────────────────────────────────────────────

    private fun detectFeet(mat: Mat, w: Float, h: Float): FootKeypoints? {
        val skinMask = buildSkinMask(mat)

        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            skinMask, contours, Mat(),
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE,
        )

        // Foot must be at least MIN_AREA_FRACTION of the image to filter noise
        val minArea = w * h * MIN_AREA_FRACTION
        val footContours = contours
            .filter { Imgproc.contourArea(it) >= minArea }
            .sortedByDescending { Imgproc.contourArea(it) }
            .take(2)

        if (footContours.isEmpty()) return null

        val feet = footContours.mapNotNull { contourToHeelToe(it, w, h) }
        if (feet.isEmpty()) return null

        return buildKeypoints(feet)
    }

    private fun buildSkinMask(src: Mat): Mat {
        // YCrCb skin segmentation — lighting-robust compared to HSV/RGB thresholds
        val ycrcb = Mat()
        Imgproc.cvtColor(src, ycrcb, Imgproc.COLOR_RGB2YCrCb)

        val mask = Mat()
        Core.inRange(
            ycrcb,
            Scalar(0.0, 133.0, 77.0),   // Cr 133–173, Cb 77–127  (standard skin range)
            Scalar(255.0, 173.0, 127.0),
            mask,
        )

        // Open: remove small noise blobs; Close: fill holes in skin region
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(9.0, 9.0))
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN,  kernel)
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)

        return mask
    }

    /**
     * Extract normalised heel and toe PointF from a contour.
     * Uses the bounding rectangle; the long axis determines heel↔toe direction.
     * Returns null if the contour is too small or too square (likely noise).
     */
    private fun contourToHeelToe(contour: MatOfPoint, w: Float, h: Float): Pair<PointF, PointF>? {
        val rect = Imgproc.boundingRect(contour)
        if (rect.width < MIN_DIM_PX || rect.height < MIN_DIM_PX) return null

        // Reject blobs that are nearly square — feet have a clear long axis
        val aspect = maxOf(rect.width, rect.height).toFloat() / minOf(rect.width, rect.height)
        if (aspect < MIN_ASPECT) return null

        val isVertical = rect.height >= rect.width
        val heel: PointF
        val toe: PointF

        if (isVertical) {
            val cx = (rect.x + rect.width  / 2f) / w
            // Bottom of bounding box = heel (foot hanging down or lying with heel low)
            heel = PointF(cx, (rect.y + rect.height).toFloat() / h)
            toe  = PointF(cx,  rect.y.toFloat()                / h)
        } else {
            val cy = (rect.y + rect.height / 2f) / h
            // Left edge = heel end (arbitrary; yaw calculation handles actual direction)
            heel = PointF( rect.x.toFloat()                / w, cy)
            toe  = PointF((rect.x + rect.width).toFloat() / w, cy)
        }

        return Pair(heel, toe)
    }

    private fun buildKeypoints(feet: List<Pair<PointF, PointF>>): FootKeypoints {
        return when (feet.size) {
            1 -> {
                val (heel, toe) = feet[0]
                FootKeypoints(
                    leftAnkle  = heel, leftHeel  = heel, leftToe  = toe,
                    rightAnkle = null, rightHeel = null, rightToe = null,
                )
            }
            else -> {
                // Sort by x: smaller x = left foot in portrait frame
                val sorted = feet.sortedBy { it.first.x }
                val (lh, lt) = sorted[0]
                val (rh, rt) = sorted[1]
                FootKeypoints(
                    leftAnkle  = lh, leftHeel  = lh, leftToe  = lt,
                    rightAnkle = rh, rightHeel = rh, rightToe = rt,
                )
            }
        }
    }

    fun close() { /* no resources to release */ }

    private fun Bitmap.rotateBitmap(degrees: Float): Bitmap {
        if (degrees == 0f) return this
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    companion object {
        private const val MIN_AREA_FRACTION = 0.04  // foot ≥ 4 % of image area
        private const val MIN_DIM_PX        = 40    // bounding-box side ≥ 40 px
        private const val MIN_ASPECT        = 1.4f  // long-axis / short-axis ratio
        private const val MAX_MISSED        = 10    // frames to hold last position on miss
    }
}
