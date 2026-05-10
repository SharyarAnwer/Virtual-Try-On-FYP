package com.fyp.virtualtryon.shoes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.data.repository.GarmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShoesViewModel @Inject constructor(
    garmentRepository: GarmentRepository,
) : ViewModel() {

    val shoesFlow = garmentRepository.getGarmentsByType(GarmentType.SHOES).asLiveData()

    private val _selectedGarment = MutableLiveData<Garment?>()
    val selectedGarment: LiveData<Garment?> = _selectedGarment

    fun selectGarment(garment: Garment) {
        _selectedGarment.value = garment
    }
}
