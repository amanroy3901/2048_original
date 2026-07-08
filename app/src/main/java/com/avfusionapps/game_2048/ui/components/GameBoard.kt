package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.core.Animatable
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
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.log2

@Composable
fun GameBoard(viewModel: GameViewModel) {
    GameBoard(
        grid = viewModel.gameState.grid,
        tileAnimationInfo = viewModel.gameState.tileAnimationInfo,
        moveCount = viewModel.gameState.moveCount,
        onAnimationsComplete = { viewModel.clearAnimationInfo() }
    )
}

@Composable
fun GameBoard(
    grid: List<List<Int>>,
    tileAnimationInfo: Map<Pair<Int, Int>, TileAnimationInfo>,
    moveCount: Int,
    onAnimationsComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
                        GameCell(
                            value = cellValue,
                            modifier = Modifier
                                .weight(1f)
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
                            spacingPx = spacingPx
                        )
                    }
                }
            }
        }
    }
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
    fontSizeMultiplier: Float = 1.0f
) {
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

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        TileView(
            value = value,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                },
            fontSizeMultiplier = fontSizeMultiplier
        )
    }
}

@Composable
fun TileView(
    value: Int,
    modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1.0f
) {
    val theme = LocalGameTheme.current
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
