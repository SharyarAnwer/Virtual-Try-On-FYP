package com.fyp.virtualtryon.shoes

import android.graphics.PointF

/**
 * Normalized (0..1) foot landmark positions, independent of the detection backend.
 *
 * For pose-based detection, indices used from PoseLandmarker:
 *   27/28 = left/right ankle   29/30 = left/right heel   31/32 = left/right toe tip
 *
 * For CV-based detection, heel/toe are the two extremes of the foot contour along its long axis.
 */
data class FootKeypoints(
    val leftAnkle:  PointF?,
    val leftHeel:   PointF?,
    val leftToe:    PointF?,
    val rightAnkle: PointF?,
    val rightHeel:  PointF?,
    val rightToe:   PointF?,
) {
    fun hasLeftFoot()  = leftAnkle  != null && leftHeel  != null && leftToe  != null
    fun hasRightFoot() = rightAnkle != null && rightHeel != null && rightToe != null
    fun isValid()      = hasLeftFoot() || hasRightFoot()
}
