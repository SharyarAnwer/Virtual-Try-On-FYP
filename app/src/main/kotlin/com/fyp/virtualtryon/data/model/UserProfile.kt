package com.fyp.virtualtryon.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,     // single-row table — always ID 1
    val name: String = "",
    val gender: GarmentGender = GarmentGender.UNISEX,
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val bodyType: BodyType = BodyType.REGULAR,
    val preferredColors: String = "",   // comma-separated color names
    val preferredGarmentTypes: String = "", // comma-separated GarmentType names
)
