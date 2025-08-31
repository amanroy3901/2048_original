package com.avfusionapps.game_2048

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.avfusionapps.game_2048.ui.screens.GameScreen
import com.avfusionapps.game_2048.ui.screens.MainScreen
import com.avfusionapps.game_2048.ui.screens.SplashScreen
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.startUpdateFlowForResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateRequestCode = 100
    private lateinit var snackbarHostState: SnackbarHostState

    // Register activity result launcher for update flow
    private val updateResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> Log.d("AppUpdate", "Update flow completed successfully")
                RESULT_CANCELED -> {
                    Log.d("AppUpdate", "User canceled the update")
                    lifecycleScope.launch {
                        showSnackbar("Update canceled. Please update later for the best experience.")
                    }
                }
                else -> {
                    Log.d("AppUpdate", "Update flow failed: ${result.resultCode}")
                    lifecycleScope.launch {
                        showSnackbar("Update failed. Please try again later.")
                    }
                }
            }
        }

    // Listener for flexible update state changes
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                lifecycleScope.launch {
                    showSnackbar("Update downloaded. Restart the app to install.")
                }
                // Trigger app restart to complete the update
                appUpdateManager.completeUpdate()
            }
            InstallStatus.FAILED -> {
                lifecycleScope.launch {
//                    showSnackbar("Update failed. Please try again later.")
                }
            }
            InstallStatus.CANCELED -> {
                lifecycleScope.launch {
                    showSnackbar("Update canceled.")
                }
            }
            else -> {
                Log.d("AppUpdate", "Install status: ${state.installStatus()}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        // Register listener for flexible updates
        appUpdateManager.registerListener(installStateUpdatedListener)

        setContent {
            _2048OriginalTheme {
                // Initialize SnackbarHostState for showing messages
                snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(navController)
                        }
                        composable("main") {
                            MainScreen(navController)
                        }
                        composable("game") {
                            GameScreen(navController)
                        }
                    }
                }
            }
        }

        // Check for updates
        checkForUpdates()
    }

    override fun onResume() {
        super.onResume()
        // Check if an update was downloaded but not yet installed
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                lifecycleScope.launch {
                    showSnackbar("Update downloaded. Restart the app to install.")
                }
                appUpdateManager.completeUpdate()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister listener to prevent memory leaks
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private fun checkForUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                // Start the flexible update flow using Kotlin extension
                startUpdateFlow(appUpdateInfo)
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Resume an in-progress update
                startUpdateFlow(appUpdateInfo)
            } else {
                Log.d("AppUpdate", "No update available or update not allowed")
            }
        }.addOnFailureListener { exception ->
            Log.e("AppUpdate", "Failed to check for updates: $exception")
            lifecycleScope.launch {
                showSnackbar("Failed to check for updates. Please try again later.")
            }
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo){
        lifecycleScope.launch {
            try {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this@MainActivity, // Use the activity directly
                    updateRequestCode
                )
                Log.d("AppUpdate", "Update available, starting flexible update flow")
            } catch (e: Exception) {
                Log.e("AppUpdate", "Failed to start update flow: $e")
                showSnackbar("Failed to start update. Please try again later.")
            }
        }
    }

    private suspend fun showSnackbar(message: String) {
        // Dismiss any existing snackbar before showing a new one
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
    }
}
