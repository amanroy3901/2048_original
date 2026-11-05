package com.avfusionapps.game_2048.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.avfusionapps.game_2048.shared.ui.App
import platform.UIKit.UIViewController

/** Entry point for the iOS app: hosts the shared Compose game in a UIViewController. */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
