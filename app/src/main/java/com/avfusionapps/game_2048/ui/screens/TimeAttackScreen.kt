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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.ui.res.stringResource
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.components.FloatingBonusText
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
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.key
import androidx.compose.ui.draw.scale
import com.avfusionapps.game_2048.ui.components.SquareIconButton
import com.avfusionapps.game_2048.ui.components.NeonCard

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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        TimeAttackScreenLandscape(
            viewModel = viewModel,
            gameState = gameState,
            highScore = highScore,
            floatingBonuses = floatingBonuses,
            onBonusAnimationFinished = { id ->
                floatingBonuses = floatingBonuses.filter { it.id != id }
            },
            onBack = { navController.popBackStack() },
            onPauseToggle = { viewModel.togglePause() },
            onHelpClick = {
                forceShowTutorial = true
                viewModel.setPaused(true)
            },
            onUndoClick = { viewModel.undoMove() },
            onRestartClick = { viewModel.startNewGame() },
            onHintClick = { viewModel.showHint(context) }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("TimeAttackScreen_Root")
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
        }
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
    val buttonColor = theme.primaryColor
    val isLight = (buttonColor.red * 0.2126f + buttonColor.green * 0.7152f + buttonColor.blue * 0.0722f) > 0.5f
    val contentColor = if (isLight) Color(0xFF1F1F1F) else Color.White

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
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = contentColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = bonus.message,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimeAttackScreenLandscape(
    viewModel: TimeAttackViewModel,
    gameState: TimeAttackState,
    highScore: Int,
    floatingBonuses: List<FloatingBonus>,
    onBonusAnimationFinished: (Long) -> Unit,
    onBack: () -> Unit,
    onPauseToggle: () -> Unit,
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

    val minutes = (gameState.timeRemainingMillis / 60000).toInt()
    val seconds = ((gameState.timeRemainingMillis % 60000) / 1000).toInt()
    val millis = ((gameState.timeRemainingMillis % 1000) / 10).toInt()

    val timerColor by animateColorAsState(
        targetValue = when {
            gameState.timeRemainingMillis < 10_000L -> Color(0xFFFF3378) // Red/Pink from theme
            gameState.timeRemainingMillis < 30_000L -> Color(0xFFFFD700) // Yellow
            else -> Color(0xFF00FF66) // Green
        },
        label = "timerColor"
    )

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
        horizontalArrangement = Arrangement.spacedBy(columnSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Left Panel (Back/Title, Timer Card, Score, Multiplier)
        Column(
            modifier = Modifier
                .weight(0.28f)
                .fillMaxHeight()
                .verticalScroll(scrollStateLeft),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            // Back Button & Title
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
                Text(
                    text = "TIME ATTACK",
                    color = theme.textColor,
                    fontSize = levelTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Timer Card
            val cardPaddingHorizontal = (12 * scaleFactor).dp
            val cardPaddingVertical = (8 * scaleFactor).dp
            NeonCard(
                accentColor = timerColor,
                isSelected = false,
                onClick = null,
                cornerRadius = (12 * scaleFactor).dp,
                borderWidth = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cardPaddingHorizontal, vertical = cardPaddingVertical),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = timerColor,
                            modifier = Modifier.size((16 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.height((2 * scaleFactor).dp))
                        Text(
                            text = String.format("%02d:%02d.%02d", minutes, seconds, millis),
                            color = timerColor,
                            fontSize = (24 * scaleFactor).sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height((2 * scaleFactor).dp))
                        Text(
                            text = stringResource(id = R.string.time_left),
                            color = theme.textColor.copy(alpha = 0.5f),
                            fontSize = (10 * scaleFactor).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    floatingBonuses.forEach { bonus ->
                        key(bonus.id) {
                            FloatingBonusText(
                                text = bonus.text,
                                onAnimationFinished = { onBonusAnimationFinished(bonus.id) }
                            )
                        }
                    }
                }
            }

            // Stat Cards: Score & Best Score & Multiplier
            LandscapeStatCard(
                icon = Icons.Rounded.Star,
                title = stringResource(id = R.string.score),
                value = gameState.score.toString(),
                accentColor = theme.primaryColor,
                scaleFactor = scaleFactor
            )

            LandscapeStatCard(
                icon = Icons.Rounded.EmojiEvents,
                title = stringResource(id = R.string.best_score),
                value = highScore.toString(),
                accentColor = theme.secondaryColor,
                scaleFactor = scaleFactor
            )

            LandscapeStatCard(
                icon = Icons.Rounded.Timer,
                title = "MULTIPLIER",
                value = String.format("x%.1f", gameState.multiplier),
                accentColor = theme.primaryColor,
                scaleFactor = scaleFactor
            )
        }

        // Column 2: Center Panel (GameBoard & Swipe indicators)
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
                    text = stringResource(id = R.string.swipe_to_move),
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

        // Column 3: Right Panel (Help/Pause icons, Action Cards)
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
                        icon = if (gameState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = "Pause/Play",
                        onClick = onPauseToggle,
                        size = backButtonSize
                    )
                }
            }

            LandscapeActionCard(
                icon = Icons.AutoMirrored.Rounded.Undo,
                title = stringResource(id = R.string.undo),
                badge = "3",
                accentColor = theme.primaryColor,
                onClick = onUndoClick,
                scaleFactor = scaleFactor
            )

            LandscapeActionCard(
                icon = Icons.Rounded.Lightbulb,
                title = stringResource(id = R.string.hint),
                badge = "5",
                accentColor = theme.secondaryColor,
                onClick = onHintClick,
                scaleFactor = scaleFactor
            )

            LandscapeActionCard(
                icon = Icons.Rounded.Refresh,
                title = stringResource(id = R.string.new_game),
                accentColor = theme.primaryColor,
                onClick = onRestartClick,
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
                        .padding(horizontal = (8 * scaleFactor).dp, vertical = (2 * scaleFactor).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = badgeFontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}






