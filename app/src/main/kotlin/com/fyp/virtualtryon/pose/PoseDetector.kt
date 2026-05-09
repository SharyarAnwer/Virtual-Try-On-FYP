package com.fyp.virtualtryon.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Wraps MediaPipe PoseLandmarker for live-stream use.
 * Drop a [PoseLandmarker] model file named "pose_landmarker_lite.task" into
 * src/main/assets/ (download from the MediaPipe Model Hub).
 */
class PoseDetector(
    context: Context,
    private val onResult: (BodyKeypoints?) -> Unit,
) {
    private val poseLandmarker: PoseLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener(::handleResult)
            .setErrorListener { error -> error.printStackTrace() }
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    /** Call from the CameraX ImageAnalysis executor thread. */
    fun detectAsync(imageProxy: ImageProxy, timestampMs: Long) {
        val bitmap = imageProxy.toBitmap().rotateBitmap(imageProxy.imageInfo.rotationDegrees.toFloat())
        val mpImage = BitmapImageBuilder(bitmap).build()
        poseLandmarker.detectAsync(mpImage, timestampMs)
        imageProxy.close()
    }

    private fun handleResult(result: PoseLandmarkerResult, @Suppress("UNUSED_PARAMETER") input: com.google.mediapipe.framework.image.MPImage) {
        val keypoints = result.landmarks().firstOrNull()
            ?.let { BodyKeypoints(it) }
        onResult(keypoints)
    }

    fun close() = poseLandmarker.close()
}

private fun Bitmap.rotateBitmap(degrees: Float): Bitmap {
    if (degrees == 0f) return this
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
