package com.avfusionapps.game_2048.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.model.DropMergeConfig
import com.avfusionapps.game_2048.model.DropMergeState
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.ui.components.BoardTileView
import com.avfusionapps.game_2048.ui.components.ComboText
import com.avfusionapps.game_2048.ui.components.ConsumedTileView
import com.avfusionapps.game_2048.ui.components.GameOverDialog
import com.avfusionapps.game_2048.ui.components.GamePauseDialog
import com.avfusionapps.game_2048.ui.components.GameScoreBoard
import com.avfusionapps.game_2048.ui.components.NeonCard
import com.avfusionapps.game_2048.ui.components.SquareIconButton
import com.avfusionapps.game_2048.ui.components.TileNumber
import com.avfusionapps.game_2048.ui.components.mergeTileColor
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.utils.SoundManager
import com.avfusionapps.game_2048.viewmodel.DropAnim
import com.avfusionapps.game_2048.viewmodel.DropGameEvent
import com.avfusionapps.game_2048.viewmodel.FallMergeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private const val FCOLS = DropMergeConfig.COLUMNS
private const val FROWS = DropMergeConfig.ROWS

/**
 * Neon Drop — the top-drop falling-merge game. The current tile hovers above
 * the board; pick a column and it free-falls onto the stack below, merging
 * and cascading with real gravity motion. Stacks build up from the floor;
 * a column that reaches the top ends the game.
 */
@Composable
fun FallMergeScreen(
    navController: NavController,
    viewModel: FallMergeViewModel = viewModel()
) {
    val theme = LocalGameTheme.current
    val gameState by viewModel.gameState.collectAsState()
    val bestScore by viewModel.bestScore.collectAsState(initial = 0)
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }

    val shakeX = remember { Animatable(0f) }

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
                    DropGameEvent.UNLOCK, DropGameEvent.PURGE, DropGameEvent.GAME_OVER ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    DropGameEvent.SHOOT, DropGameEvent.SKIP -> Unit
                }
            }
            if (soundEnabled) {
                withContext(Dispatchers.Default) {
                    when (event) {
                        DropGameEvent.SHOOT, DropGameEvent.SKIP ->
                            soundManager.playSound(SoundManager.SOUND_MOVE)
                        DropGameEvent.MERGE, DropGameEvent.COMBO, DropGameEvent.PURGE ->
                            soundManager.playSound(SoundManager.SOUND_MERGE)
                        DropGameEvent.UNLOCK -> soundManager.playSound(SoundManager.SOUND_LEVEL_UP)
                        DropGameEvent.GAME_OVER -> soundManager.playSound(SoundManager.SOUND_GAME_OVER)
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    LaunchedEffect(gameState.justUnlockedValue) {
        if (gameState.justUnlockedValue != null) {
            delay(1700)
            viewModel.clearUnlockBanner()
        }
    }

    var showGameOverDialog by remember { mutableStateOf(false) }
    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver) {
            delay(600)
            showGameOverDialog = true
        } else {
            showGameOverDialog = false
        }
    }

    BackHandler { viewModel.togglePause() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .safeDrawingPadding()
            .testTag("FallMergeScreen_Root")
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val isLandscape = screenW > screenH

        if (isLandscape) {
            FallMergeLandscape(
                screenW = screenW,
                screenH = screenH,
                gameState = gameState,
                bestScore = bestScore,
                shakeXProvider = { shakeX.value },
                viewModel = viewModel
            )
        } else {
            FallMergePortrait(
                screenW = screenW,
                screenH = screenH,
                gameState = gameState,
                bestScore = bestScore,
                shakeXProvider = { shakeX.value },
                viewModel = viewModel
            )
        }

        AnimatedVisibility(
            visible = gameState.justUnlockedValue != null,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = screenH * 0.12f)
        ) {
            FallUnlockBanner(value = gameState.justUnlockedValue ?: 0)
        }
    }

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
        GameOverDialog(
            score = gameState.score,
            onNewGame = { viewModel.startNewGame() },
            onExit = { navController.popBackStack() }
        )
    }
}

// ────────────────────────────── Orientations ────────────────────────────────

@Composable
private fun FallMergePortrait(
    screenW: Dp,
    screenH: Dp,
    gameState: DropMergeState,
    bestScore: Int,
    shakeXProvider: () -> Float,
    viewModel: FallMergeViewModel
) {
    val theme = LocalGameTheme.current
    val hPad = screenW * 0.035f
    val barH = screenH * 0.070f
    val barSpacing = screenH * 0.014f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = hPad),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(barSpacing))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barH),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SquareIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.desc_back_button),
                onClick = { viewModel.setPaused(true) },
                size = barH * 0.85f,
                modifier = Modifier.testTag("FallMerge_Button_Back")
            )
            Spacer(Modifier.width(hPad * 0.8f))
            GameScoreBoard(
                score = gameState.score,
                highScore = maxOf(bestScore, gameState.score),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(hPad * 0.8f))
            FallUnlockBadge(target = gameState.unlockTarget, height = barH)
        }

        Spacer(Modifier.height(barSpacing))

        FallBoard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset { IntOffset(shakeXProvider().roundToInt(), 0) },
            gameState = gameState,
            viewModel = viewModel
        )

        Spacer(Modifier.height(barSpacing))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barH)
                .padding(bottom = barSpacing * 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SquareIconButton(
                icon = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = stringResource(R.string.undo),
                onClick = { viewModel.undo() },
                tint = if (gameState.canUndo) theme.textColor else theme.textColor.copy(alpha = 0.3f),
                size = barH * 0.85f,
                modifier = Modifier.testTag("FallMerge_Button_Undo")
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                FallNextChip(nextValue = gameState.nextValue, height = barH * 0.62f)
                Spacer(Modifier.width(hPad * 0.9f))
                FallSkipButton(
                    enabled = gameState.canSkip && !gameState.isResolving && !gameState.isGameOver,
                    height = barH * 0.72f,
                    onClick = { viewModel.skipTile() }
                )
            }

            SquareIconButton(
                icon = Icons.Rounded.Pause,
                contentDescription = stringResource(R.string.desc_pause_button),
                onClick = { viewModel.togglePause() },
                size = barH * 0.85f,
                modifier = Modifier.testTag("FallMerge_Button_Pause")
            )
        }
    }
}

@Composable
private fun FallMergeLandscape(
    screenW: Dp,
    screenH: Dp,
    gameState: DropMergeState,
    bestScore: Int,
    shakeXProvider: () -> Float,
    viewModel: FallMergeViewModel
) {
    val theme = LocalGameTheme.current
    val pad = screenH * 0.03f
    val sideW = screenW * 0.24f
    val btnSize = screenH * 0.115f

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(pad)
    ) {
        Column(
            modifier = Modifier
                .width(sideW)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(pad),
            horizontalAlignment = Alignment.Start
        ) {
            SquareIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.desc_back_button),
                onClick = { viewModel.setPaused(true) },
                size = btnSize,
                modifier = Modifier.testTag("FallMerge_Button_Back")
            )
            GameScoreBoard(
                score = gameState.score,
                highScore = maxOf(bestScore, gameState.score),
                modifier = Modifier.fillMaxWidth()
            )
            FallUnlockBadge(target = gameState.unlockTarget, height = screenH * 0.16f)
        }

        FallBoard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .offset { IntOffset(shakeXProvider().roundToInt(), 0) },
            gameState = gameState,
            viewModel = viewModel
        )

        Column(
            modifier = Modifier
                .width(sideW)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(pad),
            horizontalAlignment = Alignment.End
        ) {
            SquareIconButton(
                icon = Icons.Rounded.Pause,
                contentDescription = stringResource(R.string.desc_pause_button),
                onClick = { viewModel.togglePause() },
                size = btnSize,
                modifier = Modifier.testTag("FallMerge_Button_Pause")
            )
            SquareIconButton(
                icon = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = stringResource(R.string.undo),
                onClick = { viewModel.undo() },
                tint = if (gameState.canUndo) theme.textColor else theme.textColor.copy(alpha = 0.3f),
                size = btnSize,
                modifier = Modifier.testTag("FallMerge_Button_Undo")
            )
            Spacer(Modifier.weight(1f))
            FallNextChip(nextValue = gameState.nextValue, height = screenH * 0.085f)
            FallSkipButton(
                enabled = gameState.canSkip && !gameState.isResolving && !gameState.isGameOver,
                height = screenH * 0.095f,
                onClick = { viewModel.skipTile() }
            )
        }
    }
}

// ─────────────────────────────────── Board ──────────────────────────────────

@Composable
private fun FallBoard(
    modifier: Modifier,
    gameState: DropMergeState,
    viewModel: FallMergeViewModel
) {
    val theme = LocalGameTheme.current
    val density = LocalDensity.current
    val currentColor = mergeTileColor(theme, gameState.currentValue)
    val inputEnabled = !gameState.isResolving && !gameState.isGameOver && !gameState.isPaused

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        val gap = (min(maxWidth.value, maxHeight.value) * 0.010f).dp.coerceIn(3.dp, 7.dp)
        val dropperGap = gap * 2.5f
        val dropperExtra = 8.dp
        val cell: Dp = min(
            ((maxWidth - gap * (FCOLS + 1)) / FCOLS).value,
            ((maxHeight - gap * (FROWS + 1) - dropperGap - dropperExtra) / (FROWS + 1)).value
        ).dp
        val boardW = cell * FCOLS + gap * (FCOLS + 1)
        val boardH = cell * FROWS + gap * (FROWS + 1)

        val cellPx = with(density) { cell.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val boardHPx = with(density) { boardH.toPx() }

        fun xOf(col: Int) = gapPx + col * (cellPx + gapPx)
        // Bottom-anchored: row 0 sits on the floor, stacks grow upward.
        fun yOf(row: Int) = boardHPx - gapPx - cellPx - row * (cellPx + gapPx)
        fun colFromX(x: Float): Int =
            ((x - gapPx / 2f) / (cellPx + gapPx)).toInt().coerceIn(0, FCOLS - 1)

        var aimCol by remember { mutableStateOf<Int?>(null) }
        var lastDropCol by remember { mutableIntStateOf(FCOLS / 2) }
        val dropperCol = aimCol ?: lastDropCol

        fun fire(col: Int) {
            lastDropCol = col
            viewModel.drop(col)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ── The dropper (top launcher) ──
            FallDropper(
                boardW = boardW,
                cell = cell,
                gap = gap,
                extra = dropperExtra,
                currentValue = gameState.currentValue,
                moveCount = gameState.moveCount,
                dropperCol = dropperCol,
                enabled = inputEnabled,
                onShoot = { if (inputEnabled) fire(it) }
            )

            Spacer(Modifier.height(dropperGap))

            // ── The board ──
            Box(
                modifier = Modifier
                    .width(boardW)
                    .height(boardH)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.surfaceColor.copy(alpha = 0.55f))
                    .pointerInput(inputEnabled) {
                        detectTapGestures { offset -> if (inputEnabled) fire(colFromX(offset.x)) }
                    }
                    .pointerInput(inputEnabled) {
                        detectDragGestures(
                            onDragStart = { offset -> if (inputEnabled) aimCol = colFromX(offset.x) },
                            onDrag = { change, _ -> if (inputEnabled) aimCol = colFromX(change.position.x) },
                            onDragEnd = {
                                if (inputEnabled) aimCol?.let { fire(it) }
                                aimCol = null
                            },
                            onDragCancel = { aimCol = null }
                        )
                    }
                    .testTag("FallMerge_Board")
            ) {
                // Column tracks; the aimed one glows from the top (entry side).
                for (c in 0 until FCOLS) {
                    val fill = gameState.columns[c].size
                    FallColumnTrack(
                        offsetX = with(density) { xOf(c).toDp() },
                        width = cell,
                        height = boardH - gap * 2,
                        topPad = gap,
                        danger = fill >= FROWS - DropMergeConfig.DANGER_FREE_CELLS,
                        critical = fill >= FROWS,
                        beamColor = if (aimCol == c) currentColor else null
                    )
                }

                // Ghost landing slot — where the tile will come to rest.
                aimCol?.let { c ->
                    val landRow = min(gameState.columns[c].size, FROWS - 1)
                    FallGhostSlot(
                        offset = IntOffset(xOf(c).roundToInt(), yOf(landRow).roundToInt()),
                        size = cell,
                        color = currentColor
                    )
                }

                // Live tiles — enter from above and free-fall to their slot.
                gameState.columns.forEachIndexed { c, columnTiles ->
                    columnTiles.forEachIndexed { r, tile ->
                        key(tile.id) {
                            BoardTileView(
                                tile = tile,
                                target = Offset(xOf(c), yOf(r)),
                                spawn = Offset(xOf(c), -cellPx * 1.2f),
                                size = cell,
                                theme = theme,
                                gravity = true
                            )
                        }
                    }
                }

                // Consumed merge ghosts / purge sparkles.
                val step = gameState.lastStep
                if (step != null && step.consumed.isNotEmpty()) {
                    step.consumed.forEach { consumed ->
                        key("fall-ghost-${consumed.tile.id}") {
                            ConsumedTileView(
                                tile = consumed.tile,
                                from = Offset(xOf(consumed.fromCol), yOf(consumed.fromRow)),
                                to = Offset(xOf(consumed.toCol), yOf(consumed.toRow)),
                                size = cell,
                                theme = theme,
                                purge = step.kind == DropStepKind.PURGE
                            )
                        }
                    }
                }

                if (gameState.comboCount >= 2) {
                    key(gameState.comboCount) {
                        ComboText(
                            combo = gameState.comboCount,
                            fontSize = (cell.value * 0.42f).sp,
                            accentColor = theme.accentColor,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FallColumnTrack(
    offsetX: Dp,
    width: Dp,
    height: Dp,
    topPad: Dp,
    danger: Boolean,
    critical: Boolean,
    beamColor: Color?
) {
    val theme = LocalGameTheme.current
    val pulse = rememberInfiniteTransition(label = "fallDangerPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = if (critical) 0.9f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (critical) 380 else 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fallDangerAlpha"
    )

    val borderColor = when {
        danger -> theme.primaryColor.copy(alpha = pulseAlpha)
        beamColor != null -> beamColor.copy(alpha = 0.65f)
        else -> theme.textColor.copy(alpha = 0.06f)
    }

    // Aimed column: gradient beam strongest at the TOP (where the tile enters).
    val backgroundModifier = if (beamColor != null) {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    beamColor.copy(alpha = 0.26f),
                    beamColor.copy(alpha = 0.10f),
                    beamColor.copy(alpha = 0.03f)
                )
            )
        )
    } else {
        Modifier.background(theme.backgroundColor.copy(alpha = 0.45f))
    }

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = topPad)
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .then(backgroundModifier)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    )
}

@Composable
private fun FallGhostSlot(offset: IntOffset, size: Dp, color: Color) {
    val pulse = rememberInfiniteTransition(label = "fallGhostPulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "fallGhostAlpha"
    )
    Box(
        modifier = Modifier
            .offset { offset }
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, color.copy(alpha = alpha), RoundedCornerShape(10.dp))
            .background(color.copy(alpha = alpha * 0.15f))
    )
}

// ────────────────────────────────── Dropper ─────────────────────────────────

/**
 * The dropper strip above the board: one slidable tile that springs to the
 * aimed column, follows horizontal drags, and pops when a new tile loads.
 */
@Composable
private fun FallDropper(
    boardW: Dp,
    cell: Dp,
    gap: Dp,
    extra: Dp,
    currentValue: Int,
    moveCount: Int,
    dropperCol: Int,
    enabled: Boolean,
    onShoot: (Int) -> Unit
) {
    val theme = LocalGameTheme.current
    val density = LocalDensity.current
    val cellPx = with(density) { cell.toPx() }
    val gapPx = with(density) { gap.toPx() }
    fun colFromX(x: Float): Int =
        ((x - gapPx / 2f) / (cellPx + gapPx)).toInt().coerceIn(0, FCOLS - 1)

    val bob = rememberInfiniteTransition(label = "dropperBob")
    val bobY by bob.animateFloat(
        initialValue = 0f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dropperBobY"
    )

    val tileX by animateDpAsState(
        targetValue = gap + (cell + gap) * dropperCol,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "dropperTileX"
    )

    val reloadScale = remember { Animatable(1f) }
    LaunchedEffect(currentValue, moveCount) {
        reloadScale.snapTo(0.55f)
        reloadScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }

    val color = mergeTileColor(theme, currentValue)
    var aimTracking by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .width(boardW)
            .height(cell + extra)
            .pointerInput(enabled) {
                detectDragGestures(
                    onDragStart = { offset -> if (enabled) aimTracking = colFromX(offset.x) },
                    onDrag = { change, _ -> if (enabled) aimTracking = colFromX(change.position.x) },
                    onDragEnd = {
                        if (enabled) aimTracking?.let { onShoot(it) }
                        aimTracking = null
                    },
                    onDragCancel = { aimTracking = null }
                )
            }
    ) {
        for (c in 0 until FCOLS) {
            Box(
                modifier = Modifier
                    .offset(x = gap + (cell + gap) * c)
                    .width(cell)
                    .height(cell + extra)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.surfaceColor.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        if (c == dropperCol) color.copy(alpha = 0.5f)
                        else theme.textColor.copy(alpha = 0.07f),
                        RoundedCornerShape(10.dp)
                    )
                    .pointerInput(enabled) {
                        detectTapGestures { if (enabled) onShoot(c) }
                    }
                    .testTag("FallMerge_DropperSlot_$c"),
                contentAlignment = Alignment.Center
            ) {
                if (c != dropperCol) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = theme.textColor.copy(alpha = 0.25f),
                        modifier = Modifier.size(cell * 0.4f)
                    )
                }
            }
        }

        // The single slidable loaded tile.
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(listOf(color, color.copy(alpha = 0.85f)))
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .alpha(if (enabled) 1f else 0.45f),
                contentAlignment = Alignment.Center
            ) {
                TileNumber(value = currentValue, cell = cell, background = color)
            }
        }
    }
}

// ───────────────────────────── Controls & badges ────────────────────────────

@Composable
private fun FallSkipButton(
    enabled: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
    val theme = LocalGameTheme.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .height(height)
            .clip(shape)
            .background(theme.surfaceColor.copy(alpha = 0.8f))
            .border(1.dp, theme.accentColor.copy(alpha = if (enabled) 0.55f else 0.2f), shape)
            .testTag("FallMerge_Button_Skip")
            .let { if (enabled) it.pointerInput(Unit) { detectTapGestures { onClick() } } else it }
            .padding(horizontal = height * 0.4f)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = stringResource(R.string.drop_skip),
            tint = theme.accentColor,
            modifier = Modifier.size(height * 0.55f)
        )
        Spacer(Modifier.width(height * 0.15f))
        Text(
            text = stringResource(R.string.drop_skip),
            color = theme.accentColor,
            fontSize = (height.value * 0.34f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun FallUnlockBadge(target: Int, height: Dp) {
    val theme = LocalGameTheme.current
    val color = mergeTileColor(theme, target)
    NeonCard(
        accentColor = color,
        isSelected = false,
        onClick = null,
        cornerRadius = height * 0.18f
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = height * 0.16f, vertical = height * 0.08f)
        ) {
            Box(
                modifier = Modifier
                    .size(height * 0.52f)
                    .clip(RoundedCornerShape(height * 0.12f))
                    .background(color.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = target.toString(),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = (height.value * if (target >= 1000) 0.14f else 0.18f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(height * 0.03f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = theme.textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(height * 0.14f)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = stringResource(R.string.drop_locked),
                    color = theme.textColor.copy(alpha = 0.6f),
                    fontSize = (height.value * 0.13f).sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FallUnlockBanner(value: Int) {
    val theme = LocalGameTheme.current
    val color = mergeTileColor(theme, value)
    NeonCard(
        accentColor = color,
        isSelected = true,
        onClick = null,
        cornerRadius = 16.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.8f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    color = Color.White,
                    fontSize = if (value >= 1000) 10.sp else 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.drop_unlocked),
                color = theme.textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun FallNextChip(nextValue: Int, height: Dp) {
    val theme = LocalGameTheme.current
    val color = mergeTileColor(theme, nextValue)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.drop_next),
            color = theme.textColor.copy(alpha = 0.55f),
            fontSize = (height.value * 0.34f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(height * 0.22f))
        key(nextValue) {
            val appear = remember { Animatable(0.6f) }
            LaunchedEffect(Unit) {
                appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
            }
            Box(
                modifier = Modifier
                    .size(height)
                    .scale(appear.value)
                    .clip(RoundedCornerShape(height * 0.24f))
                    .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.85f))))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(height * 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nextValue.toString(),
                    color = if (color.luminance() > 0.55f) Color(0xFF1E1E2E) else Color.White,
                    fontSize = (height.value * if (nextValue >= 100) 0.30f else 0.38f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
