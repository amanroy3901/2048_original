package com.avfusionapps.game_2048

// Removed deprecated Google Sign-In imports - now using Credential Manager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avfusionapps.game_2048.notification.ReminderManager
import com.avfusionapps.game_2048.ui.screens.GameScreen
import com.avfusionapps.game_2048.ui.screens.GoogleAuthScreen
import com.avfusionapps.game_2048.ui.screens.MainScreen
import com.avfusionapps.game_2048.ui.screens.SplashScreen
import com.avfusionapps.game_2048.ui.screens.ThemeSettingsScreen
import com.avfusionapps.game_2048.ui.screens.TimeAttackScreen
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.avfusionapps.game_2048.viewmodel.ThemeViewModel
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var snackbarHostState: SnackbarHostState
    private lateinit var reminderManager: ReminderManager
    private lateinit var firebaseAuth: FirebaseAuth


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

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        appUpdateManager = AppUpdateManagerFactory.create(this)
        reminderManager = ReminderManager(this)
        firebaseAuth = Firebase.auth
        
        initializeFirebaseAuth()
        appUpdateManager.registerListener(installStateUpdatedListener)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val currentTheme by themeViewModel.currentTheme.collectAsState(initial = GameTheme.NeonPink)

            _2048OriginalTheme(theme = currentTheme) {
                val navController = rememberNavController()
                snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(key1 = Unit) {
                    requestNotificationPermission()
                    checkForUpdates()
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        route = "root",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("splash") { backStackEntry ->
                            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("root") }
                            val vm: GameViewModel = viewModel(parentEntry)
                            SplashScreen(navController = navController, onSplashComplete = {
                                if (firebaseAuth.currentUser == null) {
                                    navController.navigate("googleAuth") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("main") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            })
                        }
                        composable("googleAuth") { backStackEntry ->
                            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("root") }
                            val vm: GameViewModel = viewModel(parentEntry)
                            GoogleAuthScreen(
                                firebaseAuth = firebaseAuth,
                                onAuthSuccess = {
                                    vm.loadUserDataFromFirebase()
                                    navController.navigate("main") {
                                        popUpTo("googleAuth") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("main") { backStackEntry ->
                            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("root") }
                            val vm: GameViewModel = viewModel(parentEntry)
                            MainScreen(navController = navController, viewModel = vm)
                        }
                        composable("themeSettings") {
                            ThemeSettingsScreen(navController = navController)
                        }
                        composable("timeAttack") {
                            TimeAttackScreen(navController = navController)
                        }
                        composable(
                            route = "game?resume={resume}",
                            arguments = listOf(
                                navArgument("resume") {
                                    type = NavType.StringType
                                    defaultValue = "false"
                                    nullable = true
                                },
                                navArgument("newGame") {
                                    type = NavType.StringType
                                    defaultValue = "false"
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("root") }
                            val vm: GameViewModel = viewModel(parentEntry)
                            GameScreen(navController = navController, viewModel = vm)
                        }
                    }
                }
            }
        }
    }

    private fun initializeFirebaseAuth() {
        if (firebaseAuth.currentUser == null) {
            Log.d("FirebaseAuth", "No authenticated user found. User will be prompted for Google authentication.")
        } else {
            Log.d("FirebaseAuth", "User already authenticated: ${firebaseAuth.currentUser?.uid}")
            Log.d("FirebaseAuth", "Email: ${firebaseAuth.currentUser?.email}")
            Log.d("FirebaseAuth", "Display Name: ${firebaseAuth.currentUser?.displayName}")
        }
    }

    override fun onPause() {
        super.onPause()
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
