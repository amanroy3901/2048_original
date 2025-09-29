package com.avfusionapps.game_2048

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.avfusionapps.game_2048.notification.ReminderManager
import com.avfusionapps.game_2048.ui.screens.GameScreen
import com.avfusionapps.game_2048.ui.screens.MainScreen
import com.avfusionapps.game_2048.ui.screens.SplashScreen
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.startUpdateFlowForResult
import kotlinx.coroutines.launch
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions


class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateRequestCode = 100
    private lateinit var snackbarHostState: SnackbarHostState
    private lateinit var reminderManager: ReminderManager


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
                    Log.d("AppUpdate", "Update flow failed with result code: ${result.resultCode}")
                    lifecycleScope.launch {
                        showSnackbar("Update failed. Please try again later.")
                    }
                }
            }
        }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                lifecycleScope.launch {
                    showSnackbar("Update downloaded. Restart the app to install.")
                }
                appUpdateManager.completeUpdate()
            }
            InstallStatus.FAILED -> {
                lifecycleScope.launch {
                    showSnackbar("Update failed. We will try again later.")
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Notification", "Permission granted")
        } else {
            Log.d("Notification", "Permission denied")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("Notification", "Permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        reminderManager = ReminderManager(this)
        appUpdateManager.registerListener(installStateUpdatedListener)


        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            _2048OriginalTheme {
                snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()

                LaunchedEffect(key1 = Unit) {
                    requestNotificationPermission()
                    checkForUpdates()
                }

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
                        composable(
                            route = "game?resume={resume}",
                            arguments = listOf(
                                navArgument("resume") {
                                    type = NavType.StringType
                                    defaultValue = "false"
                                    nullable = true
                                }
                            )
                        ) {
                            GameScreen(navController)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save the current game state when the app is paused
        val viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        viewModel.saveCurrentGameState()
    }
    
    override fun onResume() {
        super.onResume()
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
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private fun checkForUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
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
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
                Log.d("AppUpdate", "Update available, starting flexible update flow")
            } catch (e: Exception) {
                Log.e("AppUpdate", "Failed to start update flow: $e")
                showSnackbar("Failed to start update. Please try again later.")
            }
        }
    }

    private suspend fun showSnackbar(message: String) {
        if (::snackbarHostState.isInitialized) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        } else {
            Log.e("Snackbar", "SnackbarHostState not initialized. Cannot show snackbar.")
        }
    }
}