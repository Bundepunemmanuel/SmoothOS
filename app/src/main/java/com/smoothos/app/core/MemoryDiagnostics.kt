package com.smoothos.app.core

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import java.io.RandomAccessFile

data class MemorySnapshot(
    val totalMb: Long,
    val availableMb: Long,
    val usedMb: Long,
    val usagePercentage: Int,
    val thresholdMb: Long,
    val isLowMemory: Boolean,
    val isDirectlyMeasured: Boolean = true,
    val zRamSwapUsedMb: Long? = null,
    val hasUsageAccessPermission: Boolean = false
)

object MemoryDiagnostics {

    fun capture(context: Context): MemorySnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availableMb = memInfo.availMem / (1024 * 1024)
        val thresholdMb = memInfo.threshold / (1024 * 1024)
        val usedMb = (totalMb - availableMb).coerceAtLeast(0)
        val usagePercentage = if (totalMb > 0) ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt() else 0

        val zRamMb = readZramFromProc()
        val hasUsageAccess = checkUsageAccess(context)

        return MemorySnapshot(
            totalMb = totalMb,
            availableMb = availableMb,
            usedMb = usedMb,
            usagePercentage = usagePercentage,
            thresholdMb = thresholdMb,
            isLowMemory = memInfo.lowMemory,
            isDirectlyMeasured = true,
            zRamSwapUsedMb = zRamMb,
            hasUsageAccessPermission = hasUsageAccess
        )
    }

    private fun readZramFromProc(): Long? {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")
            var line: String?
            var swapTotalKb = 0L
            var swapFreeKb = 0L
            while (reader.readLine().also { line = it } != null) {
                val current = line ?: break
                if (current.startsWith("SwapTotal:")) {
                    swapTotalKb = current.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                } else if (current.startsWith("SwapFree:")) {
                    swapFreeKb = current.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                }
            }
            reader.close()
            if (swapTotalKb > 0) {
                ((swapTotalKb - swapFreeKb) / 1024)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun checkUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }
}
