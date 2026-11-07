package com.smoothos.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log

/**
 * SmoothOS Application Entry Point
 * Designed to have minimal startup overhead (< 15ms) and strictly conserve memory
 * on 2GB RAM devices such as the Infinix Smart 8.
 */
class SmoothOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "SmoothOS Application initialized on Android ${android.os.Build.VERSION.SDK_INT}")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.w(TAG, "High memory pressure detected (level: $level). Releasing transient caches.")
                // Purge any transient memory buffers immediately
                System.gc()
            }
        }
    }

    companion object {
        const val TAG = "SmoothOS"
    }
}
