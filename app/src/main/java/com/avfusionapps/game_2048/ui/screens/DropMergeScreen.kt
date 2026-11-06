package com.avfusionapps.game_2048.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.model.DropMergeConfig
import com.avfusionapps.game_2048.model.DropMergeState
import com.avfusionapps.game_2048.model.DropPowerUps
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.model.DropTile
import com.avfusionapps.game_2048.ui.components.GameOverDialog
import com.avfusionapps.game_2048.ui.components.GamePauseDialog
import com.avfusionapps.game_2048.ui.components.NeonCard
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.utils.SoundManager
import com.avfusionapps.game_2048.viewmodel.DropAnim
import com.avfusionapps.game_2048.viewmodel.DropGameEvent
import com.avfusionapps.game_2048.viewmodel.DropMergeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private const val COLS = DropMergeConfig.COLUMNS
private const val ROWS = DropMergeConfig.ROWS

@Composable
fun DropMergeScreen(
    navController: NavController,
    viewModel: DropMergeViewModel = viewModel()
) {
    val theme = LocalGameTheme.current
    val gameState by viewModel.gameState.collectAsState()
    val bestScore by viewModel.bestScore.collectAsState(initial = 0)
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()

    // First-play guided tutorial: tips advance as the player takes their first shots.
    val hasSeenTutorial by viewModel.hasSeenTutorial.collectAsState()
    var tutorialDismissed by rememberSaveable { mutableStateOf(false) }
    val tutorialTips = listOf(
        stringResource(R.string.drop_tut_1),
        stringResource(R.string.drop_tut_2),
        stringResource(R.string.drop_tut_3),
        stringResource(R.string.drop_tut_4),
        stringResource(R.string.drop_tut_5),
        stringResource(R.string.drop_tut_6)
    )
    val showTutorial = hasSeenTutorial == false && !tutorialDismissed && !gameState.isGameOver
    val tutorialStep = gameState.moveCount.coerceAtMost(tutorialTips.lastIndex)

    // Mark complete once the player shoots past the last tip.
    LaunchedEffect(gameState.moveCount, hasSeenTutorial) {
        if (hasSeenTutorial == false && gameState.moveCount > tutorialTips.lastIndex) {
            viewModel.setTutorialSeen()
        }
    }

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    // Board shake played on game over.
    val shakeX = remember { Animatable(0f) }

    // Sound + haptics + shake from game events (respecting user settings).
    LaunchedEffect(vibrationEnabled, soundEnabled) {
        viewModel.events.collectLatest { event ->
            if (event == DropGameEvent.GAME_OVER) {
                launch {
                    listOf(0f, -16f, 13f, -9f, 6f, -3f, 0f).forEach {
                        shakeX.animateTo(it, tween(42))
                    }
                }
            }
            if (vibrationEnabled) {
                when (event) {
                    DropGameEvent.MERGE, DropGameEvent.COMBO ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    DropGameEvent.UNLOCK, DropGameEvent.PURGE, DropGameEvent.GAME_OVER,
                    DropGameEvent.REMOVE_HIT, DropGameEvent.LEVEL_UP ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    DropGameEvent.SHOOT, DropGameEvent.SKIP, DropGameEvent.POWER -> Unit
                }
            }
            val step = viewModel.gameState.value.lastStep
            when (event) {
                DropGameEvent.SHOOT -> { com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.SHOT); com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.IMPACT, delayMs = 200) }
                DropGameEvent.SKIP -> com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.WHOOSH)
                DropGameEvent.POWER -> com.avfusionapps.game_2048.utils.SpriteSfx.play(if (viewModel.gameState.value.freezeShots > 0) com.avfusionapps.game_2048.utils.SpriteSfx.Sound.FREEZE else com.avfusionapps.game_2048.utils.SpriteSfx.Sound.WHOOSH)
                DropGameEvent.MERGE -> com.avfusionapps.game_2048.utils.SpriteSfx.merge(step?.gained ?: 4, delayMs = 155)
                DropGameEvent.COMBO -> { com.avfusionapps.game_2048.utils.SpriteSfx.merge(step?.gained ?: 4, delayMs = 155); com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.COMBO, delayMs = 260) }
                DropGameEvent.PURGE -> com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.CRUNCH)
                DropGameEvent.REMOVE_HIT -> com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.CRUNCH, delayMs = 340)
                DropGameEvent.UNLOCK -> com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.FANFARE, delayMs = 155)
                DropGameEvent.LEVEL_UP -> com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.CHIME)
                DropGameEvent.GAME_OVER -> com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.SAD)
            }
        }
    }

    // Auto-dismiss the unlock banner.
    LaunchedEffect(gameState.justUnlockedValue) {
        if (gameState.justUnlockedValue != null) {
            delay(1700)
            viewModel.clearUnlockBanner()
        }
    }

    // Let the shake read before the dialog covers the board.
    var showGameOverDialog by remember { mutableStateOf(false) }
    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver) {
            delay(600)
            showGameOverDialog = true
        } else {
            showGameOverDialog = false
        }
    }

    BackHandler { if (gameState.removeArmed) viewModel.toggleRemove() else viewModel.togglePause() }

    // On-screen bounds of anchorable controls (launcher/goal/undo/skip), captured by
    // the layout so the tutorial coach can point at the real element in any orientation.
    val anchorBounds = remember { mutableStateMapOf<TutorialAnchor, Rect>() }
    val registerAnchor = remember {
        { anchor: TutorialAnchor, rect: Rect -> anchorBounds[anchor] = rect }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(
                Modifier.drawBehind {
                    drawRect(Brush.verticalGradient(0f to Color(0xFF2B1870), 0.45f to Color(0xFF1A0E45), 1f to Color(0xFF140A33)))
                }
            )
            .safeDrawingPadding()
            .testTag("DropMergeScreen_Root")
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val isLandscape = screenW > screenH

        CompositionLocalProvider(LocalTutorialRegister provides registerAnchor) {
            if (isLandscape) {
                DropMergeLandscape(
                    screenW = screenW,
                    screenH = screenH,
                    gameState = gameState,
                    bestScore = bestScore,
                    shakeXProvider = { shakeX.value },
                    viewModel = viewModel
                )
            } else {
                // On big/tablet portrait screens, cap the content width and center it so the
                // board stays prominent and the HUD doesn't stretch sparse across the width.
                val isTablet = minOf(screenW.value, screenH.value) >= 600f
                val contentW = if (isTablet) 600.dp else screenW
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(Modifier.width(contentW).fillMaxHeight()) {
                        DropMergePortrait(
                            screenW = contentW,
                            screenH = screenH,
                            gameState = gameState,
                            bestScore = bestScore,
                            shakeXProvider = { shakeX.value },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // ── Power-up and level notices, dropped over the top of the board ──
        ShooterNotice(
            notice = gameState.notice,
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Portrait: right over the level bar, so it never hides the top row of sprites.
                .padding(top = if (isLandscape) 16.dp else (screenH * 0.11f - 2.dp).coerceAtLeast(0.dp), start = 24.dp, end = 24.dp)
        )

        // ── First-play guided tutorial coach — points at the real control per step ──
        if (showTutorial) {
            TutorialCoach(
                targetRect = anchorForStep(tutorialStep)?.let { anchorBounds[it] },
                step = tutorialStep,
                total = tutorialTips.size,
                text = tutorialTips[tutorialStep],
                isLast = tutorialStep == tutorialTips.lastIndex,
                onFinish = {
                    tutorialDismissed = true
                    viewModel.setTutorialSeen()
                }
            )
        }
    }

    // ── Dialogs ──
    if (gameState.isPaused && !gameState.isGameOver) {
        GamePauseDialog(
            currentScore = gameState.score,
            onResume = { viewModel.setPaused(false) },
            onRestart = {
                viewModel.setPaused(false)
                viewModel.startNewGame()
            },
            onQuit = {
                viewModel.setPaused(false)
                navController.popBackStack()
            },
            onDismiss = { viewModel.setPaused(false) }
        )
    }

    if (showGameOverDialog) {
        val affordable = { charges: Int -> charges > 0 || gameState.coins >= DropPowerUps.COST }
        val canRescueUndo = gameState.canUndo && affordable(gameState.powerUps.undo)
        val canRescueRemove = gameState.columns.any { it.isNotEmpty() } && affordable(gameState.powerUps.remove)
        GameOverDialog(
            score = gameState.score,
            onNewGame = { viewModel.startNewGame() },
            onExit = { navController.popBackStack() },
            rescue = if (canRescueUndo || canRescueRemove) {
                {
                    if (canRescueUndo) {
                        RescueButton(
                            title = "Undo last shot",
                            charges = gameState.powerUps.undo,
                            testTag = "DropMerge_Rescue_Undo",
                            onClick = { viewModel.undo() }
                        )
                    }
                    if (canRescueRemove) {
                        RescueButton(
                            title = "Remove a sprite",
                            charges = gameState.powerUps.remove,
                            testTag = "DropMerge_Rescue_Remove",
                            onClick = { viewModel.toggleRemove() }
                        )
                    }
                }
            } else null
        )
    }
}

// ────────────────────────────── Orientations ────────────────────────────────

@Composable
private fun DropMergePortrait(
    screenW: Dp,
    screenH: Dp,
    gameState: DropMergeState,
    bestScore: Int,
    shakeXProvider: () -> Float,
    viewModel: DropMergeViewModel
) {
    val hPad = screenW * 0.035f
    val hudH = screenH * 0.092f
    val barH = (screenH * 0.105f).coerceIn(72.dp, 96.dp)
    val vGap = screenH * 0.009f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = hPad),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(vGap))

        // ── Gamified HUD: pause + score / combo / next-goal ──
        SpriteShooterHud(
            score = gameState.score,
            best = maxOf(bestScore, gameState.score),
            combo = gameState.comboCount,
            goalTarget = gameState.unlockTarget,
            goalNear = (gameState.columns.maxOfOrNull { c -> c.maxOfOrNull { it.value } ?: 0 } ?: 0) * 2 >= gameState.unlockTarget,
            height = hudH,
            onPause = { viewModel.togglePause() },
            goalModifier = Modifier.tutorialAnchor(TutorialAnchor.GOAL)
        )

        Spacer(Modifier.height(vGap))

        // ── Level, XP and the reward chest ──
        ShooterLevelBar(
            level = gameState.level,
            xp = gameState.xp,
            xpNeeded = gameState.xpNeeded,
            levelUp = gameState.levelUp,
            onChest = { viewModel.chestTapped() }
        )

        Spacer(Modifier.height(vGap))

        // ── Board + launcher ──
        DropBoard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset { IntOffset(shakeXProvider().roundToInt(), 0) },
            gameState = gameState,
            viewModel = viewModel
        )

        ShooterTapLine(removeArmed = gameState.removeArmed, modifier = Modifier.padding(vertical = vGap * 0.6f))

        // ── Power-ups: Undo · Shuffle · NEXT · Remove · Freeze ──
        ShooterPowerBar(
            state = powerState(gameState),
            onUndo = { viewModel.undo() },
            onShuffle = { viewModel.shuffle() },
            onNext = { viewModel.skipTile() },
            onRemove = { viewModel.toggleRemove() },
            onFreeze = { viewModel.freeze() },
            nextTile = { size -> NextTileFace(gameState.nextValue, size) },
            modifier = Modifier.fillMaxWidth().height(barH + 16.dp)
        )

        Spacer(Modifier.height(vGap))
    }
}

/** Gathers what the power-up bar needs from the game state. */
private fun powerState(gameState: DropMergeState): ShooterPowerState {
    val idle = !gameState.isResolving && !gameState.isGameOver && !gameState.isPaused && gameState.removingTileId == null
    val tiles = gameState.columns.sumOf { it.size }
    return ShooterPowerState(
        powerUps = gameState.powerUps,
        canUndo = gameState.canUndo,
        canShuffle = tiles >= 2,
        canRemove = tiles >= 1,
        canSwap = gameState.canSkip,
        removeArmed = gameState.removeArmed,
        freezeShots = gameState.freezeShots,
        nextValue = gameState.nextValue,
        enabled = idle
    )
}

@Composable
private fun NextTileFace(value: Int, size: Dp) {
    val theme = LocalGameTheme.current
    com.avfusionapps.game_2048.ui.sprites.SpriteTile(value = value, modifier = Modifier.size(size), showCard = false)

}

/** A secondary button on the game-over card that spends a power-up to keep playing. */
@Composable
private fun RescueButton(title: String, charges: Int, testTag: String, onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .height(50.dp)
            .clip(shape)
            .background(theme.surfaceColor.copy(alpha = 0.6f))
            .border(1.5.dp, Color(0xFFFFC93A).copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = theme.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (charges > 0) {
            Text("$charges left", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        } else {
            com.avfusionapps.game_2048.ui.components.CoinIcon(Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("${DropPowerUps.COST}", color = Color(0xFFFFC93A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DropMergeLandscape(
    screenW: Dp,
    screenH: Dp,
    gameState: DropMergeState,
    bestScore: Int,
    shakeXProvider: () -> Float,
    viewModel: DropMergeViewModel
) {
    val theme = LocalGameTheme.current
    val pad = (screenH * 0.03f).coerceIn(10.dp, 20.dp)
    val colW = (screenW * 0.19f).coerceIn(116.dp, 220.dp)
    val cardH = (screenH * 0.19f).coerceIn(74.dp, 150.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = pad, vertical = pad * 0.7f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: pause + vertical stat HUD (score / combo / next-goal) ──
        Column(
            modifier = Modifier.width(colW).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(pad * 0.7f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HudPauseButton(size = cardH * 0.5f, onClick = { viewModel.togglePause() })
            HudCard(modifier = Modifier.fillMaxWidth(), accent = theme.primaryColor, height = cardH) {
                HudScoreContent(gameState.score, maxOf(bestScore, gameState.score), cardH)
            }
            HudCard(
                modifier = Modifier.fillMaxWidth(),
                accent = theme.accentColor, height = cardH, active = gameState.comboCount >= 2
            ) {
                HudComboContent(gameState.comboCount, cardH)
            }
            HudCard(
                modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchor.GOAL),
                accent = dropTileColor(theme, gameState.unlockTarget), height = cardH
            ) {
                HudGoalContent(gameState.unlockTarget, cardH)
            }
            ShooterLevelBar(
                level = gameState.level,
                xp = gameState.xp,
                xpNeeded = gameState.xpNeeded,
                levelUp = gameState.levelUp,
                onChest = { viewModel.chestTapped() },
                compact = true
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.width(pad))

        // ── Center: big board + launcher ──
        DropBoard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .offset { IntOffset(shakeXProvider().roundToInt(), 0) },
            gameState = gameState,
            viewModel = viewModel
        )

        Spacer(Modifier.width(pad))

        // ── Right: NEXT over the four power-ups ──
        Column(
            modifier = Modifier.width(colW).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShooterPowerBar(
                state = powerState(gameState),
                onUndo = { viewModel.undo() },
                onShuffle = { viewModel.shuffle() },
                onNext = { viewModel.skipTile() },
                onRemove = { viewModel.toggleRemove() },
                onFreeze = { viewModel.freeze() },
                nextTile = { size -> NextTileFace(gameState.nextValue, size) },
                modifier = Modifier.fillMaxWidth(),
                vertical = true
            )
        }
    }
}

// ─────────────────────────────────── Board ──────────────────────────────────

@Composable
private fun DropBoard(
    modifier: Modifier,
    gameState: DropMergeState,
    viewModel: DropMergeViewModel
) {
    val theme = LocalGameTheme.current
    SpriteDropBoard(modifier = modifier, gameState = gameState, viewModel = viewModel)
}

@Composable
private fun TileNumber(value: Int, cell: Dp, background: Color) {
    val textColor = if (background.luminance() > 0.55f) Color(0xFF1E1E2E) else Color.White
    val digits = value.toString().length
    val fontSize = when {
        digits <= 2 -> cell.value * 0.42f
        digits == 3 -> cell.value * 0.36f
        else -> cell.value * 0.28f
    }
    Text(
        text = value.toString(),
        color = textColor,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

// ───────────────────────────────── Launcher ─────────────────────────────────

/**
 * The launcher strip. The loaded tile is ONE block that slides smoothly to the
 * aimed column (spring), can be dragged along the strip, and pops on reload.
 */
@Composable
private fun Launcher(
    modifier: Modifier = Modifier,
    boardW: Dp,
    cell: Dp,
    gap: Dp,
    extra: Dp,
    currentValue: Int,
    moveCount: Int,
    launcherCol: Int,
    enabled: Boolean,
    onAim: (Int) -> Unit,
    onRelease: () -> Unit,
    onShoot: (Int) -> Unit
) {
    val theme = LocalGameTheme.current
    val density = LocalDensity.current
    val cellPx = with(density) { cell.toPx() }
    val gapPx = with(density) { gap.toPx() }
    fun colFromX(x: Float): Int =
        ((x - gapPx / 2f) / (cellPx + gapPx)).toInt().coerceIn(0, COLS - 1)

    // Idle bob on the loaded tile.
    val bob = rememberInfiniteTransition(label = "launcherBob")
    val bobY by bob.animateFloat(
        initialValue = 0f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "bobY"
    )

    // The slidable block: springs toward the aimed column.
    val tileX by animateDpAsState(
        targetValue = gap + (cell + gap) * launcherCol,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "launcherTileX"
    )

    // Reload pop whenever a new tile arrives (shot resolved or skip used).
    val reloadScale = remember { Animatable(1f) }
    LaunchedEffect(currentValue, moveCount) {
        reloadScale.snapTo(0.55f)
        reloadScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }

    val color = dropTileColor(theme, currentValue)

    Box(
        modifier = modifier
            .width(boardW)
            .height(cell + extra)
            .pointerInput(enabled) {
                detectDragGestures(
                    onDragStart = { offset -> if (enabled) onAim(colFromX(offset.x)) },
                    onDrag = { change, _ -> if (enabled) onAim(colFromX(change.position.x)) },
                    onDragEnd = { onRelease() },
                    onDragCancel = { onRelease() }
                )
            }
    ) {
        // Slot pads.
        for (c in 0 until COLS) {
            Box(
                modifier = Modifier
                    .offset(x = gap + (cell + gap) * c)
                    .width(cell)
                    .height(cell + extra)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.surfaceColor.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        if (c == launcherCol) color.copy(alpha = 0.5f)
                        else theme.textColor.copy(alpha = 0.07f),
                        RoundedCornerShape(10.dp)
                    )
                    .pointerInput(enabled) {
                        detectTapGestures { if (enabled) onShoot(c) }
                    }
                    .testTag("DropMerge_LauncherSlot_$c"),
                contentAlignment = Alignment.Center
            ) {
                if (c != launcherCol) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null,
                        tint = theme.textColor.copy(alpha = 0.25f),
                        modifier = Modifier.size(cell * 0.4f)
                    )
                }
            }
        }

        // The single slidable loaded tile, riding above the slots.
        Box(
            modifier = Modifier
                .offset(x = tileX)
                .offset { IntOffset(0, bobY.roundToInt()) }
                .width(cell)
                .height(cell + extra),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(cell * 0.92f)
                    .scale(reloadScale.value)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = 0.35f), Color.Transparent)
                            ),
                            radius = size.maxDimension * 0.8f,
                            center = center
                        )
                    }
                    .alpha(if (enabled) 1f else 0.45f)
            ) {
                TileFace(value = currentValue, size = cell * 0.92f, color = color)
            }
        }
    }
}

// ───────────────────────────── Controls & badges ────────────────────────────

// ─────────────────────────────── Neon HUD ───────────────────────────────────

private fun formatNum(n: Int): String = "%,d".format(n)
private fun lighten(c: Color, f: Float): Color = lerp(c, Color.White, f)
private fun darken(c: Color, f: Float): Color = lerp(c, Color.Black, f)

/** Glossy, beveled tile face used across the board, launcher and chips. */
@Composable
private fun TileFace(value: Int, size: Dp, color: Color, corner: Dp = size * 0.22f) {
    if (value > 0) {
        com.avfusionapps.game_2048.ui.sprites.SpriteTile(value = value, modifier = Modifier.size(size))
        return
    }
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(lighten(color, 0.26f), color, darken(color, 0.16f))))
            .border(1.5.dp, lighten(color, 0.5f).copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center
    ) {
        // Top gloss highlight for the glassy bevel.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.TopCenter)
                .padding(horizontal = corner * 0.45f, vertical = 1.5.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = corner * 0.8f, topEnd = corner * 0.8f,
                        bottomStart = corner * 0.5f, bottomEnd = corner * 0.5f
                    )
                )
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.Transparent)))
        )
        TileNumber(value = value, cell = size, background = color)
    }
}

@Composable
private fun HudPauseButton(size: Dp, onClick: () -> Unit) {
    val theme = LocalGameTheme.current
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(theme.surfaceColor.copy(alpha = 0.85f))
            .border(1.5.dp, theme.primaryColor.copy(alpha = 0.45f), shape)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .testTag("DropMerge_Button_Pause"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Pause,
            contentDescription = stringResource(R.string.desc_pause_button),
            tint = theme.textColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun HudCard(
    modifier: Modifier = Modifier,
    accent: Color,
    height: Dp,
    active: Boolean = false,
    content: @Composable () -> Unit
) {
    val theme = LocalGameTheme.current
    val shape = RoundedCornerShape(height * 0.26f)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(theme.surfaceColor.copy(alpha = 0.9f), theme.surfaceColor.copy(alpha = 0.5f))
                )
            )
            .border(1.5.dp, accent.copy(alpha = if (active) 0.9f else 0.4f), shape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun HudScoreContent(score: Int, best: Int, height: Dp) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.drop_hud_score), color = theme.textColor.copy(alpha = 0.5f),
            fontSize = (height.value * 0.135f).sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
        )
        Text(
            formatNum(score), color = theme.textColor,
            fontSize = (height.value * 0.30f).sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
        )
        Text(
            "♛ ${formatNum(best)}", color = theme.accentColor.copy(alpha = 0.9f),
            fontSize = (height.value * 0.135f).sp, fontWeight = FontWeight.SemiBold, maxLines = 1
        )
    }
}

@Composable
private fun HudComboContent(combo: Int, height: Dp) {
    val theme = LocalGameTheme.current
    val active = combo >= 2
    val comboColor = if (active) theme.accentColor else theme.textColor.copy(alpha = 0.45f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.drop_hud_combo), color = theme.textColor.copy(alpha = 0.5f),
            fontSize = (height.value * 0.135f).sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
        )
        Text(
            "×${maxOf(combo, 1)}", color = comboColor,
            fontSize = (height.value * 0.30f).sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
        )
        Row(horizontalArrangement = Arrangement.spacedBy(height * 0.028f)) {
            repeat(5) { i ->
                Box(
                    Modifier.size(height * 0.05f).clip(CircleShape)
                        .background(if (i < combo) theme.accentColor else theme.textColor.copy(alpha = 0.2f))
                )
            }
        }
    }
}

@Composable
private fun HudGoalContent(target: Int, height: Dp) {
    val theme = LocalGameTheme.current
    val color = dropTileColor(theme, target)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.drop_hud_goal), color = theme.textColor.copy(alpha = 0.5f),
            fontSize = (height.value * 0.125f).sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1
        )
        Spacer(Modifier.height(height * 0.04f))
        TileFace(value = target, size = height * 0.36f, color = color, corner = height * 0.10f)
        Spacer(Modifier.height(height * 0.03f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = theme.textColor.copy(alpha = 0.5f), modifier = Modifier.size(height * 0.12f))
            Spacer(Modifier.width(2.dp))
            Text(
                stringResource(R.string.drop_locked), color = theme.textColor.copy(alpha = 0.5f),
                fontSize = (height.value * 0.11f).sp, fontWeight = FontWeight.SemiBold, maxLines = 1
            )
        }
    }
}

// ──────────────────────────── First-play tutorial ───────────────────────────

/** Elements a tutorial tip can point at; resolved to on-screen bounds at layout time. */
internal enum class TutorialAnchor { LAUNCHER, GOAL, UNDO, SKIP }

/** Provided by the screen so anchored controls can report their on-screen bounds. */
internal val LocalTutorialRegister =
    staticCompositionLocalOf<((TutorialAnchor, Rect) -> Unit)?> { null }

/** Reports this element's bounds (root coords) so the coach can point at it. */
internal fun Modifier.tutorialAnchor(anchor: TutorialAnchor): Modifier = composed {
    val register = LocalTutorialRegister.current
    if (register != null) {
        this.onGloballyPositioned { register(anchor, it.boundsInRoot()) }
    } else {
        this
    }
}

/** Which control each tutorial step points at (null = centered over the board). */
private fun anchorForStep(step: Int): TutorialAnchor? = when (step) {
    0 -> TutorialAnchor.LAUNCHER
    2 -> TutorialAnchor.GOAL
    3 -> TutorialAnchor.UNDO
    4 -> TutorialAnchor.SKIP
    else -> null // merge (1) and the finish (5) sit centered over the board
}

/**
 * Non-blocking coach for first-time players. Positions the tip card next to the
 * [targetRect] control (root coordinates) with a pointer aimed at it, flipping
 * above/below based on available space. Because it follows the real element
 * bounds, it stays correct in any orientation and on tablets. Advances with the
 * player's own first shots.
 */
@Composable
private fun TutorialCoach(
    targetRect: Rect?,
    step: Int,
    total: Int,
    text: String,
    isLast: Boolean,
    onFinish: () -> Unit
) {
    val density = LocalDensity.current
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    val bob = rememberInfiniteTransition(label = "coachBob")
    val bobY by bob.animateFloat(
        initialValue = 0f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(640), RepeatMode.Reverse),
        label = "coachBobY"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.localToRoot(Offset.Zero) }
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val margin = with(density) { 14.dp.toPx() }
        val gapPx = with(density) { 8.dp.toPx() }
        val pointerPx = with(density) { 26.dp.toPx() }

        // Convert the target into this overlay's local coordinate space.
        val localTop = targetRect?.let { it.top - overlayOrigin.y }
        val localBottom = targetRect?.let { it.bottom - overlayOrigin.y }
        val localCx = targetRect?.let { it.center.x - overlayOrigin.x } ?: (wPx / 2f)

        val hasTarget = targetRect != null
        val placeAbove = hasTarget && ((localTop!! + localBottom!!) / 2f) > hPx * 0.52f

        val cardW = cardSize.width.toFloat()
        val cardH = cardSize.height.toFloat()
        val blockH = cardH + if (hasTarget) pointerPx else 0f

        var x = localCx - cardW / 2f
        x = x.coerceIn(margin, (wPx - cardW - margin).coerceAtLeast(margin))
        var y = when {
            !hasTarget -> hPx * 0.40f
            placeAbove -> localTop!! - blockH - gapPx
            else -> localBottom!! + gapPx
        }
        y = y.coerceIn(margin, (hPx - blockH - margin).coerceAtLeast(margin))

        // Keep the pointer aimed at the target even if the card was clamped to an edge.
        val pointerDx = localCx - (x + cardW / 2f)

        Column(
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .widthIn(max = 340.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasTarget && !placeAbove) CoachPointer(up = true, dx = pointerDx, bobY = -bobY)
            CoachCard(
                step = step, total = total, text = text, isLast = isLast, onFinish = onFinish,
                modifier = Modifier.onGloballyPositioned { cardSize = it.size }
            )
            if (hasTarget && placeAbove) CoachPointer(up = false, dx = pointerDx, bobY = bobY)
        }
    }
}

@Composable
private fun CoachPointer(up: Boolean, dx: Float, bobY: Float) {
    val theme = LocalGameTheme.current
    Icon(
        imageVector = if (up) Icons.Rounded.KeyboardDoubleArrowUp else Icons.Rounded.KeyboardDoubleArrowDown,
        contentDescription = null,
        tint = theme.accentColor,
        modifier = Modifier
            .offset { IntOffset(dx.roundToInt(), bobY.roundToInt()) }
            .size(26.dp)
    )
}

@Composable
private fun CoachCard(
    step: Int,
    total: Int,
    text: String,
    isLast: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(theme.surfaceColor.copy(alpha = 0.98f), theme.surfaceColor.copy(alpha = 0.9f))
                )
            )
            .border(1.5.dp, theme.accentColor.copy(alpha = 0.6f), shape)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.drop_how_to_play),
                color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp, modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(total) { i ->
                    Box(
                        Modifier.size(if (i == step) 8.dp else 6.dp).clip(CircleShape)
                            .background(if (i <= step) theme.accentColor else theme.textColor.copy(alpha = 0.25f))
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = text, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(10.dp))
        val btnShape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .clip(btnShape)
                .then(
                    if (isLast) Modifier.background(theme.accentColor)
                    else Modifier.border(1.dp, theme.accentColor.copy(alpha = 0.5f), btnShape)
                )
                .pointerInput(Unit) { detectTapGestures { onFinish() } }
                .padding(horizontal = 20.dp, vertical = 7.dp)
        ) {
            Text(
                text = if (isLast) stringResource(R.string.drop_tut_got_it) else stringResource(R.string.drop_tut_skip),
                color = if (isLast) {
                    if (theme.accentColor.luminance() > 0.55f) Color(0xFF1E1E2E) else Color.White
                } else theme.accentColor,
                fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Theme color for a tile value; values above 2048 reuse the 2048 accent. */
private fun dropTileColor(
    theme: GameTheme,
    value: Int
): Color = theme.tileColors[value]
    ?: theme.tileColors[2048]
    ?: theme.primaryColor
