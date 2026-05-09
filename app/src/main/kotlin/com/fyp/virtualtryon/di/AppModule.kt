package com.fyp.virtualtryon.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fyp.virtualtryon.data.dao.GarmentDao
import com.fyp.virtualtryon.data.dao.UserProfileDao
import com.fyp.virtualtryon.data.db.AppDatabase
import com.fyp.virtualtryon.data.db.DatabaseSeeder
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
            override fun onCreate(sqLiteDb: SupportSQLiteDatabase) {
                // Seed garments from assets/garments_catalog.json on first install
                CoroutineScope(Dispatchers.IO).launch {
                    val garments = DatabaseSeeder.loadGarments(context)
                    db?.garmentDao()?.insertGarments(*garments.toTypedArray())
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
