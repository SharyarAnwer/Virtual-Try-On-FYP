package com.fyp.virtualtryon.ui.recommendation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.fyp.virtualtryon.R
import com.fyp.virtualtryon.databinding.FragmentRecommendationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecommendationFragment : Fragment() {

    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecommendationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBodyTypeSpinner()
        binding.btnGenerate.setOnClickListener { onGenerateClicked() }

        viewModel.navigateToResults.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                viewModel.onNavigatedToResults()
                findNavController().navigate(R.id.action_recommendation_to_results)
            }
        }
    }

    private fun setupBodyTypeSpinner() {
        val bodyTypes = listOf("Small", "Medium", "Large", "Extra-Large")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            bodyTypes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBodyType.adapter = adapter
    }

    private fun onGenerateClicked() {
        val gender = if (binding.rgGender.checkedRadioButtonId == binding.rbMale.id) "Male" else "Female"

        val height = binding.etHeight.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() } ?: "Not specified"

        val bodyType = binding.spinnerBodyType.selectedItem?.toString() ?: "Not specified"

        val selectedColors = buildList {
            if (binding.cbBlack.isChecked)  add("Black")
            if (binding.cbWhite.isChecked)  add("White")
            if (binding.cbGrey.isChecked)   add("Grey")
            if (binding.cbNavy.isChecked)   add("Navy")
            if (binding.cbBlue.isChecked)   add("Blue")
            if (binding.cbBrown.isChecked)  add("Brown")
            if (binding.cbOrange.isChecked) add("Orange")
            if (binding.cbYellow.isChecked) add("Yellow")
            if (binding.cbRed.isChecked)    add("Red")
            if (binding.cbGreen.isChecked)  add("Green")
        }

        Log.d(TAG, "=== Generate Recommendations ===")
        Log.d(TAG, "Gender     : $gender")
        Log.d(TAG, "Height     : $height cm")
        Log.d(TAG, "Body Type  : $bodyType")
        Log.d(TAG, "Colors     : ${if (selectedColors.isEmpty()) "None selected" else selectedColors.joinToString()}")
        Log.d(TAG, "================================")

        viewModel.generateRecommendation(gender, height, bodyType, selectedColors)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "RecommendationFragment"
    }
}
