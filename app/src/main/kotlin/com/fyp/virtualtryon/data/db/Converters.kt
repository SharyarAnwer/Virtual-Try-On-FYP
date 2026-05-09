package com.fyp.virtualtryon.data.db

import androidx.room.TypeConverter
import com.fyp.virtualtryon.data.model.BodyType
import com.fyp.virtualtryon.data.model.GarmentGender
import com.fyp.virtualtryon.data.model.GarmentType

class Converters {
    @TypeConverter fun garmentTypeToString(v: GarmentType): String = v.name
    @TypeConverter fun stringToGarmentType(v: String): GarmentType = GarmentType.valueOf(v)

    @TypeConverter fun garmentGenderToString(v: GarmentGender): String = v.name
    @TypeConverter fun stringToGarmentGender(v: String): GarmentGender = GarmentGender.valueOf(v)

    @TypeConverter fun bodyTypeToString(v: BodyType): String = v.name
    @TypeConverter fun stringToBodyType(v: String): BodyType = BodyType.valueOf(v)
}
