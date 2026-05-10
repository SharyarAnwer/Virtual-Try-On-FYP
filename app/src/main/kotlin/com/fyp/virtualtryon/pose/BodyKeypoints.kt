package com.fyp.virtualtryon.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

/**
 * Wraps the 33 MediaPipe pose landmarks with named accessors for the joints we use.
 * All coordinates are normalised 0..1 relative to the image frame.
 */
data class BodyKeypoints(val landmarks: List<NormalizedLandmark>) {

    // MediaPipe BlazePose landmark indices
    val nose: NormalizedLandmark?           get() = landmarks.getOrNull(0)
    val leftEyeInner: NormalizedLandmark?  get() = landmarks.getOrNull(1)
    val leftEye: NormalizedLandmark?       get() = landmarks.getOrNull(2)
    val leftEyeOuter: NormalizedLandmark?  get() = landmarks.getOrNull(3)
    val rightEyeInner: NormalizedLandmark? get() = landmarks.getOrNull(4)
    val rightEye: NormalizedLandmark?      get() = landmarks.getOrNull(5)
    val rightEyeOuter: NormalizedLandmark? get() = landmarks.getOrNull(6)
    val leftEar: NormalizedLandmark?       get() = landmarks.getOrNull(7)
    val rightEar: NormalizedLandmark?      get() = landmarks.getOrNull(8)
    val leftShoulder: NormalizedLandmark?  get() = landmarks.getOrNull(11)
    val rightShoulder: NormalizedLandmark? get() = landmarks.getOrNull(12)
    val leftElbow: NormalizedLandmark?     get() = landmarks.getOrNull(13)
    val rightElbow: NormalizedLandmark?    get() = landmarks.getOrNull(14)
    val leftWrist: NormalizedLandmark?     get() = landmarks.getOrNull(15)
    val rightWrist: NormalizedLandmark?    get() = landmarks.getOrNull(16)
    val leftHip: NormalizedLandmark?       get() = landmarks.getOrNull(23)
    val rightHip: NormalizedLandmark?      get() = landmarks.getOrNull(24)
    val leftKnee: NormalizedLandmark?      get() = landmarks.getOrNull(25)
    val rightKnee: NormalizedLandmark?     get() = landmarks.getOrNull(26)
    val leftAnkle: NormalizedLandmark?     get() = landmarks.getOrNull(27)
    val rightAnkle: NormalizedLandmark?    get() = landmarks.getOrNull(28)
    val leftHeel: NormalizedLandmark?      get() = landmarks.getOrNull(29)
    val rightHeel: NormalizedLandmark?     get() = landmarks.getOrNull(30)
    val leftFootIndex: NormalizedLandmark? get() = landmarks.getOrNull(31)
    val rightFootIndex: NormalizedLandmark? get() = landmarks.getOrNull(32)

    fun isValid(): Boolean = landmarks.size >= 33 &&
        leftShoulder != null && rightShoulder != null &&
        leftHip != null && rightHip != null

    /** Midpoint between left and right shoulders (normalised). */
    fun shoulderMidpoint(): Pair<Float, Float>? {
        val ls = leftShoulder ?: return null
        val rs = rightShoulder ?: return null
        return Pair((ls.x() + rs.x()) / 2f, (ls.y() + rs.y()) / 2f)
    }

    /** Midpoint between left and right hips (normalised). */
    fun hipMidpoint(): Pair<Float, Float>? {
        val lh = leftHip ?: return null
        val rh = rightHip ?: return null
        return Pair((lh.x() + rh.x()) / 2f, (lh.y() + rh.y()) / 2f)
    }

    /** Midpoint between left and right ankles (normalised). */
    fun ankleMidpoint(): Pair<Float, Float>? {
        val la = leftAnkle ?: return null
        val ra = rightAnkle ?: return null
        return Pair((la.x() + ra.x()) / 2f, (la.y() + ra.y()) / 2f)
    }

    /** Eye midpoint — used to anchor glasses. */
    fun eyeMidpoint(): Pair<Float, Float>? {
        val le = leftEye ?: return null
        val re = rightEye ?: return null
        return Pair((le.x() + re.x()) / 2f, (le.y() + re.y()) / 2f)
    }
}
