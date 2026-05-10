package com.fyp.virtualtryon.ui.tryon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.graphics.BitmapFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.virtualtryon.R
import com.fyp.virtualtryon.camera.CameraManager
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.databinding.FragmentTryonBinding
import com.fyp.virtualtryon.garment.GarmentOverlay
import com.fyp.virtualtryon.pose.FaceDetector
import com.fyp.virtualtryon.pose.PoseDetector
import com.fyp.virtualtryon.warning.FitWarning
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TryOnFragment : Fragment() {

    private var _binding: FragmentTryonBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TryOnViewModel by viewModels()

    private lateinit var cameraManager: CameraManager
    private lateinit var poseDetector: PoseDetector
    private lateinit var faceDetector: FaceDetector
    private lateinit var garmentOverlay: GarmentOverlay

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var thumbAdapter: GarmentThumbAdapter

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTryonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraManager  = CameraManager(requireContext())
        garmentOverlay = GarmentOverlay(requireContext())

        poseDetector = PoseDetector(requireContext()) { keypoints ->
            viewModel.onKeypointsUpdated(keypoints)
        }

        faceDetector = FaceDetector(requireContext()) { faceKeypoints ->
            viewModel.onFaceKeypointsUpdated(faceKeypoints)
        }

        setupCategoryChips()
        setupThumbnailStrip()
        observeViewModel()

        binding.btnFlipCamera.setOnClickListener { flipCamera() }

        if (hasCameraPermission()) startCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun flipCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT
        binding.overlayView.mirrorHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        startCamera()
    }

    private fun setupThumbnailStrip() {
        thumbAdapter = GarmentThumbAdapter { garment -> viewModel.selectGarment(garment) }
        binding.rvGarmentThumbs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvGarmentThumbs.adapter = thumbAdapter
    }

    private fun setupCategoryChips() {
        binding.chipGlassesV2.setOnClickListener {
            viewModel.setCategory(GarmentType.GLASSES_V2)
        }

        binding.chipShoes.setOnClickListener {
            findNavController().navigate(R.id.action_tryon_to_shoes)
        }
    }

    private fun observeViewModel() {
        // Single combined LiveData feeds the thumbnail strip — switches automatically with category
        viewModel.displayedGarments.observe(viewLifecycleOwner) { list ->
            thumbAdapter.submitList(list)
        }

        viewModel.fitResult.observe(viewLifecycleOwner) { result ->
            binding.tvFitWarning.text = result.message
            binding.tvFitWarning.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (result.warning == FitWarning.GOOD_FIT) R.color.fit_ok else R.color.fit_warning
                )
            )
            binding.tvFitWarning.visibility = View.VISIBLE
        }

        viewModel.selectedGarment.observe(viewLifecycleOwner) { garment ->
            binding.tvSelectedGarment.text = garment?.name ?: getString(R.string.no_garment_selected)
            loadGarmentBitmap(garment)
            thumbAdapter.setSelected(garment?.id)
        }

        viewModel.currentKeypoints.observe(viewLifecycleOwner) { kp ->
            binding.overlayView.updateKeypoints(kp)
        }

        viewModel.currentFaceKeypoints.observe(viewLifecycleOwner) { fk ->
            binding.overlayView.updateFaceKeypoints(fk)
            val straight = fk?.isLookingStraight() ?: true
            binding.tvHeadPoseWarning.visibility = if (straight) View.GONE else View.VISIBLE
        }
    }

    private fun loadGarmentBitmap(garment: Garment?) {
        if (garment == null) {
            binding.overlayView.updateGarment(null, null)
            return
        }
        try {
            requireContext().assets.open(garment.imageAssetPath).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                // Use the active category so GLASSES_V2 mode is passed through to OverlayView,
                // not the DB type (which is always GLASSES for all glasses garments).
                val effectiveType = viewModel.activeCategory.value ?: garment.type
                binding.overlayView.updateGarment(bitmap, effectiveType)
            }
        } catch (e: Exception) {
            android.util.Log.e("TryOnFragment", "Failed to load ${garment.imageAssetPath}", e)
            binding.overlayView.updateGarment(null, null)
        }
    }

    private fun startCamera() {
        cameraManager.startCamera(
            lifecycleOwner = viewLifecycleOwner,
            previewView    = binding.cameraPreview,
            analyzer       = { imageProxy ->
                val timestampMs = System.currentTimeMillis()
                if (viewModel.activeCategory.value == GarmentType.GLASSES_V2) {
                    faceDetector.detectAsync(imageProxy, timestampMs)
                } else {
                    poseDetector.detectAsync(imageProxy, timestampMs)
                }
            },
            lensFacing = lensFacing,
        )
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        poseDetector.close()
        faceDetector.close()
        garmentOverlay.clearCache()
        cameraManager.stopCamera()
        _binding = null
    }
}
