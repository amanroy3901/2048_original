package com.avfusionapps.game_2048.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.avfusionapps.game_2048.ui.components.ComboText
import com.avfusionapps.game_2048.ui.components.ConsumedTileView
import com.avfusionapps.game_2048.ui.components.GameOverDialog
import com.avfusionapps.game_2048.ui.components.GamePauseDialog
import com.avfusionapps.game_2048.ui.components.GameScoreBoard
import com.avfusionapps.game_2048.ui.components.NeonCard
import com.avfusionapps.game_2048.ui.components.SquareIconButton
import com.avfusionapps.game_2048.ui.components.TileNumber
import com.avfusionapps.game_2048.ui.components.mergeTileColor
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.utils.SoundManager
import com.avfusionapps.game_2048.viewmodel.DropAnim
import com.avfusionapps.game_2048.viewmodel.DropGameEvent
import com.avfusionapps.game_2048.viewmodel.FallMergeViewModel
import com.avfusionapps.game_2048.viewmodel.FallTuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val FCOLS = FallTuning.COLS
private const val FROWS = FallTuning.ROWS

/**
 * Neon Drop — the top-drop falling-merge game, built around SIMULATED
 * gravity: tiles free-fall with per-frame velocity integration (long drops
 * visibly accelerate), bounce on impact with velocity-scaled squash, tumble
 * one full turn on entry, and cascade collapses fall like rubble. The
 * current tile floats free above a 6×10 board and follows your finger;
 * a dashed guide line shows exactly where it will land.
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
    val barSpacing = screenH * 0.012f

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
        val gap = (min(maxWidth.value, maxHeight.value) * 0.008f).dp.coerceIn(2.dp, 6.dp)
        val dropperH: Dp
        val cell: Dp
        run {
            val cellW = (maxWidth - gap * (FCOLS + 1)) / FCOLS
            val cellH = (maxHeight - gap * (FROWS + 2)) / (FROWS + 1.3f)
            cell = min(cellW.value, cellH.value).dp
            dropperH = cell * 1.3f
        }
        val boardW = cell * FCOLS + gap * (FCOLS + 1)
        val boardH = cell * FROWS + gap * (FROWS + 1)

        val cellPx = with(density) { cell.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val boardWPx = with(density) { boardW.toPx() }
        val boardHPx = with(density) { boardH.toPx() }
        val dropperHPx = with(density) { dropperH.toPx() }

        fun xOf(col: Int) = gapPx + col * (cellPx + gapPx)
        // Bottom-anchored: row 0 sits on the floor, stacks grow upward.
        fun yOf(row: Int) = boardHPx - gapPx - cellPx - row * (cellPx + gapPx)
        fun colFromX(x: Float): Int =
            ((x - gapPx / 2f) / (cellPx + gapPx)).toInt().coerceIn(0, FCOLS - 1)

        // Free-floating dropper: the tile follows the finger while dragging.
        var dragX by remember { mutableStateOf<Float?>(null) }
        var lastDropCol by remember { mutableIntStateOf(FCOLS / 2) }
        val aimCol = dragX?.let { colFromX(it) }
        val dropperCol = aimCol ?: lastDropCol

        fun fire(col: Int) {
            lastDropCol = col
            viewModel.drop(col)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(boardW)
                // One gesture surface for dropper + board: drag anywhere to
                // aim (the tile tracks your finger), release to drop; tap to
                // drop straight into that column.
                .pointerInput(inputEnabled) {
                    detectTapGestures { offset -> if (inputEnabled) fire(colFromX(offset.x)) }
                }
                .pointerInput(inputEnabled) {
                    detectDragGestures(
                        onDragStart = { offset -> if (inputEnabled) dragX = offset.x },
                        onDrag = { change, _ ->
                            if (inputEnabled) dragX = change.position.x.coerceIn(0f, boardWPx)
                        },
                        onDragEnd = {
                            if (inputEnabled) aimCol?.let { fire(it) }
                            dragX = null
                        },
                        onDragCancel = { dragX = null }
                    )
                }
        ) {
            // ── The dropper: a free tile hovering above the board ──
            FallDropper(
                boardW = boardW,
                dropperH = dropperH,
                cell = cell,
                cellPx = cellPx,
                gapPx = gapPx,
                boardWPx = boardWPx,
                currentValue = gameState.currentValue,
                moveCount = gameState.moveCount,
                dropperCol = dropperCol,
                dragX = dragX,
                enabled = inputEnabled,
                xOf = { xOf(it) }
            )

            // ── The board: open field, no filled tracks ──
            Box(
                modifier = Modifier
                    .width(boardW)
                    .height(boardH)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.surfaceColor.copy(alpha = 0.40f))
                    .testTag("FallMerge_Board")
            ) {
                val dangerPulse = rememberInfiniteTransition(label = "fallDanger")
                val dangerAlpha by dangerPulse.animateFloat(
                    initialValue = 0.25f, targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                    label = "fallDangerAlpha"
                )

                // Static decor: faint column separators + bright floor line.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (c in 1 until FCOLS) {
                        val bx = xOf(c) - gapPx / 2f
                        drawLine(
                            color = theme.textColor.copy(alpha = 0.06f),
                            start = Offset(bx, gapPx),
                            end = Offset(bx, boardHPx - gapPx),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    drawLine(
                        color = theme.accentColor.copy(alpha = 0.35f),
                        start = Offset(gapPx, boardHPx - gapPx * 0.6f),
                        end = Offset(size.width - gapPx, boardHPx - gapPx * 0.6f),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Per-column danger glow at the TOP (columns close to full).
                for (c in 0 until minOf(FCOLS, gameState.columns.size)) {
                    val fill = gameState.columns[c].size
                    if (fill >= FROWS - DropMergeConfig.DANGER_FREE_CELLS) {
                        Box(
                            modifier = Modifier
                                .offset(x = with(density) { xOf(c).toDp() })
                                .width(cell)
                                .height(cell * 1.6f)
                                .alpha(if (fill >= FROWS) dangerAlpha else dangerAlpha * 0.6f)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            theme.primaryColor.copy(alpha = 0.45f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // Guide line + ghost slot while aiming.
                aimCol?.let { c ->
                    val landRow = min(
                        gameState.columns.getOrNull(c)?.size ?: 0,
                        FROWS - 1
                    )
                    val ghostY = yOf(landRow)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = currentColor.copy(alpha = 0.45f),
                            start = Offset(xOf(c) + cellPx / 2f, 0f),
                            end = Offset(xOf(c) + cellPx / 2f, ghostY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f))
                        )
                    }
                    FallGhostSlot(
                        offset = IntOffset(xOf(c).roundToInt(), ghostY.roundToInt()),
                        size = cell,
                        color = currentColor
                    )
                }

                // Live tiles — simulated gravity: accelerate, land, bounce.
                gameState.columns.forEachIndexed { c, columnTiles ->
                    columnTiles.forEachIndexed { r, tile ->
                        key(tile.id) {
                            PhysicsTileView(
                                tile = tile,
                                target = Offset(xOf(c), yOf(r)),
                                spawn = Offset(xOf(c), -(dropperHPx * 0.6f + cellPx)),
                                size = cell,
                                cellPx = cellPx,
                                theme = theme
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

// ───────────────────────────── Physics tile view ────────────────────────────

/**
 * A board tile driven by simulated gravity. Falls with per-frame velocity
 * integration (v += g·dt), so long drops visibly accelerate; lands with a
 * velocity-scaled squash + impact ring and one bounce when it comes in hot.
 * The entry fall tumbles a full 360°. Upward corrections (rare) use a spring.
 */
@Composable
private fun PhysicsTileView(
    tile: DropTile,
    target: Offset,
    spawn: Offset,
    size: Dp,
    cellPx: Float,
    theme: GameTheme
) {
    val xAnim = remember { Animatable(spawn.x) }
    val yAnim = remember { Animatable(spawn.y) }
    val rot = remember { Animatable(0f) }
    val squashY = remember { Animatable(1f) }
    val impactRing = remember { Animatable(0f) }
    val popScale = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }
    var lastValue by remember { mutableIntStateOf(tile.value) }
    var hasEntered by remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        launch {
            if (xAnim.value != target.x) {
                xAnim.animateTo(target.x, spring(stiffness = Spring.StiffnessMedium))
            }
        }
        launch {
            val g = cellPx * FallTuning.GRAVITY_CELLS
            if (target.y > yAnim.value + 1f) {
                // Entry fall gets a full tumble timed to finish on landing.
                if (!hasEntered) {
                    val dist = target.y - yAnim.value
                    val tMs = (sqrt(2f * dist / g) * 1000f).toInt().coerceAtLeast(120)
                    launch {
                        rot.snapTo(0f)
                        rot.animateTo(360f, tween(tMs, easing = LinearEasing))
                        rot.snapTo(0f)
                    }
                }
                hasEntered = true

                // Free-fall integration.
                var vel = 0f
                var lastFrame = -1L
                while (yAnim.value < target.y - 0.5f) {
                    val frame = withFrameNanos { it }
                    if (lastFrame < 0L) {
                        lastFrame = frame
                        continue
                    }
                    val dt = ((frame - lastFrame) / 1e9f).coerceAtMost(0.032f)
                    lastFrame = frame
                    vel += g * dt
                    yAnim.snapTo(min(yAnim.value + vel * dt, target.y))
                }
                yAnim.snapTo(target.y)

                // Impact juice scaled by landing velocity.
                val impact = (vel / (cellPx * 16f)).coerceIn(0.15f, 1f)
                launch {
                    squashY.snapTo(1f - 0.30f * impact)
                    squashY.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh))
                }
                launch {
                    impactRing.snapTo(impact)
                    impactRing.animateTo(0f, tween(280))
                }

                // One bounce when it lands hard enough.
                var bounceVel = -vel * FallTuning.RESTITUTION
                if (bounceVel < -cellPx * 1.4f) {
                    var v = bounceVel
                    var lf = -1L
                    while (true) {
                        val frame = withFrameNanos { it }
                        if (lf < 0L) {
                            lf = frame
                            continue
                        }
                        val dt = ((frame - lf) / 1e9f).coerceAtMost(0.032f)
                        lf = frame
                        v += g * dt
                        val next = yAnim.value + v * dt
                        if (v > 0f && next >= target.y) {
                            yAnim.snapTo(target.y)
                            break
                        }
                        yAnim.snapTo(next)
                    }
                }
            } else {
                hasEntered = true
                yAnim.animateTo(target.y, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
            }
        }
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
                glowAlpha.animateTo(0f, tween(DropAnim.MERGE_POP * 2))
            }
        }
    }

    val color = mergeTileColor(theme, tile.value)
    val glowColor = theme.accentColor

    Box(
        modifier = Modifier
            .offset { IntOffset(xAnim.value.roundToInt(), yAnim.value.roundToInt()) }
            .size(size)
            .scale(popScale.value)
            .graphicsLayer {
                rotationZ = rot.value
                scaleY = squashY.value
                scaleX = 1f + (1f - squashY.value) * 0.7f
            }
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
                if (impactRing.value > 0f) {
                    drawCircle(
                        color = color.copy(alpha = impactRing.value * 0.6f),
                        radius = this.size.maxDimension * (0.55f + (1f - impactRing.value) * 0.6f),
                        center = center,
                        style = Stroke(width = 3.dp.toPx() * impactRing.value)
                    )
                }
            }
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(colors = listOf(color, color.copy(alpha = 0.82f)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        TileNumber(value = tile.value, cell = size, background = color)
    }
}

// ────────────────────────────────── Dropper ─────────────────────────────────

/**
 * The free dropper: no slots — the current tile hovers above the board,
 * tracks your finger exactly while dragging, and springs to the last column
 * when idle. Faint chevrons mark the columns.
 */
@Composable
private fun FallDropper(
    boardW: Dp,
    dropperH: Dp,
    cell: Dp,
    cellPx: Float,
    gapPx: Float,
    boardWPx: Float,
    currentValue: Int,
    moveCount: Int,
    dropperCol: Int,
    dragX: Float?,
    enabled: Boolean,
    xOf: (Int) -> Float
) {
    val theme = LocalGameTheme.current

    val bob = rememberInfiniteTransition(label = "fallDropperBob")
    val bobY by bob.animateFloat(
        initialValue = 0f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "fallDropperBobY"
    )

    // Idle: spring-snap to the column. Dragging: follow the finger exactly.
    val snappedX by animateFloatAsState(
        targetValue = xOf(dropperCol),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "fallDropperX"
    )
    val tileX = dragX
        ?.let { (it - cellPx / 2f).coerceIn(gapPx, boardWPx - gapPx - cellPx) }
        ?: snappedX

    val reloadScale = remember { Animatable(1f) }
    LaunchedEffect(currentValue, moveCount) {
        reloadScale.snapTo(0.55f)
        reloadScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }

    val color = mergeTileColor(theme, currentValue)
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .width(boardW)
            .height(dropperH)
    ) {
        // Faint column chevrons.
        for (c in 0 until FCOLS) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = theme.textColor.copy(alpha = if (c == dropperCol) 0.35f else 0.12f),
                modifier = Modifier
                    .offset(
                        x = with(density) { (xOf(c) + cellPx / 2f).toDp() } - cell * 0.16f,
                        y = dropperH - cell * 0.34f
                    )
                    .size(cell * 0.32f)
                    .testTag("FallMerge_DropperSlot_$c")
            )
        }

        // The free-floating current tile.
        Box(
            modifier = Modifier
                .offset { IntOffset(tileX.roundToInt(), bobY.roundToInt()) }
                .size(cell),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(cell * 0.94f)
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
