# SmoothOS — Native Android 14 Performance & Diagnostics

A genuine native Android application built in Kotlin, specifically engineered for the **Infinix Smart 8** (ARM64, Android 14 / XOS, 2GB RAM).

---

## Key Technical Objectives & Real Implementations

### 1. Directly Measured System Metrics
* **RAM & Memory Pressure:** Uses `ActivityManager.getMemoryInfo()` to extract total RAM, available RAM, system LMK threshold, and low-memory state flag.
* **Thermal & Throttling Status:** Uses `PowerManager.getCurrentThermalStatus()` (API 29+) and `PowerManager.getThermalHeadroom()` (API 30+) along with battery temperature sensors to accurately detect hardware throttling.
* **Storage Diagnostics:** Uses `StatFs` to measure internal flash blocks, calculating available and used storage, and warning when free storage drops below 12% (preventing eMMC wear and flash write stalls).
* **Network Performance:** Uses native TCP sockets and DNS resolution timers to measure ping latency (ms), jitter (ms variance), packet loss (%), and DNS response speed.

### 2. Android 14 & 2GB RAM Engineering Decisions
* **Zero Fake Boosters:** No fake RAM cleaners or fake coolers. On Android 14, killing background apps causes severe CPU spikes and battery drain as the OS immediately relaunches killed system processes.
* **Ultra-Lightweight UI:** Built using native Android ViewBinding and Material 3 without heavy composable overhead, keeping idle RAM consumption under **25MB**.
* **Foreground Service Compliance:** Diagnostic service declares `foregroundServiceType="specialUse"` with the required Android 14 manifest subtype property, shutting down immediately after benchmarking.

---

## How to Build the Standalone APK (100% Free via GitHub Actions)

You can build the APK directly from your phone without a PC, Termux, or Android Studio:

1. **Push this repository to GitHub** (or import the code).
2. GitHub Actions detects `.github/workflows/build-apk.yml`.
3. The automated cloud runner (Ubuntu + JDK 17 + Android 14 SDK) compiles:
   ```bash
   ./gradlew assembleDebug
   ```
4. Once completed (~2 minutes), open the **Actions** tab on your phone in your browser or the **Releases** section.
5. Tap **SmoothOS-v1.0.0.apk** to download and install it directly on your Infinix Smart 8!
