package com.fyp.virtualtryon.recommendation

import com.fyp.virtualtryon.data.model.BodyType
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentGender
import com.fyp.virtualtryon.data.model.UserProfile

/**
 * Lightweight rule-based recommendation engine.
 *
 * Scoring approach:
 *   +3  gender match
 *   +2  body-type match
 *   +1  per preferred color match
 *   +1  per preferred garment type match
 *
 * Returns garments sorted by score (highest first), filtering out
 * items with score 0 when the catalogue has enough options.
 */
object RecommendationEngine {

    data class ScoredGarment(val garment: Garment, val score: Int)

    fun recommend(
        allGarments: List<Garment>,
        profile: UserProfile,
        topN: Int = 10,
    ): List<Garment> {
        if (allGarments.isEmpty()) return emptyList()

        val preferredColors = profile.preferredColors
            .split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val preferredTypes  = profile.preferredGarmentTypes
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val scored = allGarments.map { garment ->
            var score = 0

            // Gender match
            val suitableGenders = garment.suitableGenders.split(",").map { it.trim() }
            if (GarmentGender.UNISEX.name in suitableGenders ||
                profile.gender.name in suitableGenders) score += 3

            // Body type match
            val suitableBodyTypes = garment.suitableBodyTypes.split(",").map { it.trim() }
            if (profile.bodyType.name in suitableBodyTypes) score += 2

            // Color preference match
            if (preferredColors.any { it.equals(garment.color, ignoreCase = true) }) score += 1

            // Garment type preference match
            if (garment.type.name in preferredTypes) score += 1

            ScoredGarment(garment, score)
        }

        return scored
            .sortedByDescending { it.score }
            .take(topN)
            .map { it.garment }
    }

    /**
     * Returns a human-readable explanation for why a garment was recommended.
     */
    fun explainRecommendation(garment: Garment, profile: UserProfile): String {
        val reasons = mutableListOf<String>()

        val suitableBodyTypes = garment.suitableBodyTypes.split(",").map { it.trim() }
        if (profile.bodyType.name in suitableBodyTypes) {
            reasons += "suits your ${profile.bodyType.name.lowercase()} body type"
        }

        val preferredColors = profile.preferredColors
            .split(",").map { it.trim().lowercase() }
        if (preferredColors.any { it.equals(garment.color, ignoreCase = true) }) {
            reasons += "matches your color preference"
        }

        if (reasons.isEmpty()) reasons += "popular choice"
        return reasons.joinToString(" and ").replaceFirstChar { it.uppercase() }
    }
}
