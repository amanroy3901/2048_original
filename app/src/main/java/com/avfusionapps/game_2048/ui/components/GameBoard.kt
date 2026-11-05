package com.avfusionapps.game_2048.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.model.TileAnimationInfo
import com.avfusionapps.game_2048.ui.screens.drawHammerIcon
import com.avfusionapps.game_2048.ui.sprites.ElementalSprites
import com.avfusionapps.game_2048.ui.sprites.SpriteDir
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.ui.sprites.SpriteMotion
import com.avfusionapps.game_2048.ui.sprites.SpriteMotionKind
import com.avfusionapps.game_2048.ui.sprites.SpriteParticle
import com.avfusionapps.game_2048.ui.sprites.SpriteFloatText
import com.avfusionapps.game_2048.ui.sprites.SpriteTile
import com.avfusionapps.game_2048.ui.sprites.drawParticle
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.BoardNotice
import com.avfusionapps.game_2048.viewmodel.Direction
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.avfusionapps.game_2048.viewmodel.MoveGain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log2
import kotlin.math.sin
import kotlin.random.Random

/** Slide timing from the prototype: 130 ms, quick out of the gate and settling softly. */
private val SlideEasing = CubicBezierEasing(0.3f, 0.75f, 0.35f, 1f)
/** Shuffle: tiles swing back a touch, then overshoot into their new cells. */
private val ShuffleEasing = CubicBezierEasing(0.5f, -0.2f, 0.4f, 1.3f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private const val SLIDE_MS = 130
private const val SHUFFLE_MS = 460
/** New sprites wait for the slide, then a portal opens (the sprite pops out 160 ms later). */
private const val SPAWN_DELAY_MS = 175
private const val HAMMER_HIT_MS = 340

private val Danger = Color(0xFFFF4D6D)
private val Outline = Color(0xFF2A0F4A)

private var motionCounter = 0
private fun nextMotionKey() = ++motionCounter

@Composable
fun GameBoard(viewModel: GameViewModel) {
    val context = LocalContext.current
    val state = viewModel.gameState
    GameBoard(
        grid = state.grid,
        tileAnimationInfo = state.tileAnimationInfo,
        moveCount = state.moveCount,
        onAnimationsComplete = { viewModel.clearAnimationInfo() },
        isGameOver = state.isGameOver,
        hammerArmed = state.hammerArmed,
        onCellTap = { r, c -> viewModel.hammerAt(r, c, context) },
        breakingCell = state.breakingCell,
        frozen = state.freezeMoves > 0,
        lastGain = state.lastGain,
        rockTtl = state.rockTtl,
        bonkKey = state.bonkKey,
        bonkDirection = state.bonkDirection,
        // The sprite screen reads its notices aloud in Blaze's bubble; only a bonk also flashes on the board.
        notice = if (LocalGameTheme.current.usesSprites) state.notice?.takeIf { it.text.startsWith("Bonk") } else state.notice,
        rewindKey = state.rewindKey
    )
}

/** Remembers, per cell, how many moves a sprite has sat there and how long it has had a twin next to it. */
private class BoardTrack {
    var size = 0
    var lastMove = Int.MIN_VALUE
    var ages = Array(0) { IntArray(0) }
    var pairs = Array(0) { IntArray(0) }

    fun advance(grid: List<List<Int>>, info: Map<Pair<Int, Int>, TileAnimationInfo>, moveCount: Int) {
        val n = grid.size
        if (n != size || moveCount < lastMove) {
            size = n
            ages = Array(n) { IntArray(n) }
            pairs = Array(n) { IntArray(n) }
        } else if (moveCount != lastMove) {
            val newAges = Array(n) { IntArray(n) }
            val newPairs = Array(n) { IntArray(n) }
            for (r in 0 until n) for (c in 0 until n) {
                val v = grid[r][c]
                if (v == 0) continue
                val a = info[r to c]
                if (a?.isNew == true || a?.isMerged == true) continue
                val (sr, sc) = a?.startPosition ?: (r to c)
                newAges[r][c] = (ages.getOrNull(sr)?.getOrNull(sc) ?: 0) + 1
                newPairs[r][c] = if (twinOf(grid, r, c) != null) (pairs.getOrNull(sr)?.getOrNull(sc) ?: 0) + 1 else 0
            }
            for (r in 0 until n) for (c in 0 until n) {
                val a = info[r to c]
                if ((a?.isNew == true || a?.isMerged == true) && twinOf(grid, r, c) != null) newPairs[r][c] = 1
            }
            ages = newAges
            pairs = newPairs
        }
        lastMove = moveCount
    }
}

private fun twinOf(grid: List<List<Int>>, i: Int, j: Int): Pair<Int, Int>? {
    val v = grid[i][j]
    if (v <= 0) return null
    for ((dr, dc) in listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)) {
        if (grid.getOrNull(i + dr)?.getOrNull(j + dc) == v) return dr to dc
    }
    return null
}

@Composable
fun GameBoard(
    grid: List<List<Int>>,
    tileAnimationInfo: Map<Pair<Int, Int>, TileAnimationInfo>,
    moveCount: Int,
    onAnimationsComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isGameOver: Boolean = false,
    hammerArmed: Boolean = false,
    onCellTap: ((Int, Int) -> Unit)? = null,
    breakingCell: Pair<Int, Int>? = null,
    frozen: Boolean = false,
    lastGain: MoveGain? = null,
    rockTtl: Int = 0,
    bonkKey: Int = 0,
    bonkDirection: Direction? = null,
    notice: BoardNotice? = null,
    rewindKey: Int = 0
) {
    val theme = LocalGameTheme.current
    val density = LocalDensity.current
    val sprites = theme.usesSprites

    // Measure cell size once from the board dimensions
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val gridSize = grid.size
    // The sprite board has the prototype's roomier gaps (8 of 360).
    val spacingPx = if (sprites && boardSize.width > 0) boardSize.width * 0.022f else with(density) { 4.dp.toPx() }
    val paddingPx = if (sprites && boardSize.width > 0) boardSize.width * 0.022f else with(density) { 6.dp.toPx() }
    val spacingDp = with(density) { spacingPx.toDp() }
    val paddingDp = with(density) { paddingPx.toDp() }

    val cellSizePx = remember(boardSize, gridSize, spacingPx, paddingPx) {
        if (boardSize.width > 0 && gridSize > 0) {
            val usableWidth = boardSize.width - (2 * paddingPx) - ((gridSize - 1) * spacingPx)
            usableWidth / gridSize
        } else 0f
    }
    val stride = cellSizePx + spacingPx

    // Clear animations after they complete
    LaunchedEffect(moveCount) {
        if (tileAnimationInfo.isNotEmpty() && onAnimationsComplete != null) {
            delay(if (sprites) 700 else 350)
            onAnimationsComplete()
        }
    }

    // Sprites get bored: thinking after 6 s without a move, asleep after 14 s. Waking up startles them.
    var idleLevel by remember { mutableIntStateOf(0) }
    var startled by remember { mutableStateOf(false) }
    // Timers that switch a mood back off must outlive the effect that started them.
    val moodScope = rememberCoroutineScope()
    if (sprites) {
        LaunchedEffect(moveCount) {
            if (idleLevel >= 2) {
                startled = true
                moodScope.launch { delay(600); startled = false }
            }
            idleLevel = 0
            delay(6_000)
            idleLevel = 1
            delay(8_000)
            idleLevel = 2
        }
    }
    val emptyCount = remember(grid) { grid.sumOf { row -> row.count { it == 0 } } }
    val danger = sprites && emptyCount <= 2 && !isGameOver

    val track = remember { BoardTrack() }
    remember(moveCount, grid) { track.advance(grid, tileAnimationInfo, moveCount); 0 }

    // After a move every sprite glances the way it went, for 650 ms.
    val moveDir = remember(moveCount) {
        tileAnimationInfo.entries.firstNotNullOfOrNull { (to, a) ->
            val from = a.startPosition
            if (from == null || from == to || a.shuffled) null else (to.first - from.first).coerceIn(-1, 1) to (to.second - from.second).coerceIn(-1, 1)
        }
    }
    var glancing by remember(moveCount) { mutableStateOf(moveDir != null) }
    LaunchedEffect(moveCount) { delay(650); glancing = false }

    // Bonk: the board knocks toward the wall, every sprite squashes into it and goes dizzy.
    val bonkShake = remember { Animatable(1f) }
    val bonkKeyNow = rememberUpdatedState(bonkKey)
    var dizzy by remember { mutableStateOf(false) }
    val boardMotions = remember { mutableStateMapOf<Pair<Int, Int>, SpriteMotion>() }
    LaunchedEffect(bonkKey) {
        if (bonkKey == 0) return@LaunchedEffect
        dizzy = true
        val dir = bonkDirection?.toSpriteDir()
        if (sprites && dir != null) {
            grid.forEachIndexed { r, row -> row.forEachIndexed { c, v -> if (v != 0) boardMotions[r to c] = SpriteMotion(SpriteMotionKind.BONK, nextMotionKey(), dir) } }
        }
        val myKey = bonkKey
        moodScope.launch { delay(1300); if (myKey == bonkKeyNow.value) dizzy = false }
        bonkShake.snapTo(0f)
        bonkShake.animateTo(1f, tween(260, easing = LinearEasing))
    }
    val knockPx = with(density) { 7.dp.toPx() }
    val bonkVector = when (bonkDirection) {
        Direction.LEFT -> -1f to 0f
        Direction.RIGHT -> 1f to 0f
        Direction.UP -> 0f to -1f
        Direction.DOWN -> 0f to 1f
        else -> 0f to 0f
    }

    // Undo: every sprite fades back in with a little twist.
    LaunchedEffect(rewindKey) {
        if (rewindKey == 0 || !sprites) return@LaunchedEffect
        grid.forEachIndexed { r, row -> row.forEachIndexed { c, v -> if (v != 0) boardMotions[r to c] = SpriteMotion(SpriteMotionKind.REWIND, nextMotionKey()) } }
    }

    // Hammer: the others flinch; the board bumps when it lands.
    val bump = remember { Animatable(1f) }
    var flinch by remember { mutableStateOf(false) }
    LaunchedEffect(breakingCell) {
        if (breakingCell == null || !sprites) return@LaunchedEffect
        flinch = true
        moodScope.launch { delay(800); flinch = false }
        delay(HAMMER_HIT_MS.toLong())
        bump.snapTo(0f)
        bump.animateTo(1f, tween(180, easing = LinearEasing))
    }

    // Freeze: a burst of ice from the middle, and everyone is startled.
    var freezeBurstKey by remember { mutableIntStateOf(0) }
    var chilled by remember { mutableStateOf(false) }
    var wasFrozen by remember { mutableStateOf(frozen) }
    LaunchedEffect(frozen) {
        if (frozen && !wasFrozen && sprites) {
            freezeBurstKey++
            chilled = true
            moodScope.launch { delay(700); chilled = false }
        }
        wasFrozen = frozen
    }

    // Idle fidgets: every 1.9 s one relaxed sprite wiggles, rocks, hops or twirls.
    val relaxedCells = remember { ArrayList<Pair<Int, Int>>() }
    if (sprites) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1900)
                if (idleLevel < 2 && !isGameOver && relaxedCells.isNotEmpty()) {
                    val cell = relaxedCells[Random.nextInt(relaxedCells.size)]
                    boardMotions[cell] = SpriteMotion(SpriteMotionKind.FIDGET, nextMotionKey(), variant = Random.nextInt(4))
                }
            }
        }
    }

    val pokes = remember { mutableStateMapOf<Pair<Int, Int>, Int>() }
    val over by animateFloatAsState(if (isGameOver && sprites) 1f else 0f, tween(600), label = "over")
    val dangerT = rememberInfiniteTransition(label = "danger")
    val dangerP by dangerT.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "dangerP")
    val boardRadius = if (sprites && boardSize.width > 0) with(density) { (boardSize.width * 0.072f).toDp() } else 12.dp
    val boardShape = RoundedCornerShape(boardRadius)

    relaxedCells.clear()
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .graphicsLayer {
                val b = bonkShake.value
                // 7 px toward the wall, 4 px back, settle (260 ms)
                val k = if (b < 1f) {
                    when {
                        b < 1f / 3f -> 7f * (b * 3f)
                        b < 2f / 3f -> 7f - 11f * ((b - 1f / 3f) * 3f)
                        else -> -4f + 4f * ((b - 2f / 3f) * 3f)
                    } / 7f * knockPx
                } else 0f
                translationX = bonkVector.first * k
                translationY = bonkVector.second * k + if (bump.value < 1f) sin(PI.toFloat() * bump.value) * 5.dp.toPx() else 0f
            }
            .onSizeChanged { boardSize = it }
            .then(
                if (sprites) Modifier
                    .drawBehind { drawSpriteBoardGlow(boardRadius.toPx(), danger, dangerP) }
                    .clip(boardShape)
                    .background(Brush.verticalGradient(listOf(Color(0xBF3C1E82), Color(0xD9200E50))))
                    .drawBehind { drawInnerShadow(boardRadius.toPx(), 30.dp.toPx(), Color.Black.copy(alpha = 0.35f)) }
                    .border(2.dp, Color(0x66BE96FF), boardShape)
                else Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.surfaceColor)
                    .border(2.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
            .padding(paddingDp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (over > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = gameOverEffect(over)
                    }
                },
            verticalArrangement = Arrangement.spacedBy(spacingDp)
        ) {
            grid.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacingDp)
                ) {
                    row.forEachIndexed { j, cellValue ->
                        val tileInfo = tileAnimationInfo[Pair(i, j)]
                        val mood = when {
                            !sprites || cellValue == 0 -> SpriteMood.Neutral
                            else -> spriteMood(
                                grid = grid, i = i, j = j, value = cellValue,
                                isGameOver = isGameOver, idleLevel = idleLevel, emptyCount = emptyCount,
                                dizzy = dizzy, startled = startled || chilled || (flinch && breakingCell != i to j),
                                age = track.ages.getOrNull(i)?.getOrNull(j) ?: 0,
                                pair = track.pairs.getOrNull(i)?.getOrNull(j) ?: 0,
                                glance = if (glancing) moveDir else null
                            )
                        }
                        if (sprites && cellValue > 0 && mood.emotion == SpriteEmotion.IDLE) relaxedCells.add(i to j)
                        // Tap: the hammer hits when armed, otherwise a sprite gets poked.
                        val canHammer = hammerArmed && onCellTap != null && cellValue != 0
                        val canPoke = sprites && cellValue != 0
                        GameCell(
                            value = cellValue,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (canHammer || canPoke) Modifier.pointerInput(i, j, canHammer) {
                                        detectTapGestures {
                                            if (canHammer) onCellTap?.invoke(i, j)
                                            else pokes[i to j] = (pokes[i to j] ?: 0) + 1
                                        }
                                    } else Modifier
                                )
                                .testTag("GameScreen_Item_GameCell_${i}_${j}")
                                .semantics {
                                    contentDescription = if (cellValue == 0) "Empty Tile at row $i column $j" else "Tile $cellValue at row $i column $j"
                                },
                            isNew = tileInfo?.isNew ?: false,
                            isMerged = tileInfo?.isMerged ?: false,
                            startPosition = tileInfo?.startPosition,
                            currentPosition = Pair(i, j),
                            moveCount = moveCount,
                            cellSizePx = cellSizePx,
                            spacingPx = spacingPx,
                            emotion = mood.emotion,
                            lookX = mood.lookX,
                            lookY = mood.lookY,
                            wobble = hammerArmed && cellValue != 0,
                            breaking = breakingCell == Pair(i, j),
                            pokeKey = pokes[i to j],
                            badgeText = if (cellValue < 0) rockTtl.toString() else null,
                            shuffled = tileInfo?.shuffled ?: false,
                            boardMotion = boardMotions[i to j]
                        )
                    }
                }
            }
        }
        if (frozen) FrostOverlay(sprites)
        if (sprites) {
            if (lastGain != null && cellSizePx > 0f) {
                key(lastGain.key) { MergeFloats(lastGain, tileAnimationInfo, grid, cellSizePx, stride, boardSize, paddingPx) }
            }
            if (freezeBurstKey > 0) key(freezeBurstKey) { FreezeBurst() }
        } else if (lastGain != null) {
            ScorePopup(lastGain)
        }
        if (notice != null) {
            if (sprites) key(notice.key) { BonkFloat() } else NoticePopup(notice)
        }
    }
}

/** The prototype's "Bonk!": big gold letters popping up in the middle of the board. */
@Composable
private fun BoxScope.BonkFloat() {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(1300f, tween(1300, easing = LinearEasing)) }
    BoxWithConstraints(Modifier.matchParentSize()) {
        val at = with(LocalDensity.current) { Offset(maxWidth.toPx() / 2f, maxHeight.toPx() / 2f) }
        SpriteFloatText("Bonk!", at, clock, 0f, 1200f, 60f, Color(0xFFFFC93A), 34.sp, display = true)
    }
}

private fun Direction.toSpriteDir(): SpriteDir = when (this) {
    Direction.LEFT -> SpriteDir.LEFT
    Direction.RIGHT -> SpriteDir.RIGHT
    Direction.UP -> SpriteDir.UP
    Direction.DOWN -> SpriteDir.DOWN
}

/** grayscale(.5) brightness(.85) once nobody can move. */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
private fun gameOverEffect(p: Float): androidx.compose.ui.graphics.RenderEffect {
    val cm = android.graphics.ColorMatrix().apply { setSaturation(1f - 0.5f * p) }
    val b = 1f - 0.15f * p
    cm.postConcat(android.graphics.ColorMatrix().apply { setScale(b, b, b, 1f) })
    return android.graphics.RenderEffect.createColorFilterEffect(android.graphics.ColorMatrixColorFilter(cm)).asComposeRenderEffect()
}

/** The board's violet glow, and a pulsing red ring when it is nearly full. */
private fun DrawScope.drawSpriteBoardGlow(radius: Float, danger: Boolean, p: Float) {
    val nc = drawContext.canvas.nativeCanvas
    val pulse = (1f - cos(2f * PI.toFloat() * p)) / 2f
    val glowBlur = if (danger) 24.dp.toPx() + 10.dp.toPx() * pulse else 24.dp.toPx()
    val glowColor = if (danger) Danger.copy(alpha = 0.3f + 0.3f * pulse) else Color(0x598C5AFF)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = glowColor.toArgb()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = glowBlur * 0.6f
        maskFilter = BlurMaskFilter(glowBlur * 0.5f, BlurMaskFilter.Blur.NORMAL)
    }
    nc.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
    if (danger) {
        val spread = 4.dp.toPx() * pulse
        if (spread > 0.3f) {
            drawRoundRect(
                Danger,
                topLeft = Offset(-spread / 2, -spread / 2),
                size = Size(size.width + spread, size.height + spread),
                cornerRadius = CornerRadius(radius + spread / 2),
                style = Stroke(width = spread)
            )
        }
    }
}

/** CSS inset box-shadow (0 0 blur color) inside a rounded rect the size of this scope. */
private fun DrawScope.drawInnerShadow(radius: Float, blur: Float, color: Color, dy: Float = 0f) {
    val card = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius))) }
    val ring = Path().apply {
        addRect(Rect(-size.width, -size.height, size.width * 2, size.height * 2))
        val hole = Path().apply { addRoundRect(RoundRect(0f, dy, size.width, size.height + dy, CornerRadius(radius))) }
        op(this, hole, PathOperation.Difference)
    }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter((blur / 2f / 0.577f).coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
    }
    clipPath(card) { drawContext.canvas.nativeCanvas.drawPath(ring.asAndroidPath(), paint) }
}

/** A short message in the middle of the board: bonks, rocks, new friends. */
@Composable
private fun BoxScope.NoticePopup(notice: BoardNotice) {
    val p = remember(notice.key) { Animatable(0f) }
    LaunchedEffect(notice.key) { p.animateTo(1f, tween(1700, easing = LinearEasing)) }
    val t = p.value
    if (t >= 1f) return
    val alpha = when {
        t < 0.1f -> t / 0.1f
        t > 0.8f -> (1f - t) / 0.2f
        else -> 1f
    }
    Text(
        text = notice.text,
        color = Color(0xFFFFC93A),
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        style = TextStyle(shadow = Shadow(Outline, Offset(0f, 3f), 3f)),
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .background(Color(0xCC12082E), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                val pop = if (t < 0.1f) 0.8f + 2f * t else 1f
                scaleX = pop
                scaleY = pop
            }
    )
}

/** Icy rim while Freeze is active (the prototype's frost: a bright inner rim and frosted corners). */
@Composable
private fun BoxScope.FrostOverlay(sprites: Boolean) {
    val pulse = rememberInfiniteTransition(label = "frost")
    val a by pulse.animateFloat(0.55f, 0.95f, infiniteRepeatable(tween(1100), androidx.compose.animation.core.RepeatMode.Reverse), label = "frostAlpha")
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                if (sprites) Modifier.drawBehind {
                    drawInnerShadow(12.dp.toPx(), 40.dp.toPx(), Color(0xFFA0E6FF).copy(alpha = 0.55f))
                    drawCircle(Brush.radialGradient(listOf(Color(0x59DCFAFF), Color.Transparent), center = Offset.Zero, radius = size.width * 0.3f), radius = size.width * 0.3f, center = Offset.Zero)
                    drawCircle(Brush.radialGradient(listOf(Color(0x59DCFAFF), Color.Transparent), center = Offset(size.width, size.height), radius = size.width * 0.3f), radius = size.width * 0.3f, center = Offset(size.width, size.height))
                } else Modifier
            )
            .border(3.dp, Color(0xFFC8F5FF).copy(alpha = if (sprites) 0.9f else a), RoundedCornerShape(10.dp))
            .then(
                if (sprites) Modifier else Modifier.background(
                    Brush.radialGradient(listOf(Color.Transparent, Color(0xFFA6ECFF).copy(alpha = 0.22f * a)), radius = 900f),
                    RoundedCornerShape(10.dp)
                )
            )
    )
}

/** Freeze: 24 ice shards burst from the middle of the board. */
@Composable
private fun BoxScope.FreezeBurst() {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1300f, tween(1300, easing = LinearEasing)) }
    val seed = remember { Random.nextInt() }
    Canvas(Modifier.matchParentSize()) {
        val ms = p.value
        if (ms >= 1300f) return@Canvas
        val spread = size.width * 0.6f
        val rnd = Random(seed)
        repeat(24) {
            val sz = spread * 0.11f * (0.6f + rnd.nextFloat() * 0.7f)
            val ang = rnd.nextFloat() * 2f * PI.toFloat()
            val dist = spread * (0.45f + rnd.nextFloat() * 0.55f)
            val dur = 900f + rnd.nextFloat() * 300f
            val d = rnd.nextFloat() * 60f
            val rot = rnd.nextFloat() * 360f
            val t = (ms - d) / dur
            if (t in 0f..1f) {
                val e = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f).transform(t)
                drawParticle(SpriteParticle.SHARD, Offset(center.x + cos(ang) * dist * e, center.y + sin(ang) * dist * e), sz * (1f - 0.75f * e), rot * e, 1f - e)
            }
        }
    }
}

/** Floating text: "+64" over each merge, "+2 coins" under it, and "Combo x3" across the top. */
@Composable
private fun BoxScope.MergeFloats(
    gain: MoveGain,
    info: Map<Pair<Int, Int>, TileAnimationInfo>,
    grid: List<List<Int>>,
    cell: Float,
    stride: Float,
    boardSize: IntSize,
    paddingPx: Float
) {
    val merges = remember(gain.key) {
        info.filterValues { it.isMerged }.keys.mapNotNull { (r, c) -> grid.getOrNull(r)?.getOrNull(c)?.takeIf { it > 0 }?.let { Triple(r, c, it) } }
    }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(1500f, tween(1500, easing = LinearEasing)) }
    val start = SLIDE_MS.toFloat()
    merges.forEach { (r, c, v) ->
        val cx = c * stride + cell / 2f
        val cy = r * stride + cell / 2f
        SpriteFloatText("+%,d".format(v * gain.multiplier), Offset(cx, cy - cell * 0.2f), clock, start, 900f, 60f, Color.White, 20.sp)
        if (v >= 32) SpriteFloatText("+${v / 32} coins", Offset(cx, cy + cell * 0.15f), clock, start, 900f, 40f, Color(0xFFFFC93A), 15.sp)
    }
    if (gain.multiplier >= 2) {
        val inner = boardSize.width - 2 * paddingPx
        SpriteFloatText("Combo x${gain.multiplier}", Offset(inner / 2f, inner * 0.18f), clock, start, 1200f, 90f, Color(0xFFFFC93A), 32.sp, display = true)
    }
}

/** "+64", "Combo x3" and "+2 coins" floating up over the board after a scoring move (non-sprite themes). */
@Composable
private fun BoxScope.ScorePopup(gain: MoveGain) {
    val p = remember(gain.key) { Animatable(0f) }
    LaunchedEffect(gain.key) { p.animateTo(1f, tween(1000, easing = LinearEasing)) }
    val t = p.value
    if (t >= 1f) return
    val alpha = if (t < 0.15f) t / 0.15f else (1f - (t - 0.15f) / 0.85f)
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 18.dp)
            .graphicsLayer {
                translationY = -60f * t
                this.alpha = alpha.coerceIn(0f, 1f)
                val pop = if (t < 0.2f) 0.7f + 1.6f * t else 1f
                scaleX = pop
                scaleY = pop
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val outline = Shadow(Outline, Offset(0f, 3f), 2f)
        Text("+${gain.points}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, style = TextStyle(shadow = outline))
        if (gain.multiplier >= 2) {
            Text("Combo x${gain.multiplier}", color = Color(0xFFFFC93A), fontSize = 22.sp, fontWeight = FontWeight.Black, style = TextStyle(shadow = outline))
        }
        if (gain.coins > 0) {
            Text("+${gain.coins} coins", color = Color(0xFFFFE07A), fontSize = 16.sp, fontWeight = FontWeight.Bold, style = TextStyle(shadow = outline))
        }
    }
}

/** What a sprite on the board feels and where it looks, from the board alone. */
data class SpriteMood(val emotion: SpriteEmotion, val lookX: Float = 0f, val lookY: Float = 0f) {
    companion object {
        val Neutral = SpriteMood(SpriteEmotion.IDLE)
    }
}

/** The prototype's priorities: game over, dizzy, startled, asleep, thinking, twin (love after 2 moves), crowded, bored. */
private fun spriteMood(
    grid: List<List<Int>>,
    i: Int,
    j: Int,
    value: Int,
    isGameOver: Boolean,
    idleLevel: Int,
    emptyCount: Int,
    dizzy: Boolean,
    startled: Boolean,
    age: Int,
    pair: Int,
    glance: Pair<Int, Int>?
): SpriteMood {
    if (value == 0) return SpriteMood.Neutral
    if (value < 0) return SpriteMood(SpriteEmotion.GRUMPY)
    if (isGameOver) return SpriteMood(SpriteEmotion.SAD)
    val gx = (glance?.second ?: 0) * 2f
    val gy = (glance?.first ?: 0) * 1.8f
    if (dizzy) return SpriteMood(SpriteEmotion.DIZZY)
    if (startled) return SpriteMood(SpriteEmotion.SURPRISED, gx, gy)
    if (idleLevel >= 2) return SpriteMood(SpriteEmotion.SLEEPY)
    if (idleLevel == 1) return SpriteMood(SpriteEmotion.THINKING)
    // A twin right next door: get excited and look at it (doubles as a hint); in love once it lasts.
    twinOf(grid, i, j)?.let { (dr, dc) ->
        return SpriteMood(if (pair >= 2) SpriteEmotion.LOVE else SpriteEmotion.EXCITED, lookX = dc * 2f, lookY = dr * 1.8f)
    }
    if (emptyCount <= 2) return SpriteMood(SpriteEmotion.WORRIED, gx, gy)
    if (age >= 20) return SpriteMood(SpriteEmotion.GRUMPY, gx, gy)
    return SpriteMood(SpriteEmotion.IDLE, gx, gy)
}

@Composable
fun GameCell(
    value: Int,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    isMerged: Boolean = false,
    startPosition: Pair<Int, Int>? = null,
    currentPosition: Pair<Int, Int>? = null,
    moveCount: Int = 0,
    cellSizePx: Float = 0f,
    spacingPx: Float = 0f,
    fontSizeMultiplier: Float = 1.0f,
    emotion: SpriteEmotion = SpriteEmotion.IDLE,
    lookX: Float = 0f,
    lookY: Float = 0f,
    wobble: Boolean = false,
    breaking: Boolean = false,
    pokeKey: Int? = null,
    badgeText: String? = null,
    shuffled: Boolean = false,
    boardMotion: SpriteMotion? = null
) {
    val sprites = LocalGameTheme.current.usesSprites
    if (sprites && value != 0) {
        SpriteCell(
            value = value, modifier = modifier, isNew = isNew, isMerged = isMerged, startPosition = startPosition,
            currentPosition = currentPosition, moveCount = moveCount, cellSizePx = cellSizePx, spacingPx = spacingPx,
            fontSizeMultiplier = fontSizeMultiplier, emotion = emotion, lookX = lookX, lookY = lookY, wobble = wobble,
            breaking = breaking, pokeKey = pokeKey, badgeText = badgeText, shuffled = shuffled, boardMotion = boardMotion
        )
        return
    }
    // Hammer armed: tiles wiggle, asking to be hit. Breaking: shake, then shrink away.
    val wiggle = rememberInfiniteTransition(label = "wiggle")
    val wigglePhase by wiggle.animateFloat(0f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing)), label = "wigglePhase")
    val breakProgress = remember { Animatable(0f) }
    LaunchedEffect(breaking) {
        if (breaking) {
            breakProgress.snapTo(0f)
            breakProgress.animateTo(1f, tween(420, easing = LinearEasing))
        } else breakProgress.snapTo(0f)
    }
    // Compute pixel offsets based on grid coordinate difference
    val hasSlide = startPosition != null && startPosition != currentPosition
    val rowDiff = if (hasSlide) (startPosition!!.first - currentPosition!!.first) else 0
    val colDiff = if (hasSlide) (startPosition!!.second - currentPosition!!.second) else 0
    val cellStride = cellSizePx + spacingPx

    // Synchronously initialize state based on moveCount to avoid a 1-frame flash at target position
    val slideProgress = remember(moveCount) { Animatable(if (hasSlide) 0f else 1f) }
    var scale by remember(moveCount) { mutableStateOf(if (isNew) 0f else 1f) }

    LaunchedEffect(moveCount) {
        if (hasSlide || isMerged || isNew) {
            if (hasSlide) {
                slideProgress.animateTo(1f, animationSpec = tween(durationMillis = 150))
            }
            if (isMerged) {
                animate(1f, 1.15f, animationSpec = tween(100)) { v, _ -> scale = v }
                animate(1.15f, 1f, animationSpec = tween(100)) { v, _ -> scale = v }
            }
            if (isNew) {
                animate(0f, 1f, animationSpec = tween(durationMillis = 120)) { v, _ -> scale = v }
            }
        }
    }

    val progress = slideProgress.value
    val offsetX = colDiff * cellStride * (1f - progress)
    val offsetY = rowDiff * cellStride * (1f - progress)

    val bp = breakProgress.value
    val breakScale = if (breaking && bp > 0.55f) (1f - (bp - 0.55f) / 0.45f).coerceIn(0f, 1f) else 1f
    val shake = if (breaking && bp in 0.35f..0.6f) sin(bp * 90f) * 6f else 0f
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        TileView(
            value = value,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX + shake
                    translationY = offsetY
                    scaleX = scale * breakScale
                    scaleY = scale * breakScale
                    rotationZ = if (wobble) sin(wigglePhase * 2f * PI.toFloat()) * 3f else 0f
                },
            fontSizeMultiplier = fontSizeMultiplier
        )
        if (breaking && bp < 0.75f) {
            val swing = if (bp < 0.4f) 60f - 100f * (bp / 0.4f) else -40f + 15f * ((bp - 0.4f) / 0.35f)
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.75f)
                    .graphicsLayer {
                        translationX = size.width * 0.35f
                        translationY = -size.height * 0.35f
                        rotationZ = swing
                        transformOrigin = TransformOrigin(0.15f, 0.85f)
                        alpha = if (bp > 0.6f) (0.75f - bp) / 0.15f else 1f
                    }
            ) { drawHammerIcon() }
        }
    }
}

/** What this cell's sprite is doing on this move, fixed when the move starts. */
private class CellRole(val from: Pair<Int, Int>?, val merged: Boolean, val spawned: Boolean, val shuffled: Boolean)

/**
 * A sprite in a classic cell, choreographed like the prototype: it stretches along the slide with a glow streak
 * behind it and squashes against the wall it lands on. A merge slides one twin onto the other, then the new sprite
 * evolves out of them. New sprites drop out of a portal once the slide is over.
 */
@Composable
private fun SpriteCell(
    value: Int,
    modifier: Modifier,
    isNew: Boolean,
    isMerged: Boolean,
    startPosition: Pair<Int, Int>?,
    currentPosition: Pair<Int, Int>?,
    moveCount: Int,
    cellSizePx: Float,
    spacingPx: Float,
    fontSizeMultiplier: Float,
    emotion: SpriteEmotion,
    lookX: Float,
    lookY: Float,
    wobble: Boolean,
    breaking: Boolean,
    pokeKey: Int?,
    badgeText: String?,
    shuffled: Boolean,
    boardMotion: SpriteMotion?
) {
    val role = remember(moveCount) {
        CellRole(
            from = startPosition?.takeIf { currentPosition != null && it != currentPosition },
            merged = isMerged,
            spawned = isNew,
            shuffled = shuffled
        )
    }
    // A sprite that arrived this move is a new character in this cell; one that stayed keeps its state.
    val identity = remember { intArrayOf(Int.MIN_VALUE) }
    remember(moveCount) {
        if (role.from != null || role.merged || role.spawned || identity[0] == Int.MIN_VALUE) identity[0] = moveCount
        0
    }
    val stride = cellSizePx + spacingPx
    val fromDx = if (role.from != null && currentPosition != null) (role.from.second - currentPosition.second) * stride else 0f
    val fromDy = if (role.from != null && currentPosition != null) (role.from.first - currentPosition.first) * stride else 0f
    val dir = when {
        role.from == null -> SpriteDir.DOWN
        kotlin.math.abs(fromDx) >= kotlin.math.abs(fromDy) -> if (fromDx > 0f) SpriteDir.LEFT else SpriteDir.RIGHT
        else -> if (fromDy > 0f) SpriteDir.UP else SpriteDir.DOWN
    }
    val slide = remember(moveCount) { Animatable(if (role.from != null) 0f else 1f) }
    var slideDone by remember(moveCount) { mutableStateOf(role.from == null) }
    var motion by remember { mutableStateOf<SpriteMotion?>(null) }
    // Motions from before this sprite arrived belong to the one that left.
    val motionFloor = remember { intArrayOf(0) }
    remember(identity[0]) { motionFloor[0] = motionCounter; 0 }
    val ownMotion = motion?.takeIf { it.key > motionFloor[0] }
    var spinDizzy by remember(moveCount) { mutableStateOf(role.shuffled) }

    LaunchedEffect(moveCount) {
        if (role.from == null) return@LaunchedEffect
        motion = if (role.shuffled) SpriteMotion(SpriteMotionKind.SPIN, nextMotionKey())
        else SpriteMotion(SpriteMotionKind.STRETCH, nextMotionKey(), dir, durationMs = 135)
        launch { if (role.shuffled) { delay(900); spinDizzy = false } }
        slide.animateTo(1f, tween(if (role.shuffled) SHUFFLE_MS else SLIDE_MS, easing = if (role.shuffled) ShuffleEasing else SlideEasing))
        slideDone = true
        if (!role.merged) motion = SpriteMotion(SpriteMotionKind.LAND, nextMotionKey(), if (role.shuffled) SpriteDir.DOWN else dir)
    }
    LaunchedEffect(boardMotion) { if (boardMotion != null) motion = boardMotion }

    val slideLayer = Modifier.graphicsLayer {
        val k = 1f - slide.value
        translationX = fromDx * k
        translationY = fromDy * k
    }
    val mood = if (spinDizzy) SpriteEmotion.DIZZY else emotion
    Box(modifier = modifier.aspectRatio(1f)) {
        if (role.from != null && !role.shuffled) {
            SlideStreak(
                glow = ElementalSprites.specFor(if (role.merged) value / 2 else value).glow,
                fromDx = fromDx, fromDy = fromDy, cell = cellSizePx, key = moveCount
            )
        }
        if (role.merged && !slideDone) {
            // The mover slides under its twin, which waits in this cell.
            SpriteTile(value = value / 2, modifier = Modifier.fillMaxSize().then(slideLayer), emotion = SpriteEmotion.EXCITED, motion = motion, showEffects = false)
            SpriteTile(value = value / 2, modifier = Modifier.fillMaxSize(), emotion = SpriteEmotion.EXCITED, showEffects = false)
        } else {
            key(identity[0]) {
                SpriteTile(
                    value = value,
                    modifier = Modifier.fillMaxSize().then(slideLayer),
                    emotion = mood,
                    lookX = lookX,
                    lookY = lookY,
                    motion = ownMotion,
                    mergeKey = if (role.merged) moveCount else null,
                    spawnKey = if (role.spawned) moveCount else null,
                    spawnDelayMs = SPAWN_DELAY_MS,
                    breaking = breaking,
                    breakHitMs = HAMMER_HIT_MS,
                    pokeKey = pokeKey,
                    badgeText = badgeText,
                    fontSizeMultiplier = fontSizeMultiplier,
                    wantHitDeg = if (wobble) 3f else 0f
                )
            }
        }
        if (breaking) HammerSwing()
    }
}

/** The glow streak a sliding sprite leaves behind (fades over 320 ms). */
@Composable
private fun SlideStreak(glow: Color, fromDx: Float, fromDy: Float, cell: Float, key: Int) {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.animateTo(1f, tween(320, easing = LinearEasing)) }
    Canvas(Modifier.fillMaxSize()) {
        val t = p.value
        if (t >= 1f) return@Canvas
        val a = 0.75f * (1f - EaseOut.transform(t))
        val start = Offset(center.x + fromDx, center.y + fromDy)
        val end = center
        val len = hypot(end.x - start.x, end.y - start.y)
        if (len < 4f) return@Canvas
        val h = cell * 0.42f
        val angle = Math.toDegrees(atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())).toFloat()
        drawContext.transform.rotate(angle, start)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), glow.copy(alpha = 0.8f * a)), startX = start.x, endX = start.x + len),
            topLeft = Offset(start.x, start.y - h / 2),
            size = Size(len, h),
            cornerRadius = CornerRadius(h / 2)
        )
        drawContext.transform.rotate(-angle, start)
    }
}

/** The design's hammer swings in from the top right, slams down at 340 ms, and fades (560 ms). */
@Composable
private fun BoxScope.HammerSwing() {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(560, easing = LinearEasing)) }
    BoxWithConstraints(Modifier.matchParentSize()) {
        val w = maxWidth * 0.9f
        Canvas(
            modifier = Modifier
                .size(w)
                .offset(x = maxWidth / 2 - w * 0.2f, y = maxHeight / 2 - w * 0.95f)
                .graphicsLayer {
                    val e = EaseIn.transform(p.value)
                    rotationZ = kfLinear(e, floatArrayOf(0f, 0.35f, 0.6f, 0.75f, 1f), floatArrayOf(50f, 70f, -25f, -15f, -15f))
                    alpha = kfLinear(e, floatArrayOf(0f, 0.35f, 0.75f, 1f), floatArrayOf(0f, 1f, 1f, 0f))
                    transformOrigin = TransformOrigin(0.85f, 0.85f)
                }
        ) { drawHammerIcon() }
    }
}

private fun kfLinear(p: Float, at: FloatArray, v: FloatArray): Float {
    if (p <= at[0]) return v[0]
    for (k in 0 until at.size - 1) if (p <= at[k + 1]) return v[k] + (v[k + 1] - v[k]) * (p - at[k]) / (at[k + 1] - at[k])
    return v.last()
}

@Composable
fun TileView(
    value: Int,
    modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1.0f,
    emotion: SpriteEmotion = SpriteEmotion.IDLE,
    lookX: Float = 0f,
    lookY: Float = 0f,
    charScaleX: Float = 1f,
    charScaleY: Float = 1f,
    mergeKey: Int? = null,
    spawnKey: Int? = null,
    breaking: Boolean = false,
    pokeKey: Int? = null,
    badgeText: String? = null
) {
    val theme = LocalGameTheme.current
    // The Shadow Rock is always drawn as a sprite, whatever the theme.
    if ((theme.usesSprites && value != 0) || value < 0) {
        SpriteTile(
            value = value,
            modifier = modifier,
            emotion = emotion,
            lookX = lookX,
            lookY = lookY,
            charScaleX = charScaleX,
            charScaleY = charScaleY,
            mergeKey = mergeKey,
            spawnKey = spawnKey,
            breaking = breaking,
            pokeKey = pokeKey,
            badgeText = badgeText,
            fontSizeMultiplier = fontSizeMultiplier
        )
        return
    }
    if (theme.usesSprites && value == 0) {
        EmptySpriteCell(modifier)
        return
    }
    val backgroundColor = theme.tileColors[value] ?: theme.tileColors[0]!!
    val textColor = if (value <= 4) theme.textColor else Color.White

    val textSize = remember(value, fontSizeMultiplier) {
        val baseSize = when {
            value >= 10000 -> 14
            value >= 1000 -> 20
            value >= 100 -> 24
            else -> 28
        }
        (baseSize * fontSizeMultiplier).sp
    }

    val cellShadowModifier = if (value > 0) {
        Modifier.shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(8.dp),
            clip = false,
            ambientColor = backgroundColor.copy(alpha = 0.4f),
            spotColor = backgroundColor.copy(alpha = 0.6f)
        )
    } else {
        Modifier
    }

    val textShadow = remember(value) {
        if (value > 0) {
            val logValue = log2(value.toFloat()).coerceAtLeast(1f)
            val baseBlur = 2f
            val scaleFactor = 1.5f
            val blurRadius = (baseBlur + (logValue * scaleFactor)).coerceIn(2f, 25f)
            Shadow(
                color = if (value > 128) textColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.6f),
                offset = Offset.Zero,
                blurRadius = blurRadius
            )
        } else {
            null
        }
    }

    val borderModifier = if (value > 0) {
        Modifier.border(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    backgroundColor.copy(alpha = 0.2f)
                )
            ),
            shape = RoundedCornerShape(8.dp)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = theme.textColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        )
    }

    val backgroundBrush = remember(value, backgroundColor) {
        if (value > 0) {
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.95f),
                    backgroundColor.copy(alpha = 0.6f),
                    backgroundColor.copy(alpha = 0.6f),
                    backgroundColor.copy(alpha = 0.45f),
                    backgroundColor.copy(alpha = 1f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.01f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .then(cellShadowModifier)
            .clip(RoundedCornerShape(8.dp))
            .background(brush = backgroundBrush)
            .then(borderModifier),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                color = textColor,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = textShadow
                ),
                modifier = Modifier.drawBehind {
                    val paint = Paint()
                        .asFrameworkPaint().apply {
                            this.style = android.graphics.Paint.Style.STROKE
                            this.strokeWidth = 2f
                            this.color = Color.White.copy(alpha = 0.7f).toArgb()
                            this.textSize = textSize.toPx()
                            this.isAntiAlias = true
                            this.flags = this.flags or android.graphics.Paint.SUBPIXEL_TEXT_FLAG
                        }

                    val textBounds = android.graphics.Rect()
                    val text = value.toString()
                    paint.getTextBounds(text, 0, text.length, textBounds)

                    drawIntoCanvas { canvas ->
                        val x = (size.width - textBounds.width()) / 2f
                        val y = (size.height + textBounds.height()) / 2f
                        canvas.nativeCanvas.drawText(
                            text,
                            x,
                            y,
                            paint
                        )
                    }
                }
            )
        }
    }
}

/** An empty slot on the sprite board: a dark well with an inner shadow and a faint lower lip. */
@Composable
private fun EmptySpriteCell(modifier: Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            val r = size.width * 0.18f
            val shape = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
            onDrawBehind {
                drawPath(shape, Color(0xA6140834))
                drawInnerShadow(r, 8.dp.toPx(), Color.Black.copy(alpha = 0.45f), dy = 3.dp.toPx())
                clipPath(shape) {
                    drawRoundRect(Color.White.copy(alpha = 0.06f), topLeft = Offset(0f, -1.dp.toPx()), size = size, cornerRadius = CornerRadius(r), style = Stroke(1.dp.toPx()))
                }
            }
        }
    )
}

@Preview
@Composable
fun GameCellBoardPreview() {
    val values = listOf(0, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048)

    _2048OriginalTheme(theme = GameTheme.NeonPink) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(values) { value ->
                Box(
                    modifier = Modifier.padding(4.dp)
                ) {
                    TileView(value, Modifier.size(75.dp))
                }
            }
        }
    }
}
