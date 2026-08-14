package com.avfusionapps.game_2048.utils

import android.app.Activity
import android.util.Log
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {
    private const val TAG = "AdManager"
    private const val APP_KEY = "25689f1d5"

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    fun initialize(activity: Activity) {
        // Create the init request
        val initRequest = LevelPlayInitRequest.Builder(APP_KEY)
            .build()

        val initListener = object : LevelPlayInitListener {
            override fun onInitFailed(error: LevelPlayInitError) {
                Log.e(TAG, "LevelPlay initialization failed: ${error.errorMessage}")
                _isInitialized.value = false
            }

            override fun onInitSuccess(configuration: com.unity3d.mediation.LevelPlayConfiguration) {
                 Log.d(TAG, "LevelPlay initialization successful")
                 _isInitialized.value = true
            }
        }

        LevelPlay.init(activity, initRequest, initListener)
    }
}
