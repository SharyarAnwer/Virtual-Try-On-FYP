package com.fyp.virtualtryon.data.repository

import com.fyp.virtualtryon.data.dao.UserProfileDao
import com.fyp.virtualtryon.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserProfileRepository @Inject constructor(private val dao: UserProfileDao) {

    fun getProfile(): Flow<UserProfile?> = dao.getProfile()

    suspend fun saveProfile(profile: UserProfile) = dao.saveProfile(profile)
}
