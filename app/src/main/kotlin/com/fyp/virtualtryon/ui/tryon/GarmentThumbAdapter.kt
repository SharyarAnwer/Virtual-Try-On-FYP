package com.fyp.virtualtryon.ui.tryon

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.databinding.ItemGarmentThumbBinding

/**
 * Horizontal strip adapter showing garment thumbnails.
 * Loads the same transparent PNG asset that's used for the overlay.
 */
class GarmentThumbAdapter(
    private val onItemClick: (Garment) -> Unit,
) : ListAdapter<Garment, GarmentThumbAdapter.ViewHolder>(DiffCallback) {

    private var selectedId: Long = -1L

    fun setSelected(id: Long?) {
        selectedId = id ?: -1L
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemGarmentThumbBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(garment: Garment) {
            try {
                binding.root.context.assets.open(garment.imageAssetPath).use { stream ->
                    binding.ivThumb.setImageBitmap(BitmapFactory.decodeStream(stream))
                }
            } catch (e: Exception) {
                binding.ivThumb.setImageBitmap(null)
            }

            binding.cardThumb.strokeWidth = if (garment.id == selectedId) 6 else 0
            binding.cardThumb.setOnClickListener { onItemClick(garment) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGarmentThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<Garment>() {
        override fun areItemsTheSame(oldItem: Garment, newItem: Garment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Garment, newItem: Garment) = oldItem == newItem
    }
}
