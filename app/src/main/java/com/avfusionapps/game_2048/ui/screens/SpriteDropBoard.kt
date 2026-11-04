package com.avfusionapps.game_2048.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.model.DropMergeConfig
import com.avfusionapps.game_2048.model.DropMergeState
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.ui.sprites.ElementalSprites
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.ui.sprites.SpriteTile
import com.avfusionapps.game_2048.viewmodel.DropMergeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val COLS = DropMergeConfig.COLUMNS
private const val ROWS = DropMergeConfig.ROWS

/** The launcher sprite is a touch bigger than the board sprites; it shrinks to board size in flight. */
private const val LAUNCH_SCALE = 1.12f
private const val FLIGHT_MS = 160

private val BoardGold = Color(0xFFFFC45A)
private val LaneTrack = Color(0x8C140834)
private val BeamGold = Color(0xFFFFC04A)
private val BeamOrange = Color(0xFFFF9A2E)
private val BeamBlocked = Color(0xFFFF4D6D)

private val arrowPath by lazy { PathParser().parsePathString("M12 3.5 L20.5 12.5 H15.2 V20.5 H8.8 V12.5 H3.5 Z").toPath() }

/**
 * The Elemental Sprites Neon Shooter board: lanes with launch pads at their foot, a warm light cone that
 * rises from the launcher to the landing slot, and sprite tiles that fly, collide, fuse and react.
 * Same rules and view model as the classic board; only the presentation differs.
 */
@Composable
internal fun SpriteDropBoard(
    modifier: Modifier,
    gameState: DropMergeState,
    viewModel: DropMergeViewModel
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val inputEnabled = !gameState.isResolving && !gameState.isGameOver && !gameState.isPaused

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        // Geometry — 8 rows of cells, then the launch pads inside the board.
        val gap = (maxWidth.value * 0.016f).dp.coerceIn(4.dp, 8.dp)
        val padsGap = 8.dp
        val laneByW = (maxWidth - gap * (COLS + 1)) / COLS
        val laneByH = (maxHeight - gap * 2 - padsGap) / (ROWS * 0.86f + 0.92f)
        val lane: Dp = min(laneByW.value, laneByH.value).dp
        val cellH = lane * 0.86f
        val padsH = lane * 0.92f
        val boardW = lane * COLS + gap * (COLS + 1)
        val boardH = gap + cellH * ROWS + padsGap + padsH + gap

        val lanePx = with(density) { lane.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val cellHPx = with(density) { cellH.toPx() }
        val padsHPx = with(density) { padsH.toPx() }
        val padsTop = gapPx + ROWS * cellHPx + with(density) { padsGap.toPx() }
        val tileSizePx = min(lanePx * 1.08f, cellHPx * 1.24f)
        val tileSize = with(density) { tileSizePx.toDp() }

        fun laneX(c: Int) = gapPx + c * (lanePx + gapPx)
        fun cellCenter(c: Int, r: Int) = Offset(laneX(c) + lanePx / 2f, gapPx + r * cellHPx + cellHPx / 2f)
        fun launchCenter(c: Int) = Offset(laneX(c) + lanePx / 2f, padsTop + padsHPx / 2f - padsHPx * 0.08f)
        fun colFromX(x: Float) = ((x - gapPx / 2f) / (lanePx + gapPx)).toInt().coerceIn(0, COLS - 1)

        var aimCol by remember { mutableStateOf<Int?>(null) }
        var lastShotCol by remember { mutableIntStateOf(COLS / 2) }
        val launcherCol = aimCol ?: lastShotCol
        var firing by remember { mutableStateOf(false) }
        val recoil = remember { Animatable(0f) }

        // Recoil first (the launcher crouches), then the view model takes the shot.
        fun fire(col: Int) {
            if (firing || !inputEnabled) return
            lastShotCol = col
            aimCol = null
            firing = true
            scope.launch {
                recoil.snapTo(0f)
                recoil.animateTo(1f, tween(70, easing = FastOutLinearInEasing))
                viewModel.shoot(col)
                recoil.snapTo(0f)
                firing = false
            }
        }

        // Boredom: thinking after 6 s without a shot, asleep after 14 s.
        var idleLevel by remember { mutableIntStateOf(0) }
        LaunchedEffect(gameState.moveCount, aimCol) {
            idleLevel = 0
            delay(6_000)
            idleLevel = 1
            delay(8_000)
            idleLevel = 2
        }

        val aimedColumn = gameState.columns[launcherCol]
        val aimedFull = aimedColumn.size >= ROWS
        val blocked = aimedFull && aimedColumn.lastOrNull()?.value != gameState.currentValue

        // One animated lane position so the light, chevrons and launcher always slide together.
        val aimX by animateFloatAsState(laneX(launcherCol), spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium), label = "aimX")

        val phaseT = rememberInfiniteTransition(label = "shooterPhase")
        val phase by phaseT.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "phase")

        Box(
            modifier = Modifier
                .size(boardW, boardH)
                .drawBehind {
                    // Gold frame glow behind the board.
                    drawRoundRect(
                        brush = Brush.radialGradient(listOf(BoardGold.copy(alpha = 0.28f), Color.Transparent), radius = size.maxDimension * 0.7f),
                        cornerRadius = CornerRadius(26.dp.toPx())
                    )
                }
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Color(0x993C1E82), Color(0xCC200E50))))
                .border(2.dp, BoardGold.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                .pointerInput(inputEnabled) {
                    detectTapGestures { offset -> if (inputEnabled) fire(colFromX(offset.x)) }
                }
                .pointerInput(inputEnabled) {
                    detectDragGestures(
                        onDragStart = { offset -> if (inputEnabled) aimCol = colFromX(offset.x) },
                        onDrag = { change, _ -> if (inputEnabled) aimCol = colFromX(change.position.x) },
                        onDragEnd = { aimCol?.let { fire(it) }; aimCol = null },
                        onDragCancel = { aimCol = null }
                    )
                }
                .testTag("DropMerge_Board")
        ) {
            // ── Lane tracks (they run down behind the pads) ──
            for (c in 0 until COLS) {
                val fill = gameState.columns[c].size
                LaneTrack(
                    x = laneX(c),
                    widthDp = lane,
                    heightDp = boardH - gap,
                    topPx = gapPx / 2f,
                    danger = fill >= ROWS - DropMergeConfig.DANGER_FREE_CELLS,
                    critical = fill >= ROWS
                )
            }

            // ── Light cone from the launcher up to the landing slot ──
            if (!gameState.isGameOver) {
                val landRow = min(aimedColumn.size, ROWS - 1)
                val coneTop = if (aimedFull) gapPx else cellCenter(launcherCol, landRow).y - cellHPx / 2f
                val coneBottom = launchCenter(launcherCol).y
                Canvas(Modifier.fillMaxSize()) {
                    drawLightCone(aimX, lanePx, coneTop, coneBottom, blocked, phase)
                }
            }

            // ── Launch pads (the aimed one is covered by the launcher) ──
            // Hide only the pad the launcher is sitting over right now (it slides between lanes).
            val launcherOver = ((aimX - gapPx) / (lanePx + gapPx)).roundToInt().coerceIn(0, COLS - 1)
            for (c in 0 until COLS) {
                if (c == launcherOver && !gameState.isResolving) continue
                val padSize = with(density) { (padsHPx * 0.86f).toDp() }
                val center = Offset(laneX(c) + lanePx / 2f, padsTop + padsHPx / 2f)
                LaunchPad(center = center, size = padSize, blocked = gameState.columns[c].let { it.size >= ROWS && it.last().value != gameState.currentValue }, modifier = Modifier.testTag("DropMerge_LauncherSlot_$c"))
            }

            // ── Chevrons above the launcher ──
            if (!blocked && !gameState.isGameOver) {
                val lc = launchCenter(launcherCol)
                val chevTopPx = lc.y - tileSizePx * LAUNCH_SCALE * 0.5f - with(density) { 40.dp.toPx() }
                val landBottom = cellCenter(launcherCol, min(aimedColumn.size, ROWS - 1)).y + cellHPx * 0.4f
                if (chevTopPx > landBottom) {
                    Canvas(Modifier.fillMaxSize()) { drawChevrons(Offset(aimX + lanePx / 2f, chevTopPx), phase) }
                }
            }

            // ── Live sprites, keyed by id so moves and merges animate ──
            val step = gameState.lastStep
            val mergeKeyBase = gameState.moveCount * 32
            gameState.columns.forEachIndexed { c, column ->
                column.forEachIndexed { r, tile ->
                    key(tile.id) {
                        val isEnd = r == column.lastIndex
                        val emotion = when {
                            gameState.isGameOver -> SpriteEmotion.SAD
                            idleLevel >= 2 -> SpriteEmotion.SLEEPY
                            idleLevel == 1 -> SpriteEmotion.THINKING
                            isEnd && tile.value == gameState.currentValue && !gameState.isResolving -> SpriteEmotion.EXCITED
                            column.size >= ROWS - DropMergeConfig.DANGER_FREE_CELLS -> SpriteEmotion.WORRIED
                            else -> SpriteEmotion.IDLE
                        }
                        val merged = step?.kind == DropStepKind.MERGE && step.mergedTileId == tile.id
                        SpriteBoardTile(
                            value = tile.value,
                            target = cellCenter(c, r),
                            launch = launchCenter(c),
                            sizePx = tileSizePx,
                            size = tileSize,
                            fromLauncher = !gameState.justUndid,
                            emotion = emotion,
                            mergeReady = emotion == SpriteEmotion.EXCITED,
                            mergeKey = if (merged) mergeKeyBase + step!!.chainIndex else null
                        )
                    }
                }
            }

            // ── Sprites consumed this step: fuse into where the merged sprite ends up, or purge in place ──
            if (step != null && step.consumed.isNotEmpty()) {
                val fuseTo = step.mergedTileId?.let { id ->
                    step.columns.forEachIndexed { c, col -> col.forEachIndexed { r, t -> if (t.id == id) return@let cellCenter(c, r) } }
                    null
                }
                step.consumed.forEach { consumed ->
                    key("eaten-${consumed.tile.id}") {
                        SpriteConsumedTile(
                            value = consumed.tile.value,
                            from = cellCenter(consumed.fromCol, consumed.fromRow),
                            to = fuseTo ?: cellCenter(consumed.toCol, consumed.toRow),
                            sizePx = tileSizePx,
                            size = tileSize,
                            purge = step.kind == DropStepKind.PURGE
                        )
                    }
                }
            }

            // ── The shot itself: afterimages, glow streak and impact sparks ──
            if (step?.kind == DropStepKind.PLACE) {
                val placedCol = lastShotCol
                val placed = step.columns[placedCol].lastOrNull()
                if (placed != null) {
                    key("shot-${gameState.moveCount}") {
                        ShotEffects(
                            value = placed.value,
                            from = launchCenter(placedCol),
                            to = cellCenter(placedCol, step.columns[placedCol].lastIndex),
                            sizePx = tileSizePx,
                            size = tileSize
                        )
                    }
                }
            }

            // ── Launcher ──
            val lc = launchCenter(launcherCol)
            Launcher(
                value = gameState.currentValue,
                reloadKey = gameState.moveCount * 1000 + gameState.currentValue,
                center = Offset(aimX + lanePx / 2f, lc.y),
                sizePx = tileSizePx * LAUNCH_SCALE,
                size = tileSize * LAUNCH_SCALE,
                hidden = gameState.isResolving,
                blocked = blocked,
                recoil = recoil.value,
                phase = phase,
                modifier = Modifier.tutorialAnchor(TutorialAnchor.LAUNCHER)
            )

            if (gameState.comboCount >= 2) {
                key(gameState.comboCount) {
                    ComboText(
                        combo = gameState.comboCount,
                        fontSize = (lane.value * 0.42f).sp,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun LaneTrack(x: Float, widthDp: Dp, heightDp: Dp, topPx: Float, danger: Boolean, critical: Boolean) {
    val pulse = rememberInfiniteTransition(label = "laneDanger")
    val a by pulse.animateFloat(0.2f, if (critical) 0.95f else 0.7f, infiniteRepeatable(tween(if (critical) 380 else 650), RepeatMode.Reverse), label = "laneDangerA")
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), topPx.roundToInt()) }
            .size(widthDp, heightDp)
            .clip(RoundedCornerShape(16.dp))
            .background(LaneTrack)
            .then(if (danger) Modifier.border(2.dp, BeamBlocked.copy(alpha = a), RoundedCornerShape(16.dp)) else Modifier)
    )
}

/** Warm cone: narrow at the launcher, full lane width at the landing slot, feathered edges, light drifting up. */
private fun DrawScope.drawLightCone(laneLeft: Float, laneW: Float, top: Float, bottom: Float, blocked: Boolean, phase: Float) {
    if (bottom <= top) return
    val warm = if (blocked) BeamBlocked else BeamGold
    val far = if (blocked) BeamBlocked else BeamOrange
    val cx = laneLeft + laneW / 2f
    // Three nested trapezoids fake the feathered edge.
    listOf(1f to 0.35f, 0.82f to 0.55f, 0.62f to 0.8f).forEach { (wf, alpha) ->
        val topW = laneW * wf
        val botW = laneW * 0.52f * wf
        val path = Path().apply {
            moveTo(cx - botW / 2f, bottom)
            lineTo(cx + botW / 2f, bottom)
            lineTo(cx + topW / 2f, top)
            lineTo(cx - topW / 2f, top)
            close()
        }
        drawPath(
            path,
            Brush.verticalGradient(
                0f to far.copy(alpha = 0.03f * alpha),
                0.5f to far.copy(alpha = 0.22f * alpha),
                1f to warm.copy(alpha = 0.7f * alpha),
                startY = top,
                endY = bottom
            )
        )
        if (wf == 0.62f) {
            // faint bands of light drifting upward
            clipPath(path) {
                val band = 70.dp.toPx()
                val bandH = 9.dp.toPx()
                var y = bottom - (phase * band)
                while (y > top) {
                    drawRect(Color(0xFFFFECB4).copy(alpha = 0.035f), topLeft = Offset(cx - topW, y - bandH), size = Size(topW * 2, bandH))
                    y -= band
                }
            }
        }
    }
    // Bright base glow under the launcher.
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFFAE0).copy(alpha = 0.9f), warm.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(cx, bottom),
            radius = laneW * 0.45f
        ),
        topLeft = Offset(cx - laneW * 0.45f, bottom - laneW * 0.3f),
        size = Size(laneW * 0.9f, laneW * 0.6f)
    )
}

private fun DrawScope.drawChevrons(top: Offset, phase: Float) {
    val w = 26.dp.toPx()
    val h = 11.dp.toPx()
    for (k in 0 until 3) {
        val y = top.y + k * (h + 2.dp.toPx())
        // the top chevron lights first, so the light reads as climbing
        val a = 0.25f + 0.75f * ((sin((phase - k * 0.15f) * 2f * PI.toFloat()) + 1f) / 2f)
        val p = Path().apply {
            moveTo(top.x - w / 2f, y + h)
            lineTo(top.x, y)
            lineTo(top.x + w / 2f, y + h)
        }
        drawPath(p, Color(0xFFFFD25A).copy(alpha = a), style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun LaunchPad(center: Offset, size: Dp, blocked: Boolean, modifier: Modifier) {
    val density = LocalDensity.current
    val half = with(density) { size.toPx() } / 2f
    Box(
        modifier = modifier
            .offset { IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt()) }
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    if (blocked) listOf(Color(0x8C962846), Color(0xB3501028)) else listOf(Color(0x8C5A4296), Color(0xB32C1A5C))
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.62f)) {
            val s = this.size.width / 24f
            scale(s, s, pivot = Offset.Zero) { drawPath(arrowPath, Color.White.copy(alpha = 0.78f)) }
        }
    }
}

/** A sprite on the board: flies in from the launcher, collides, then springs to new rows as the stack shifts. */
@Composable
private fun SpriteBoardTile(
    value: Int,
    target: Offset,
    launch: Offset,
    sizePx: Float,
    size: Dp,
    fromLauncher: Boolean,
    emotion: SpriteEmotion,
    mergeReady: Boolean,
    mergeKey: Int?
) {
    val pos = remember { Animatable(if (fromLauncher) launch else target, Offset.VectorConverter) }
    val flightScale = remember { Animatable(if (fromLauncher) LAUNCH_SCALE else 0.45f) }
    val squashX = remember { Animatable(1f) }
    val squashY = remember { Animatable(1f) }
    var landed by remember { mutableStateOf(!fromLauncher) }
    var surprised by remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        if (!landed) {
            launch { flightScale.animateTo(1f, tween(FLIGHT_MS, easing = FastOutSlowInEasing)) }
            squashX.snapTo(0.84f)
            squashY.snapTo(1.22f)
            launch { squashX.animateTo(1f, tween(FLIGHT_MS)) }
            launch { squashY.animateTo(1f, tween(FLIGHT_MS)) }
            pos.animateTo(target, tween(FLIGHT_MS, easing = FastOutSlowInEasing))
            landed = true
            // Impact: flattened against the stack, then a springy settle.
            surprised = true
            launch { squashX.snapTo(1.14f); squashX.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 600f)) }
            launch { squashY.snapTo(0.82f); squashY.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 600f)) }
            delay(650)
            surprised = false
        } else {
            launch { if (flightScale.value < 1f) flightScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) }
            pos.animateTo(target, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    val half = sizePx / 2f
    Box(
        modifier = Modifier
            .offset { IntOffset((pos.value.x - half).roundToInt(), (pos.value.y - half).roundToInt()) }
            .size(size)
            .graphicsLayer {
                scaleX = flightScale.value
                scaleY = flightScale.value
            }
            .drawBehind {
                if (mergeReady) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(ElementalSprites.specFor(value).glow.copy(alpha = 0.55f), Color.Transparent)),
                        radius = this.size.minDimension * 0.62f
                    )
                }
            }
    ) {
        SpriteTile(
            value = value,
            modifier = Modifier.fillMaxSize(),
            emotion = if (surprised) SpriteEmotion.SURPRISED else emotion,
            lookY = if (mergeReady) 2f else 0f,
            charScaleX = squashX.value,
            charScaleY = squashY.value,
            mergeKey = mergeKey,
            showCard = false
        )
    }
}

@Composable
private fun SpriteConsumedTile(value: Int, from: Offset, to: Offset, sizePx: Float, size: Dp, purge: Boolean) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(if (purge) 380 else 150, easing = FastOutLinearInEasing)) }
    val t = p.value
    val at = if (purge) from else lerp(from, to, t)
    val half = sizePx / 2f
    Box(
        modifier = Modifier
            .offset { IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt()) }
            .size(size)
            .graphicsLayer {
                val s = if (purge) 1f - 0.9f * t else 1f - 0.35f * t
                scaleX = s
                scaleY = s
                alpha = 1f - t
            }
    ) {
        SpriteTile(value = value, modifier = Modifier.fillMaxSize(), emotion = SpriteEmotion.HAPPY, showCard = false)
    }
}

/** Fading copies chasing the shot, a glow streak along the lane, and sparks where it hits. */
@Composable
private fun ShotEffects(value: Int, from: Offset, to: Offset, sizePx: Float, size: Dp) {
    val glow = ElementalSprites.specFor(value).glow
    val ghosts = listOf(0.42f, 0.26f, 0.14f).mapIndexed { k, op -> Triple(k, op, remember { Animatable(0f) }) }
    val streak = remember { Animatable(0f) }
    val sparks = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        ghosts.forEach { (k, _, a) -> launch { delay(30L * (k + 1)); a.animateTo(1f, tween(FLIGHT_MS + 70, easing = FastOutSlowInEasing)) } }
        launch { streak.animateTo(1f, tween(FLIGHT_MS + 160)) }
        launch { delay(FLIGHT_MS.toLong()); sparks.animateTo(1f, tween(360)) }
    }
    val half = sizePx / 2f
    ghosts.forEach { (k, op, a) ->
        val t = a.value
        if (t < 1f) {
            val at = lerp(from, to, t * (0.82f - k * 0.14f))
            Box(
                modifier = Modifier
                    .offset { IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt()) }
                    .size(size)
                    .graphicsLayer {
                        alpha = op * (1f - t)
                        scaleX = 0.92f
                        scaleY = 1.12f
                    }
            ) {
                SpriteTile(value = value, modifier = Modifier.fillMaxSize(), showCard = false, showNumber = false)
            }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val s = streak.value
        if (s < 1f) {
            val reach = (s / 0.5f).coerceAtMost(1f)
            val end = lerp(from, to, reach)
            drawLine(
                brush = Brush.verticalGradient(listOf(glow.copy(alpha = 0.85f * (1f - s)), Color.Transparent), startY = end.y, endY = from.y),
                start = from,
                end = end,
                strokeWidth = sizePx * 0.42f,
                cap = StrokeCap.Round
            )
        }
        val sp = sparks.value
        if (sp in 0.001f..0.999f) {
            val contact = Offset(to.x, to.y - sizePx * 0.42f)
            val len = sizePx * 0.32f
            for (i in 0 until 9) {
                val ang = (-PI / 2 + (i / 8f - 0.5f) * PI * 1.1).toFloat()
                val dir = Offset(kotlin.math.cos(ang), kotlin.math.sin(ang))
                val start = contact + dir * (len * 0.9f * sp)
                val endPt = contact + dir * (len * (0.4f + 1.1f * sp))
                drawLine(
                    color = Color(0xFFFFE07A).copy(alpha = 1f - sp),
                    start = start,
                    end = endPt,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun Launcher(
    value: Int,
    reloadKey: Int,
    center: Offset,
    sizePx: Float,
    size: Dp,
    hidden: Boolean,
    blocked: Boolean,
    recoil: Float,
    phase: Float,
    modifier: Modifier
) {
    val reload = remember { Animatable(1f) }
    LaunchedEffect(reloadKey) {
        reload.snapTo(0f)
        reload.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    val r = reload.value
    val rise = (1f - r) * 0.6f
    val bob = sin(phase * 2f * PI.toFloat()) * 3f
    val shake = if (blocked) sin(phase * 2f * PI.toFloat() * 6f) * 3f else 0f
    val glow = if (blocked) BeamBlocked else ElementalSprites.specFor(value).glow
    val half = sizePx / 2f
    Box(
        modifier = Modifier
            .offset { IntOffset((center.x - half + shake).roundToInt(), (center.y - half + bob + rise * sizePx).roundToInt()) }
            .then(modifier)
            .size(size)
            .graphicsLayer {
                alpha = if (hidden) 0f else (0.2f + 0.8f * r)
                val pop = if (r < 0.6f) 0.5f + r / 0.6f * 0.62f else 1.12f - (r - 0.6f) / 0.4f * 0.12f
                scaleX = pop
                scaleY = pop
            }
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), Color.Transparent)),
                    radius = this.size.minDimension * 0.68f
                )
            }
    ) {
        SpriteTile(
            value = value,
            modifier = Modifier.fillMaxSize(),
            emotion = if (blocked) SpriteEmotion.SURPRISED else SpriteEmotion.EXCITED,
            charScaleX = 1f + 0.18f * recoil,
            charScaleY = 1f - 0.22f * recoil,
            showCard = false
        )
    }
}
