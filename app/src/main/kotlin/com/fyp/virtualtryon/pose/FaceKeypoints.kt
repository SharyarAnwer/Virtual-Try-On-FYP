package com.fyp.virtualtryon.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

/**
 * Wraps MediaPipe FaceLandmarker's 478 face landmarks.
 * Exposes the specific points needed for glasses placement.
 *
 * MediaPipe face mesh index reference (same numbering in FaceLandmarker):
 *   Right eye outer corner : 33   Right eye inner corner : 133
 *   Right eye top          : 159  Right eye bottom       : 145
 *   Left eye outer corner  : 362  Left eye inner corner  : 263
 *   Left eye top           : 386  Left eye bottom        : 374
 *   Right iris centre      : 468  Left iris centre       : 473
 */
data class FaceKeypoints(val landmarks: List<NormalizedLandmark>) {

    // Right eye (person's right — appears on LEFT side of a mirrored selfie)
    val rightEyeOuter: NormalizedLandmark?  get() = landmarks.getOrNull(33)
    val rightEyeInner: NormalizedLandmark?  get() = landmarks.getOrNull(133)
    val rightEyeTop: NormalizedLandmark?    get() = landmarks.getOrNull(159)
    val rightEyeBottom: NormalizedLandmark? get() = landmarks.getOrNull(145)

    // Left eye (person's left — appears on RIGHT side of a mirrored selfie)
    val leftEyeOuter: NormalizedLandmark?   get() = landmarks.getOrNull(362)
    val leftEyeInner: NormalizedLandmark?   get() = landmarks.getOrNull(263)
    val leftEyeTop: NormalizedLandmark?     get() = landmarks.getOrNull(386)
    val leftEyeBottom: NormalizedLandmark?  get() = landmarks.getOrNull(374)

    // Iris centres — only present when FaceLandmarker outputs iris landmarks (478 total)
    val rightIris: NormalizedLandmark?      get() = landmarks.getOrNull(468)
    val leftIris: NormalizedLandmark?       get() = landmarks.getOrNull(473)

    fun isValid(): Boolean =
        landmarks.size >= 468 &&
        rightEyeOuter != null && leftEyeOuter != null &&
        rightEyeTop   != null && rightEyeBottom != null

    /**
     * Returns true when the face is roughly straight-on to the camera.
     *
     * Yaw  — nose tip x should sit ~halfway between the two outer eye corners.
     * Pitch — nose tip y should sit ~55 % of the way from forehead (10) to chin (152).
     * Thresholds are intentionally loose so the warning only fires on obvious tilts.
     */
    /**
     * Returns false only when the face is clearly turned left or right.
     *
     * Strategy: compare the apparent widths of both eyes.
     * When looking straight, both eyes appear roughly the same width.
     * When the face turns, the far eye becomes much narrower than the near eye.
     * This approach is robust to camera mirroring and individual face proportions.
     *
     * Warning fires only when one eye is less than 45 % the width of the other
     * — a clear, visible turn of ~30°+.
     */
    fun isLookingStraight(): Boolean {
        val rOuter = landmarks.getOrNull(33)  ?: return true  // right eye outer corner
        val rInner = landmarks.getOrNull(133) ?: return true  // right eye inner corner
        val lOuter = landmarks.getOrNull(362) ?: return true  // left  eye outer corner
        val lInner = landmarks.getOrNull(263) ?: return true  // left  eye inner corner

        val rWidth = kotlin.math.abs(rOuter.x() - rInner.x())
        val lWidth = kotlin.math.abs(lOuter.x() - lInner.x())

        if (rWidth <= 0f || lWidth <= 0f) return true
        val ratio = minOf(rWidth, lWidth) / maxOf(rWidth, lWidth)
        // ratio ≈ 1.0 → straight; ratio < 0.70 → one eye noticeably narrower → turned
        return ratio >= 0.70f
    }
}
