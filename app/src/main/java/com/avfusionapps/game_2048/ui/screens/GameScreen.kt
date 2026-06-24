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
import com.avfusionapps.game_2048.ui.NeonCutCornerButton
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.components.ClassicTopBar
import com.avfusionapps.game_2048.ui.components.GameTutorialOverlay
import com.avfusionapps.game_2048.ui.components.ClassicBottomBar
import com.avfusionapps.game_2048.ui.components.GameScoreBoard
import com.avfusionapps.game_2048.ui.components.GameSwipeIndicator
import com.avfusionapps.game_2048.ui.components.GridSizeBottomSheet
import com.avfusionapps.game_2048.ui.components.GameBoard
import com.avfusionapps.game_2048.ui.components.GameOverDialog
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.ui.components.LevelPlayBanner
import com.avfusionapps.game_2048.ui.components.AnimatedLevelUnlockDialog
import com.avfusionapps.game_2048.viewmodel.Direction
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
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

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
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        GameScreenLandscape(
            viewModel = viewModel,
            gameState = gameState,
            persistentHighScore = persistentHighScore,
            onBack = {
                if (hasProgress) {
                    showExitDialog = true
                } else {
                    navController.navigateUp()
                }
            },
            onSettingsClick = { showExitDialog = true },
            onHelpClick = { forceShowTutorial = true },
            onUndoClick = { viewModel.undoMove(context) },
            onRestartClick = { showGridSizeDialog = true },
            onHintClick = { viewModel.showHint(context) }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("GameScreen_Root")
                .background(theme.backgroundColor)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                onHelpClick = { forceShowTutorial = true },
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

@Composable
private fun GameScreenLandscape(
    viewModel: GameViewModel,
    gameState: GameState,
    persistentHighScore: Int,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRestartClick: () -> Unit,
    onHintClick: () -> Unit
) {
    val theme = LocalGameTheme.current
    val scrollStateLeft = rememberScrollState()
    val scrollStateRight = rememberScrollState()

    // Responsive scaling based on landscape height
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val scaleFactor = (screenHeight / 400f).coerceIn(0.75f, 1.5f)

    val containerPaddingHorizontal = (16 * scaleFactor).dp
    val containerPaddingVertical = (12 * scaleFactor).dp
    val columnSpacing = (16 * scaleFactor).dp
    val itemSpacing = (8 * scaleFactor).dp
    
    val backButtonSize = (40 * scaleFactor).dp
    val levelTitleFontSize = (14 * scaleFactor).sp
    val dotSize = (5 * scaleFactor).dp
    val dotRadius = (2.5 * scaleFactor).dp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .safeDrawingPadding()
            .padding(horizontal = containerPaddingHorizontal, vertical = containerPaddingVertical)
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
        horizontalArrangement = Arrangement.spacedBy(columnSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Left Panel
        Column(
            modifier = Modifier
                .weight(0.28f)
                .fillMaxHeight()
                .verticalScroll(scrollStateLeft),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            // Back Button & Level Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
            ) {
                SquareIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    size = backButtonSize
                )
                Column {
                    Text(
                        text = "LEVEL ${gameState.currentLevel}",
                        color = theme.textColor,
                        fontSize = levelTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height((4 * scaleFactor).dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((4 * scaleFactor).dp)) {
                        Box(modifier = Modifier.size(dotSize).clip(RoundedCornerShape(dotRadius)).background(if (gameState.currentLevel == 1) theme.primaryColor else theme.textColor.copy(alpha = 0.2f)))
                        Box(modifier = Modifier.size(dotSize).clip(RoundedCornerShape(dotRadius)).background(if (gameState.currentLevel == 2) theme.primaryColor else theme.textColor.copy(alpha = 0.2f)))
                        Box(modifier = Modifier.size(dotSize).clip(RoundedCornerShape(dotRadius)).background(if (gameState.currentLevel >= 3) theme.primaryColor else theme.textColor.copy(alpha = 0.2f)))
                    }
                }
            }

            // Stat Cards
            LandscapeStatCard(
                icon = Icons.Rounded.EmojiEvents,
                title = "SCORE",
                value = gameState.score.toString(),
                accentColor = theme.primaryColor,
                scaleFactor = scaleFactor
            )

            LandscapeStatCard(
                icon = Icons.Rounded.WorkspacePremium,
                title = "BEST SCORE",
                value = persistentHighScore.toString(),
                accentColor = theme.secondaryColor,
                scaleFactor = scaleFactor
            )

            val highestTile = gameState.grid.flatten().maxOrNull() ?: 0
            LandscapeStatCard(
                icon = Icons.Rounded.Star,
                title = "HIGHEST TILE",
                value = highestTile.toString(),
                accentColor = theme.primaryColor,
                scaleFactor = scaleFactor
            )

            // Next Target Card
            val nextTarget = when {
                highestTile < 128 -> 128
                highestTile < 256 -> 256
                highestTile < 512 -> 512
                highestTile < 1024 -> 1024
                else -> 2048
            }
            val progress = (highestTile.toFloat() / nextTarget.toFloat()).coerceIn(0f, 1f)
            
            val nextTargetPaddingHorizontal = (12 * scaleFactor).dp
            val nextTargetPaddingVertical = (8 * scaleFactor).dp
            val nextTargetTitleFontSize = (8 * scaleFactor).sp
            val nextTargetValueFontSize = (16 * scaleFactor).sp
            val nextTargetBarHeight = (4 * scaleFactor).dp
            val nextTargetChevronSize = (16 * scaleFactor).dp
            val nextTargetCornerRadius = (12 * scaleFactor).dp

            NeonCard(
                accentColor = theme.secondaryColor,
                isSelected = false,
                onClick = null,
                cornerRadius = nextTargetCornerRadius,
                borderWidth = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = nextTargetPaddingHorizontal, vertical = nextTargetPaddingVertical)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NEXT TARGET",
                                fontSize = nextTargetTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height((2 * scaleFactor).dp))
                            Text(
                                text = nextTarget.toString(),
                                fontSize = nextTargetValueFontSize,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = theme.secondaryColor,
                            modifier = Modifier.size(nextTargetChevronSize)
                        )
                    }
                    Spacer(modifier = Modifier.height((4 * scaleFactor).dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(nextTargetBarHeight)
                            .clip(RoundedCornerShape(nextTargetBarHeight / 2))
                            .background(theme.textColor.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(nextTargetBarHeight / 2))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            theme.primaryColor,
                                            theme.secondaryColor
                                        )
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height((4 * scaleFactor).dp))
                    Text(
                        text = "$highestTile / $nextTarget",
                        fontSize = nextTargetTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Column 2: Center Panel (Board & Swipe indicators)
        Column(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GameBoard(
                grid = gameState.grid,
                tileAnimationInfo = gameState.tileAnimationInfo,
                moveCount = gameState.moveCount,
                onAnimationsComplete = { viewModel.clearAnimationInfo() },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(vertical = (4 * scaleFactor).dp)
            )

            Spacer(modifier = Modifier.height((4 * scaleFactor).dp))

            // Swipe Indicator Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "<<<",
                    color = theme.primaryColor,
                    fontSize = (10 * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                Text(
                    text = "SWIPE TO MOVE TILES",
                    color = theme.textColor.copy(alpha = 0.7f),
                    fontSize = (10 * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                Text(
                    text = ">>>",
                    color = theme.secondaryColor,
                    fontSize = (10 * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Column 3: Right Panel (Help/Settings icons, Action Cards)
        Column(
            modifier = Modifier
                .weight(0.28f)
                .fillMaxHeight()
                .verticalScroll(scrollStateRight),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)) {
                    SquareIconButton(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = "Help",
                        onClick = onHelpClick,
                        size = backButtonSize
                    )
                    SquareIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        onClick = onSettingsClick,
                        size = backButtonSize
                    )
                }
            }

            LandscapeActionCard(
                icon = Icons.AutoMirrored.Rounded.Undo,
                title = "UNDO",
                badge = "3",
                accentColor = theme.primaryColor,
                onClick = onUndoClick,
                scaleFactor = scaleFactor
            )

            LandscapeActionCard(
                icon = Icons.Rounded.Lightbulb,
                title = "HINT",
                badge = "5",
                accentColor = theme.secondaryColor,
                onClick = onHintClick,
                scaleFactor = scaleFactor
            )

            LandscapeActionCard(
                icon = Icons.Rounded.Refresh,
                title = "RESTART",
                accentColor = theme.primaryColor,
                onClick = onRestartClick,
                scaleFactor = scaleFactor
            )

            LandscapeActionCard(
                icon = Icons.Rounded.Settings,
                title = "SETTINGS",
                accentColor = theme.secondaryColor,
                onClick = onSettingsClick,
                scaleFactor = scaleFactor
            )
        }
    }
}

@Composable
private fun LandscapeStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    scaleFactor: Float,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val cardPaddingHorizontal = (12 * scaleFactor).dp
    val cardPaddingVertical = (6 * scaleFactor).dp
    val badgeSize = (32 * scaleFactor).dp
    val iconSize = (16 * scaleFactor).dp
    val titleFontSize = (8 * scaleFactor).sp
    val valueFontSize = (16 * scaleFactor).sp
    val spacing = (12 * scaleFactor).dp
    val cornerRadius = (12 * scaleFactor).dp

    NeonCard(
        accentColor = accentColor,
        isSelected = false,
        onClick = null,
        cornerRadius = cornerRadius,
        borderWidth = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = cardPaddingHorizontal, vertical = cardPaddingVertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .clip(RoundedCornerShape(badgeSize / 2))
                    .background(theme.backgroundColor)
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(badgeSize / 2)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(spacing))

            Column {
                Text(
                    text = title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = value,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor
                )
            }
        }
    }
}

@Composable
private fun LandscapeActionCard(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    accentColor: Color,
    onClick: () -> Unit,
    scaleFactor: Float,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val cardPaddingHorizontal = (12 * scaleFactor).dp
    val cardPaddingVertical = (8 * scaleFactor).dp
    val iconSize = (18 * scaleFactor).dp
    val spacing = (12 * scaleFactor).dp
    val titleFontSize = (10 * scaleFactor).sp
    val badgeFontSize = (9 * scaleFactor).sp
    val cornerRadius = (12 * scaleFactor).dp

    NeonCard(
        accentColor = accentColor,
        isSelected = true,
        onClick = onClick,
        cornerRadius = cornerRadius,
        borderWidth = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = cardPaddingHorizontal, vertical = cardPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(spacing))
                Text(
                    text = title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                    letterSpacing = 1.sp
                )
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * scaleFactor).dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = (6 * scaleFactor).dp, vertical = (2 * scaleFactor).dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = badgeFontSize,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}


