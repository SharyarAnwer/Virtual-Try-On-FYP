package com.fyp.virtualtryon

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class VirtualTryOnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initLocal()) {
            Log.e("VirtualTryOnApp", "OpenCV initialization failed")
        }
    }
}
