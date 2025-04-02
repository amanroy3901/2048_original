package com.ignitarium.game_2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ignitarium.game_2048.ui.screens.GameScreen
import com.ignitarium.game_2048.ui.screens.MainScreen
import com.ignitarium.game_2048.ui.screens.SplashScreen
import com.ignitarium.game_2048.ui.theme._2048OriginalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        // Enable edge-to-edge explicitly if not already enabled in theme
//        enableEdgeToEdge()
        setContent {
            _2048OriginalTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding) // Apply innerPadding here
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
    }
}