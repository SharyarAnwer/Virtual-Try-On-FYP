package com.fyp.virtualtryon.garment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.fyp.virtualtryon.data.model.Garment
import com.fyp.virtualtryon.data.model.GarmentType
import com.fyp.virtualtryon.pose.BodyKeypoints

/**
 * High-level orchestrator. Given the current camera [Bitmap], the detected
 * [BodyKeypoints], and the selected [Garment], applies the correct warper.
 */
class GarmentOverlay(private val context: Context) {

    private val garmentBitmapCache = mutableMapOf<Long, Bitmap>()

    /**
     * Returns a new bitmap with the garment overlaid on [frame].
     * Returns the original [frame] unchanged if keypoints are invalid.
     */
    fun apply(frame: Bitmap, keypoints: BodyKeypoints, garment: Garment): Bitmap {
        if (!keypoints.isValid()) return frame

        val garmentBitmap = loadGarmentBitmap(garment) ?: return frame

        return when (garment.type) {
            GarmentType.SHIRT   -> GarmentWarper.overlayUpperBody(frame, garmentBitmap, keypoints)
            GarmentType.PANTS   -> GarmentWarper.overlayLowerBody(frame, garmentBitmap, keypoints)
            GarmentType.GLASSES -> GarmentWarper.overlayUpperBody(frame, garmentBitmap, keypoints)
            GarmentType.SHOES   -> GarmentWarper.overlayLowerBody(frame, garmentBitmap, keypoints)
        }
    }

    private fun loadGarmentBitmap(garment: Garment): Bitmap? {
        return garmentBitmapCache.getOrPut(garment.id) {
            try {
                context.assets.open(garment.imageAssetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }

    fun clearCache() = garmentBitmapCache.clear()
}
