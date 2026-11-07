package com.smoothos.app.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs
import kotlin.math.sqrt

data class NetworkStatusSnapshot(
    val isConnected: Boolean,
    val transportType: String,
    val interfaceDetails: String,
    val linkSpeedMbps: Int?,
    val rssiDb: Int?,
    val carrierName: String?
)

data class NetworkBenchmarkResult(
    val pingMs: Long,
    val jitterMs: Long,
    val packetLossPercent: Int,
    val dnsResolutionMs: Long,
    val qualityRating: String,
    val recommendations: List<String>
)

object NetworkDiagnostics {

    fun getActiveStatus(context: Context): NetworkStatusSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)

        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkStatusSnapshot(
                isConnected = false,
                transportType = "Disconnected",
                interfaceDetails = "No active Internet connection",
                linkSpeedMbps = null,
                rssiDb = null,
                carrierName = null
            )
        }

        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        var linkSpeed: Int? = null
        var rssi: Int? = null
        var carrier: String? = null
        val transport: String

        if (isWifi) {
            transport = "Wi-Fi"
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wm.connectionInfo
            linkSpeed = wifiInfo.linkSpeed.takeIf { it > 0 }
            rssi = wifiInfo.rssi
        } else if (isCellular) {
            transport = "Mobile Data"
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            carrier = tm.networkOperatorName.takeIf { it.isNotBlank() } ?: "Cellular"
            linkSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                caps.linkDownstreamBandwidthKbps / 1000
            } else null
        } else {
            transport = "Ethernet / Other"
        }

        val details = when {
            isWifi && linkSpeed != null -> "Wi-Fi Connected (${linkSpeed} Mbps PHY rate)"
            isCellular && carrier != null -> "Cellular Data ($carrier)"
            else -> "Connected via $transport"
        }

        return NetworkStatusSnapshot(
            isConnected = true,
            transportType = transport,
            interfaceDetails = details,
            linkSpeedMbps = linkSpeed,
            rssiDb = rssi,
            carrierName = carrier
        )
    }

    suspend fun runBenchmark(): NetworkBenchmarkResult = withContext(Dispatchers.IO) {
        val probeHost = "1.1.1.1"
        val probePort = 53
        val probeCount = 4
        val latencies = mutableListOf<Long>()
        var dropped = 0

        // 1. Run Socket TCP Round-Trip Probes
        for (i in 0 until probeCount) {
            try {
                val start = System.currentTimeMillis()
                val socket = Socket()
                socket.connect(InetSocketAddress(probeHost, probePort), 2500)
                val elapsed = System.currentTimeMillis() - start
                socket.close()
                latencies.add(elapsed)
            } catch (_: Exception) {
                dropped++
            }
        }

        val avgPing = if (latencies.isNotEmpty()) latencies.average().toLong() else -1L
        val packetLoss = ((dropped.toDouble() / probeCount.toDouble()) * 100).toInt()

        // 2. Compute Jitter (Mean Absolute Difference between consecutive probes)
        val jitter = if (latencies.size > 1) {
            var diffSum = 0L
            for (i in 0 until latencies.size - 1) {
                diffSum += abs(latencies[i + 1] - latencies[i])
            }
            (diffSum / (latencies.size - 1))
        } else 0L

        // 3. Measure DNS Resolution Latency
        val dnsStart = System.currentTimeMillis()
        val dnsSuccess = try {
            InetAddress.getAllByName("www.google.com")
            true
        } catch (_: Exception) {
            false
        }
        val dnsTime = if (dnsSuccess) (System.currentTimeMillis() - dnsStart) else -1L

        val rating = when {
            packetLoss > 25 || avgPing > 250 -> "Poor (High Lag)"
            avgPing in 0..65 && jitter < 15 -> "Excellent"
            avgPing in 66..130 -> "Good"
            else -> "Fair"
        }

        val advice = mutableListOf<String>()
        if (jitter > 25) {
            advice.add("High network jitter detected. Packet latency is inconsistent, which causes buffering or voice call stutter.")
        }
        if (packetLoss > 0) {
            advice.add("Packet loss detected (${packetLoss}%). Check for Wi-Fi interference or weak mobile tower reception.")
        }
        if (dnsTime > 150) {
            advice.add("DNS lookup is slow (${dnsTime} ms). Consider using Private DNS in Android Settings (e.g., dns.google or one.one.one.one).")
        }
        if (advice.isEmpty()) {
            advice.add("Network latency, stability, and DNS response times are all optimal.")
        }

        NetworkBenchmarkResult(
            pingMs = avgPing.coerceAtLeast(0),
            jitterMs = jitter,
            packetLossPercent = packetLoss,
            dnsResolutionMs = dnsTime.coerceAtLeast(0),
            qualityRating = rating,
            recommendations = advice
        )
    }
}
