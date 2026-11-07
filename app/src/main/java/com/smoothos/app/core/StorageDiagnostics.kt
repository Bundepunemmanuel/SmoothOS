package com.smoothos.app.core

import android.os.Environment
import android.os.StatFs
import java.io.File

data class StorageSnapshot(
    val totalGb: Double,
    val availableGb: Double,
    val usedGb: Double,
    val usagePercentage: Int,
    val isStorageBottleneck: Boolean,
    val warningMessage: String?
)

object StorageDiagnostics {

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
        val isBottleneck = usagePercent >= 88

        val warning = if (isBottleneck) {
            "Storage is over 88% full. Flash controllers on eMMC 5.1 devices like Infinix Smart 8 slow down heavily when free blocks are scarce, causing random app freezes."
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
}
