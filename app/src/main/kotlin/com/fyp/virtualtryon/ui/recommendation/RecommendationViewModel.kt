package com.fyp.virtualtryon.ui.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.repository.GarmentRepository
import com.fyp.virtualtryon.data.repository.UserProfileRepository
import com.fyp.virtualtryon.recommendation.RecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    garmentRepository: GarmentRepository,
    userProfileRepository: UserProfileRepository,
) : ViewModel() {

    val recommendations = combine(
        garmentRepository.getAllGarments(),
        userProfileRepository.getProfile()
    ) { garments, profile ->
        if (profile == null) emptyList()
        else RecommendationEngine.recommend(garments, profile)
    }.asLiveData()
}
