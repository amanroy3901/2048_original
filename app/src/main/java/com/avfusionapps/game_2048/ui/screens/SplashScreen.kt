package com.avfusionapps.game_2048.ui.screens

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
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.PurpleDarkBackground
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data class remains the same
data class TileState(
    val id: Char, // Now the initial number
    val targetChar: Char, // Now the target letter
    val displayChar: MutableState<Char>,
    val offsetY: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>
)

@Composable
fun rememberTileState(
    initialChar: Char, // Will be the number ('2', '0', '4', '8')
    targetChar: Char,  // Will be the letter ('E', 'S', 'H', 'A')
    initialOffsetY: Dp = (-100).dp,
    initialAlpha: Float = 0f,
    initialScale: Float = 1f
): TileState {
    // Initialize displayChar with the initial number
    val displayCharState = remember { mutableStateOf(initialChar) }
    val initialOffsetYFloat = remember { initialOffsetY.value }
    return remember {
        TileState(
            id = initialChar,       // Keep original ID as the number if needed, or use targetChar
            targetChar = targetChar, // Store the letter
            displayChar = displayCharState, // Start showing the number
            offsetY = Animatable(initialOffsetYFloat),
            alpha = Animatable(initialAlpha),
            scale = Animatable(initialScale)
        )
    }
}

// Tile Composable remains the same
@Composable
fun Tile(
    state: TileState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Box(
        modifier = modifier
            .offset(y = state.offsetY.value.dp)
            .scale(state.scale.value)
            .alpha(state.alpha.value)
            .size(75.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = state.displayChar.value.toString(),
            color = contentColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


// Previews remain the same
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
    val tilesData = remember {
        // Initial character is the number, target is the letter
        listOf(
            '2' to 'E', '0' to 'S', '4' to 'H', '8' to 'A'
        )
    }

    val tileStates = tilesData.map { (initial, target) ->
        // Pass the number as initialChar, letter as targetChar
        rememberTileState(initialChar = initial, targetChar = target)
    }
    // --- End Tile Animation Setup ---

    LaunchedEffect(key1 = true) {
        // --- Animation Sequence ---
        val dropDelay = 150L
        val glowDelay = 100L // Delay between glows starting
        val moveUpDelay = 100L // Delay between move-ups starting
        val pauseAfterDrop = 300L
        val pauseAfterGlow = 500L // Pause after the glow sequence
        val pauseBeforeNav = 200L // Final pause before navigating

        // Animation Specs
        val dropOffsetYSpec = tween<Float>(durationMillis = 400, easing = LinearOutSlowInEasing)
        val dropAlphaSpec = tween<Float>(durationMillis = 200)
        val glowScaleSpec = tween<Float>(durationMillis = 200, easing = LinearEasing) // For glow pulse
        // Combined spec for moving up, fading out, and scaling down
        val moveUpCombinedSpec = tween<Float>(durationMillis = 200) // Adjust duration as needed


        // 1. Drop tiles (showing numbers) sequentially
        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * dropDelay)
                launch { state.offsetY.animateTo(0f, animationSpec = dropOffsetYSpec) }
                launch { state.alpha.animateTo(1f, animationSpec = dropAlphaSpec) }
            }
        }
        // Wait for all drops to roughly finish
        val dropDuration = dropOffsetYSpec.durationMillis // Use the longer duration
        delay(tileStates.size * dropDelay + dropDuration)
        delay(pauseAfterDrop)

        // 2. Glow tiles sequentially (scale pulse)
        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * glowDelay)
                state.scale.animateTo(1.2f, animationSpec = glowScaleSpec)
                state.scale.animateTo(1.0f, animationSpec = glowScaleSpec)
            }
        }
        // Wait for glows to finish
        val glowDuration = glowScaleSpec.durationMillis * 2
        delay(tileStates.size * glowDelay + glowDuration)
        delay(pauseAfterGlow)

        // 3. Move tiles up, scale down, fade out, and transform to letters sequentially
        val targetOffsetY = (-200).dp.value // Target Y offset (off-screen top)
        val targetScale = 1.5f              // Target scale when vanishing
        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * moveUpDelay)
                // Change character to the letter *at the start* of this tile's move-up animation
                state.displayChar.value = state.targetChar

                // Animate move up, fade out, and scale down simultaneously
                launch { state.offsetY.animateTo(targetOffsetY, animationSpec = moveUpCombinedSpec) }
                launch { state.alpha.animateTo(0f, animationSpec = moveUpCombinedSpec) }
                launch { state.scale.animateTo(targetScale, animationSpec = moveUpCombinedSpec) }
            }
        }
        // Wait for vanishing
        val moveUpDuration = moveUpCombinedSpec.durationMillis
        delay(tileStates.size * moveUpDelay + moveUpDuration)
        delay(pauseBeforeNav) // Final short pause
        // --- End Animation Sequence ---


        // --- Navigation ---
        navController.navigate("main") {
            popUpTo("splash") { inclusive = true }
        }
        // --- End Navigation ---
    }

    // --- Layout --- (Remains the same)
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = PurpleDarkBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tileStates.forEach { state ->
                    Tile(
                        state = state,
                        backgroundColor = HighLighter,
                        contentColor = Color.White
                    )
                }
            }
        }
    }
    // --- End Layout ---
}