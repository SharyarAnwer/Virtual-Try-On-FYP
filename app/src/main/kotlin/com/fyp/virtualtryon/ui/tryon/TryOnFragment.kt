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
import com.fyp.virtualtryon.R
import com.fyp.virtualtryon.camera.CameraManager
import com.fyp.virtualtryon.databinding.FragmentTryonBinding
import com.fyp.virtualtryon.garment.GarmentOverlay
import com.fyp.virtualtryon.pose.PoseDetector
import com.fyp.virtualtryon.warning.FitWarning
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TryOnFragment : Fragment() {

    private var _binding: FragmentTryonBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TryOnViewModel by viewModels()

    private lateinit var cameraManager: CameraManager
    private lateinit var poseDetector: PoseDetector
    private lateinit var garmentOverlay: GarmentOverlay

    private var lensFacing = CameraSelector.LENS_FACING_FRONT

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

        cameraManager   = CameraManager(requireContext())
        garmentOverlay  = GarmentOverlay(requireContext())

        poseDetector = PoseDetector(requireContext()) { keypoints ->
            viewModel.onKeypointsUpdated(keypoints)
        }

        setupCategoryChips()
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

    private fun setupCategoryChips() {
        binding.chipShirts.setOnClickListener {
            viewModel.setCategory(com.fyp.virtualtryon.data.model.GarmentType.SHIRT)
        }
        binding.chipPants.setOnClickListener {
            viewModel.setCategory(com.fyp.virtualtryon.data.model.GarmentType.PANTS)
        }
        binding.chipGlasses.setOnClickListener {
            viewModel.setCategory(com.fyp.virtualtryon.data.model.GarmentType.GLASSES)
        }
        binding.chipShoes.setOnClickListener {
            viewModel.setCategory(com.fyp.virtualtryon.data.model.GarmentType.SHOES)
        }
    }

    private fun observeViewModel() {
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
        }

        viewModel.currentKeypoints.observe(viewLifecycleOwner) { kp ->
            binding.overlayView.updateKeypoints(kp)
        }
    }

    private fun startCamera() {
        cameraManager.startCamera(
            lifecycleOwner = viewLifecycleOwner,
            previewView    = binding.cameraPreview,
            analyzer       = { imageProxy ->
                val timestampMs = System.currentTimeMillis()
                poseDetector.detectAsync(imageProxy, timestampMs)
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
        garmentOverlay.clearCache()
        cameraManager.stopCamera()
        _binding = null
    }
}
