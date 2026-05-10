package com.fyp.virtualtryon.shoes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Pose-landmarker based foot detector (backup). Requires the full body to be roughly visible.
 * Primary detector for shoes try-on is FootDetectorCV.
 */
class FootDetector(
    context: Context,
    private val onResult: (FootKeypoints?) -> Unit,
) {
    private val poseLandmarker: PoseLandmarker

    private var lastKeypoints: FootKeypoints? = null
    private var missedFrames = 0
    private val MAX_MISSED_FRAMES = 8

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.3f)
            .setMinPosePresenceConfidence(0.3f)
            .setMinTrackingConfidence(0.3f)
            .setResultListener(::handleResult)
            .setErrorListener { error -> error.printStackTrace() }
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun detectAsync(imageProxy: ImageProxy, timestampMs: Long) {
        val bitmap = imageProxy.toBitmap()
            .rotateBitmap(imageProxy.imageInfo.rotationDegrees.toFloat())
        val mpImage = BitmapImageBuilder(bitmap).build()
        poseLandmarker.detectAsync(mpImage, timestampMs)
        imageProxy.close()
    }

    private fun handleResult(
        result: PoseLandmarkerResult,
        @Suppress("UNUSED_PARAMETER") input: com.google.mediapipe.framework.image.MPImage,
    ) {
        val lm = result.landmarks().firstOrNull()
        if (lm != null) {
            missedFrames = 0
            lastKeypoints = FootKeypoints(
                leftAnkle  = lm.getOrNull(27)?.let { PointF(it.x(), it.y()) },
                leftHeel   = lm.getOrNull(29)?.let { PointF(it.x(), it.y()) },
                leftToe    = lm.getOrNull(31)?.let { PointF(it.x(), it.y()) },
                rightAnkle = lm.getOrNull(28)?.let { PointF(it.x(), it.y()) },
                rightHeel  = lm.getOrNull(30)?.let { PointF(it.x(), it.y()) },
                rightToe   = lm.getOrNull(32)?.let { PointF(it.x(), it.y()) },
            )
            onResult(lastKeypoints)
        } else {
            missedFrames++
            onResult(if (missedFrames <= MAX_MISSED_FRAMES) lastKeypoints else null)
            if (missedFrames > MAX_MISSED_FRAMES) lastKeypoints = null
        }
    }

    fun close() = poseLandmarker.close()
}

private fun Bitmap.rotateBitmap(degrees: Float): Bitmap {
    if (degrees == 0f) return this
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
