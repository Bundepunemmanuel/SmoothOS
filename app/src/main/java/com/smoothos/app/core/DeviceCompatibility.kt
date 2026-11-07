package com.smoothos.app.core

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class DeviceHardwareProfile(
    val manufacturer: String,
    val model: String,
    val deviceCode: String,
    val androidVersion: String,
    val apiLevel: Int,
    val cpuAbi: String,
    val isLowRamDevice: Boolean,
    val isAndroid14OrHigher: Boolean,
    val isInfinixSmart8: Boolean,
    val ramCapacityTier: String,
    val recommendations: List<String>
)

object DeviceCompatibility {

    fun evaluate(context: Context): DeviceHardwareProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRam = am.isLowRamDevice

        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val deviceCode = Build.DEVICE
        val apiLevel = Build.VERSION.SDK_INT
        val androidVersion = Build.VERSION.RELEASE
        val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown ARM"

        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)

        // Check if device matches Infinix Smart 8 (Model X6525 or variants)
        val isInfinix = manufacturer.equals("Infinix", ignoreCase = true) ||
                model.contains("Infinix", ignoreCase = true) ||
                model.contains("X6525", ignoreCase = true)
        val isSmart8 = model.contains("Smart 8", ignoreCase = true) || model.contains("X6525", ignoreCase = true)

        val ramTier = when {
            totalRamMb <= 2200 -> "2 GB (Low Memory Target)"
            totalRamMb <= 3300 -> "3 GB"
            totalRamMb <= 4400 -> "4 GB"
            else -> "${(totalRamMb + 500) / 1024} GB"
        }

        val adviceList = mutableListOf<String>()

        if (totalRamMb <= 2200 || isLowRam) {
            adviceList.add("Disable XOS 'MemFusion' (Virtual RAM) if experiencing micro-stutters: virtual paging to eMMC flash causes CPU I/O wait lag.")
            adviceList.add("Avoid background task cleaner apps: killing apps on Android 14 causes immediate restart thrashing.")
            adviceList.add("Keep at least 15% free internal flash storage to avoid TRIM and write-amplification lag.")
        } else {
            adviceList.add("System memory capacity is healthy.")
        }

        if (apiLevel >= 34) {
            adviceList.add("Android 14 aggressive process freeze is active: cached apps are frozen cleanly without consuming CPU cycles.")
        }

        return DeviceHardwareProfile(
            manufacturer = manufacturer,
            model = model,
            deviceCode = deviceCode,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            cpuAbi = cpuAbi,
            isLowRamDevice = isLowRam,
            isAndroid14OrHigher = apiLevel >= 34,
            isInfinixSmart8 = isInfinix && isSmart8,
            ramCapacityTier = ramTier,
            recommendations = adviceList
        )
    }
}
