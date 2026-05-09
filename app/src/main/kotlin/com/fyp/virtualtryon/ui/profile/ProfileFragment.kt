package com.fyp.virtualtryon.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fyp.virtualtryon.R
import com.fyp.virtualtryon.data.model.BodyType
import com.fyp.virtualtryon.data.model.GarmentGender
import com.fyp.virtualtryon.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            profile ?: return@observe
            binding.etName.setText(profile.name)
            binding.etHeight.setText(profile.heightCm.toInt().toString())
            binding.etWeight.setText(profile.weightKg.toInt().toString())
            binding.etColors.setText(profile.preferredColors)

            val genderPos = GarmentGender.values().indexOf(profile.gender)
            binding.spinnerGender.setSelection(genderPos.coerceAtLeast(0))

            val bodyTypePos = BodyType.values().indexOf(profile.bodyType)
            binding.spinnerBodyType.setSelection(bodyTypePos.coerceAtLeast(0))
        }

        binding.btnSave.setOnClickListener {
            val name    = binding.etName.text.toString().trim()
            val height  = binding.etHeight.text.toString().toFloatOrNull() ?: 170f
            val weight  = binding.etWeight.text.toString().toFloatOrNull() ?: 70f
            val colors  = binding.etColors.text.toString().trim()
            val gender  = GarmentGender.values()[binding.spinnerGender.selectedItemPosition]
            val body    = BodyType.values()[binding.spinnerBodyType.selectedItemPosition]

            viewModel.saveProfile(name, gender, height, weight, body, colors)
            Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        val genderAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            GarmentGender.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
        )
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = genderAdapter

        val bodyTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            BodyType.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
        )
        bodyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBodyType.adapter = bodyTypeAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
