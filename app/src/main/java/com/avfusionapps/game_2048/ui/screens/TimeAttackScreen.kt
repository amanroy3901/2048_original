package com.avfusionapps.game_2048.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.Purple80
import com.avfusionapps.game_2048.ui.theme.PurpleDarkBackground
import com.avfusionapps.game_2048.viewmodel.Direction
import com.avfusionapps.game_2048.ui.components.TimeAttackTopBar
import com.avfusionapps.game_2048.ui.components.FloatingBonus
import com.avfusionapps.game_2048.ui.components.GameTutorialOverlay
import com.avfusionapps.game_2048.ui.components.GameScoreBoard
import com.avfusionapps.game_2048.ui.components.GameSwipeIndicator
import com.avfusionapps.game_2048.ui.components.TimeAttackBottomBar
import com.avfusionapps.game_2048.ui.components.GameBoard
import com.avfusionapps.game_2048.ui.components.TimeAttackGameOverDialog
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.model.TimeAttackState
import com.avfusionapps.game_2048.viewmodel.TimeAttackViewModel
import androidx.activity.compose.BackHandler
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TimeAttackScreen(
    navController: NavController,
    viewModel: TimeAttackViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState(initial = TimeAttackState())
    val highScore by viewModel.highScore.collectAsState(initial = 0)
    val hasSeenTimeAttackTutorial by viewModel.hasSeenTimeAttackTutorial.collectAsState()
    var forceShowTutorial by remember { mutableStateOf(false) }
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState(initial = true)
    var floatingBonuses by remember { mutableStateOf(listOf<FloatingBonus>()) }
    val context = LocalContext.current

    val theme = LocalGameTheme.current

    LaunchedEffect(key1 = viewModel) {
        viewModel.timeBonusEvent.collectLatest { bonusText ->
            val newBonus = FloatingBonus(
                id = System.currentTimeMillis() + (0..1000).random(),
                text = bonusText
            )
            floatingBonuses = floatingBonuses + newBonus
        }
    }

    LaunchedEffect(key1 = viewModel, key2 = vibrationEnabled) {
        viewModel.mergeEvent.collectLatest {
            if (vibrationEnabled) {
                println("TimeAttack: Merge event received. Attempting direct vibration.")

                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) { // Check if vibrator exists
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val vibrationEffect = VibrationEffect.createOneShot(
                                50,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                            vibrator.vibrate(vibrationEffect)
                            println("TimeAttack: Direct vibration (Oreo+) attempted.")
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(50) // 50ms vibration
                            println("TimeAttack: Direct vibration (Legacy) attempted.")
                        }
                    } catch (e: Exception) {
                        println("TimeAttack: Error during direct vibration: ${e.message}")
                    }
                } else {
                    println("TimeAttack: Device does not have a vibrator or service not found.")
                }
            }
        }
    }

    BackHandler(enabled = !gameState.isGameOver) {
        if (!gameState.isPaused) {
            viewModel.togglePause()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("TimeAttackScreen_Root")
            .background(theme.backgroundColor)
            .padding(start = 16.dp, end = 16.dp, top = 50.dp)
            .pointerInput(Unit) {
                var totalX = 0f
                var totalY = 0f
                detectDragGestures(
                    onDragEnd = {
                        if (!gameState.isGameOver) {
                            val minSwipeDistance = 50
                            when {
                                abs(totalX) > abs(totalY) && abs(totalX) > minSwipeDistance -> {
                                    viewModel.onSwipe(if (totalX > 0) Direction.RIGHT else Direction.LEFT)
                                }

                                abs(totalY) > abs(totalX) && abs(totalY) > minSwipeDistance -> {
                                    viewModel.onSwipe(if (totalY > 0) Direction.DOWN else Direction.UP)
                                }
                            }
                        }
                        totalX = 0f; totalY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalX += dragAmount.x
                    totalY += dragAmount.y
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        TimeAttackTopBar(
            timeRemainingMillis = gameState.timeRemainingMillis,
            isPaused = gameState.isPaused,
            onPauseToggle = { viewModel.togglePause() },
            onHelpClick = {
                forceShowTutorial = true
                viewModel.setPaused(true)
            },
            onBack = { navController.popBackStack() },
            floatingBonuses = floatingBonuses,
            onBonusAnimationFinished = { id ->
                floatingBonuses = floatingBonuses.filter { it.id != id }
            }
        )

        GameScoreBoard(
            score = gameState.score,
            highScore = highScore
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Game grid
        GameBoard(
            grid = gameState.grid,
            tileAnimationInfo = gameState.tileAnimationInfo,
            moveCount = gameState.moveCount,
            onAnimationsComplete = { viewModel.clearAnimationInfo() }
        )

        Spacer(modifier = Modifier.weight(1f))

        GameSwipeIndicator()

        TimeAttackBottomBar(
            onUndoClick = { viewModel.undoMove() },
            onNewGameClick = { viewModel.startNewGame() },
            onHintClick = { viewModel.showHint(context) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Pause overlay
    val isTutorialShowing = (hasSeenTimeAttackTutorial == false || forceShowTutorial)
    if (gameState.isPaused && !isTutorialShowing) {
        com.avfusionapps.game_2048.ui.components.GamePauseDialog(
            currentScore = gameState.score,
            onResume = { viewModel.togglePause() },
            onRestart = { viewModel.startNewGame() },
            onQuit = { navController.popBackStack() },
            onDismiss = { viewModel.togglePause() }
        )
    }

    // Game Over overlay
    if (gameState.isGameOver) {
        TimeAttackGameOverDialog(
            finalScore = gameState.score,
            timeSurvived = gameState.totalTimeConfiguredMillis - gameState.timeRemainingMillis,
            isTimeUp = gameState.timeRemainingMillis <= 0,
            onPlayAgain = { viewModel.startNewGame() },
            onExit = { navController.popBackStack() }
        )
    }

    if (hasSeenTimeAttackTutorial == false || forceShowTutorial) {
        GameTutorialOverlay(
            isTimeAttack = true,
            onDismiss = {
                viewModel.setHasSeenTimeAttackTutorial(true)
                forceShowTutorial = false
                viewModel.setPaused(false)
            }
        )
    }
}

@Composable
private fun BonusNotification(bonus: com.avfusionapps.game_2048.model.BonusType) {
    val theme = LocalGameTheme.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = theme.primaryColor.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color.White
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = bonus.message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}






