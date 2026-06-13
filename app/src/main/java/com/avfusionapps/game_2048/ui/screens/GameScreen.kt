package com.avfusionapps.game_2048.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.NeonCutCornerButton
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.components.ClassicTopBar
import com.avfusionapps.game_2048.ui.components.ClassicBottomBar
import com.avfusionapps.game_2048.ui.components.GameScoreBoard
import com.avfusionapps.game_2048.ui.components.GameSwipeIndicator
import com.avfusionapps.game_2048.ui.components.GridSizeBottomSheet
import com.avfusionapps.game_2048.ui.components.GameBoard
import com.avfusionapps.game_2048.ui.components.GameOverDialog
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.ui.components.AnimatedLevelUnlockDialog
import com.avfusionapps.game_2048.viewmodel.Direction
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.log2
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Game Screen Dark Preview"
)
@Composable
fun DarkModePreviewGame() {
    rememberNavController()
    val theme = LocalGameTheme.current
    _2048OriginalTheme {
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(theme.backgroundColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                "Preview Player",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text("Score: 128", color = Color.White)
                            Text("High Score: 1024", color = Color.White)
                        }
                        Button(onClick = {}) { Text("New Game") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .background(theme.primaryColor)
                    ) {
                        Text(
                            "4x4 Grid Preview",
                            Modifier.align(Alignment.Center),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    navController: NavController,
    viewModel: GameViewModel = viewModel(),
    ) {

    val theme = LocalGameTheme.current
    val gameState = viewModel.gameState
    val persistentHighScore by viewModel.persistentHighScore.collectAsState()
    val persistentPlayerName by viewModel.persistentPlayerName.collectAsState()
    val newlyUnlockedTileValue by viewModel.newlyUnlockedTileValue.collectAsState()

    var showGridSizeDialog by remember { mutableStateOf(false) }
    val canUndo by viewModel.canUndo.collectAsState()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = viewModel) {
        viewModel.mergeEvent.collectLatest {
            println("Merge event received. Attempting direct vibration.")

            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) { // Check if vibrator exists
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val vibrationEffect = VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                        vibrator.vibrate(vibrationEffect)
                        println("Direct vibration (Oreo+) attempted.")
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50) // 50ms vibration
                        println("Direct vibration (Legacy) attempted.")
                    }
                } catch (e: Exception) {
                    println("Error during direct vibration: ${e.message}")
                }
            } else {
                println("Device does not have a vibrator or service not found.")
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ -> /* no-op */ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasProgress = viewModel.gameState.moveCount > 0 ||
            viewModel.gameState.grid.any { row -> row.any { it != 0 } }

    BackHandler(enabled = true) {
        if (hasProgress) {
            showExitDialog = true
        } else {
            navController.navigateUp()
        }
    }

    if (showExitDialog) {
        com.avfusionapps.game_2048.ui.components.GamePauseDialog(
            currentScore = gameState.score,
            onResume = { showExitDialog = false },
            onRestart = {
                showExitDialog = false
                showGridSizeDialog = true
            },
            onQuit = {
                viewModel.markResumableWithoutMove()
                showExitDialog = false
                navController.navigateUp()
            },
            onDismiss = { showExitDialog = false }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("GameScreen_Root")
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
                                    viewModel.move(if (totalX > 0) Direction.RIGHT else Direction.LEFT)
                                }

                                abs(totalY) > abs(totalX) && abs(totalY) > minSwipeDistance -> {
                                    viewModel.move(if (totalY > 0) Direction.DOWN else Direction.UP)
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
        ClassicTopBar(
            currentLevel = gameState.currentLevel,
            onSettingsClick = { showExitDialog = true },
            onBack = {
                if (hasProgress) {
                    showExitDialog = true
                } else {
                    navController.navigateUp()
                }
            }
        )

        GameScoreBoard(
            score = gameState.score,
            highScore = persistentHighScore,
            scoreIcon = Icons.Rounded.EmojiEvents, // Trophy
            highScoreIcon = Icons.Rounded.WorkspacePremium, // Ribbon/Crown
            highScoreTint = theme.secondaryColor // Replaced hardcoded Gold color with theme.secondaryColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        GameBoard(viewModel = viewModel)

        Spacer(modifier = Modifier.weight(1f))

        GameSwipeIndicator()

        ClassicBottomBar(
            onUndoClick = { viewModel.undoMove(context) },
            onRestartClick = { showGridSizeDialog = true },
            onHintClick = { viewModel.showHint(context) }
        )
    }

    if (showGridSizeDialog) {
        GridSizeBottomSheet(
            currentSize = gameState.gridSize,
            onSizeSelected = { size ->
                viewModel.updateGridSize(size)
                showGridSizeDialog = false
            },
            onDismiss = {
                if (gameState.grid.all { r -> r.all { it == 0 } }) {
                    navController.navigateUp()
                } else {
                    showGridSizeDialog = false
                }
            }
        )
    }

    if (gameState.isGameOver) {
        GameOverDialog(
            score = gameState.score,
            onNewGame = {
                showGridSizeDialog = true
            },
            onExit = { navController.navigateUp() }
        )
    }

    newlyUnlockedTileValue?.let { unlockedTileValue ->
        AnimatedLevelUnlockDialog(
            unlockedTileValue = unlockedTileValue,
            onDismiss = {
                viewModel.consumeNewlyUnlockedLevel()
            }
        )
    }
}


