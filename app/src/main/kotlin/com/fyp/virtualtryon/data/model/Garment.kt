package com.fyp.virtualtryon.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GarmentType { SHIRT, PANTS, GLASSES, SHOES }
enum class GarmentGender { MALE, FEMALE, UNISEX }
enum class BodyType { SLIM, REGULAR, LARGE }

@Entity(tableName = "garments")
data class Garment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: GarmentType,
    val gender: GarmentGender,
    val color: String,               // e.g. "blue", "red"
    val imageAssetPath: String,      // path inside assets/, e.g. "garments/shirt_blue.png"
    val widthCm: Float,              // garment physical width (shoulders for shirt, waist for pants)
    val heightCm: Float,             // garment physical height
    val sizeLabel: String,           // "S", "M", "L", "XL"
    val suitableBodyTypes: String,   // comma-separated BodyType names
    val suitableGenders: String,     // comma-separated GarmentGender names
)
