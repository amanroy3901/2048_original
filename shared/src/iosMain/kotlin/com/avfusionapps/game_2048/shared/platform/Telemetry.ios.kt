package com.avfusionapps.game_2048.shared.platform

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {
    // Record the Kotlin exception (with its Kotlin stack) first; the default hook then terminates the app,
    // and the SDK's own crash handler captures the native crash as well.
    val previous = setUnhandledExceptionHook { t ->
        runCatching { onCrash(t) }
    }
    if (previous != null) {
        setUnhandledExceptionHook { t ->
            runCatching { onCrash(t) }
            previous(t)
        }
    }
}
