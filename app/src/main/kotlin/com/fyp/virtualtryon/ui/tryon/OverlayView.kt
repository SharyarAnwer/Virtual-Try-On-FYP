package com.fyp.virtualtryon.ui.tryon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.pose.BodyKeypoints
import com.fyp.virtualtryon.pose.FaceKeypoints
import kotlin.math.atan2

/**
 * Transparent overlay drawn on top of the camera preview.
 *
 * - Draws debug skeleton (toggleable via [showSkeleton])
 * - Draws the currently-selected garment bitmap warped/scaled/rotated
 *   to match the user's body using pose keypoints
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var keypoints: BodyKeypoints? = null
    private var faceKeypoints: FaceKeypoints? = null
    private var garmentBitmap: Bitmap? = null
    private var garmentType: GarmentType? = null

    /** Set true to mirror x-coordinate (front camera). */
    var mirrorHorizontal: Boolean = true

    /** Set true to draw debug skeleton; false to hide for production. */
    var showSkeleton: Boolean = true

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 255, 0)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val garmentPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun updateKeypoints(kp: BodyKeypoints?) {
        keypoints = kp
        postInvalidate()
    }

    fun updateFaceKeypoints(fk: FaceKeypoints?) {
        faceKeypoints = fk
        postInvalidate()
    }

    fun updateGarment(bitmap: Bitmap?, type: GarmentType?) {
        garmentBitmap = bitmap
        garmentType   = type
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // GLASSES_V2 uses FaceLandmarker — body keypoints not needed
        if (garmentType == GarmentType.GLASSES_V2) {
            val fk = faceKeypoints
            if (fk != null && fk.isValid()) {
                garmentBitmap?.let { drawGlassesV2Garment(canvas, fk, it) }
            }
            if (showSkeleton) faceKeypoints?.let { drawFaceDebug(canvas, it) }
            return
        }

        // All other garments require body keypoints
        val kp = keypoints ?: return
        val bitmap = garmentBitmap
        if (bitmap != null) {
            val canDraw = if (garmentType == GarmentType.GLASSES)
                kp.leftEye != null && kp.rightEye != null
            else
                kp.isValid()
            if (canDraw) drawGarment(canvas, kp, bitmap, garmentType)
        }
        if (showSkeleton && kp.isValid()) drawSkeleton(canvas, kp)
    }

    private fun drawGarment(canvas: Canvas, kp: BodyKeypoints, bitmap: Bitmap, type: GarmentType?) {
        val w = width.toFloat()
        val h = height.toFloat()

        fun mapX(nx: Float) = if (mirrorHorizontal) (1f - nx) * w else nx * w
        fun mapY(ny: Float) = ny * h

        when (type) {
            GarmentType.SHIRT, null  -> drawUpperBodyGarment(canvas, kp, bitmap, ::mapX, ::mapY)
            GarmentType.GLASSES      -> drawGlassesGarment(canvas, kp, bitmap, ::mapX, ::mapY)
            GarmentType.PANTS, GarmentType.SHOES -> drawLowerBodyGarment(canvas, kp, bitmap, ::mapX, ::mapY)
            GarmentType.GLASSES_V2   -> { /* handled separately via faceKeypoints in onDraw */ }
        }
    }

    private fun drawUpperBodyGarment(
        canvas: Canvas, kp: BodyKeypoints, bitmap: Bitmap,
        mapX: (Float) -> Float, mapY: (Float) -> Float,
    ) {
        val ls = kp.leftShoulder ?: return
        val rs = kp.rightShoulder ?: return
        val lh = kp.leftHip ?: return
        val rh = kp.rightHip ?: return

        val lsX = mapX(ls.x()); val lsY = mapY(ls.y())
        val rsX = mapX(rs.x()); val rsY = mapY(rs.y())
        val lhX = mapX(lh.x()); val lhY = mapY(lh.y())
        val rhX = mapX(rh.x()); val rhY = mapY(rh.y())

        val shoulderMidX = (lsX + rsX) / 2f
        val shoulderMidY = (lsY + rsY) / 2f
        val hipMidX      = (lhX + rhX) / 2f
        val hipMidY      = (lhY + rhY) / 2f

        // Center of the torso — where we anchor the centre of the garment
        val torsoCenterX = (shoulderMidX + hipMidX) / 2f
        val torsoCenterY = (shoulderMidY + hipMidY) / 2f

        val shoulderWidth = kotlin.math.abs(rsX - lsX)
        val torsoHeight   = kotlin.math.abs(hipMidY - shoulderMidY).coerceAtLeast(1f)

        val targetWidth   = shoulderWidth * 1.7f
        val targetHeight  = torsoHeight * 1.4f

        // Tilt: shoulder line angle (left → right shoulder)
        val angleDeg = Math.toDegrees(
            atan2((rsY - lsY).toDouble(), (rsX - lsX).toDouble())
        ).toFloat()

        val scaleX = targetWidth / bitmap.width
        val scaleY = targetHeight / bitmap.height

        // Step 1: scale around (0,0) → bitmap occupies (0,0)-(W*sx, H*sy)
        // Step 2: translate so bitmap CENTER is at origin (0,0)
        // Step 3: rotate around origin
        // Step 4: translate origin to torso center
        val matrix = Matrix().apply {
            postScale(scaleX, scaleY)
            postTranslate(-(bitmap.width * scaleX) / 2f, -(bitmap.height * scaleY) / 2f)
            postRotate(angleDeg)
            postTranslate(torsoCenterX, torsoCenterY)
        }
        canvas.drawBitmap(bitmap, matrix, garmentPaint)
    }

    private fun drawLowerBodyGarment(
        canvas: Canvas, kp: BodyKeypoints, bitmap: Bitmap,
        mapX: (Float) -> Float, mapY: (Float) -> Float,
    ) {
        val lh = kp.leftHip ?: return
        val rh = kp.rightHip ?: return
        val la = kp.leftAnkle ?: return
        val ra = kp.rightAnkle ?: return

        val lhX = mapX(lh.x()); val lhY = mapY(lh.y())
        val rhX = mapX(rh.x()); val rhY = mapY(rh.y())
        val laY = mapY(la.y())
        val raY = mapY(ra.y())

        val hipMidX = (lhX + rhX) / 2f
        val hipMidY = (lhY + rhY) / 2f
        val ankleMidY = (laY + raY) / 2f

        val hipWidth = kotlin.math.abs(rhX - lhX)
        val legHeight = kotlin.math.abs(ankleMidY - hipMidY).coerceAtLeast(1f)

        val targetWidth = hipWidth * 1.5f
        val targetHeight = legHeight * 1.05f

        // Anchor center-X at hip mid, top-Y at hip mid (pants extend downward)
        val centerX = hipMidX
        val centerY = hipMidY + targetHeight / 2f

        val scaleX = targetWidth / bitmap.width
        val scaleY = targetHeight / bitmap.height

        val matrix = Matrix().apply {
            postScale(scaleX, scaleY)
            postTranslate(-(bitmap.width * scaleX) / 2f, -(bitmap.height * scaleY) / 2f)
            postTranslate(centerX, centerY)
        }
        canvas.drawBitmap(bitmap, matrix, garmentPaint)
    }

    private fun drawGlassesGarment(
        canvas: Canvas, kp: BodyKeypoints, bitmap: Bitmap,
        mapX: (Float) -> Float, mapY: (Float) -> Float,
    ) {
        // Require at least the pupil centres
        val le = kp.leftEye  ?: return
        val re = kp.rightEye ?: return

        // Outer eye corners give the true lens-edge-to-lens-edge span.
        // Fall back to pupil centres if outer corners aren't detected.
        val leftRef  = kp.leftEyeOuter  ?: le
        val rightRef = kp.rightEyeOuter ?: re

        val lrX = mapX(leftRef.x());  val lrY = mapY(leftRef.y())
        val rrX = mapX(rightRef.x()); val rrY = mapY(rightRef.y())
        val leX = mapX(le.x());       val leY = mapY(le.y())
        val reX = mapX(re.x());       val reY = mapY(re.y())

        // Anchor = midpoint between the two pupil centres
        val eyeMidX = (leX + reX) / 2f
        val eyeMidY = (leY + reY) / 2f

        // Width = outer-corner-to-outer-corner × 1.5
        // (frames extend ~25% beyond each outer corner on both sides)
        val outerDist = kotlin.math.sqrt(
            ((rrX - lrX) * (rrX - lrX) + (rrY - lrY) * (rrY - lrY)).toDouble()
        ).toFloat().coerceAtLeast(1f)
        val targetWidth = outerDist * 1.5f

        // Height derived from the image aspect ratio — no stretching
        val targetHeight = targetWidth * (bitmap.height.toFloat() / bitmap.width.toFloat())

        // Tilt: angle of the line connecting the two outer eye corners
        val angleDeg = Math.toDegrees(
            atan2((rrY - lrY).toDouble(), (rrX - lrX).toDouble())
        ).toFloat()

        val scaleX = targetWidth / bitmap.width
        val scaleY = targetHeight / bitmap.height

        // Place bitmap centre at eye-midpoint, then shift UP by half the frame height
        // so the lenses sit over the eyes rather than below them.
        val yAnchor = eyeMidY - targetHeight * 0.15f

        val matrix = Matrix().apply {
            postScale(scaleX, scaleY)
            postTranslate(-(bitmap.width * scaleX) / 2f, -(bitmap.height * scaleY) / 2f)
            postRotate(angleDeg)
            postTranslate(eyeMidX, yAnchor)
        }
        canvas.drawBitmap(bitmap, matrix, garmentPaint)
    }

    /**
     * Glasses V2 — uses FaceLandmarker landmarks for precise eye coverage.
     *
     * Width  = outer-eye-corner to outer-eye-corner × 1.35
     *          (frames extend ~17% past each outer corner on both sides)
     * Height = actual eye-opening height × 3.5
     *          (glasses lens is ~3.5× the height of the open eye)
     * Anchor = midpoint between the two iris centres (or eye centres as fallback)
     * Tilt   = angle of the outer-corner-to-outer-corner line
     */
    private fun drawGlassesV2Garment(canvas: Canvas, fk: FaceKeypoints, bitmap: Bitmap) {
        val w = width.toFloat()
        val h = height.toFloat()

        fun mapX(nx: Float) = if (mirrorHorizontal) (1f - nx) * w else nx * w
        fun mapY(ny: Float) = ny * h

        val rOuter = fk.rightEyeOuter ?: return
        val lOuter = fk.leftEyeOuter  ?: return
        val rTop   = fk.rightEyeTop   ?: return
        val rBot   = fk.rightEyeBottom ?: return
        val lTop   = fk.leftEyeTop    ?: return
        val lBot   = fk.leftEyeBottom  ?: return

        val roX = mapX(rOuter.x()); val roY = mapY(rOuter.y())
        val loX = mapX(lOuter.x()); val loY = mapY(lOuter.y())

        // Anchor: midpoint between iris centres (fallback: outer-corner midpoint)
        val rIris = fk.rightIris
        val lIris = fk.leftIris
        val anchorX: Float
        val anchorY: Float
        if (rIris != null && lIris != null) {
            anchorX = (mapX(rIris.x()) + mapX(lIris.x())) / 2f
            anchorY = (mapY(rIris.y()) + mapY(lIris.y())) / 2f
        } else {
            anchorX = (roX + loX) / 2f
            anchorY = (roY + loY) / 2f
        }

        // Width: outer-corner-to-outer-corner × 2.2
        // Outer corners span ~40% of face width; real frames reach ~88% → factor ≈ 2.2
        val outerDist = kotlin.math.sqrt(
            ((loX - roX) * (loX - roX) + (loY - roY) * (loY - roY)).toDouble()
        ).toFloat().coerceAtLeast(1f)
        val targetWidth = outerDist * 3.1f

        // Height locked to the garment's own aspect ratio — no squashing
        val targetHeight = targetWidth * (bitmap.height.toFloat() / bitmap.width)

        // Tilt: angle from LEFT outer corner → RIGHT outer corner (loX < roX after mirroring).
        // Going left→right gives ~0° for a level face; right→left would give ~180° (inverted).
        val angleDeg = Math.toDegrees(
            atan2((roY - loY).toDouble(), (roX - loX).toDouble())
        ).toFloat()

        val scaleX = targetWidth / bitmap.width
        val scaleY = targetHeight / bitmap.height

        // Fine-tune placement: slight rightward nudge + modest upward lift
        val xAnchor = anchorX + targetWidth * 0.01f
        val yAnchor = anchorY - targetHeight * 0.05f

        val matrix = Matrix().apply {
            postScale(scaleX, scaleY)
            postTranslate(-(bitmap.width * scaleX) / 2f, -(bitmap.height * scaleY) / 2f)
            postRotate(angleDeg)
            postTranslate(xAnchor, yAnchor)
        }
        canvas.drawBitmap(bitmap, matrix, garmentPaint)
    }

    private fun drawFaceDebug(canvas: Canvas, fk: FaceKeypoints) {
        val w = width.toFloat()
        val h = height.toFloat()
        fun mapX(nx: Float) = if (mirrorHorizontal) (1f - nx) * w else nx * w
        fun mapY(ny: Float) = ny * h

        // 16-point contour indices for each eye (MediaPipe FaceLandmarker)
        val rightEyeContour = listOf(33,7,163,144,145,153,154,155,133,173,157,158,159,160,161,246)
        val leftEyeContour  = listOf(362,382,381,380,374,373,390,249,263,466,388,387,386,385,384,398)

        // Iris centre indices
        val irisIndices = listOf(468, 473)

        val eyeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 0, 255, 0)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        // Draw connected ovals for each eye contour
        for (contour in listOf(rightEyeContour, leftEyeContour)) {
            val pts = contour.mapNotNull { fk.landmarks.getOrNull(it) }
            if (pts.size < 2) continue
            val path = android.graphics.Path()
            path.moveTo(mapX(pts[0].x()), mapY(pts[0].y()))
            for (i in 1 until pts.size) path.lineTo(mapX(pts[i].x()), mapY(pts[i].y()))
            path.close()
            canvas.drawPath(path, eyeLinePaint)
            // Draw each contour point
            for (pt in pts) canvas.drawCircle(mapX(pt.x()), mapY(pt.y()), 5f, dotPaint)
        }

        // Draw iris centres in a different colour
        val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.CYAN
            style = Paint.Style.FILL
        }
        for (idx in irisIndices) {
            val lm = fk.landmarks.getOrNull(idx) ?: continue
            canvas.drawCircle(mapX(lm.x()), mapY(lm.y()), 8f, irisPaint)
        }
    }

    private fun drawSkeleton(canvas: Canvas, kp: BodyKeypoints) {
        val w = width.toFloat()
        val h = height.toFloat()

        fun mapX(nx: Float) = if (mirrorHorizontal) (1f - nx) * w else nx * w
        fun mapY(ny: Float) = ny * h

        // Bones (pairs of landmark indices we want connected)
        val bones = listOf(
            11 to 12,  // shoulder line
            11 to 13, 13 to 15,  // left arm
            12 to 14, 14 to 16,  // right arm
            11 to 23, 12 to 24,  // torso sides
            23 to 24,            // hip line
            23 to 25, 25 to 27, 27 to 31,  // left leg + foot
            24 to 26, 26 to 28, 28 to 32,  // right leg + foot
        )

        for ((aIdx, bIdx) in bones) {
            val a = kp.landmarks.getOrNull(aIdx) ?: continue
            val b = kp.landmarks.getOrNull(bIdx) ?: continue
            canvas.drawLine(mapX(a.x()), mapY(a.y()), mapX(b.x()), mapY(b.y()), linePaint)
        }

        // Dots for every landmark
        for (lm in kp.landmarks) {
            canvas.drawCircle(mapX(lm.x()), mapY(lm.y()), 8f, dotPaint)
        }
    }
}
