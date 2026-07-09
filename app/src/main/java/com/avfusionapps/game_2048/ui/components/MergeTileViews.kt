package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.model.DropTile
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.viewmodel.DropAnim
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * Shared tile rendering for the merge-shooter games (Neon Drop & Neon Rise).
 * Both use the same top-anchored board model, so tiles animate identically:
 * stable-id position springs, value pops with glow, consume fly-ins.
 */

/** Theme color for a tile value; values above 2048 reuse the 2048 accent. */
fun mergeTileColor(theme: GameTheme, value: Int): Color =
    theme.tileColors[value]
        ?: theme.tileColors[2048]
        ?: theme.primaryColor

/**
 * A live board tile: animates position by id, pops + glows on value growth.
 * [tumbleOnSpawn] adds a 360° tumble to the entrance flight (Neon Rise shots).
 */
@Composable
fun BoardTileView(
    tile: DropTile,
    target: Offset,
    spawn: Offset,
    size: Dp,
    theme: GameTheme,
    tumbleOnSpawn: Boolean = false
) {
    val position = remember { Animatable(spawn, Offset.VectorConverter) }
    val popScale = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(if (tumbleOnSpawn) -360f else 0f) }
    var lastValue by remember { mutableIntStateOf(tile.value) }

    LaunchedEffect(target) {
        launch {
            position.animateTo(
                target,
                spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        if (rotation.value != 0f) {
            launch {
                rotation.animateTo(0f, tween(DropAnim.MERGE_POP * 2, easing = LinearOutSlowInEasing))
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
                glowAlpha.animateTo(0f, tween(DropAnim.MERGE_POP * 2, easing = LinearOutSlowInEasing))
            }
        }
    }

    val color = mergeTileColor(theme, tile.value)
    val glowColor = theme.accentColor

    Box(
        modifier = Modifier
            .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
            .size(size)
            .scale(popScale.value)
            .graphicsLayer { rotationZ = rotation.value }
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
fun ConsumedTileView(
    tile: DropTile,
    from: Offset,
    to: Offset,
    size: Dp,
    theme: GameTheme,
    purge: Boolean
) {
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

    val color = mergeTileColor(theme, tile.value)
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

/** Auto-sized tile number with contrast-aware color. */
@Composable
fun TileNumber(value: Int, cell: Dp, background: Color) {
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

/** Floating "COMBO ×N" flair — scale-in, rise, fade. */
@Composable
fun ComboText(
    combo: Int,
    fontSize: TextUnit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
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
        color = accentColor,
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
