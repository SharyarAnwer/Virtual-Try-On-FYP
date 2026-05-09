package com.fyp.virtualtryon.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fyp.virtualtryon.R
import com.fyp.virtualtryon.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            binding.tvWelcome.text = if (profile?.name.isNullOrBlank())
                getString(R.string.welcome_generic)
            else
                getString(R.string.welcome_user, profile!!.name)
        }

        binding.cardTryOn.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_tryon)
        }

        binding.cardRecommendations.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_recommendation)
        }

        binding.cardProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }

        viewModel.shirts.observe(viewLifecycleOwner) { shirts ->
            binding.tvShirtCount.text = getString(R.string.garment_count, shirts.size, "Shirts")
        }
        viewModel.pants.observe(viewLifecycleOwner) { pants ->
            binding.tvPantsCount.text = getString(R.string.garment_count, pants.size, "Pants")
        }
        viewModel.glasses.observe(viewLifecycleOwner) { glasses ->
            binding.tvGlassesCount.text = getString(R.string.garment_count, glasses.size, "Glasses")
        }
        viewModel.shoes.observe(viewLifecycleOwner) { shoes ->
            binding.tvShoesCount.text = getString(R.string.garment_count, shoes.size, "Shoes")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
