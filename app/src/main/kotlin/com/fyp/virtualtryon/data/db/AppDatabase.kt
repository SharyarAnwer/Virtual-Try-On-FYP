package com.fyp.virtualtryon.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fyp.virtualtryon.data.dao.GarmentDao
import com.fyp.virtualtryon.data.dao.UserProfileDao
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.UserProfile

@Database(
    entities = [Garment::class, UserProfile::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun garmentDao(): GarmentDao
    abstract fun userProfileDao(): UserProfileDao
}
