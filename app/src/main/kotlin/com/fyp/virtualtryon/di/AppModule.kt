package com.fyp.virtualtryon.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fyp.virtualtryon.data.dao.GarmentDao
import com.fyp.virtualtryon.data.dao.UserProfileDao
import com.fyp.virtualtryon.data.db.AppDatabase
import com.fyp.virtualtryon.data.db.DatabaseSeeder
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentGender
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.data.repository.GarmentRepository
import com.fyp.virtualtryon.data.repository.UserProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        var db: AppDatabase? = null
        val callback = object : RoomDatabase.Callback() {
            override fun onOpen(sqLiteDb: SupportSQLiteDatabase) {
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = db?.garmentDao() ?: return@launch
                    if (dao.count() == 0) {
                        val garments = DatabaseSeeder.loadGarments(context)
                        dao.insertGarments(*garments.toTypedArray())
                    }
                    scanShoes3dFolder(context, dao)
                }
            }
        }
        db = Room.databaseBuilder(context, AppDatabase::class.java, "virtualtryon.db")
            .fallbackToDestructiveMigration()
            .addCallback(callback)
            .build()
        return db
    }

    @Provides
    fun provideGarmentDao(db: AppDatabase): GarmentDao = db.garmentDao()

    @Provides
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    @Singleton
    fun provideGarmentRepository(dao: GarmentDao): GarmentRepository = GarmentRepository(dao)

    @Provides
    @Singleton
    fun provideUserProfileRepository(dao: UserProfileDao): UserProfileRepository =
        UserProfileRepository(dao)
}

/**
 * Scans assets/shoes_3d/ for PNG thumbnails and inserts any that are not yet in the DB.
 * Drop a PNG + matching GLB (same base name) into shoes_3d/ to have it appear automatically.
 */
private suspend fun scanShoes3dFolder(context: Context, dao: GarmentDao) {
    val files = try { context.assets.list("shoes_3d") ?: return } catch (e: Exception) { return }
    for (file in files) {
        if (!file.endsWith(".png", ignoreCase = true)) continue
        val assetPath = "shoes_3d/$file"
        if (dao.countByImagePath(assetPath) > 0) continue
        val displayName = file.removeSuffix(".png")
            .replace('_', ' ').trim().replaceFirstChar { it.uppercase() }
        dao.insertGarments(makeShoesEntry(displayName, assetPath))
    }
}

private fun makeShoesEntry(name: String, imageAssetPath: String) = Garment(
    name              = name,
    type              = GarmentType.SHOES,
    gender            = GarmentGender.UNISEX,
    color             = "custom",
    imageAssetPath    = imageAssetPath,
    widthCm           = 28f,
    heightCm          = 12f,
    sizeLabel         = "ONE SIZE",
    suitableBodyTypes = "SLIM,REGULAR,LARGE",
    suitableGenders   = "MALE,FEMALE,UNISEX",
)
