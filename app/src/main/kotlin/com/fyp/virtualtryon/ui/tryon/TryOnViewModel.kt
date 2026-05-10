package com.fyp.virtualtryon.ui.tryon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fyp.virtualtryon.data.model.BodyMeasurements
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.data.repository.GarmentRepository
import com.fyp.virtualtryon.data.repository.UserProfileRepository
import com.fyp.virtualtryon.pose.BodyKeypoints
import com.fyp.virtualtryon.pose.FaceKeypoints
import com.fyp.virtualtryon.warning.FitResult
import com.fyp.virtualtryon.warning.FitWarning
import com.fyp.virtualtryon.warning.FitWarningEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TryOnViewModel @Inject constructor(
    private val garmentRepository: GarmentRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    val profile = userProfileRepository.getProfile().asLiveData()

    private val _selectedGarment = MutableLiveData<Garment?>()
    val selectedGarment: LiveData<Garment?> = _selectedGarment

    private val _currentKeypoints = MutableLiveData<BodyKeypoints?>()
    val currentKeypoints: LiveData<BodyKeypoints?> = _currentKeypoints

    private val _currentFaceKeypoints = MutableLiveData<FaceKeypoints?>()
    val currentFaceKeypoints: LiveData<FaceKeypoints?> = _currentFaceKeypoints

    private val _fitResult = MutableLiveData<FitResult>()
    val fitResult: LiveData<FitResult> = _fitResult

    private val _activeCategory = MutableLiveData(GarmentType.GLASSES_V2)
    val activeCategory: LiveData<GarmentType> = _activeCategory

    val shirtsFlow  = garmentRepository.getGarmentsByType(GarmentType.SHIRT).asLiveData()
    val pantsFlow   = garmentRepository.getGarmentsByType(GarmentType.PANTS).asLiveData()
    val glassesFlow = garmentRepository.getGarmentsByType(GarmentType.GLASSES).asLiveData()
    val shoesFlow   = garmentRepository.getGarmentsByType(GarmentType.SHOES).asLiveData()

    /** Garments to display in the thumbnail strip — switches with active category. */
    val displayedGarments: LiveData<List<Garment>> = MediatorLiveData<List<Garment>>().apply {
        fun refresh() {
            value = when (_activeCategory.value) {
                GarmentType.PANTS                       -> pantsFlow.value ?: emptyList()
                GarmentType.GLASSES, GarmentType.GLASSES_V2 -> glassesFlow.value ?: emptyList()
                GarmentType.SHOES                       -> shoesFlow.value ?: emptyList()
                else                                    -> shirtsFlow.value ?: emptyList()
            }
        }
        addSource(_activeCategory) { refresh() }
        addSource(shirtsFlow)      { refresh() }
        addSource(pantsFlow)       { refresh() }
        addSource(glassesFlow)     { refresh() }
        addSource(shoesFlow)       { refresh() }
    }

    fun selectGarment(garment: Garment) {
        _selectedGarment.value = garment
        evaluateFit(_currentKeypoints.value)
    }

    fun onKeypointsUpdated(keypoints: BodyKeypoints?) {
        // Called from MediaPipe's background thread — must use postValue()
        _currentKeypoints.postValue(keypoints)
        evaluateFit(keypoints)
    }

    fun onFaceKeypointsUpdated(keypoints: FaceKeypoints?) {
        _currentFaceKeypoints.postValue(keypoints)
    }

    fun setCategory(type: GarmentType) {
        _activeCategory.value = type
        _selectedGarment.value = null
    }

    private fun evaluateFit(kp: BodyKeypoints?) {
        val garment  = _selectedGarment.value ?: return
        val profile  = profile.value ?: return
        kp ?: return
        if (!kp.isValid()) return

        val frameW = 1f  // normalised; GarmentWarper uses this for pixel space
        val frameH = 1f

        // Build approximate pixel measurements from normalised coords
        // We treat the full image as 1000x1000 for ratio computation
        val scale = 1000f
        val ls = kp.leftShoulder!!; val rs = kp.rightShoulder!!
        val lh = kp.leftHip!!;     val rh = kp.rightHip!!
        val la = kp.leftAnkle;     val ra = kp.rightAnkle
        val le = kp.leftEye;       val re = kp.rightEye

        val measurements = BodyMeasurements(
            shoulderWidthPx = (rs.x() - ls.x()) * scale,
            hipWidthPx      = ((rh.x() - lh.x()).coerceAtLeast(0f)) * scale,
            torsoHeightPx   = ((lh.y() + rh.y()) / 2f - (ls.y() + rs.y()) / 2f) * scale,
            inseamPx        = if (la != null && ra != null)
                ((la.y() + ra.y()) / 2f - (lh.y() + rh.y()) / 2f) * scale else 0f,
            faceHeightPx    = if (le != null && re != null)
                ((ls.y() + rs.y()) / 2f - (le.y() + re.y()) / 2f) * scale else 0f,
            imageWidthPx    = scale.toInt(),
            imageHeightPx   = scale.toInt(),
        )

        _fitResult.postValue(FitWarningEngine.evaluate(measurements, garment, profile))
    }
}
