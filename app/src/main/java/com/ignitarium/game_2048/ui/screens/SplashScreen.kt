package com.ignitarium.game_2048.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ignitarium.game_2048.R // Import your R file
import com.ignitarium.game_2048.ui.theme.HighLighter
import com.ignitarium.game_2048.ui.theme.Purple80
import com.ignitarium.game_2048.ui.theme._2048OriginalTheme
import kotlinx.coroutines.delay

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkModePreviewSplash() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        SplashScreen(navController = previewNavController)

    }
}

//If you want to have a dynamic color preview:
@Preview(showSystemUi = true, apiLevel = 31)
@Composable
fun DynamicDarkModePreviewSplash() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SplashScreen(navController = previewNavController)
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    var logoAppearing by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        delay(1000) // Delay before logo appears
        logoAppearing = true // Trigger logo appearance
        delay(1500) // Delay after logo appears
        navController.navigate("main") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background( brush = Brush.radialGradient(
                colors = listOf(
                    HighLighter, // Hot Pink
                    HighLighter.copy(0.8f),
                    Purple80  // Purple
                ),
                center = Offset(200f, 200f)
            )),
        contentAlignment = Alignment.Center
    ) {
        // Logo Image
        val logoAlpha by animateFloatAsState(
            targetValue = if (logoAppearing) 1f else 0f,
            animationSpec = tween(durationMillis = 1000),
            label = "Logo Fade Animation"
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_logo), // Replace with your logo drawable
            contentDescription = "App Logo",
            tint = Color.White,
            modifier = Modifier
                .size(350.dp) // Adjust logo size as needed
                .alpha(logoAlpha)
        )
    }
}