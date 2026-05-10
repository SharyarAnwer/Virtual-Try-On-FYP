package com.fyp.virtualtryon.ui.recommendation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.data.repository.GarmentRepository
import com.fyp.virtualtryon.recommendation.RecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val garmentRepository: GarmentRepository,
) : ViewModel() {

    val results = MutableLiveData<List<Garment>>()
    val navigateToResults = MutableLiveData(false)

    fun generateRecommendation(
        gender: String,
        height: String,
        bodyType: String,
        colors: List<String>,
    ) {
        viewModelScope.launch {
            val heightCm = height.toFloatOrNull() ?: 170f
            val shirts = garmentRepository.getGarmentsByType(GarmentType.SHIRT).first()
            val recommended = RecommendationEngine.recommend(shirts, gender, heightCm, bodyType, colors)
            results.value = recommended
            navigateToResults.value = true
        }
    }

    fun onNavigatedToResults() {
        navigateToResults.value = false
    }
}
