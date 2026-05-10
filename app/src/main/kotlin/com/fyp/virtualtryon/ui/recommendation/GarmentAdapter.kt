package com.fyp.virtualtryon.ui.recommendation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.databinding.ItemGarmentBinding

class GarmentAdapter(
    private val onItemClick: ((Garment) -> Unit)? = null,
) : ListAdapter<Garment, GarmentAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemGarmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(garment: Garment) {
            binding.tvGarmentName.text = garment.name
            binding.tvGarmentSize.text = garment.sizeLabel
            binding.tvGarmentType.text = garment.type.name.lowercase().replaceFirstChar { it.uppercase() }
            binding.root.setOnClickListener { onItemClick?.invoke(garment) }

            Glide.with(binding.ivGarment.context)
                .load("file:///android_asset/${garment.imageAssetPath}")
                .centerCrop()
                .into(binding.ivGarment)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGarmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<Garment>() {
        override fun areItemsTheSame(oldItem: Garment, newItem: Garment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Garment, newItem: Garment) = oldItem == newItem
    }
}
