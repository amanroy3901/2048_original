package com.avfusionapps.game_2048.shared.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What the host app plugs in to send crash reports and gameplay stats (Firebase on iOS).
 * Shared code never talks to an SDK directly; it goes through [Telemetry], which honours the player's choices.
 */
interface TelemetryBackend {
    fun setAnalyticsEnabled(enabled: Boolean)
    fun setCrashReportsEnabled(enabled: Boolean)
    fun logEvent(name: String, params: Map<String, String>)
    /** A breadcrumb attached to the next crash report. */
    fun log(message: String)
    fun recordException(name: String, reason: String, stack: List<String>)
}

/**
 * The player's consent and the one door to the backend.
 *
 * - Crash reports: on unless the player turns them off in Profile.
 * - Gameplay stats: off until the player taps Allow on the consent card (or turns them on in Profile).
 *   [shouldAsk] stays true until they answer, either way, and the card never comes back after that.
 */
object Telemetry {
    private const val CRASH_KEY = "telemetry_crash_reports"
    private const val ANALYTICS_KEY = "telemetry_analytics" // "granted" / "denied"; absent = not asked yet

    private var backend: TelemetryBackend? = null
    private var loaded = false

    var crashReports by mutableStateOf(true)
        private set
    var analytics by mutableStateOf(false)
        private set
    var shouldAsk by mutableStateOf(false)
        private set

    private fun load() {
        if (loaded) return
        loaded = true
        crashReports = KeyValueStore.getBool(CRASH_KEY, true)
        val a = KeyValueStore.getString(ANALYTICS_KEY)
        analytics = a == "granted"
        shouldAsk = a == null
    }

    /** Called once by the host app at launch, after the SDK is configured. */
    fun install(backend: TelemetryBackend) {
        load()
        this.backend = backend
        backend.setCrashReportsEnabled(crashReports)
        backend.setAnalyticsEnabled(analytics)
        installCrashHook { recordThrowable(it) }
    }

    /** Makes the saved choices readable by the UI even when no backend is installed (Android, previews). */
    fun ensureLoaded() = load()

    fun setCrashReportsEnabled(enabled: Boolean) {
        load()
        crashReports = enabled
        KeyValueStore.putBool(CRASH_KEY, enabled)
        backend?.setCrashReportsEnabled(enabled)
    }

    /** The answer to the consent card, or the Profile switch. */
    fun setAnalyticsEnabled(enabled: Boolean) {
        load()
        analytics = enabled
        shouldAsk = false
        KeyValueStore.putString(ANALYTICS_KEY, if (enabled) "granted" else "denied")
        backend?.setAnalyticsEnabled(enabled)
    }

    /** A gameplay event (same names as the Android app). Dropped unless the player allowed stats. */
    fun event(name: String, vararg params: Pair<String, Any>) {
        if (!analytics) return
        backend?.logEvent(name, params.associate { (k, v) -> k to v.toString() })
    }

    /** A breadcrumb for crash reports (no personal data). */
    fun breadcrumb(message: String) {
        if (crashReports) backend?.log(message)
    }

    fun recordThrowable(t: Throwable) {
        if (!crashReports) return
        val b = backend ?: return
        b.recordException(t::class.simpleName ?: "Throwable", t.message ?: "", t.stackTraceToString().lines().take(64))
    }
}

/** Sends an uncaught Kotlin exception to [onCrash] before the app goes down (iOS); no-op on Android. */
internal expect fun installCrashHook(onCrash: (Throwable) -> Unit)
