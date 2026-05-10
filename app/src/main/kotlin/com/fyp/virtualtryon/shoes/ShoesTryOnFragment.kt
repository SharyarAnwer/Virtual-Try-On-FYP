package com.fyp.virtualtryon.shoes

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.virtualtryon.R
import com.fyp.virtualtryon.camera.CameraManager
import com.fyp.virtualtryon.databinding.FragmentShoesTryonBinding
import com.fyp.virtualtryon.ui.tryon.GarmentThumbAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShoesTryOnFragment : Fragment() {

    private var _binding: FragmentShoesTryonBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoesViewModel by viewModels()

    private lateinit var cameraManager: CameraManager
    private lateinit var footDetector: FootDetectorCV
    private lateinit var thumbAdapter: GarmentThumbAdapter

    // ── Step 2 (disabled): SceneView + shoe nodes live here when re-enabled ──

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
        _binding = FragmentShoesTryonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraManager = CameraManager(requireContext())

        // Step 1: foot keypoint detection — results fed into the debug overlay
        footDetector = FootDetectorCV { fk ->
            requireActivity().runOnUiThread {
                binding.keypointOverlay.update(fk)
                // Step 2 (TODO): also call updateShoePositions(fk) once 3D is re-enabled
            }
        }

        setupThumbnailStrip()
        observeViewModel()
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        if (hasCameraPermission()) startCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── UI wiring ─────────────────────────────────────────────────────────────

    private fun setupThumbnailStrip() {
        thumbAdapter = GarmentThumbAdapter { viewModel.selectGarment(it) }
        binding.rvShoeThumbs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvShoeThumbs.adapter = thumbAdapter
    }

    private fun observeViewModel() {
        viewModel.shoesFlow.observe(viewLifecycleOwner) { thumbAdapter.submitList(it) }

        viewModel.selectedGarment.observe(viewLifecycleOwner) { garment ->
            binding.tvSelectedShoe.text = garment?.name ?: getString(R.string.no_garment_selected)
            thumbAdapter.setSelected(garment?.id)
            // Step 2 (TODO): load GLB model when garment changes
        }
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun startCamera() {
        cameraManager.startCamera(
            lifecycleOwner = viewLifecycleOwner,
            previewView    = binding.cameraPreview,
            analyzer       = { imageProxy ->
                footDetector.detectAsync(imageProxy, System.currentTimeMillis())
            },
            lensFacing = CameraSelector.LENS_FACING_BACK,
        )
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        footDetector.close()
        cameraManager.stopCamera()
        _binding = null
    }
}
