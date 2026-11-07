package com.smoothos.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.smoothos.app.R
import com.smoothos.app.core.DeviceCompatibility
import com.smoothos.app.core.MemoryDiagnostics
import com.smoothos.app.core.NetworkDiagnostics
import com.smoothos.app.core.StorageDiagnostics
import com.smoothos.app.core.ThermalDiagnostics
import com.smoothos.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        refreshAllDiagnostics()
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            refreshAllDiagnostics()
            Toast.makeText(this, "Diagnostics updated", Toast.LENGTH_SHORT).show()
        }

        binding.swipeRefresh.setOnRefreshListener {
            refreshAllDiagnostics()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnOpenUsageAccess.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Locate SmoothOS to grant Usage Access for per-app stats", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Usage Access settings not available on this ROM", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRunNetworkTest.setOnClickListener {
            runNetworkBenchmark()
        }
    }

    private fun refreshAllDiagnostics() {
        // 1. Device Hardware Profile & Compatibility Check
        val hardware = DeviceCompatibility.evaluate(this)
        val deviceName = "${hardware.manufacturer} ${hardware.model}"
        binding.tvDeviceBadge.text = "$deviceName • Android ${hardware.androidVersion} • ${hardware.ramCapacityTier}"

        binding.tvHardwareDetails.text = buildString {
            append("• Model: $deviceName (${hardware.deviceCode})\n")
            append("• OS: Android ${hardware.androidVersion} (API ${hardware.apiLevel})\n")
            append("• CPU ABI: ${hardware.cpuAbi}\n")
            append("• Low RAM Device Flag: ${if (hardware.isLowRamDevice) "Enabled (Aggressive LMK)" else "Disabled"}\n")
            if (hardware.isInfinixSmart8) {
                append("• Target Match: Verified Infinix Smart 8 Profile")
            }
        }

        // 2. Memory & RAM Pressure (Directly Measured)
        val memory = MemoryDiagnostics.capture(this)
        binding.tvRamUsagePercent.text = "${memory.usagePercentage}%"
        binding.tvRamBreakdown.text = "${memory.usedMb} MB used of ${memory.totalMb} MB"
        binding.pbRamPressure.progress = memory.usagePercentage

        // Color coding memory pressure
        val ramColor = when {
            memory.usagePercentage >= 85 -> ContextCompat.getColor(this, R.color.status_red)
            memory.usagePercentage >= 70 -> ContextCompat.getColor(this, R.color.status_yellow)
            else -> ContextCompat.getColor(this, R.color.primary_blue)
        }
        binding.pbRamPressure.progressTintList = android.content.res.ColorStateList.valueOf(ramColor)

        val swapText = memory.zRamSwapUsedMb?.let { "• zRAM Swap: ${it} MB" } ?: ""
        binding.tvRamThresholdInfo.text = "System LMK threshold: ${memory.thresholdMb} MB • Low RAM alert: ${memory.isLowMemory} $swapText"

        if (memory.hasUsageAccessPermission) {
            binding.btnOpenUsageAccess.text = "Usage Access: Granted (Active)"
            binding.btnOpenUsageAccess.isEnabled = false
        } else {
            binding.btnOpenUsageAccess.text = "Inspect Heavy Apps (Grant Usage Access)"
            binding.btnOpenUsageAccess.isEnabled = true
        }

        // 3. Thermal Diagnostics
        val thermal = ThermalDiagnostics.capture(this)
        binding.tvThermalStatusBadge.text = thermal.statusDescription.uppercase()
        val tempText = thermal.batteryTempCelsius?.let { "${it}°C" } ?: "N/A"
        val headroomText = thermal.thermalHeadroom?.let { String.format("%.2f", it) } ?: "Not Exposed"
        binding.tvThermalDetails.text = "Battery Temp: $tempText • Headroom Index: $headroomText • State: ${thermal.statusDescription}"

        // 4. Storage Diagnostics
        val storage = StorageDiagnostics.capture()
        binding.pbStorageUsage.progress = storage.usagePercentage
        binding.tvStorageDetails.text = "Available: ${storage.availableGb} GB • Total: ${storage.totalGb} GB (${storage.usagePercentage}% used)"

        // 5. Active Network Status
        val netStatus = NetworkDiagnostics.getActiveStatus(this)
        binding.tvNetworkStateBadge.text = if (netStatus.isConnected) "CONNECTED" else "OFFLINE"
        binding.tvNetworkActiveDetails.text = netStatus.interfaceDetails

        // 6. Actionable System Advice
        val combinedAdvice = mutableListOf<String>()
        combinedAdvice.addAll(hardware.recommendations)
        if (storage.isStorageBottleneck && storage.warningMessage != null) {
            combinedAdvice.add(storage.warningMessage)
        }
        if (memory.usagePercentage >= 85) {
            combinedAdvice.add("RAM pressure is high (>85%). Android 14 will terminate background tasks when switching apps. Close idle browser tabs.")
        }
        binding.tvActionableAdvice.text = combinedAdvice.joinToString("\n\n• ", prefix = "• ")
    }

    private fun runNetworkBenchmark() {
        binding.btnRunNetworkTest.isEnabled = false
        binding.btnRunNetworkTest.text = "Testing network latency & DNS..."

        lifecycleScope.launch {
            try {
                val benchmark = NetworkDiagnostics.runBenchmark()
                binding.tvPingResult.text = "${benchmark.pingMs} ms"
                binding.tvJitterResult.text = "${benchmark.jitterMs} ms"
                binding.tvLossResult.text = "${benchmark.packetLossPercent}%"
                binding.tvDnsResult.text = "${benchmark.dnsResolutionMs} ms"

                val adviceText = benchmark.recommendations.joinToString("\n• ", prefix = "Network Quality: ${benchmark.qualityRating}\n• ")
                Toast.makeText(this@MainActivity, "Network Benchmark Completed", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity, "Network benchmark encountered an error", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnRunNetworkTest.isEnabled = true
                binding.btnRunNetworkTest.text = "Run Live Ping, Jitter & DNS Test"
            }
        }
    }
}
