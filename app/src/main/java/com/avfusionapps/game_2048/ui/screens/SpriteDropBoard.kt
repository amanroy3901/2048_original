package com.avfusionapps.game_2048.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
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
import com.avfusionapps.game_2048.model.DropStep
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.ui.sprites.ConfettiBurst
import com.avfusionapps.game_2048.ui.sprites.ElementalSprites
import com.avfusionapps.game_2048.ui.sprites.ParticleBurst
import com.avfusionapps.game_2048.ui.sprites.SparkBurst
import com.avfusionapps.game_2048.ui.sprites.SpriteBanner
import com.avfusionapps.game_2048.ui.sprites.SpriteDir
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.ui.sprites.SpriteFloatText
import com.avfusionapps.game_2048.ui.sprites.SpriteMotion
import com.avfusionapps.game_2048.ui.sprites.SpriteMotionKind
import com.avfusionapps.game_2048.ui.sprites.SpriteParticle
import com.avfusionapps.game_2048.ui.sprites.SpriteTile
import com.avfusionapps.game_2048.ui.sprites.drawGlowStreak
import com.avfusionapps.game_2048.ui.sprites.drawSparks
import com.avfusionapps.game_2048.viewmodel.DropMergeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val COLS = DropMergeConfig.COLUMNS
private const val ROWS = DropMergeConfig.ROWS

/** The launcher sprite is a touch bigger than the board sprites; it shrinks to board size in flight. */
private const val LAUNCH_SCALE = 1.12f
/** A merge: the eaten sprites fly into the survivor for 155 ms, then it evolves. */
private const val FUSE_MS = 155

private val BoardGold = Color(0xFFFFC45A)
private val LaneTrack = Color(0x8C140834)
private val LaneAimed = Color(0x80462882)
private val LaneAimedBlocked = Color(0x735A1432)
private val BeamGold = Color(0xFFFFC04A)
private val BeamOrange = Color(0xFFFF9A2E)
private val BeamBlocked = Color(0xFFFF4D6D)

private val FlightEasing = CubicBezierEasing(0.2f, 0.85f, 0.3f, 1f)
private val StackEasing = CubicBezierEasing(0.3f, 0.85f, 0.35f, 1.15f)
private val EatEasing = CubicBezierEasing(0.5f, 0f, 0.8f, 0.4f)
private val GhostEasing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

private val arrowPath by lazy { PathParser().parsePathString("M12 3.5 L20.5 12.5 H15.2 V20.5 H8.8 V12.5 H3.5 Z").toPath() }
private val chevronPath by lazy { PathParser().parsePathString("M3 11 L13 3 L23 11").toPath() }

private var shooterMotionCounter = 0
private fun nextKey() = ++shooterMotionCounter

private fun cycle(t: Float, period: Float, delay: Float = 0f): Float {
    val x = (t - delay) / period
    return x - floor(x)
}

/** Ease-in-out triangle: 0 → 1 → 0 over one cycle, like a CSS 0%/50%/100% loop. */
private fun pingPong(c: Float): Float = EaseInOut.transform(if (c < 0.5f) c * 2f else (1f - c) * 2f)

/**
 * The Elemental Sprites Neon Shooter board: lanes with launch pads at their foot, a warm light cone that
 * rises from the launcher to the landing slot, and sprite tiles that fly, collide, fuse and react.
 * Same rules and view model as the classic board; only the presentation (and its pacing) differs.
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

    // The sprite board plays the prototype's slower, showier timing.
    LaunchedEffect(Unit) { viewModel.spritePacing = true }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { viewModel.spritePacing = false } }

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
        val boardWPx = with(density) { boardW.toPx() }
        val boardHPx = with(density) { boardH.toPx() }

        fun laneX(c: Int) = gapPx + c * (lanePx + gapPx)
        fun cellCenter(c: Int, r: Int) = Offset(laneX(c) + lanePx / 2f, gapPx + r * cellHPx + cellHPx / 2f)
        fun launchCenter(c: Int) = Offset(laneX(c) + lanePx / 2f, padsTop + padsHPx / 2f - padsHPx * 0.08f)
        fun colFromX(x: Float) = ((x - gapPx / 2f) / (lanePx + gapPx)).toInt().coerceIn(0, COLS - 1)
        fun rowFromY(y: Float) = ((y - gapPx) / cellHPx).toInt()
        /** The prototype's flight: 110 ms plus a little per pixel climbed, 130–220 ms. */
        fun flightMs(from: Offset, to: Offset): Int = (110f + (from.y - to.y) / density.density * 0.18f).roundToInt().coerceIn(130, 220)
        val targeting = gameState.removeArmed

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

        val clockT = rememberInfiniteTransition(label = "shooterClock")
        val clock by clockT.animateFloat(0f, 3600f, infiniteRepeatable(tween(3_600_000, easing = LinearEasing)), label = "clock")

        // Board shake (game over, a hammer hit) and the little bump when a shot lands.
        val shake = remember { Animatable(1f) }
        val bump = remember { Animatable(1f) }
        fun shakeBoard() = scope.launch { shake.snapTo(0f); shake.animateTo(1f, tween(450, easing = LinearEasing)) }
        var wasOver by remember { mutableStateOf(gameState.isGameOver) }
        LaunchedEffect(gameState.isGameOver) {
            if (gameState.isGameOver && !wasOver) shakeBoard()
            wasOver = gameState.isGameOver
        }
        LaunchedEffect(gameState.removingTileId) {
            if (gameState.removingTileId == null) return@LaunchedEffect
            delay(340)
            shakeBoard()
        }

        val step = gameState.lastStep
        // The shot landing: bump the board and squash the sprite it hit.
        var impactTarget by remember { mutableStateOf<Pair<Long, Int>?>(null) }
        LaunchedEffect(step) {
            if (step?.kind != DropStepKind.PLACE) return@LaunchedEffect
            val col = step.columns[lastShotCol]
            val placed = col.lastOrNull() ?: return@LaunchedEffect
            val ms = flightMs(launchCenter(lastShotCol), cellCenter(lastShotCol, col.lastIndex))
            delay(ms.toLong())
            bump.snapTo(0f)
            launch { bump.animateTo(1f, tween(140, easing = LinearEasing)) }
            col.getOrNull(col.lastIndex - 1)?.let { impactTarget = it.id to nextKey() }
        }

        // Freeze: ice shards burst up out of the launcher.
        var freezeBurst by remember { mutableIntStateOf(0) }
        var wasFrozen by remember { mutableStateOf(gameState.freezeShots > 0) }
        LaunchedEffect(gameState.freezeShots) {
            if (gameState.freezeShots > 0 && !wasFrozen) freezeBurst++
            wasFrozen = gameState.freezeShots > 0
        }

        val shakePx = with(density) { 1.dp.toPx() }
        Box(
            modifier = Modifier
                .size(boardW, boardH)
                .graphicsLayer {
                    val s = shake.value
                    // bShake: -8, 7, -5, 3, 0 px over 450 ms
                    translationX = if (s < 1f) kfLinear(EaseInOut.transform(s), SHAKE_AT, SHAKE_X) * shakePx else 0f
                    translationY = if (bump.value < 1f) -3f * shakePx * sin(PI.toFloat() * bump.value) else 0f
                }
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
                .pointerInput(inputEnabled, targeting, gameState.columns) {
                    detectTapGestures { offset ->
                        if (targeting) {
                            // Remove is armed: hit the sprite under the finger (a tap just below a lane's end counts too).
                            val column = gameState.columns[colFromX(offset.x)]
                            val row = rowFromY(offset.y)
                            (column.getOrNull(row) ?: column.lastOrNull()?.takeIf { row == column.size })
                                ?.let { viewModel.removeTile(it.id) }
                        } else if (inputEnabled) {
                            fire(colFromX(offset.x))
                        }
                    }
                }
                .pointerInput(inputEnabled, targeting) {
                    if (targeting) return@pointerInput
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
                    critical = fill >= ROWS,
                    aimed = c == launcherCol && !gameState.isGameOver && !targeting,
                    aimedBlocked = c == launcherCol && blocked
                )
            }

            // ── Light cone from the launcher up to the landing slot ──
            if (!gameState.isGameOver && !targeting) {
                val landRow = min(aimedColumn.size, ROWS - 1)
                val coneTop = if (aimedFull) gapPx else cellCenter(launcherCol, landRow).y - cellHPx / 2f
                val coneBottom = launchCenter(launcherCol).y
                Canvas(Modifier.fillMaxSize()) {
                    drawLightCone(aimX, lanePx, coneTop, coneBottom, blocked, clock)
                }
            }

            // ── Launch pads (the aimed one is covered by the launcher) ──
            val launcherOver = ((aimX - gapPx) / (lanePx + gapPx)).roundToInt().coerceIn(0, COLS - 1)
            for (c in 0 until COLS) {
                if (c == launcherOver && !(gameState.isResolving && !gameState.powerResolving)) continue
                val padSize = with(density) { (padsHPx * 0.86f).toDp() }
                val center = Offset(laneX(c) + lanePx / 2f, padsTop + padsHPx / 2f)
                LaunchPad(center = center, size = padSize, blocked = gameState.columns[c].let { it.size >= ROWS && it.last().value != gameState.currentValue }, modifier = Modifier.testTag("DropMerge_LauncherSlot_$c"))
            }

            // ── Chevrons above the launcher ──
            if (!blocked && !gameState.isGameOver && !targeting) {
                val lc = launchCenter(launcherCol)
                val chevTopPx = lc.y - tileSizePx * LAUNCH_SCALE * 0.5f - with(density) { 40.dp.toPx() }
                val landBottom = cellCenter(launcherCol, min(aimedColumn.size, ROWS - 1)).y + cellHPx * 0.4f
                if (chevTopPx > landBottom) {
                    Canvas(Modifier.fillMaxSize()) { drawChevrons(Offset(aimX + lanePx / 2f, chevTopPx), clock) }
                }
            }

            // ── Live sprites, keyed by id so moves and merges animate ──
            val mergeKeyBase = gameState.actionCount * 32
            val fuseTargetId = if (step?.kind == DropStepKind.MERGE) step.mergedTileId else null
            gameState.columns.forEachIndexed { c, column ->
                column.forEachIndexed { r, tile ->
                    key(tile.id) {
                        val isEnd = r == column.lastIndex
                        val ready = isEnd && tile.value == gameState.currentValue && !gameState.isResolving && !gameState.isGameOver
                        val emotion = when {
                            gameState.removingTileId == tile.id -> SpriteEmotion.SURPRISED
                            targeting -> SpriteEmotion.WORRIED
                            gameState.isGameOver -> SpriteEmotion.SAD
                            idleLevel >= 2 -> SpriteEmotion.SLEEPY
                            idleLevel == 1 -> SpriteEmotion.THINKING
                            ready -> SpriteEmotion.EXCITED
                            column.size >= ROWS - DropMergeConfig.DANGER_FREE_CELLS -> SpriteEmotion.WORRIED
                            else -> SpriteEmotion.IDLE
                        }
                        val merged = step?.kind == DropStepKind.MERGE && step.mergedTileId == tile.id
                        val mult = if (merged) 1 shl step!!.consumed.size else 1
                        val target = cellCenter(c, r)
                        SpriteBoardTile(
                            value = tile.value,
                            target = target,
                            launch = launchCenter(c),
                            flightMs = flightMs(launchCenter(c), target),
                            sizePx = tileSizePx,
                            size = tileSize,
                            fromLauncher = !gameState.justUndid,
                            emotion = emotion,
                            mergeReady = ready,
                            mergeKey = if (merged) mergeKeyBase + step!!.chainIndex else null,
                            powered = merged && mult >= 4,
                            fusing = tile.id == fuseTargetId,
                            wobble = targeting,
                            hit = gameState.removingTileId == tile.id,
                            spinKey = gameState.shuffleKey,
                            impactKey = impactTarget?.takeIf { it.first == tile.id }?.second,
                            clock = { clock }
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
                        val from = cellCenter(consumed.fromCol, consumed.fromRow)
                        val to = fuseTo ?: cellCenter(consumed.toCol, consumed.toRow)
                        SpriteConsumedTile(
                            value = consumed.tile.value,
                            from = from,
                            to = to,
                            sizePx = tileSizePx,
                            size = tileSize,
                            purge = step.kind != DropStepKind.MERGE,
                            sameRow = kotlin.math.abs(from.y - to.y) < 1f
                        )
                    }
                }
            }

            // ── The shot itself: afterimages, glow streak and impact sparks ──
            if (step?.kind == DropStepKind.PLACE) {
                val placedCol = lastShotCol
                val placed = step.columns[placedCol].lastOrNull()
                if (placed != null) {
                    key("shot-${gameState.moveCount}-${placed.id}") {
                        val from = launchCenter(placedCol)
                        val to = cellCenter(placedCol, step.columns[placedCol].lastIndex)
                        ShotEffects(value = placed.value, from = from, to = to, flightMs = flightMs(from, to), sizePx = tileSizePx, size = tileSize)
                    }
                }
            }

            // ── Merge and purge effects: sparks, points, combo, banners ──
            if (step != null) key(step) { StepEffects(step, fuseTargetCenter = step.mergedTileId?.let { id ->
                step.columns.forEachIndexed { c, col -> col.forEachIndexed { r, t -> if (t.id == id) return@let cellCenter(c, r) } }
                null
            }, consumedCenters = step.consumed.map { cellCenter(it.fromCol, it.fromRow) }, tileSizePx = tileSizePx, boardW = boardWPx, boardH = boardHPx) }

            // ── The Remove hammer swinging down on its target ──
            gameState.removingTileId?.let { id ->
                gameState.columns.forEachIndexed { c, col ->
                    col.forEachIndexed { r, t ->
                        if (t.id == id) key("hammer-$id") { HammerSwing(cellCenter(c, r), tileSizePx) }
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
                hidden = gameState.isResolving && !gameState.powerResolving,
                blocked = blocked && !targeting,
                recoil = recoil.value,
                clock = { clock },
                frozenShots = gameState.freezeShots,
                dimmed = targeting,
                modifier = Modifier.tutorialAnchor(TutorialAnchor.LAUNCHER)
            )
            if (freezeBurst > 0) key(freezeBurst) {
                ParticleBurst(at = Offset(aimX + lanePx / 2f, lc.y - with(density) { 20.dp.toPx() }), kind = SpriteParticle.SHARD, count = 14, spread = with(density) { 70.dp.toPx() }, up = true)
            }
        }
    }
}

private val SHAKE_AT = floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
private val SHAKE_X = floatArrayOf(0f, -8f, 7f, -5f, 3f, 0f)

private fun kfLinear(p: Float, at: FloatArray, v: FloatArray): Float {
    if (p <= at[0]) return v[0]
    for (k in 0 until at.size - 1) if (p <= at[k + 1]) return v[k] + (v[k + 1] - v[k]) * (p - at[k]) / (at[k + 1] - at[k])
    return v.last()
}

/** What a merge or purge step shows on top of the board, timed like the prototype. */
@Composable
private fun BoxScope.StepEffects(
    step: DropStep,
    fuseTargetCenter: Offset?,
    consumedCenters: List<Offset>,
    tileSizePx: Float,
    boardW: Float,
    boardH: Float
) {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(2000f, tween(2000, easing = LinearEasing)) }
    when (step.kind) {
        DropStepKind.MERGE -> {
            val at = fuseTargetCenter ?: return
            val mult = 1 shl step.consumed.size
            SparkBurst(at = at, count = 7, length = tileSizePx * 0.3f, up = false, delayMs = FUSE_MS)
            SpriteFloatText(
                text = "+%,d".format(step.gained) + if (mult >= 4) "  ×$mult" else "",
                at = Offset(at.x, at.y - tileSizePx * 0.25f),
                clock = clock, startMs = FUSE_MS.toFloat(),
                durationMs = if (mult >= 4) 1200f else 900f, rise = 60f,
                color = if (mult >= 4) Color(0xFFFFC93A) else Color.White,
                fontSize = if (mult >= 4) 30.sp else 20.sp,
                display = mult >= 4
            )
            if (step.chainIndex >= 2) {
                SpriteFloatText(
                    text = "COMBO ×${step.chainIndex}",
                    at = Offset(boardW / 2f, boardH * 0.2f),
                    clock = clock, startMs = FUSE_MS.toFloat(), durationMs = 1200f, rise = 90f,
                    color = Color(0xFFFFC93A), fontSize = 32.sp, display = true
                )
            }
            step.unlockedValue?.let { v ->
                SpriteBanner("Unlocked $v · ${ElementalSprites.specFor(v).name}!")
                ConfettiBurst(40)
            }
        }
        DropStepKind.PURGE -> {
            val biggest = step.consumed.maxOfOrNull { it.tile.value } ?: 0
            SpriteBanner("Purge! All ${biggest}s cleared")
            consumedCenters.forEach { c -> ParticleBurst(at = c, kind = SpriteParticle.STAR, count = 6, spread = tileSizePx * 0.8f) }
        }
        else -> Unit
    }
}

@Composable
private fun LaneTrack(x: Float, widthDp: Dp, heightDp: Dp, topPx: Float, danger: Boolean, critical: Boolean, aimed: Boolean, aimedBlocked: Boolean) {
    val pulse = rememberInfiniteTransition(label = "laneDanger")
    val a by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(if (critical) 550 else 1000, easing = LinearEasing)), label = "laneDangerA")
    val fill = when {
        aimedBlocked -> LaneAimedBlocked
        aimed -> LaneAimed
        else -> LaneTrack
    }
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), topPx.roundToInt()) }
            .size(widthDp, heightDp)
            .drawBehind {
                if (danger) {
                    val k = pingPong(a)
                    val r = CornerRadius(16.dp.toPx())
                    drawRoundRect(BeamBlocked.copy(alpha = 0.45f * k), topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()), size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), cornerRadius = CornerRadius(20.dp.toPx()), style = Stroke(6.dp.toPx() * k + 0.5f))
                    drawRoundRect(fill, cornerRadius = r)
                    drawRoundRect(BeamBlocked.copy(alpha = 0.25f + 0.65f * k), cornerRadius = r, style = Stroke((1f + k) * 1.dp.toPx()))
                } else {
                    drawRoundRect(fill, cornerRadius = CornerRadius(16.dp.toPx()))
                    drawRoundRect(Color.White.copy(alpha = 0.05f), cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(1.dp.toPx()))
                }
            }
    )
}

/**
 * The prototype's beam: a trapezoid of warm light (narrow at the launcher, the lane's full width at the landing
 * slot), feathered at the sides, with faint bands drifting up and a bright pulsing glow at its base.
 */
private fun DrawScope.drawLightCone(laneLeft: Float, laneW: Float, top: Float, bottom: Float, blocked: Boolean, t: Float) {
    if (bottom <= top) return
    val b1 = if (blocked) BeamBlocked.copy(alpha = 0.72f) else BeamGold.copy(alpha = 0.72f)
    val b2 = if (blocked) BeamBlocked.copy(alpha = 0.26f) else BeamOrange.copy(alpha = 0.26f)
    val b3 = if (blocked) BeamBlocked.copy(alpha = 0.03f) else BeamOrange.copy(alpha = 0.03f)
    val h = bottom - top
    val path = Path().apply {
        moveTo(laneLeft + laneW * 0.24f, bottom)
        lineTo(laneLeft + laneW * 0.76f, bottom)
        lineTo(laneLeft + laneW, top)
        lineTo(laneLeft, top)
        close()
    }
    // vertical light × a side feather, in one shader pass (no offscreen layer)
    val light = androidx.compose.ui.graphics.LinearGradientShader(Offset(0f, top), Offset(0f, bottom), listOf(b3, b2, b1), listOf(0f, 0.5f, 1f))
    val feather = androidx.compose.ui.graphics.LinearGradientShader(
        Offset(laneLeft, 0f), Offset(laneLeft + laneW, 0f),
        listOf(Color.Transparent, Color.Black, Color.Black, Color.Transparent), listOf(0f, 0.24f, 0.76f, 1f)
    )
    drawPath(path, androidx.compose.ui.graphics.ShaderBrush(android.graphics.ComposeShader(light, feather, android.graphics.PorterDuff.Mode.DST_IN)))
    clipPath(path) {
        // bands of light rising 70 px every 1.2 s
        val band = 70.dp.toPx()
        var y = bottom - cycle(t, 1.2f) * band
        while (y > top - band) {
            drawRect(Color(0xFFFFECB4).copy(alpha = 0.035f), topLeft = Offset(laneLeft, y - 44.dp.toPx()), size = Size(laneW, 10.dp.toPx()))
            y -= band
        }
    }
    // the bright cone at its foot (pulses 1.4 s)
    val pulse = 0.55f + 0.45f * pingPong(cycle(t, 1.4f))
    val cw = laneW * 0.4f
    val ch = h * 0.34f
    val cTop = bottom + h * 0.06f - ch
    drawOval(
        brush = Brush.radialGradient(
            0f to Color(0xFFFFFADC).copy(alpha = 0.95f * pulse), 0.45f to Color(0xFFFFE696).copy(alpha = 0.35f * pulse), 0.72f to Color.Transparent,
            center = Offset(laneLeft + laneW / 2f, cTop + ch), radius = ch
        ),
        topLeft = Offset(laneLeft + laneW * 0.3f, cTop),
        size = Size(cw, ch)
    )
}

/** Three chevrons climbing toward the lane: the bottom one lights first (1 s loop). */
private fun DrawScope.drawChevrons(top: Offset, t: Float) {
    val w = 26.dp.toPx()
    val h = 13.dp.toPx()
    for (k in 0 until 3) {
        val c = pingPong(cycle(t, 1f, (2 - k) * 0.15f))
        val a = 0.25f + 0.75f * c
        val dy = (4f - 6f * c) * 1.dp.toPx()
        val y = top.y + k * h + dy
        withTransformTo(Offset(top.x - w / 2f, y), w / 26f) {
            drawPath(chevronPath, Color(0xFFFFD25A).copy(alpha = a), style = Stroke(width = 3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private inline fun DrawScope.withTransformTo(at: Offset, s: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.translate(at.x, at.y)
    drawContext.transform.scale(s, s, Offset.Zero)
    block()
    drawContext.transform.scale(1f / s, 1f / s, Offset.Zero)
    drawContext.transform.translate(-at.x, -at.y)
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
            .drawBehind {
                // inset 0 2px 0 white/.12 on top, inset 0 -4px 0 black/.25 at the bottom
                drawRect(Color.White.copy(alpha = 0.12f), size = Size(this.size.width, 2.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(0f, this.size.height - 4.dp.toPx()), size = Size(this.size.width, 4.dp.toPx()))
            }
            .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.62f)) {
            val s = this.size.width / 24f
            scale(s, s, pivot = Offset.Zero) { drawPath(arrowPath, Color.White.copy(alpha = 0.78f)) }
        }
    }
}

/**
 * A sprite on the board: flies in from the launcher, collides, then swings to new rows as the stack shifts.
 * Merges evolve it (with a pink chain glow, and a gold ring for ×4 and bigger); the hammer knocks it out.
 */
@Composable
private fun SpriteBoardTile(
    value: Int,
    target: Offset,
    launch: Offset,
    flightMs: Int,
    sizePx: Float,
    size: Dp,
    fromLauncher: Boolean,
    emotion: SpriteEmotion,
    mergeReady: Boolean,
    mergeKey: Int?,
    powered: Boolean,
    fusing: Boolean,
    wobble: Boolean = false,
    hit: Boolean = false,
    spinKey: Int = 0,
    impactKey: Int? = null,
    clock: () -> Float
) {
    // Timers that switch a look back off run in this scope, so a new step can't cancel them half-way.
    val tileScope = rememberCoroutineScope()
    val pos = remember { Animatable(if (fromLauncher) launch else target, Offset.VectorConverter) }
    val flight = remember { Animatable(if (fromLauncher) 0f else 1f) }
    var landed by remember { mutableStateOf(!fromLauncher) }
    var surprised by remember { mutableStateOf(false) }
    var motion by remember { mutableStateOf<SpriteMotion?>(null) }

    LaunchedEffect(target) {
        if (!landed) {
            launch { flight.animateTo(1f, tween(flightMs, easing = LinearEasing)) }
            pos.animateTo(target, tween(flightMs, easing = FlightEasing))
            landed = true
            // Impact: squashed against what it hit, eyes wide.
            surprised = true
            motion = SpriteMotion(SpriteMotionKind.LAND, nextKey(), SpriteDir.UP)
            tileScope.launch { delay(650); surprised = false }
        } else {
            pos.animateTo(target, tween(if (fusing) 150 else 220, easing = if (fusing) EatEasing else StackEasing))
        }
    }
    // The shot hit this sprite from below: it squashes down onto its feet.
    LaunchedEffect(impactKey) {
        if (impactKey != null) motion = SpriteMotion(SpriteMotionKind.LAND, nextKey(), SpriteDir.DOWN)
    }
    // Shuffle: a full spin while it swings to its new slot, looking dizzy.
    var dizzy by remember { mutableStateOf(false) }
    val firstSpinKey = remember { spinKey }
    LaunchedEffect(spinKey) {
        if (spinKey == firstSpinKey) return@LaunchedEffect
        dizzy = true
        motion = SpriteMotion(SpriteMotionKind.SPIN, nextKey())
        tileScope.launch { delay(900); dizzy = false }
    }
    // After a merge: a pink chain glow for 0.9 s, and a spinning gold ring for big (×4+) merges.
    var chain by remember { mutableStateOf(false) }
    val ring = remember { Animatable(0f) }
    LaunchedEffect(mergeKey) {
        if (mergeKey == null) return@LaunchedEffect
        val big = powered
        tileScope.launch {
            delay(FUSE_MS.toLong())
            chain = true
            if (big) launch { ring.snapTo(0f); ring.animateTo(1f, tween(2600, easing = LinearEasing)) }
            delay(900)
            chain = false
        }
    }

    val fp = flight.value
    // In flight: shrinking from launcher size, stretched tall, then a squash on landing.
    val flightScale = if (fp < 1f) LAUNCH_SCALE + (1f - LAUNCH_SCALE) * EaseOut.transform(fp) else 1f
    val sqx = if (fp < 1f) kfLinear(fp, FLIGHT_AT, FLIGHT_X) else 1f
    val sqy = if (fp < 1f) kfLinear(fp, FLIGHT_AT, FLIGHT_Y) else 1f
    val spec = ElementalSprites.specFor(value)

    val half = sizePx / 2f
    Box(
        modifier = Modifier
            .offset { IntOffset((pos.value.x - half).roundToInt(), (pos.value.y - half).roundToInt()) }
            .size(size)
            .graphicsLayer {
                scaleX = flightScale
                scaleY = flightScale
            }
            .drawBehind {
                val t = clock()
                if (mergeReady) {
                    // readyGlow: pulse .35 ↔ 1 and .9 ↔ 1.1 every second
                    val k = pingPong(cycle(t, 1f))
                    val r = this.size.minDimension * 0.55f * (0.9f + 0.2f * k)
                    drawCircle(Brush.radialGradient(listOf(spec.glow.copy(alpha = 0.6f * (0.35f + 0.65f * k)), Color.Transparent), radius = r), radius = r)
                }
                val rp = ring.value
                if (rp in 0.001f..0.999f) {
                    val pulse = 1f + 0.06f * sin(PI.toFloat() * ((rp * 2600f / 1400f) % 1f))
                    val r = this.size.minDimension * 0.62f * pulse
                    val fade = if (rp > 0.9f) (1f - rp) / 0.1f else 1f
                    drawCircle(Color(0xFFFFC83C).copy(alpha = 0.35f * fade), radius = r + 6.dp.toPx(), style = Stroke(10.dp.toPx()))
                    drawCircle(Color(0xFFFFD65A).copy(alpha = 0.85f * fade), radius = r, style = Stroke(3.dp.toPx()))
                }
            }
    ) {
        SpriteTile(
            value = value,
            modifier = Modifier.fillMaxSize(),
            emotion = when {
                dizzy -> SpriteEmotion.DIZZY
                surprised -> SpriteEmotion.SURPRISED
                else -> emotion
            },
            lookY = if (mergeReady) 2f else 0f,
            charScaleX = sqx,
            charScaleY = sqy,
            mergeKey = mergeKey,
            mergeDelayMs = FUSE_MS,
            motion = motion,
            breaking = hit,
            breakHitMs = 340,
            wingsFrom = 1024,
            chainGlow = chain,
            wantHitDeg = if (wobble && !hit) 4f else 0f,
            showCard = false
        )
    }
}

private val FLIGHT_AT = floatArrayOf(0f, 0.7f, 1f)
private val FLIGHT_X = floatArrayOf(0.82f, 0.86f, 1f)
private val FLIGHT_Y = floatArrayOf(1.25f, 1.2f, 1f)

/** A sprite eaten by a merge (it dives into the survivor) or cleared by a purge (it crumbles where it is). */
@Composable
private fun SpriteConsumedTile(value: Int, from: Offset, to: Offset, sizePx: Float, size: Dp, purge: Boolean, sameRow: Boolean) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(150, easing = LinearEasing)) }
    val streak = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!purge) streak.animateTo(1f, tween(320, easing = LinearEasing)) }
    val glow = ElementalSprites.specFor(value).glow
    val motion = remember { if (purge) null else SpriteMotion(SpriteMotionKind.STRETCH, nextKey(), if (sameRow) SpriteDir.LEFT else SpriteDir.UP, durationMs = 150) }
    if (!purge) {
        Canvas(Modifier.fillMaxSize()) { drawGlowStreak(from, to, sizePx * 0.8f * 0.42f, glow.copy(alpha = 0.8f), streak.value) }
    }
    val half = sizePx / 2f
    Box(
        modifier = Modifier
            .offset {
                val at = if (purge) from else lerp(from, to, EatEasing.transform(p.value))
                IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt())
            }
            .size(size)
            .graphicsLayer { if (!purge) alpha = 1f - EaseIn.transform(p.value) }
    ) {
        SpriteTile(
            value = value,
            modifier = Modifier.fillMaxSize(),
            emotion = SpriteEmotion.HAPPY,
            showCard = false,
            motion = motion,
            breaking = purge,
            breakHitMs = 0,
            showEffects = false
        )
    }
}

/** Fading copies chasing the shot, a glow streak along the lane, and sparks where it hits. */
@Composable
private fun ShotEffects(value: Int, from: Offset, to: Offset, flightMs: Int, sizePx: Float, size: Dp) {
    val glow = ElementalSprites.specFor(value).glow
    val ghosts = listOf(0.42f, 0.26f, 0.14f).mapIndexed { k, op -> Triple(k, op, remember { Animatable(0f) }) }
    val streak = remember { Animatable(0f) }
    val sparks = remember { Animatable(0f) }
    val seed = remember { Random.nextInt() }
    LaunchedEffect(Unit) {
        ghosts.forEach { (k, _, a) -> launch { delay(30L * (k + 1)); a.animateTo(1f, tween(flightMs + 70, easing = LinearEasing)) } }
        launch { streak.animateTo(1f, tween(320, easing = LinearEasing)) }
        launch { delay(flightMs.toLong()); sparks.animateTo(1f, tween(360, easing = LinearEasing)) }
    }
    val half = sizePx / 2f
    Canvas(Modifier.fillMaxSize()) { drawGlowStreak(from, to, sizePx * 0.42f, glow.copy(alpha = 0.85f), streak.value) }
    ghosts.forEach { (k, op, a) ->
        val t = a.value
        if (t < 1f) {
            Box(
                modifier = Modifier
                    .offset {
                        val at = lerp(from, to, GhostEasing.transform(t) * (0.82f - k * 0.14f))
                        IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt())
                    }
                    .size(size)
                    .graphicsLayer {
                        val e = GhostEasing.transform(a.value)
                        alpha = op * (1f - e)
                        scaleX = 1f - 0.08f * e
                        scaleY = 1f + 0.12f * e
                    }
            ) {
                SpriteTile(value = value, modifier = Modifier.fillMaxSize(), showCard = false, showAura = false, showEffects = false)
            }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        drawSparks(Offset(to.x, to.y - sizePx * 0.42f), 9, sizePx * 0.32f, sparks.value, up = true, seed = seed)
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
    clock: () -> Float,
    frozenShots: Int,
    dimmed: Boolean,
    modifier: Modifier
) {
    val frozen = frozenShots > 0
    val reload = remember { Animatable(1f) }
    LaunchedEffect(reloadKey) {
        reload.snapTo(0f)
        reload.animateTo(1f, tween(320, easing = LinearEasing))
    }
    val glow = if (blocked) BeamBlocked else ElementalSprites.specFor(value).glow
    val half = sizePx / 2f
    val density = LocalDensity.current
    val px = density.density
    Box(
        modifier = Modifier
            .offset { IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt()) }
            .then(modifier)
            .size(size)
            .graphicsLayer {
                val t = clock()
                val r = EaseOut.transform(reload.value)
                // reload: up from 60 % below at half size → overshoot 10 % up, squashed (1.12, .92) → settle
                val ty = kfLinear(r, RELOAD_AT, RELOAD_TY)
                val sx = kfLinear(r, RELOAD_AT, RELOAD_SX)
                val sy = kfLinear(r, RELOAD_AT, RELOAD_SY)
                val a = kfLinear(r, RELOAD_AT, RELOAD_A)
                // bob 1.6 s, 4 px; blocked: shake ±4 px every .35 s
                val bob = -4f * px * pingPong(cycle(t, 1.6f))
                val no = if (blocked) kfLinear(EaseInOut.transform(cycle(t, 0.35f)), NO_AT, NO_X) * px else 0f
                translationX = no
                translationY = ty * this.size.height + bob
                scaleX = sx
                scaleY = sy
                alpha = if (hidden) 0f else a * (if (dimmed) 0.45f else 1f)
            }
            .drawBehind {
                // the halo (inset -18 %, pulsing 1.4 s)
                val k = 0.55f + 0.45f * pingPong(cycle(clock(), 1.4f))
                val r = this.size.minDimension * 0.96f
                drawCircle(Brush.radialGradient(0f to glow.copy(alpha = 0.55f * k), 0.66f to Color.Transparent, radius = r), radius = r)
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
        if (frozen) {
            // The ice frame from the design: a frosted, glowing rounded square around the sprite.
            Canvas(Modifier.fillMaxSize()) {
                val inset = -this.size.width * 0.04f
                val w = this.size.width - 2 * inset
                val rr = CornerRadius(w * 0.22f)
                val tl = Offset(inset, inset)
                val sz = Size(w, w)
                drawRoundRect(Color(0xFFA0E6FF).copy(alpha = 0.35f), topLeft = tl - Offset(4.dp.toPx(), 4.dp.toPx()), size = Size(w + 8.dp.toPx(), w + 8.dp.toPx()), cornerRadius = CornerRadius(w * 0.26f), style = Stroke(8.dp.toPx()))
                drawRoundRect(Color(0x2EA6ECFF), topLeft = tl, size = sz, cornerRadius = rr)
                // inset 0 0 14px: frost only along the inside of the frame
                drawRoundRect(Brush.radialGradient(0f to Color.Transparent, 0.66f to Color.Transparent, 1f to Color(0x99A0E6FF), center = center, radius = w * 0.72f), topLeft = tl, size = sz, cornerRadius = rr)
                drawRoundRect(Color(0xF2C8F5FF), topLeft = tl, size = sz, cornerRadius = rr, style = Stroke(3.dp.toPx()))
            }
            FrozenBadge(shots = frozenShots, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

private val RELOAD_AT = floatArrayOf(0f, 0.6f, 1f)
private val RELOAD_TY = floatArrayOf(0.6f, -0.1f, 0f)
private val RELOAD_SX = floatArrayOf(0.5f, 1.12f, 1f)
private val RELOAD_SY = floatArrayOf(0.5f, 0.92f, 1f)
private val RELOAD_A = floatArrayOf(0f, 1f, 1f)
private val NO_AT = floatArrayOf(0f, 0.25f, 0.75f, 1f)
private val NO_X = floatArrayOf(0f, -4f, 4f, 0f)

/** The Remove hammer, swung like the prototype: in from the right, slams down at 340 ms, rests, fades (560 ms). */
@Composable
private fun HammerSwing(at: Offset, tileSizePx: Float) {
    val swing = remember { Animatable(0f) }
    LaunchedEffect(Unit) { swing.animateTo(1f, tween(560, easing = LinearEasing)) }
    val density = LocalDensity.current
    val w = tileSizePx * 0.9f
    Canvas(
        modifier = Modifier
            .offset { IntOffset((at.x - w * 0.2f).roundToInt(), (at.y - w * 0.95f).roundToInt()) }
            .size(with(density) { w.toDp() })
            .graphicsLayer {
                val e = EaseIn.transform(swing.value)
                rotationZ = kfLinear(e, HAMMER_AT, HAMMER_R)
                alpha = kfLinear(e, HAMMER_A_AT, HAMMER_A)
                transformOrigin = TransformOrigin(0.85f, 0.85f)
            }
    ) { drawHammerIcon() }
}

private val HAMMER_AT = floatArrayOf(0f, 0.35f, 0.6f, 0.75f, 1f)
private val HAMMER_R = floatArrayOf(50f, 70f, -25f, -15f, -15f)
private val HAMMER_A_AT = floatArrayOf(0f, 0.35f, 0.75f, 1f)
private val HAMMER_A = floatArrayOf(0f, 1f, 1f, 0f)
