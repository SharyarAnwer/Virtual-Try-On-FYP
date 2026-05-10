package com.fyp.virtualtryon.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentType
import kotlinx.coroutines.flow.Flow

@Dao
interface GarmentDao {

    @Query("SELECT * FROM garments")
    fun getAllGarments(): Flow<List<Garment>>

    @Query("SELECT * FROM garments WHERE type = :type")
    fun getGarmentsByType(type: GarmentType): Flow<List<Garment>>

    @Query("SELECT * FROM garments WHERE id = :id")
    suspend fun getGarmentById(id: Long): Garment?

    @Query("SELECT COUNT(*) FROM garments")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM garments WHERE type = :type")
    suspend fun countByType(type: GarmentType): Int

    @Query("SELECT COUNT(*) FROM garments WHERE imageAssetPath = :path")
    suspend fun countByImagePath(path: String): Int

    @Query("SELECT COUNT(*) FROM garments WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarments(vararg garments: Garment)

    @Query("DELETE FROM garments WHERE id = :id")
    suspend fun deleteGarment(id: Long)
}
