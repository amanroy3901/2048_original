package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.model.TileAnimationInfo
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.ui.sprites.SpriteTile
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.avfusionapps.game_2048.viewmodel.MoveGain
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.sin

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
        lastGain = state.lastGain
    )
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
    lastGain: MoveGain? = null
) {
    val theme = LocalGameTheme.current
    val density = LocalDensity.current

    // Measure cell size once from the board dimensions
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val gridSize = grid.size
    val spacingPx = with(density) { 4.dp.toPx() }
    val paddingPx = with(density) { 6.dp.toPx() }

    // Calculate cell dimensions from measured board size
    val cellSizePx = remember(boardSize, gridSize) {
        if (boardSize.width > 0 && gridSize > 0) {
            val usableWidth = boardSize.width - (2 * paddingPx) - ((gridSize - 1) * spacingPx)
            usableWidth / gridSize
        } else 0f
    }

    // Clear animations after they complete
    LaunchedEffect(moveCount) {
        if (tileAnimationInfo.isNotEmpty() && onAnimationsComplete != null) {
            kotlinx.coroutines.delay(350)
            onAnimationsComplete()
        }
    }

    // Sprites get bored: thinking after 6 s without a move, asleep after 14 s.
    var idleLevel by remember { mutableIntStateOf(0) }
    if (theme.usesSprites) {
        LaunchedEffect(moveCount) {
            idleLevel = 0
            kotlinx.coroutines.delay(6_000)
            idleLevel = 1
            kotlinx.coroutines.delay(8_000)
            idleLevel = 2
        }
    }
    val emptyCount = remember(grid) { grid.sumOf { row -> row.count { it == 0 } } }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .onSizeChanged { boardSize = it }
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surfaceColor)
            .border(2.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grid.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEachIndexed { j, cellValue ->
                        val tileInfo = tileAnimationInfo[Pair(i, j)]
                        val mood = if (theme.usesSprites) {
                            spriteMood(grid, i, j, cellValue, isGameOver, idleLevel, emptyCount)
                        } else SpriteMood.Neutral
                        val tappable = hammerArmed && onCellTap != null && cellValue != 0
                        GameCell(
                            value = cellValue,
                            modifier = Modifier
                                .weight(1f)
                                .then(if (tappable) Modifier.clickable { onCellTap?.invoke(i, j) } else Modifier)
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
                            breaking = breakingCell == Pair(i, j)
                        )
                    }
                }
            }
        }
        if (frozen) FrostOverlay()
        if (lastGain != null) ScorePopup(lastGain)
    }
}

/** Icy rim while Freeze is active. */
@Composable
private fun BoxScope.FrostOverlay() {
    val pulse = rememberInfiniteTransition(label = "frost")
    val a by pulse.animateFloat(0.55f, 0.95f, infiniteRepeatable(tween(1100), androidx.compose.animation.core.RepeatMode.Reverse), label = "frostAlpha")
    Box(
        modifier = Modifier
            .matchParentSize()
            .border(3.dp, Color(0xFFC8F5FF).copy(alpha = a), RoundedCornerShape(10.dp))
            .background(
                Brush.radialGradient(
                    listOf(Color.Transparent, Color(0xFFA6ECFF).copy(alpha = 0.22f * a)),
                    radius = 900f
                ),
                RoundedCornerShape(10.dp)
            )
    )
}

/** "+64", "Combo x3" and "+2 coins" floating up over the board after a scoring move. */
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
        val outline = Shadow(Color(0xFF2A0F4A), Offset(0f, 3f), 2f)
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

private fun spriteMood(
    grid: List<List<Int>>,
    i: Int,
    j: Int,
    value: Int,
    isGameOver: Boolean,
    idleLevel: Int,
    emptyCount: Int
): SpriteMood {
    if (value == 0) return SpriteMood.Neutral
    if (isGameOver) return SpriteMood(SpriteEmotion.SAD)
    if (idleLevel >= 2) return SpriteMood(SpriteEmotion.SLEEPY)
    if (idleLevel == 1) return SpriteMood(SpriteEmotion.THINKING)
    // A twin right next door: get excited and look at it (doubles as a hint).
    for ((dr, dc) in listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)) {
        if (grid.getOrNull(i + dr)?.getOrNull(j + dc) == value) {
            return SpriteMood(SpriteEmotion.EXCITED, lookX = dc * 1.8f, lookY = dr * 1.6f)
        }
    }
    if (emptyCount <= 2) return SpriteMood(SpriteEmotion.WORRIED)
    return SpriteMood.Neutral
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
    breaking: Boolean = false
) {
    val sprites = LocalGameTheme.current.usesSprites
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
    // Sprites only: squash and stretch of the character (the card stays square).
    val landing = remember(moveCount) { Animatable(1f) }
    val evolve = remember(moveCount) { Animatable(1f) }

    LaunchedEffect(moveCount) {
        if (sprites && (hasSlide || isMerged || isNew)) {
            if (isNew) {
                kotlinx.coroutines.delay(110)
                animate(0f, 1f, animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) { v, _ -> scale = v }
                return@LaunchedEffect
            }
            if (hasSlide) slideProgress.animateTo(1f, animationSpec = tween(durationMillis = 150))
            if (isMerged) {
                evolve.snapTo(0f)
                evolve.animateTo(1f, tween(560))
            } else if (hasSlide) {
                landing.snapTo(0f)
                landing.animateTo(1f, tween(320))
            }
            return@LaunchedEffect
        }
        if (hasSlide || isMerged || isNew) {
            if (hasSlide) {
                // Already initialized to 0f, just animate to 1f
                slideProgress.animateTo(1f, animationSpec = tween(durationMillis = 150))
            }

            // After slide completes, run secondary animations
            if (isMerged) {
                // Pop effect for merged tile
                animate(1f, 1.15f, animationSpec = tween(100)) { v, _ -> scale = v }
                animate(1.15f, 1f, animationSpec = tween(100)) { v, _ -> scale = v }
            }

            if (isNew) {
                // Scale-in for new tile (after slide completes for other tiles)
                animate(0f, 1f, animationSpec = tween(durationMillis = 120)) { v, _ -> scale = v }
            }
        }
    }

    val progress = slideProgress.value
    val offsetX = colDiff * cellStride * (1f - progress)
    val offsetY = rowDiff * cellStride * (1f - progress)
    val horizontal = colDiff != 0
    var charScaleX = 1f
    var charScaleY = 1f
    if (sprites) {
        if (hasSlide && progress < 1f) {
            val st = sin(PI * progress).toFloat() * 0.2f
            if (horizontal) { charScaleX = 1f + st; charScaleY = 1f - st * 0.75f } else { charScaleY = 1f + st; charScaleX = 1f - st * 0.75f }
        }
        val l = landing.value
        if (l < 1f) {
            val k = sin(PI * 2 * l).toFloat() * (1f - l) * 0.16f
            if (horizontal) { charScaleX = 1f - k; charScaleY = 1f + k * 0.6f } else { charScaleY = 1f - k; charScaleX = 1f + k * 0.6f }
        }
        val e = evolve.value
        if (e < 1f) {
            val (sx, sy) = evolveSquash(e)
            charScaleX = sx; charScaleY = sy
        }
    }

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
            fontSizeMultiplier = fontSizeMultiplier,
            emotion = emotion,
            lookX = lookX,
            lookY = lookY,
            charScaleX = charScaleX,
            charScaleY = charScaleY,
            mergeKey = if (sprites && isMerged) moveCount else null,
            spawnKey = if (sprites && isNew) moveCount else null,
            breaking = breaking
        )
        if (breaking && bp < 0.75f) {
            // The hammer swings down from the top right and hits at ~40%.
            val swing = if (bp < 0.4f) 60f - 100f * (bp / 0.4f) else -40f + 15f * ((bp - 0.4f) / 0.35f)
            Icon(
                imageVector = Icons.Rounded.Gavel,
                contentDescription = null,
                tint = Color(0xFFE9E3FF),
                modifier = Modifier
                    .fillMaxSize(0.75f)
                    .graphicsLayer {
                        translationX = size.width * 0.35f
                        translationY = -size.height * 0.35f
                        rotationZ = swing
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.15f, 0.85f)
                        alpha = if (bp > 0.6f) (0.75f - bp) / 0.15f else 1f
                    }
            )
        }
    }
}

/** The pop a sprite does when it evolves: tiny, wide, tall, settle. */
private fun evolveSquash(p: Float): Pair<Float, Float> {
    val keys = listOf(
        0f to (0.3f to 0.3f),
        0.3f to (1.34f to 0.76f),
        0.55f to (0.86f to 1.2f),
        0.8f to (1.06f to 0.96f),
        1f to (1f to 1f)
    )
    val k = keys.indexOfLast { it.first <= p }.coerceIn(0, keys.size - 2)
    val (p0, a) = keys[k]
    val (p1, b) = keys[k + 1]
    val f = ((p - p0) / (p1 - p0)).coerceIn(0f, 1f)
    return Pair(a.first + (b.first - a.first) * f, a.second + (b.second - a.second) * f)
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
    breaking: Boolean = false
) {
    val theme = LocalGameTheme.current
    if (theme.usesSprites && value != 0) {
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
            fontSizeMultiplier = fontSizeMultiplier
        )
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
