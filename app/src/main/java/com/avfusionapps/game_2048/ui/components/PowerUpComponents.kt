package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.viewmodel.PowerUps

/**
 * The classic-mode power-up row: Undo, Hammer, Shuffle, Freeze, Hint and Restart.
 * Each shows its charges left, or its coin price once it has run out.
 */
@Composable
fun PowerUpBar(
    powerUps: PowerUps,
    hammerArmed: Boolean,
    freezeActive: Boolean,
    onUndo: () -> Unit,
    onHammer: () -> Unit,
    onShuffle: () -> Unit,
    onFreeze: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PowerUpButton("Undo", Icons.AutoMirrored.Rounded.Undo, powerUps.undo, listOf(Color(0xFFB57BFF), Color(0xFF6B35D6)), onClick = onUndo)
        PowerUpButton("Hammer", Icons.Rounded.Gavel, powerUps.hammer, listOf(Color(0xFFC9A8FF), Color(0xFF7E4FE0)), active = hammerArmed, onClick = onHammer)
        PowerUpButton("Shuffle", Icons.Rounded.Shuffle, powerUps.shuffle, listOf(Color(0xFFFFB25A), Color(0xFFF2601E)), onClick = onShuffle)
        PowerUpButton("Freeze", Icons.Rounded.AcUnit, powerUps.freeze, listOf(Color(0xFF7FD8FF), Color(0xFF2E7BE0)), active = freezeActive, onClick = onFreeze)
        PowerUpButton("Hint", Icons.Rounded.Lightbulb, powerUps.hint, listOf(Color(0xFFFFE07A), Color(0xFFE59A12)), onClick = onHint)
        PowerUpButton("Restart", Icons.Rounded.Refresh, null, listOf(Color(0xFF4A3590), Color(0xFF2A1A66)), onClick = onRestart)
    }
}

@Composable
private fun PowerUpButton(
    label: String,
    icon: ImageVector,
    charges: Int?,
    colors: List<Color>,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pulse = rememberInfiniteTransition(label = "powerUpPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "glow"
    )
    val buy = charges != null && charges <= 0
    val description = when {
        charges == null -> label
        buy -> "$label, buy one for ${PowerUps.COST} coins"
        else -> "$label, $charges left"
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .testTag("PowerUp_$label")
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(if (pressed) 0.88f else 1f)
                .shadow(
                    elevation = if (active) (6 + 10 * glow).dp else 8.dp,
                    shape = RoundedCornerShape(15.dp),
                    ambientColor = colors[0],
                    spotColor = colors[0]
                )
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(colors))
                .border(
                    width = if (active) 3.dp else 1.5.dp,
                    color = Color.White.copy(alpha = if (active) glow else 0.45f),
                    shape = RoundedCornerShape(15.dp)
                )
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        if (charges != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .widthIn(min = 22.dp)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(if (buy) Color(0xFFFFC93A) else Color.White)
                    .border(2.dp, Color(0xFF2A0F4A), CircleShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (buy) "${PowerUps.COST}" else "$charges",
                    color = Color(0xFF2A0F4A),
                    fontSize = if (buy) 10.sp else 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/** Coins, combo and freeze at a glance, plus the hammer instruction while it is armed. */
@Composable
fun GameStatusRow(
    coins: Int,
    combo: Int,
    freezeMoves: Int,
    hammerArmed: Boolean,
    modifier: Modifier = Modifier,
    rockTtl: Int = 0,
    foundSprites: Int? = null,
    onCollectionClick: (() -> Unit)? = null
) {
    val theme = LocalGameTheme.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusPill(background = Color(0x8C0A041E)) {
            CoinIcon(Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("%,d".format(coins), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        if (hammerArmed) {
            Text(
                "Tap a tile to break it",
                color = Color(0xFFFFE07A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            return@Row
        }
        val multiplier = combo.coerceIn(1, 5)
        StatusPill(background = if (combo >= 2) Color(0xFFE0337A) else Color(0x8C0A041E)) {
            Text(
                "Combo x$multiplier",
                color = if (combo >= 2) Color.White else theme.textColor.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (freezeMoves > 0) {
            StatusPill(background = Color(0xFF3C8EDB)) {
                Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("$freezeMoves", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
        if (rockTtl > 0) {
            StatusPill(background = Color(0xFF3E3560)) {
                Text("Rock $rockTtl", color = Color(0xFFE9DDFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.weight(1f))
        if (foundSprites != null && onCollectionClick != null) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x8C0A041E))
                    .border(1.dp, Color(0xFFFFC93A).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onCollectionClick)
                    .padding(start = 4.dp, end = 10.dp)
                    .semantics { contentDescription = "Collection, $foundSprites of 11 found" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.avfusionapps.game_2048.ui.sprites.SpriteTile(
                    value = 64,
                    showCard = false,
                                    showNumber = false,
                    modifier = Modifier.size(28.dp)
                )
                Text("$foundSprites/11", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun StatusPill(background: Color, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
fun CoinIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFF3A6), Color(0xFFFFC93A), Color(0xFFE5850F))),
            radius = r
        )
        drawCircle(color = Color(0xFF8A4A08), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.14f))
        val star = Path().apply {
            for (k in 0 until 10) {
                val rr = if (k % 2 == 0) r * 0.55f else r * 0.24f
                val a = -Math.PI / 2 + k * Math.PI / 5
                val p = Offset(center.x + rr * kotlin.math.cos(a).toFloat(), center.y + rr * kotlin.math.sin(a).toFloat())
                if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(star, Color(0xFFFFF3A6))
    }
}
