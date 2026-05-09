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
}
