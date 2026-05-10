package com.fyp.virtualtryon.recommendation

import com.fyp.virtualtryon.data.model.Garment

object RecommendationEngine {

    fun recommend(
        allGarments: List<Garment>,
        gender: String,
        height: Float,
        bodyType: String,
        colors: List<String>,
    ): List<Garment> {
        val matchingSizes = shiftSizes(heightToSizes(height), bodyType)
        val catalogBodyType = bodyTypeToCatalog(bodyType)
        val preferredColors = colors.map { it.lowercase() }

        return allGarments
            .filter { garment ->
                val suitableGenders = garment.suitableGenders
                    .split(",").map { it.trim().uppercase() }
                gender.uppercase() in suitableGenders || "UNISEX" in suitableGenders
            }
            .filter { garment ->
                garment.sizeLabel in matchingSizes
            }
            .filter { garment ->
                val suitableBodyTypes = garment.suitableBodyTypes
                    .split(",").map { it.trim().uppercase() }
                catalogBodyType in suitableBodyTypes
            }
            .sortedBy { garment ->
                if (preferredColors.isEmpty() || garment.color.lowercase() in preferredColors) 0 else 1
            }
    }

    private val sizeOrder = listOf("XS", "S", "M", "L", "XL")

    private fun heightToSizes(heightCm: Float): Set<String> = when {
        heightCm < 160f -> setOf("XS", "S")
        heightCm < 170f -> setOf("S", "M")
        heightCm < 180f -> setOf("M", "L")
        else            -> setOf("L", "XL")
    }

    private fun shiftSizes(sizes: Set<String>, bodyType: String): Set<String> {
        val shift = when (bodyType.lowercase()) {
            "small"       -> -1
            "medium"      -> 0
            "large"       ->  1
            "extra-large" ->  2
            else          ->  0
        }
        if (shift == 0) return sizes
        return sizes.map { size ->
            val index = (sizeOrder.indexOf(size) + shift).coerceIn(0, sizeOrder.lastIndex)
            sizeOrder[index]
        }.toSet()
    }

    private fun bodyTypeToCatalog(bodyType: String): String = when (bodyType.lowercase()) {
        "small"       -> "SLIM"
        "medium"      -> "REGULAR"
        "large"       -> "LARGE"
        "extra-large" -> "LARGE"
        else          -> "REGULAR"
    }
}
