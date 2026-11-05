package com.avfusionapps.game_2048.shared.platform

/** Android uses its own Crashlytics setup in the app module. */
internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {}
