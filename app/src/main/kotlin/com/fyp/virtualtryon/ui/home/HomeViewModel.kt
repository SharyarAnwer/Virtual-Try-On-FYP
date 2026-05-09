package com.fyp.virtualtryon.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.data.repository.GarmentRepository
import com.fyp.virtualtryon.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    garmentRepository: GarmentRepository,
    userProfileRepository: UserProfileRepository,
) : ViewModel() {

    val shirts  = garmentRepository.getGarmentsByType(GarmentType.SHIRT).asLiveData()
    val pants   = garmentRepository.getGarmentsByType(GarmentType.PANTS).asLiveData()
    val glasses = garmentRepository.getGarmentsByType(GarmentType.GLASSES).asLiveData()
    val shoes   = garmentRepository.getGarmentsByType(GarmentType.SHOES).asLiveData()
    val profile = userProfileRepository.getProfile().asLiveData()
}
