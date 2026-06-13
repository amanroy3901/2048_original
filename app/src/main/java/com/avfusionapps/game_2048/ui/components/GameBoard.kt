package com.avfusionapps.game_2048.ui.components

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.log2

@Composable
fun GameBoard(viewModel: GameViewModel) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surfaceColor)
            .border(2.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            viewModel.gameState.grid.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEachIndexed { j, cellValue ->
                        val tileInfo = viewModel.gameState.tileAnimationInfo[Pair(i, j)]
                        GameCell(
                            value = cellValue,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("GameScreen_Item_GameCell_${i}_${j}")
                                .semantics {
                                    contentDescription = if (cellValue == 0) "Empty Tile at row $i column $j" else "Tile $cellValue at row $i column $j"
                                },
                            isNew = tileInfo?.isNew ?: false,
                            isMerged = tileInfo?.isMerged ?: false
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel.gameState.moveCount) {
        if (viewModel.gameState.tileAnimationInfo.isNotEmpty()) {
            delay(300) // Adjust delay based on longest animation
            viewModel.clearAnimationInfo()
        }
    }
}

@Composable
fun GameCell(
    value: Int,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    isMerged: Boolean = false,
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

    // Dynamic colored glow shadow matching the cell color
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

    // Glassmorphism border: highlights at the top, blends with background at the bottom
    val borderModifier = if (value > 0) {
        Modifier.border(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    backgroundColor.copy(alpha = 0.15f)
                )
            ),
            shape = RoundedCornerShape(8.dp)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp)
        )
    }

    // Glassmorphism background brush (transparent gradient)
    val backgroundBrush = remember(value, backgroundColor) {
        if (value > 0) {
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.8f),
                    backgroundColor.copy(alpha = 0.35f),
                    backgroundColor.copy(alpha = 0.6f),
                    backgroundColor.copy(alpha = 0.35f),
                    backgroundColor.copy(alpha = 0.85f)
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

    var scale by remember { mutableStateOf(1f) }
    LaunchedEffect(key1 = value, key2 = isNew, key3 = isMerged) {
        scale = 1f // Reset scale
        if (isNew) {
            scale = 0.1f
            animate(0.1f, 1f, animationSpec = tween(150, 50)) { v, _ -> scale = v }
        } else if (isMerged) {
            animate(1f, 1.2f, animationSpec = tween(80)) { v, _ -> scale = v }
            animate(1.2f, 1f, animationSpec = tween(80)) { v, _ -> scale = v }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(cellShadowModifier) // Apply glow
            .clip(RoundedCornerShape(8.dp))
            .background(brush = backgroundBrush)
            .then(borderModifier), // Apply border
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
                            this.strokeWidth = 2f  // Increased stroke width
                            this.color = Color.White.copy(alpha = 0.7f).hashCode()
                            this.textSize = textSize.toPx()
                            this.isAntiAlias = true  // Enable anti-aliasing
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

    _2048OriginalTheme(theme = GameTheme.Emerald) {
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
                    GameCell(value, Modifier.size(75.dp))
                }
            }
        }
    }
}

