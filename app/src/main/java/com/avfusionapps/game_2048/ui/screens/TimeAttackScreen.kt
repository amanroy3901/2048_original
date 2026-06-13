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
import com.avfusionapps.game_2048.ui.components.GameScoreBoard
import com.avfusionapps.game_2048.ui.components.GameSwipeIndicator
import com.avfusionapps.game_2048.ui.components.TimeAttackBottomBar
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.viewmodel.TimeAttackViewModel
import com.avfusionapps.game_2048.model.TimeAttackState
import kotlin.math.abs

@Composable
fun TimeAttackScreen(
    navController: NavController,
    viewModel: TimeAttackViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState(initial = TimeAttackState())
    val highScore by viewModel.highScore.collectAsState(initial = 0)
    val context = LocalContext.current

    val theme = LocalGameTheme.current

    LaunchedEffect(true) {
        // No-op for now
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            onBack = { navController.popBackStack() }
        )

        GameScoreBoard(
            score = gameState.score,
            highScore = highScore
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Last bonus notification
        AnimatedVisibility(
            visible = gameState.lastBonus != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            gameState.lastBonus?.let { bonus ->
                BonusNotification(bonus = bonus)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game grid
        TimeAttackGameBoard(gameState.grid)

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
    if (gameState.isPaused) {
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
        TimeAttackGameOverOverlay(
            finalScore = gameState.score,
            timeSurvived = gameState.totalTimeConfiguredMillis - gameState.timeRemainingMillis,
            onPlayAgain = { viewModel.startNewGame() },
            onBackToMenu = { navController.popBackStack() }
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

@Composable
private fun TimeAttackGameBoard(grid: List<List<Int>>) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surfaceColor)
            .border(2.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grid.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEachIndexed { j, cellValue ->
                        TimeAttackGameCell(
                            value = cellValue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeAttackGameCell(
    value: Int,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val backgroundColor = theme.tileColors[value] ?: theme.tileColors[0]!!
    val textColor = if (value <= 4) theme.textColor else Color.White

    val textSize = remember(value) {
        when {
            value >= 10000 -> 14.sp
            value >= 1000 -> 20.sp
            value >= 100 -> 24.sp
            else -> 28.sp
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                color = textColor,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}



@Composable
private fun TimeAttackGameOverOverlay(
    finalScore: Int,
    timeSurvived: Long,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val minutes = (timeSurvived / 60000).toInt()
    val seconds = ((timeSurvived % 60000) / 1000).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = PurpleDarkBackground
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "TIME'S UP!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HighLighter,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Final Score: $finalScore",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Text(
                    text = "Time Survived: ${String.format("%02d:%02d", minutes, seconds)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                NeonRoundedButton(
                    text = "Play Again",
                    onClick = onPlayAgain
                )

                NeonRoundedButton(
                    text = "Back to Menu",
                    onClick = onBackToMenu
                )
            }
        }
    }
}
