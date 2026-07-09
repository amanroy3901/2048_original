package com.avfusionapps.game_2048.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.model.DropTile
import com.avfusionapps.game_2048.model.RiseConfig
import com.avfusionapps.game_2048.model.RiseState
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
import com.avfusionapps.game_2048.viewmodel.RiseGameEvent
import com.avfusionapps.game_2048.viewmodel.RiseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private const val LANES = RiseConfig.LANES
private const val RISE_ROWS = RiseConfig.ROWS

@Composable
fun RiseScreen(
    navController: NavController,
    viewModel: RiseViewModel = viewModel()
) {
    val theme = LocalGameTheme.current
    val gameState by viewModel.gameState.collectAsState()
    val bestScore by viewModel.bestScore.collectAsState(initial = 0)
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }

    // Horizontal shake on game over; vertical dip when a pressure row slams in.
    val shakeX = remember { Animatable(0f) }
    val dipY = remember { Animatable(0f) }

    LaunchedEffect(vibrationEnabled, soundEnabled) {
        viewModel.events.collectLatest { event ->
            when (event) {
                RiseGameEvent.GAME_OVER -> launch {
                    listOf(0f, -16f, 13f, -9f, 6f, -3f, 0f).forEach {
                        shakeX.animateTo(it, tween(42))
                    }
                }
                RiseGameEvent.PRESSURE -> launch {
                    dipY.animateTo(12f, tween(90))
                    dipY.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                }
                else -> Unit
            }
            if (vibrationEnabled) {
                when (event) {
                    RiseGameEvent.MERGE, RiseGameEvent.COMBO ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    RiseGameEvent.UNLOCK, RiseGameEvent.PRESSURE, RiseGameEvent.GAME_OVER ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    RiseGameEvent.SHOOT, RiseGameEvent.RERACK -> Unit
                }
            }
            if (soundEnabled) {
                withContext(Dispatchers.Default) {
                    when (event) {
                        RiseGameEvent.SHOOT, RiseGameEvent.RERACK ->
                            soundManager.playSound(SoundManager.SOUND_MOVE)
                        RiseGameEvent.MERGE, RiseGameEvent.COMBO, RiseGameEvent.PRESSURE ->
                            soundManager.playSound(SoundManager.SOUND_MERGE)
                        RiseGameEvent.UNLOCK -> soundManager.playSound(SoundManager.SOUND_LEVEL_UP)
                        RiseGameEvent.GAME_OVER -> soundManager.playSound(SoundManager.SOUND_GAME_OVER)
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
            .testTag("RiseScreen_Root")
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val isLandscape = screenW > screenH

        if (isLandscape) {
            RiseLandscape(
                screenW = screenW,
                screenH = screenH,
                gameState = gameState,
                bestScore = bestScore,
                shakeProvider = { IntOffset(shakeX.value.roundToInt(), dipY.value.roundToInt()) },
                viewModel = viewModel
            )
        } else {
            RisePortrait(
                screenW = screenW,
                screenH = screenH,
                gameState = gameState,
                bestScore = bestScore,
                shakeProvider = { IntOffset(shakeX.value.roundToInt(), dipY.value.roundToInt()) },
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
            RiseUnlockBanner(value = gameState.justUnlockedValue ?: 0)
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
private fun RisePortrait(
    screenW: Dp,
    screenH: Dp,
    gameState: RiseState,
    bestScore: Int,
    shakeProvider: () -> IntOffset,
    viewModel: RiseViewModel
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
                modifier = Modifier.testTag("Rise_Button_Back")
            )
            Spacer(Modifier.width(hPad * 0.8f))
            GameScoreBoard(
                score = gameState.score,
                highScore = maxOf(bestScore, gameState.score),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(hPad * 0.8f))
            RiseUnlockBadge(target = gameState.unlockTarget, height = barH)
        }

        Spacer(Modifier.height(barSpacing))

        RiseBoard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset { shakeProvider() },
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
                modifier = Modifier.testTag("Rise_Button_Undo")
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                PressureRing(
                    shotsLeft = gameState.shotsUntilPressure,
                    total = gameState.pressureEvery,
                    size = barH * 0.85f
                )
                Spacer(Modifier.width(hPad))
                Text(
                    text = stringResource(
                        if (gameState.canReRack) R.string.rise_rerack_hint else R.string.rise_rerack_used
                    ),
                    color = theme.textColor.copy(alpha = if (gameState.canReRack) 0.6f else 0.3f),
                    fontSize = (barH.value * 0.20f).sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            SquareIconButton(
                icon = Icons.Rounded.Pause,
                contentDescription = stringResource(R.string.desc_pause_button),
                onClick = { viewModel.togglePause() },
                size = barH * 0.85f,
                modifier = Modifier.testTag("Rise_Button_Pause")
            )
        }
    }
}

@Composable
private fun RiseLandscape(
    screenW: Dp,
    screenH: Dp,
    gameState: RiseState,
    bestScore: Int,
    shakeProvider: () -> IntOffset,
    viewModel: RiseViewModel
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
                modifier = Modifier.testTag("Rise_Button_Back")
            )
            GameScoreBoard(
                score = gameState.score,
                highScore = maxOf(bestScore, gameState.score),
                modifier = Modifier.fillMaxWidth()
            )
            RiseUnlockBadge(target = gameState.unlockTarget, height = screenH * 0.16f)
        }

        RiseBoard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .offset { shakeProvider() },
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
                modifier = Modifier.testTag("Rise_Button_Pause")
            )
            SquareIconButton(
                icon = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = stringResource(R.string.undo),
                onClick = { viewModel.undo() },
                tint = if (gameState.canUndo) theme.textColor else theme.textColor.copy(alpha = 0.3f),
                size = btnSize,
                modifier = Modifier.testTag("Rise_Button_Undo")
            )
            Spacer(Modifier.weight(1f))
            PressureRing(
                shotsLeft = gameState.shotsUntilPressure,
                total = gameState.pressureEvery,
                size = screenH * 0.14f
            )
            Text(
                text = stringResource(
                    if (gameState.canReRack) R.string.rise_rerack_hint else R.string.rise_rerack_used
                ),
                color = theme.textColor.copy(alpha = if (gameState.canReRack) 0.6f else 0.3f),
                fontSize = (screenH.value * 0.022f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────── Board ──────────────────────────────────

@Composable
private fun RiseBoard(
    modifier: Modifier,
    gameState: RiseState,
    viewModel: RiseViewModel
) {
    val theme = LocalGameTheme.current
    val density = LocalDensity.current
    val inputEnabled = !gameState.isResolving && !gameState.isGameOver && !gameState.isPaused

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        val gap = (min(maxWidth.value, maxHeight.value) * 0.010f).dp.coerceIn(2.dp, 6.dp)
        val rackGap = gap * 2.5f
        val rackExtra = 6.dp
        val cell: Dp = min(
            ((maxWidth - gap * (LANES + 1)) / LANES).value,
            ((maxHeight - gap * (RISE_ROWS + 1) - rackGap - rackExtra) / (RISE_ROWS + 1)).value
        ).dp
        val boardW = cell * LANES + gap * (LANES + 1)
        val boardH = cell * RISE_ROWS + gap * (RISE_ROWS + 1)

        val cellPx = with(density) { cell.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val boardHPx = with(density) { boardH.toPx() }

        fun xOf(lane: Int) = gapPx + lane * (cellPx + gapPx)
        fun yOf(row: Int) = gapPx + row * (cellPx + gapPx)
        fun laneFromX(x: Float): Int =
            ((x - gapPx / 2f) / (cellPx + gapPx)).toInt().coerceIn(0, LANES - 1)

        // Danger line sits on the boundary above row DANGER_ROW.
        val dangerY = yOf(RiseConfig.DANGER_ROW) - gapPx / 2f
        val anyNearLine = gameState.columns.any { it.size >= RiseConfig.DANGER_ROW - 1 }

        val pulse = rememberInfiniteTransition(label = "dangerLine")
        val linePulse by pulse.animateFloat(
            initialValue = if (anyNearLine) 0.45f else 0.28f,
            targetValue = if (anyNearLine) 1f else 0.38f,
            animationSpec = infiniteRepeatable(
                tween(if (gameState.graceActive) 260 else 600),
                RepeatMode.Reverse
            ),
            label = "dangerAlpha"
        )
        val lineColor = if (gameState.graceActive) Color(0xFFFF1744) else theme.accentColor

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(boardW)
                    .height(boardH)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.surfaceColor.copy(alpha = 0.55f))
                    .pointerInput(inputEnabled) {
                        detectTapGestures { offset ->
                            if (inputEnabled) viewModel.shoot(laneFromX(offset.x))
                        }
                    }
                    .testTag("Rise_Board")
            ) {
                // Lane tracks.
                for (lane in 0 until LANES) {
                    Box(
                        modifier = Modifier
                            .offset(x = with(density) { xOf(lane).toDp() }, y = gap)
                            .width(cell)
                            .height(boardH - gap * 2)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.backgroundColor.copy(alpha = 0.45f))
                            .border(1.dp, theme.textColor.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    )
                }

                // Dashed danger line.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = lineColor.copy(alpha = linePulse),
                        start = Offset(gapPx, dangerY),
                        end = Offset(size.width - gapPx, dangerY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(26f, 18f))
                    )
                }

                // Live tiles — shots enter from the rack with a tumble.
                gameState.columns.forEachIndexed { lane, stack ->
                    stack.forEachIndexed { row, tile ->
                        key(tile.id) {
                            BoardTileView(
                                tile = tile,
                                target = Offset(xOf(lane), yOf(row)),
                                spawn = Offset(xOf(lane), boardHPx + cellPx * 0.55f),
                                size = cell,
                                theme = theme,
                                tumbleOnSpawn = true
                            )
                        }
                    }
                }

                // Consumed merge ghosts.
                val step = gameState.lastStep
                if (step != null && step.consumed.isNotEmpty()) {
                    step.consumed.forEach { consumed ->
                        key("rise-ghost-${consumed.tile.id}") {
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

            Spacer(Modifier.height(rackGap))

            RiseRack(
                boardW = boardW,
                cell = cell,
                gap = gap,
                extra = rackExtra,
                rack = gameState.rack,
                enabled = inputEnabled,
                onShoot = { viewModel.shoot(it) },
                onReRack = { viewModel.reRack(it) }
            )
        }
    }
}

// ─────────────────────────────────── Rack ───────────────────────────────────

/** The floor rack: tap a tile to fire it up its lane, long-press to re-rack. */
@Composable
private fun RiseRack(
    boardW: Dp,
    cell: Dp,
    gap: Dp,
    extra: Dp,
    rack: List<DropTile?>,
    enabled: Boolean,
    onShoot: (Int) -> Unit,
    onReRack: (Int) -> Unit
) {
    val theme = LocalGameTheme.current

    Box(
        modifier = Modifier
            .width(boardW)
            .height(cell + extra)
    ) {
        for (lane in 0 until LANES) {
            val tile = rack.getOrNull(lane)
            Box(
                modifier = Modifier
                    .offset(x = gap + (cell + gap) * lane)
                    .width(cell)
                    .height(cell + extra)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.surfaceColor.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        if (tile != null) theme.textColor.copy(alpha = 0.14f)
                        else theme.textColor.copy(alpha = 0.05f),
                        RoundedCornerShape(10.dp)
                    )
                    .pointerInput(enabled, tile?.id) {
                        detectTapGestures(
                            onTap = { if (enabled && tile != null) onShoot(lane) },
                            onLongPress = { if (enabled && tile != null) onReRack(lane) }
                        )
                    }
                    .testTag("Rise_RackSlot_$lane"),
                contentAlignment = Alignment.Center
            ) {
                if (tile != null) {
                    // Keyed by id so refills and re-racks pop in fresh.
                    key(tile.id) {
                        val appear = remember { Animatable(0.4f) }
                        LaunchedEffect(Unit) {
                            appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                        }
                        val color = mergeTileColor(theme, tile.value)
                        Box(
                            modifier = Modifier
                                .size(cell * 0.92f)
                                .scale(appear.value)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(color.copy(alpha = 0.30f), Color.Transparent)
                                        ),
                                        radius = size.maxDimension * 0.8f,
                                        center = center
                                    )
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.85f))))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                                .alpha(if (enabled) 1f else 0.45f),
                            contentAlignment = Alignment.Center
                        ) {
                            TileNumber(value = tile.value, cell = cell, background = color)
                        }
                    }
                } else {
                    // Empty socket while refilling.
                    Box(
                        modifier = Modifier
                            .size(cell * 0.5f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.textColor.copy(alpha = 0.05f))
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Indicators & badges ──────────────────────────

/** Circular countdown to the next pressure row; fills as shots are spent. */
@Composable
private fun PressureRing(
    shotsLeft: Int,
    total: Int,
    size: Dp
) {
    val theme = LocalGameTheme.current
    val progress = 1f - (shotsLeft.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val urgent = shotsLeft <= 2

    val pulse = rememberInfiniteTransition(label = "ringPulse")
    val ringAlpha by pulse.animateFloat(
        initialValue = if (urgent) 0.55f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (urgent) 320 else 800), RepeatMode.Reverse),
        label = "ringAlpha"
    )
    val ringColor = if (urgent) theme.primaryColor else theme.accentColor

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.10f
            drawArc(
                color = ringColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor.copy(alpha = ringAlpha),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = shotsLeft.toString(),
            color = theme.textColor,
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun RiseUnlockBadge(target: Int, height: Dp) {
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
private fun RiseUnlockBanner(value: Int) {
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
