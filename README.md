# PerfScout

[![JitPack](https://jitpack.io/v/dee2604/perfScout.svg)](https://jitpack.io/#dee2604/perfScout)

**PerfScout** provides unified, safe, and modular access to all vital app and device performance metrics and monitoring features through a single, easy-to-use API.

- All metrics are returned as `PerfResult` (Success/Error) for robust error handling.
- Main categories:
  - **Device metrics:** CPU, RAM, GPU, Battery, Thermal, Device Info
  - **App metrics:** App Memory, App Uptime, GC Stats, Startup Time
  - **System metrics:** Storage, Thread/Process, Network Quality
  - **Advanced features:** Frame Rendering/Jank, Media Quality Recommendation
  - **Monitoring:** Crash, ANR, Jank, StrictMode
- **Crash monitoring:**
  - The crash listener will be called for any uncaught exception in the host app, any library, or PerfScout itself.
  - It does not catch handled exceptions and does not prevent the app from crashing.
  - It chains to any previous uncaught exception handler (e.g., Crashlytics).
- **Startup time tracking:**
  - Requires explicit calls to `startupTime.recordActivityOnCreate` and `startupTime.recordFirstDraw` in your MainActivity lifecycle.
- All features are modular and can be used independently.

---

## Threading Requirements ⚠️

> **Important:** Some synchronous `get()` methods (such as for frame rendering and startup time) must **not** be called from the main/UI thread. Calling these methods on the main thread can cause ANR (Application Not Responding) errors. Always use them from a background thread, or prefer the suspend/async versions in coroutines or with callbacks.

- Synchronous `get()` methods for metrics like `PerfScoutMetrics.frameRendering.get()` and `PerfScoutMetrics.startupTime.get(context)` are annotated with `@WorkerThread` and should only be called from a background thread.
- For UI or main-thread code, always use the `getAsync()` suspend or callback-based APIs.

---

## Features at a Glance

- CPU, RAM, GPU, Battery, Thermal, Network, Storage, Thread/Process, App Memory, App CPU, GC, App Uptime, Device Info, Frame Rendering
- Startup Time (cold start, with clear setup instructions)
- ANR Detection (callback, safe, non-intrusive)
- Crash Monitoring (callback, chains to previous handler)
- Jank (Slow/Frozen Frame) Monitoring (callback, rolling window)
- Media Quality Recommendation (for multimedia apps)
- StrictMode Violation Reporting (opt-in, callback, API 28+)
- Network Quality (backward compatible, no ANR risk)

---

## Quick Start

```kotlin
// Add to your Application or MainActivity
PerfScoutMetrics.setCrashListener { crashInfo ->
    Log.e("PerfScout", "Crash detected: $crashInfo")
}
PerfScoutMetrics.setAnrListener { anrInfo ->
    Log.w("PerfScout", "ANR detected: $anrInfo")
}
PerfScoutMetrics.setJankListener { jankInfo ->
    Log.w("PerfScout", "Jank detected: $jankInfo")
}
PerfScoutMetrics.enableStrictMode(penaltyCallback = { violation ->
    Log.w("PerfScout", "StrictMode violation: $violation")
})
```

---

## Setup for Host Apps

### 1. Add as a dependency

#### Maven Central (Recommended)
```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.dee2604:perfscout:1.1.8")
}
```

#### JitPack (Alternative)
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.dee2604:perfScout:1.1.8")
}
```

#### Local Maven Repository (Development)
```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.dee2604:perfscout:1.1.8")
}
```

> _If you're using this as a module, add to your `settings.gradle` and `build.gradle` as appropriate._

### 2. Startup Time Tracking Setup (Optional)

For accurate startup time measurements, add these calls to your MainActivity:

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PerfScoutMetrics.startupTime.recordActivityOnCreate(this)
        // ... rest of onCreate
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            PerfScoutMetrics.startupTime.recordFirstDraw(this)
        }
    }
}
```

**Note:** Startup time tracking is optional. If you don't add these calls, startup time methods will return null values.

---

## Usage Examples (New API)

### Basic Metrics
```kotlin
// All metrics return PerfResult<T> (Success or Error)
val cpuResult = PerfScoutMetrics.cpu.get() // or getAsync()
val ramResult = PerfScoutMetrics.ram.get(context)
val gpuResult = PerfScoutMetrics.gpu.get()
val batteryResult = PerfScoutMetrics.battery.get(context)
val thermalResult = PerfScoutMetrics.thermal.get(context)
val appMemoryResult = PerfScoutMetrics.appMemory.get()
val appUptimeResult = PerfScoutMetrics.appUptime.get(context)
val gcStatsResult = PerfScoutMetrics.gcStats.get()
val networkResult = PerfScoutMetrics.network.get(context)
val storageResult = PerfScoutMetrics.storage.get(context)
val threadResult = PerfScoutMetrics.threadProcess.get(context)
val mediaQualityResult = PerfScoutMetrics.mediaQuality.get(context)
val mediaQualityAnalysisResult = PerfScoutMetrics.mediaQualityAnalysis.get(context)

// ⚠️ For frame rendering and startup time, prefer async usage:
// In a coroutine scope (recommended for UI/main thread)
val frameRenderingResult = PerfScoutMetrics.frameRendering.getAsync()
val startupResult = PerfScoutMetrics.startupTime.getAsync(context)

// If you must use get(), call only from a background thread:
// val frameRenderingResult = PerfScoutMetrics.frameRendering.get() // @WorkerThread only
// val startupResult = PerfScoutMetrics.startupTime.get(context)    // @WorkerThread only
```

**Handling results:**
```kotlin
when (cpuResult) {
    is PerfResult.Success -> Log.d("PerfScout", "CPU: ${cpuResult.info}")
    is PerfResult.Error -> Log.e("PerfScout", "Error: ${cpuResult.message}")
}
```

### Startup Time Tracking

Add these calls to your `MainActivity`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    PerfScoutMetrics.startupTime.recordActivityOnCreate(this)
    // ...
}
override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
        PerfScoutMetrics.startupTime.recordFirstDraw(this)
    }
}
```

Get startup time info (async, recommended):
```kotlin
lifecycleScope.launch {
    val startupResult = PerfScoutMetrics.startupTime.getAsync(context)
    // handle result
}
```

---

### Frame Rendering/Jank

**Using the new delegate (recommended, async):**
```kotlin
lifecycleScope.launch {
    val frameResult = PerfScoutMetrics.frameRendering.getAsync()
    when (frameResult) {
        is PerfResult.Success -> Log.d("PerfScout", "Frame Info: ${frameResult.info}")
        is PerfResult.Error -> Log.e("PerfScout", "Error: ${frameResult.message}")
    }
}
```

**Using the legacy async method:**
```kotlin
lifecycleScope.launch {
    val frameResult = PerfScoutMetrics.getFrameRenderingInfoAsync(3000)
    when (frameResult) {
        is PerfResult.Success -> Log.d("PerfScout", "Frame Info: ${frameResult.info}")
        is PerfResult.Error -> Log.e("PerfScout", "Error: ${frameResult.message}")
    }
}
```

---

### Crash & ANR Monitoring

**Set up listeners:**
```kotlin
PerfScoutMetrics.setCrashListener { crashInfo ->
    Log.e("PerfScout", "Crash detected: $crashInfo")
}
PerfScoutMetrics.setAnrListener { anrInfo ->
    Log.w("PerfScout", "ANR detected: $anrInfo")
}
```

**How Crash Monitoring Works:**
- The crash listener will be called for **any uncaught exception** in the host app, any library, or PerfScout itself.
- It does **not** catch exceptions that are handled by try/catch.
- It does **not** prevent the app from crashing; the process will still terminate after your callback.

| Scenario                        | Will PerfScout crash listener be called? |
|----------------------------------|:---------------------------------------:|
| Uncaught exception in host code  | Yes                                     |
| Uncaught exception in library    | Yes                                     |
| Uncaught exception in PerfScout  | Yes                                     |
| Exception caught by try/catch    | No                                      |
| ANR (App Not Responding)         | No (use ANR listener)                   |

**Demo in Sample App:**
- In the "Advanced Features" section, use the "Trigger Crash" button to throw a deliberate exception.
- Use the "Trigger ANR" button to block the main thread for 10 seconds.
- Your listeners will be called when these events occur.

---

### StrictMode, Jank, and More

```kotlin
PerfScout.setJankListener { jankInfo ->
    Log.w("PerfScout", "Jank detected: $jankInfo")
}
PerfScout.enableStrictMode(penaltyCallback = { violation ->
    Log.w("PerfScout", "StrictMode violation: $violation")
})
```

---

## Notes & Best Practices

- All metrics are safe: if a metric is unavailable, you get a `PerfResult.Error`.
- Startup time tracking requires the host app to call the lifecycle methods.
- Crash/ANR demo buttons are for testing only—remove in production.
- Frame rendering analysis requires UI thread access and should be used during user interaction.
- Thermal information availability varies by device and Android version.
- Network quality measurements may not work on all devices or network configurations.
- GPU info may not be available on all devices.
- All features are modular and can be used independently.
- Memory leak warning is a simple heuristic: it triggers if memory usage grows by a set threshold over a rolling window. For deep leak analysis, use LeakCanary in your app.
- Crash monitoring uses a global uncaught exception handler and will not interfere with existing crash reporting (it chains to previous handlers).
- Jank monitoring reports the percentage of slow (>16ms) and frozen (>700ms) frames every 5 seconds.
- StrictMode violation reporting is opt-in and should only be enabled in debug builds. It helps catch bad practices that can lead to Vitals issues.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

© 2024 PerfScout 
