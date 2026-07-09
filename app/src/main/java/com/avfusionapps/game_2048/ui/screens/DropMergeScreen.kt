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
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import com.avfusionapps.game_2048.model.DropTile
import com.avfusionapps.game_2048.ui.components.GameOverDialog
import com.avfusionapps.game_2048.ui.components.GamePauseDialog
import com.avfusionapps.game_2048.ui.components.GameScoreBoard
import com.avfusionapps.game_2048.ui.components.NeonCard
import com.avfusionapps.game_2048.ui.components.SquareIconButton
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

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }

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
                    DropGameEvent.UNLOCK, DropGameEvent.PURGE, DropGameEvent.GAME_OVER ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    DropGameEvent.SHOOT, DropGameEvent.SKIP -> Unit
                }
            }
            if (soundEnabled) {
                // SoundManager sleeps between tones — keep it off the main thread.
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

    BackHandler { viewModel.togglePause() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .safeDrawingPadding()
            .testTag("DropMergeScreen_Root")
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val isLandscape = screenW > screenH

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
            DropMergePortrait(
                screenW = screenW,
                screenH = screenH,
                gameState = gameState,
                bestScore = bestScore,
                shakeXProvider = { shakeX.value },
                viewModel = viewModel
            )
        }

        // ── Unlock banner ──
        AnimatedVisibility(
            visible = gameState.justUnlockedValue != null,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = screenH * 0.12f)
        ) {
            val unlockedValue = gameState.justUnlockedValue ?: 0
            UnlockBanner(value = unlockedValue)
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
        GameOverDialog(
            score = gameState.score,
            onNewGame = { viewModel.startNewGame() },
            onExit = { navController.popBackStack() }
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

        // ── Top bar: back / score / next-unlock badge ──
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
                modifier = Modifier.testTag("DropMerge_Button_Back")
            )
            Spacer(Modifier.width(hPad * 0.8f))
            GameScoreBoard(
                score = gameState.score,
                highScore = maxOf(bestScore, gameState.score),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(hPad * 0.8f))
            UnlockBadge(target = gameState.unlockTarget, height = barH)
        }

        Spacer(Modifier.height(barSpacing))

        // ── Board + launcher ──
        DropBoard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset { IntOffset(shakeXProvider().roundToInt(), 0) },
            gameState = gameState,
            viewModel = viewModel
        )

        Spacer(Modifier.height(barSpacing))

        // ── Bottom bar: undo / skip + next / pause ──
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
                modifier = Modifier.testTag("DropMerge_Button_Undo")
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                NextTileChip(nextValue = gameState.nextValue, height = barH * 0.62f)
                Spacer(Modifier.width(hPad * 0.9f))
                SkipTileButton(
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
                modifier = Modifier.testTag("DropMerge_Button_Pause")
            )
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
    val pad = screenH * 0.03f
    val sideW = screenW * 0.24f
    val btnSize = screenH * 0.115f

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(pad)
    ) {
        // ── Left panel: back, score, unlock badge ──
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
                modifier = Modifier.testTag("DropMerge_Button_Back")
            )
            GameScoreBoard(
                score = gameState.score,
                highScore = maxOf(bestScore, gameState.score),
                modifier = Modifier.fillMaxWidth()
            )
            UnlockBadge(target = gameState.unlockTarget, height = screenH * 0.16f)
        }

        // ── Center: board + launcher ──
        DropBoard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .offset { IntOffset(shakeXProvider().roundToInt(), 0) },
            gameState = gameState,
            viewModel = viewModel
        )

        // ── Right panel: controls ──
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
                modifier = Modifier.testTag("DropMerge_Button_Pause")
            )
            SquareIconButton(
                icon = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = stringResource(R.string.undo),
                onClick = { viewModel.undo() },
                tint = if (gameState.canUndo) theme.textColor else theme.textColor.copy(alpha = 0.3f),
                size = btnSize,
                modifier = Modifier.testTag("DropMerge_Button_Undo")
            )
            Spacer(Modifier.weight(1f))
            NextTileChip(nextValue = gameState.nextValue, height = screenH * 0.085f)
            SkipTileButton(
                enabled = gameState.canSkip && !gameState.isResolving && !gameState.isGameOver,
                height = screenH * 0.095f,
                onClick = { viewModel.skipTile() }
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
    val density = LocalDensity.current
    val currentColor = dropTileColor(theme, gameState.currentValue)
    val inputEnabled = !gameState.isResolving && !gameState.isGameOver && !gameState.isPaused

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // Geometry — everything derived from the available constraints.
        val gap = (min(maxWidth.value, maxHeight.value) * 0.010f).dp.coerceIn(3.dp, 7.dp)
        val launcherGap = gap * 2.5f
        val launcherExtra = 8.dp
        val cell: Dp = min(
            ((maxWidth - gap * (COLS + 1)) / COLS).value,
            ((maxHeight - gap * (ROWS + 1) - launcherGap - launcherExtra) / (ROWS + 1)).value
        ).dp
        val boardW = cell * COLS + gap * (COLS + 1)
        val boardH = cell * ROWS + gap * (ROWS + 1)

        val cellPx = with(density) { cell.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val boardHPx = with(density) { boardH.toPx() }

        fun xOf(col: Int) = gapPx + col * (cellPx + gapPx)
        fun yOf(row: Int) = gapPx + row * (cellPx + gapPx)
        fun colFromX(x: Float): Int =
            ((x - gapPx / 2f) / (cellPx + gapPx)).toInt().coerceIn(0, COLS - 1)

        var aimCol by remember { mutableStateOf<Int?>(null) }
        var lastShotCol by remember { mutableIntStateOf(COLS / 2) }
        val launcherCol = aimCol ?: lastShotCol

        fun fire(col: Int) {
            lastShotCol = col
            viewModel.shoot(col)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    .testTag("DropMerge_Board")
            ) {
                // Column tracks; the aimed one glows with the current tile's color.
                for (c in 0 until COLS) {
                    val fill = gameState.columns[c].size
                    ColumnTrack(
                        offsetX = with(density) { xOf(c).toDp() },
                        width = cell,
                        height = boardH - gap * 2,
                        topPad = gap,
                        danger = fill >= ROWS - DropMergeConfig.DANGER_FREE_CELLS,
                        critical = fill >= ROWS,
                        beamColor = if (aimCol == c) currentColor else null
                    )
                }

                // Ghost landing slot while aiming — tinted like the tile.
                aimCol?.let { c ->
                    val landRow = min(gameState.columns[c].size, ROWS - 1)
                    GhostSlot(
                        offset = IntOffset(xOf(c).roundToInt(), yOf(landRow).roundToInt()),
                        size = cell,
                        color = currentColor
                    )
                }

                // Live tiles — keyed by stable id so moves/merges animate.
                gameState.columns.forEachIndexed { c, columnTiles ->
                    columnTiles.forEachIndexed { r, tile ->
                        key(tile.id) {
                            BoardTileView(
                                tile = tile,
                                target = Offset(xOf(c), yOf(r)),
                                spawn = Offset(xOf(c), boardHPx + cellPx * 0.4f),
                                size = cell
                            )
                        }
                    }
                }

                // Consumed tiles from the current step — fly into the merge
                // target (MERGE) or shrink away in place (PURGE).
                val step = gameState.lastStep
                if (step != null && step.consumed.isNotEmpty()) {
                    step.consumed.forEach { consumed ->
                        key("ghost-${consumed.tile.id}") {
                            ConsumedTileView(
                                tile = consumed.tile,
                                from = Offset(xOf(consumed.fromCol), yOf(consumed.fromRow)),
                                to = Offset(xOf(consumed.toCol), yOf(consumed.toRow)),
                                size = cell,
                                purge = step.kind == DropStepKind.PURGE
                            )
                        }
                    }
                }

                // Combo flair.
                if (gameState.comboCount >= 2) {
                    key(gameState.comboCount) {
                        ComboText(
                            combo = gameState.comboCount,
                            fontSize = (cell.value * 0.42f).sp,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }

            Spacer(Modifier.height(launcherGap))

            // ── Launcher ──
            Launcher(
                boardW = boardW,
                cell = cell,
                gap = gap,
                extra = launcherExtra,
                currentValue = gameState.currentValue,
                moveCount = gameState.moveCount,
                launcherCol = launcherCol,
                enabled = inputEnabled,
                onAim = { aimCol = it },
                onRelease = {
                    if (inputEnabled) aimCol?.let { fire(it) }
                    aimCol = null
                },
                onShoot = { if (inputEnabled) fire(it) }
            )
        }
    }
}

@Composable
private fun ColumnTrack(
    offsetX: Dp,
    width: Dp,
    height: Dp,
    topPad: Dp,
    danger: Boolean,
    critical: Boolean,
    beamColor: Color?
) {
    val theme = LocalGameTheme.current
    val pulse = rememberInfiniteTransition(label = "dangerPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = if (critical) 0.9f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (critical) 380 else 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dangerAlpha"
    )

    val borderColor = when {
        danger -> theme.primaryColor.copy(alpha = pulseAlpha)
        beamColor != null -> beamColor.copy(alpha = 0.65f)
        else -> theme.textColor.copy(alpha = 0.06f)
    }

    // Aimed column: a soft gradient beam in the incoming tile's color,
    // strongest where the tile enters (bottom) and fading toward the ceiling.
    val backgroundModifier = if (beamColor != null) {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    beamColor.copy(alpha = 0.03f),
                    beamColor.copy(alpha = 0.10f),
                    beamColor.copy(alpha = 0.26f)
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
private fun GhostSlot(offset: IntOffset, size: Dp, color: Color) {
    val pulse = rememberInfiniteTransition(label = "ghostPulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "ghostAlpha"
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

/** A live tile: animates position by id, pops + glows on value growth. */
@Composable
private fun BoardTileView(
    tile: DropTile,
    target: Offset,
    spawn: Offset,
    size: Dp
) {
    val theme = LocalGameTheme.current
    val position = remember { Animatable(spawn, Offset.VectorConverter) }
    val popScale = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }
    var lastValue by remember { mutableIntStateOf(tile.value) }

    LaunchedEffect(target) {
        position.animateTo(
            target,
            spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    LaunchedEffect(tile.value) {
        if (tile.value != lastValue) {
            lastValue = tile.value
            launch {
                popScale.snapTo(1.28f)
                popScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
            }
            launch {
                glowAlpha.snapTo(0.85f)
                glowAlpha.animateTo(0f, tween(DropAnim.MERGE_POP * 2, easing = LinearOutSlowInEasing))
            }
        }
    }

    val color = dropTileColor(theme, tile.value)
    val glowColor = theme.accentColor

    Box(
        modifier = Modifier
            .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
            .size(size)
            .scale(popScale.value)
            .drawBehind {
                if (glowAlpha.value > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = glowAlpha.value), Color.Transparent)
                        ),
                        radius = this.size.maxDimension * 0.85f,
                        center = center
                    )
                }
            }
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.82f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        TileNumber(value = tile.value, cell = size, background = color)
    }
}

/** A consumed tile ghost: flies into the merge target or purges in place. */
@Composable
private fun ConsumedTileView(
    tile: DropTile,
    from: Offset,
    to: Offset,
    size: Dp,
    purge: Boolean
) {
    val theme = LocalGameTheme.current
    val position = remember { Animatable(from, Offset.VectorConverter) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            if (!purge) position.animateTo(to, tween(DropAnim.CONSUME_FLY, easing = LinearOutSlowInEasing))
        }
        launch { alpha.animateTo(0f, tween(if (purge) DropAnim.MERGE_POP * 2 else DropAnim.CONSUME_FLY)) }
        launch { scale.animateTo(if (purge) 0.1f else 0.6f, tween(if (purge) DropAnim.MERGE_POP * 2 else DropAnim.CONSUME_FLY)) }
    }

    val color = dropTileColor(theme, tile.value)
    Box(
        modifier = Modifier
            .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
            .size(size)
            .scale(scale.value)
            .alpha(alpha.value)
            .clip(RoundedCornerShape(10.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        TileNumber(value = tile.value, cell = size, background = color)
    }
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

@Composable
private fun ComboText(
    combo: Int,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val rise = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) }
        launch { rise.animateTo(-38f, tween(750, easing = LinearOutSlowInEasing)) }
        launch {
            delay(420)
            alpha.animateTo(0f, tween(330))
        }
    }
    Text(
        text = stringResource(R.string.drop_combo, combo),
        color = theme.accentColor,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        modifier = modifier
            .padding(top = 46.dp)
            .offset { IntOffset(0, rise.value.roundToInt()) }
            .scale(scale.value)
            .alpha(alpha.value)
    )
}

// ───────────────────────────────── Launcher ─────────────────────────────────

/**
 * The launcher strip. The loaded tile is ONE block that slides smoothly to the
 * aimed column (spring), can be dragged along the strip, and pops on reload.
 */
@Composable
private fun Launcher(
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
        modifier = Modifier
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
private fun SkipTileButton(
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
            .testTag("DropMerge_Button_Skip")
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
private fun UnlockBadge(target: Int, height: Dp) {
    val theme = LocalGameTheme.current
    val color = dropTileColor(theme, target)
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
private fun UnlockBanner(value: Int) {
    val theme = LocalGameTheme.current
    val color = dropTileColor(theme, value)
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
private fun NextTileChip(nextValue: Int, height: Dp) {
    val theme = LocalGameTheme.current
    val color = dropTileColor(theme, nextValue)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.drop_next),
            color = theme.textColor.copy(alpha = 0.55f),
            fontSize = (height.value * 0.34f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(height * 0.22f))
        // Animated swap when the value changes.
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

/** Theme color for a tile value; values above 2048 reuse the 2048 accent. */
private fun dropTileColor(
    theme: GameTheme,
    value: Int
): Color = theme.tileColors[value]
    ?: theme.tileColors[2048]
    ?: theme.primaryColor
