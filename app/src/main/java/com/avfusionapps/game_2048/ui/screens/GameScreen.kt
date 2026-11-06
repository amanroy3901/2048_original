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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.components.GameTutorialOverlay
import com.avfusionapps.game_2048.ui.components.GridSizeBottomSheet
import com.avfusionapps.game_2048.ui.components.GameBoard
import com.avfusionapps.game_2048.ui.components.GameOverDialog
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.ui.components.AnimatedLevelUnlockDialog
import com.avfusionapps.game_2048.viewmodel.Direction
import com.avfusionapps.game_2048.utils.SoundManager
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.avfusionapps.game_2048.viewmodel.GameState
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.log2
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.avfusionapps.game_2048.ui.components.SquareIconButton
import com.avfusionapps.game_2048.ui.components.NeonCard
import androidx.compose.material3.Icon

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
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val hasSeenClassicTutorial by viewModel.hasSeenClassicTutorial.collectAsState()
    var forceShowTutorial by remember { mutableStateOf(false) }

    var showGridSizeDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(true) }
    val canUndo by viewModel.canUndo.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val seenSprites by viewModel.seenSprites.collectAsState()
    var showCollection by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    // Sprite sound effects: one cue per move (slide / merge / milestone / game over).
    LaunchedEffect(viewModel, soundEnabled) {
        viewModel.soundEvent.collect { soundId ->
            // The sprite sound set: slide, a merge chime pitched by the sprite made, a pop as the new one lands.
            val s = viewModel.gameState
            when (soundId) {
                SoundManager.SOUND_MOVE -> { com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.SLIDE); com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.POP, delayMs = 335) }
                SoundManager.SOUND_MERGE -> {
                    val top = s.tileAnimationInfo.filterValues { it.isMerged }.keys.maxOfOrNull { (r, c) -> s.grid.getOrNull(r)?.getOrNull(c) ?: 0 } ?: 4
                    com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.SLIDE)
                    com.avfusionapps.game_2048.utils.SpriteSfx.merge(top, delayMs = 130)
                    if ((s.lastGain?.multiplier ?: 1) >= 2) com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.COMBO, delayMs = 260)
                    com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.POP, delayMs = 335)
                }
                SoundManager.SOUND_LEVEL_UP -> com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.FANFARE, delayMs = 130)
                SoundManager.SOUND_GAME_OVER -> com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.SAD)
                else -> Unit
            }
        }
    }

    LaunchedEffect(key1 = viewModel, key2 = vibrationEnabled) {
        viewModel.mergeEvent.collectLatest {
            if (vibrationEnabled) {
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
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.saveCurrentGameState()
            }
        }
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
    
    SpriteClassicLayout(
        viewModel = viewModel,
        gameState = gameState,
        best = persistentHighScore,
        coins = coins,
        soundEnabled = soundEnabled,
        onMenu = { showExitDialog = true },
        onHelp = { forceShowTutorial = true },
        onCollection = { showCollection = true },
        onProfile = {
            viewModel.saveCurrentGameState()
            navController.navigate("profile")
        }
    )

    if (showCollection) {
        com.avfusionapps.game_2048.ui.components.SpriteCollectionSheet(
            seen = seenSprites,
            onDismiss = { showCollection = false }
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

    LaunchedEffect(gameState.isGameOver) {
        if (!gameState.isGameOver) {
            showGameOverDialog = true
        }
    }

    if (gameState.isGameOver && showGameOverDialog) {
        GameOverDialog(
            score = gameState.score,
            onNewGame = {
                showGameOverDialog = false
                showGridSizeDialog = true
            },
            onExit = { navController.navigateUp() }
        )
    }

    newlyUnlockedTileValue?.let { unlockedTileValue ->
        AnimatedLevelUnlockDialog(
            unlockedTileValue = unlockedTileValue,
            soundEnabled = soundEnabled,
            onDismiss = {
                viewModel.consumeNewlyUnlockedLevel()
            }
        )
    }

    if (hasSeenClassicTutorial == false || forceShowTutorial) {
        GameTutorialOverlay(
            isTimeAttack = false,
            onDismiss = {
                viewModel.setHasSeenClassicTutorial(true)
                forceShowTutorial = false
            }
        )
    }
}
