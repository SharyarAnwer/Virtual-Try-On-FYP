package com.fyp.virtualtryon.data.model

/**
 * Live body measurements derived from MediaPipe keypoints each frame.
 * All values are in normalised image coordinates (0..1) unless noted.
 */
data class BodyMeasurements(
    // Shoulder width in pixels (left shoulder x → right shoulder x)
    val shoulderWidthPx: Float,
    // Hip width in pixels
    val hipWidthPx: Float,
    // Torso height in pixels (shoulder midpoint y → hip midpoint y)
    val torsoHeightPx: Float,
    // Inseam in pixels (hip midpoint y → ankle midpoint y)
    val inseamPx: Float,
    // Face height in pixels (nose y → chin estimate)
    val faceHeightPx: Float,
    // Image width and height — used to convert to real-world estimates
    val imageWidthPx: Int,
    val imageHeightPx: Int,
)
