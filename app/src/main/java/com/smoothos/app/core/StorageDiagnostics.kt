package com.smoothos.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.File

data class StorageSnapshot(
    val totalGb: Double,
    val availableGb: Double,
    val usedGb: Double,
    val usagePercentage: Int,
    val isStorageBottleneck: Boolean,
    val warningMessage: String?
)

data class CacheTargetApp(
    val appName: String,
    val packageName: String,
    val category: String,
    val isInstalled: Boolean
)

object StorageDiagnostics {

    // Common applications known for accumulating gigabytes of media/web cache on budget phones
    private val CANDIDATE_PACKAGES = listOf(
        "com.android.chrome" to ("Google Chrome" to "Web Browser"),
        "com.google.android.youtube" to ("YouTube" to "Video Streaming"),
        "com.whatsapp" to ("WhatsApp" to "Messaging & Media"),
        "com.zhiliaoapp.musically" to ("TikTok" to "Short Video Cache"),
        "com.instagram.android" to ("Instagram" to "Social Media"),
        "com.facebook.katana" to ("Facebook" to "Social Network"),
        "com.facebook.lite" to ("Facebook Lite" to "Social Network"),
        "org.telegram.messenger" to ("Telegram" to "Cloud Messaging"),
        "com.twitter.android" to ("X (Twitter)" to "Social Feed"),
        "com.google.android.apps.photos" to ("Google Photos" to "Media Cache"),
        "com.spotify.music" to ("Spotify" to "Audio Cache")
    )

    fun capture(): StorageSnapshot {
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.path)

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val availableBytes = availableBlocks * blockSize
        val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0)

        val bytesInGb = 1024.0 * 1024.0 * 1024.0
        val totalGb = String.format("%.1f", totalBytes / bytesInGb).toDoubleOrNull() ?: 0.0
        val availableGb = String.format("%.1f", availableBytes / bytesInGb).toDoubleOrNull() ?: 0.0
        val usedGb = String.format("%.1f", usedBytes / bytesInGb).toDoubleOrNull() ?: 0.0

        val usagePercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0
        val isBottleneck = usagePercent >= 85

        val warning = if (isBottleneck) {
            "Flash storage is over 85% full. Flash controllers on eMMC 5.1 devices like Infinix Smart 8 slow down heavily when free blocks are scarce, causing random app freezes and high I/O wait."
        } else null

        return StorageSnapshot(
            totalGb = totalGb,
            availableGb = availableGb,
            usedGb = usedGb,
            usagePercentage = usagePercent,
            isStorageBottleneck = isBottleneck,
            warningMessage = warning
        )
    }

    /**
     * Inspects package manager to discover which high-cache candidate applications
     * are currently installed on the user's Infinix device.
     */
    fun getInstalledCacheCandidates(context: Context): List<CacheTargetApp> {
        val pm = context.packageManager
        val installedApps = mutableListOf<CacheTargetApp>()

        for ((pkg, info) in CANDIDATE_PACKAGES) {
            val (name, category) = info
            val isInstalled = try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }

            if (isInstalled) {
                installedApps.add(
                    CacheTargetApp(
                        appName = name,
                        packageName = pkg,
                        category = category,
                        isInstalled = true
                    )
                )
            }
        }

        return installedApps
    }

    /**
     * Creates direct intent navigation to open the specific application's settings page
     * where the user can tap "Storage & cache" -> "Clear cache" with one tap.
     */
    fun createClearCacheIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates intent navigation to the system storage dashboard or manage applications screen.
     */
    fun createStorageSettingsIntent(): Intent {
        return Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
