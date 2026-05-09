package com.fyp.virtualtryon.data.repository

import com.fyp.virtualtryon.data.dao.GarmentDao
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GarmentRepository @Inject constructor(private val dao: GarmentDao) {

    fun getAllGarments(): Flow<List<Garment>> = dao.getAllGarments()

    fun getGarmentsByType(type: GarmentType): Flow<List<Garment>> = dao.getGarmentsByType(type)

    suspend fun getGarmentById(id: Long): Garment? = dao.getGarmentById(id)

    suspend fun addGarment(garment: Garment) = dao.insertGarments(garment)

    suspend fun deleteGarment(id: Long) = dao.deleteGarment(id)
}
