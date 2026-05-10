package com.fyp.virtualtryon.ui.recommendation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.fyp.virtualtryon.databinding.FragmentRecommendationResultBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecommendationResultFragment : Fragment() {

    private var _binding: FragmentRecommendationResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecommendationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecommendationResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = GarmentAdapter()
        binding.rvResults.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvResults.adapter = adapter

        viewModel.results.observe(viewLifecycleOwner) { garments ->
            adapter.submitList(garments)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
