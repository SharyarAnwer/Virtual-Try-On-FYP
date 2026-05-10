package com.fyp.virtualtryon.warning

import com.fyp.virtualtryon.data.model.BodyMeasurements
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.data.model.UserProfile

enum class FitWarning {
    TOO_NARROW,   // garment too narrow for body
    TOO_WIDE,     // garment too wide
    TOO_SHORT,    // garment too short
    TOO_LONG,     // garment too long
    GOOD_FIT,
}

data class FitResult(
    val warning: FitWarning,
    val message: String,
)

/**
 * Compares measured body proportions against garment dimensions and
 * the user's declared height/weight to produce a fit warning.
 *
 * Uses the user's height as a scale reference: the ratio of shoulder
 * width in pixels to the full-body pixel height gives us a rough
 * real-world shoulder width (shoulder_px / body_px * height_cm).
 */
object FitWarningEngine {

    fun evaluate(
        measurements: BodyMeasurements,
        garment: Garment,
        profile: UserProfile,
    ): FitResult {
        return when (garment.type) {
            GarmentType.SHIRT   -> evaluateShirt(measurements, garment, profile)
            GarmentType.PANTS   -> evaluatePants(measurements, garment, profile)
            GarmentType.GLASSES, GarmentType.GLASSES_V2 -> FitResult(FitWarning.GOOD_FIT, "Accessories always fit!")
            GarmentType.SHOES   -> FitResult(FitWarning.GOOD_FIT, "Select your shoe size for best results.")
        }
    }

    private fun evaluateShirt(
        m: BodyMeasurements,
        garment: Garment,
        profile: UserProfile,
    ): FitResult {
        // Estimate real-world shoulder width using body pixel height as scale
        val bodyHeightPx = m.torsoHeightPx + m.inseamPx  // approximate full body
        if (bodyHeightPx <= 0f) return FitResult(FitWarning.GOOD_FIT, "Pose unclear — try again.")

        val pxPerCm = bodyHeightPx / profile.heightCm
        val estimatedShoulderCm = m.shoulderWidthPx / pxPerCm
        val estimatedTorsoCm    = m.torsoHeightPx    / pxPerCm

        val widthRatio  = garment.widthCm  / estimatedShoulderCm
        val heightRatio = garment.heightCm / estimatedTorsoCm

        return when {
            widthRatio < 0.85f  -> FitResult(FitWarning.TOO_NARROW,
                "This shirt may be too narrow for your shoulders.")
            widthRatio > 1.30f  -> FitResult(FitWarning.TOO_WIDE,
                "This shirt may be too loose for your body.")
            heightRatio < 0.80f -> FitResult(FitWarning.TOO_SHORT,
                "This shirt may be too short for your torso.")
            heightRatio > 1.35f -> FitResult(FitWarning.TOO_LONG,
                "This shirt may be too long for your torso.")
            else                -> FitResult(FitWarning.GOOD_FIT,
                "This shirt looks like a great fit!")
        }
    }

    private fun evaluatePants(
        m: BodyMeasurements,
        garment: Garment,
        profile: UserProfile,
    ): FitResult {
        val bodyHeightPx = m.torsoHeightPx + m.inseamPx
        if (bodyHeightPx <= 0f) return FitResult(FitWarning.GOOD_FIT, "Full body not visible — step back.")

        val pxPerCm = bodyHeightPx / profile.heightCm
        val estimatedHipCm    = m.hipWidthPx    / pxPerCm
        val estimatedInseamCm = m.inseamPx      / pxPerCm

        val widthRatio  = garment.widthCm  / estimatedHipCm
        val heightRatio = garment.heightCm / estimatedInseamCm

        return when {
            widthRatio < 0.85f  -> FitResult(FitWarning.TOO_NARROW, "These pants may be too tight around the hips.")
            widthRatio > 1.30f  -> FitResult(FitWarning.TOO_WIDE,   "These pants may be too loose around the hips.")
            heightRatio < 0.80f -> FitResult(FitWarning.TOO_SHORT,  "These pants may be too short for your legs.")
            heightRatio > 1.35f -> FitResult(FitWarning.TOO_LONG,   "These pants may be too long — consider a hem.")
            else                -> FitResult(FitWarning.GOOD_FIT,   "These pants look like a great fit!")
        }
    }
}
