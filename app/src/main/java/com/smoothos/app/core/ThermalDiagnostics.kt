package com.smoothos.app.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

data class ThermalSnapshot(
    val statusLevel: Int,
    val statusDescription: String,
    val thermalHeadroom: Float?,
    val batteryTempCelsius: Float?,
    val isThrottling: Boolean,
    val isDirectlyMeasured: Boolean = true
)

object ThermalDiagnostics {

    fun capture(context: Context): ThermalSnapshot {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val statusLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.currentThermalStatus
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }

        val headroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                pm.getThermalHeadroom(0)
            } catch (_: Exception) {
                null
            }
        } else null

        val batteryTemp = readBatteryTemperature(context)

        val description = when (statusLevel) {
            PowerManager.THERMAL_STATUS_NONE -> "Nominal (No Throttling)"
            PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
            PowerManager.THERMAL_STATUS_MODERATE -> "Moderate (Minor Throttling)"
            PowerManager.THERMAL_STATUS_SEVERE -> "Severe (CPU Throttling Active)"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Overheating"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Cooldown"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Shutdown Imminent"
            else -> "Unknown State"
        }

        val isThrottling = statusLevel >= PowerManager.THERMAL_STATUS_MODERATE

        return ThermalSnapshot(
            statusLevel = statusLevel,
            statusDescription = description,
            thermalHeadroom = headroom,
            batteryTempCelsius = batteryTemp,
            isThrottling = isThrottling,
            isDirectlyMeasured = true
        )
    }

    private fun readBatteryTemperature(context: Context): Float? {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (tempTenths > 0) {
                tempTenths / 10.0f
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
