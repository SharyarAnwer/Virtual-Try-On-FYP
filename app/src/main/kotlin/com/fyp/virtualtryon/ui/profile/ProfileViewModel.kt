package com.fyp.virtualtryon.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fyp.virtualtryon.data.model.BodyType
import com.fyp.virtualtryon.data.model.GarmentGender
import com.fyp.virtualtryon.data.model.UserProfile
import com.fyp.virtualtryon.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    val profile = userProfileRepository.getProfile().asLiveData()

    fun saveProfile(
        name: String,
        gender: GarmentGender,
        heightCm: Float,
        weightKg: Float,
        bodyType: BodyType,
        preferredColors: String,
    ) {
        viewModelScope.launch {
            userProfileRepository.saveProfile(
                UserProfile(
                    id = 1,
                    name = name,
                    gender = gender,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    bodyType = bodyType,
                    preferredColors = preferredColors,
                )
            )
        }
    }
}
