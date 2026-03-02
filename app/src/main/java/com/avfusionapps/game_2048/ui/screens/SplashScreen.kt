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
import androidx.compose.ui.platform.testTag
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

data class TileState(
    val id: Char,
    val targetChar: Char,
    val displayChar: MutableState<Char>,
    val offsetY: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>
)

@Composable
fun rememberTileState(
    initialChar: Char,
    targetChar: Char,
    initialOffsetY: Dp = (-100).dp,
    initialAlpha: Float = 0f,
    initialScale: Float = 1f
): TileState {
    val displayCharState = remember { mutableStateOf(initialChar) }
    val initialOffsetYFloat = remember { initialOffsetY.value }
    return remember {
        TileState(
            id = initialChar,
            targetChar = targetChar,
            displayChar = displayCharState,
            offsetY = Animatable(initialOffsetYFloat),
            alpha = Animatable(initialAlpha),
            scale = Animatable(initialScale)
        )
    }
}


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
fun SplashScreen(
    navController: NavController,
    onSplashComplete: () -> Unit = {
        navController.navigate("main") {
            popUpTo("splash") { inclusive = true }
        }
    }
) {
    val tilesData = remember {
        listOf(
            '2' to 'E', '0' to 'S', '4' to 'H', '8' to 'A'
        )
    }

    val tileStates = tilesData.map { (initial, target) ->
        rememberTileState(initialChar = initial, targetChar = target)
    }


    LaunchedEffect(key1 = true) {
        val dropDelay = 150L
        val glowDelay = 100L
        val moveUpDelay = 100L
        val pauseAfterDrop = 300L
        val pauseAfterGlow = 500L
        val pauseBeforeNav = 200L

        val dropOffsetYSpec = tween<Float>(durationMillis = 400, easing = LinearOutSlowInEasing)
        val dropAlphaSpec = tween<Float>(durationMillis = 200)
        val glowScaleSpec = tween<Float>(durationMillis = 200, easing = LinearEasing)
        val moveUpCombinedSpec = tween<Float>(durationMillis = 200)



        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * dropDelay)
                launch { state.offsetY.animateTo(0f, animationSpec = dropOffsetYSpec) }
                launch { state.alpha.animateTo(1f, animationSpec = dropAlphaSpec) }
            }
        }
        val dropDuration = dropOffsetYSpec.durationMillis
        delay(tileStates.size * dropDelay + dropDuration)
        delay(pauseAfterDrop)


        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * glowDelay)
                state.scale.animateTo(1.2f, animationSpec = glowScaleSpec)
                state.scale.animateTo(1.0f, animationSpec = glowScaleSpec)
            }
        }
        val glowDuration = glowScaleSpec.durationMillis * 2
        delay(tileStates.size * glowDelay + glowDuration)
        delay(pauseAfterGlow)

        val targetOffsetY = (-200).dp.value
        val targetScale = 1.5f
        tileStates.forEachIndexed { index, state ->
            launch {
                delay(index * moveUpDelay)
                state.displayChar.value = state.targetChar
                launch { state.offsetY.animateTo(targetOffsetY, animationSpec = moveUpCombinedSpec) }
                launch { state.alpha.animateTo(0f, animationSpec = moveUpCombinedSpec) }
                launch { state.scale.animateTo(targetScale, animationSpec = moveUpCombinedSpec) }
            }
        }
        val moveUpDuration = moveUpCombinedSpec.durationMillis
        delay(tileStates.size * moveUpDelay + moveUpDuration)
        delay(pauseBeforeNav)
        onSplashComplete()
    }


    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("SplashScreen_Root"),
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

}