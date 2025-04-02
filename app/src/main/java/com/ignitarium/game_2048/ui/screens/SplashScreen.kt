package com.ignitarium.game_2048.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ignitarium.game_2048.ui.theme.HighLighter
import com.ignitarium.game_2048.ui.theme.PurpleDarkBackground
import com.ignitarium.game_2048.ui.theme._2048OriginalTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data class to hold the state for a single tile
data class TileState(
    val id: Char, // Original letter
    val targetChar: Char, // Target number
    val displayChar: MutableState<Char>,
    val offsetY: Animatable<Float, AnimationVector1D>, // Changed to Float
    val alpha: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>
)

@Composable
fun rememberTileState(
    initialChar: Char,
    targetChar: Char,
    initialOffsetY: Dp = (-100).dp, // Start above the screen
    initialAlpha: Float = 0f,
    initialScale: Float = 1f
): TileState {
    val displayCharState = remember { mutableStateOf(initialChar) }
    val initialOffsetYFloat = remember { initialOffsetY.value}
    return remember {
        TileState(
            id = initialChar,
            targetChar = targetChar,
            displayChar = displayCharState,
            offsetY = Animatable(initialOffsetYFloat), // Store as Float
            alpha = Animatable(initialAlpha),
            scale = Animatable(initialScale)
        )
    }
}

@Composable
fun Tile(
    state: TileState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary, // Use M3 colorScheme
    contentColor: Color = MaterialTheme.colorScheme.onPrimary // Use M3 colorScheme
) {
    Box(
        modifier = modifier
            .offset(y = state.offsetY.value.dp) // Apply drop animation
            .scale(state.scale.value)      // Apply scale animation (for transform/vanish)
            .alpha(state.alpha.value)      // Apply alpha animation (for appear/vanish)
            .size(60.dp) // Fixed size for the tile
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = state.displayChar.value.toString(),
            color = contentColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkModePreviewSplash() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

            SplashScreen(navController = previewNavController)
        }
    }
}

//If you want to have a dynamic color preview:
@Preview(showSystemUi = true, apiLevel = 31)
@Composable
fun DynamicDarkModePreviewSplash() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SplashScreen(navController = previewNavController)
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    // --- Tile Animation Setup ---
    val tilesData = remember { // Remember the list itself
        listOf(
            'E' to '2', 'S' to '0', 'H' to '4', 'A' to '8'
        )
    }

    // Remember state for each tile
    val tileStates = tilesData.map { (initial, target) ->
        // Pass initial state values if needed, otherwise use defaults in rememberTileState
        rememberTileState(initialChar = initial, targetChar = target)
    }
    // --- End Tile Animation Setup ---


    LaunchedEffect(key1 = true) { // Use Unit or true for one-time effect
        // --- Animation Sequence ---
        val dropDelay = 150L
        val transformDelay = 100L
        val vanishDelay = 100L
        val pauseAfterDrop = 300L
        val pauseAfterTransform = 500L
        val pauseAfterVanish = 200L

        // Animation Specs
        val dropSpec = tween<Dp>(durationMillis = 500, easing = LinearOutSlowInEasing)
        val alphaSpec = tween<Float>(durationMillis = 300)
        val scaleSpec = tween<Float>(durationMillis = 200, easing = LinearEasing) // Simple scale
        val vanishSpec = tween<Float>(durationMillis = 300)


        // 1. Drop tiles sequentially
        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * dropDelay) // Stagger start
                // Animate drop and fade in simultaneously
                launch { state.offsetY.animateTo(0f, animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)) } // Animate to 0f
                launch { state.alpha.animateTo(1f, animationSpec = alphaSpec) }
            }
        }
        // Wait for all drops to roughly finish
        delay(tileStates.size * dropDelay + dropSpec.durationMillis)
        delay(pauseAfterDrop) // Pause after landing

        // 2. Transform letters to numbers sequentially
        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * transformDelay) // Stagger transformation
                // Optional: Quick scale pulse during transformation
                state.scale.animateTo(1.2f, animationSpec = scaleSpec)
                state.displayChar.value = state.targetChar // Change the character
                state.scale.animateTo(1.0f, animationSpec = scaleSpec)
            }
        }
        // Wait for transformations to finish
        delay(tileStates.size * transformDelay + (scaleSpec.durationMillis * 2))
        delay(pauseAfterTransform) // Pause after transformation

        // 3. Vanish tiles sequentially
        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * vanishDelay) // Stagger vanishing
                // Animate fade out and scale down simultaneously
                launch { state.alpha.animateTo(0f, animationSpec = vanishSpec) }
                launch { state.scale.animateTo(0.5f, animationSpec = vanishSpec) }
                // Optional: Move up slightly while vanishing
                // launch { state.offsetY.animateTo((-20).dp, animationSpec = vanishSpec) }
            }
        }
        // Wait for vanishing
        delay(tileStates.size * vanishDelay + vanishSpec.durationMillis)
        delay(pauseAfterVanish) // Final short pause
        // --- End Animation Sequence ---


        // --- Navigation ---
        // Navigate after all animations are complete
        navController.navigate("main") {
            popUpTo("splash") { inclusive = true }
        }
        // --- End Navigation ---
    }

    // --- Layout ---
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = PurpleDarkBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center // Center the Row of tiles
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp) // Space between tiles
            ) {
                tileStates.forEach { state ->
                    // Use colors from your theme for the tiles
                    Tile(
                        state = state,
                        backgroundColor = HighLighter, // Example: Use HighLighter for tile bg
                        contentColor = PurpleDarkBackground // Example: Use background for text
                        // Or use MaterialTheme.colorScheme.primary/onPrimary etc.
                        // backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        // contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
    // --- End Layout ---
}
