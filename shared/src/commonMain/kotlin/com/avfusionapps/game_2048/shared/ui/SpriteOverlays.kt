package com.avfusionapps.game_2048.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.avfusionapps.game_2048.shared.game.Achievement
import com.avfusionapps.game_2048.shared.game.Progress
import com.avfusionapps.game_2048.shared.game.ProgressEvent
import com.avfusionapps.game_2048.shared.game.Tier
import com.avfusionapps.game_2048.shared.sprites.ConfettiBurst
import com.avfusionapps.game_2048.shared.sprites.ElementalSprites
import com.avfusionapps.game_2048.shared.sprites.SpriteEmotion
import com.avfusionapps.game_2048.shared.sprites.SpriteMotion
import com.avfusionapps.game_2048.shared.sprites.SpriteMotionKind
import com.avfusionapps.game_2048.shared.sprites.SpriteTile
import kotlinx.coroutines.delay

private val Muted = Color(0xFFC9B8F0)
private val Line = Color(0x52BE96FF)
private val Gold = Color(0xFFFFC93A)
private val Bounce = CubicBezierEasing(0.3f, 1.6f, 0.5f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

// ─────────────────────────────── Shared bits ───────────────────────────────

/** The prototype's primary button: a gold pill with a darker lip and brown text. */
@Composable
internal fun SpriteGoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 52.dp) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .heightIn(min = height)
            .scale(if (pressed) 0.96f else 1f)
            .drawBehind {
                drawRoundRect(Color(0xFF8A4A08), topLeft = Offset(0f, 4.dp.toPx()), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
            }
            .background(Brush.verticalGradient(listOf(Color(0xFFFFD25A), Color(0xFFFF9A1F))), RoundedCornerShape(50))
            .border(2.dp, Color(0xFFFFE7A6), RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF3A1400), fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** The prototype's secondary button: a translucent pill with a light outline. */
@Composable
internal fun SpriteGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 48.dp) {
    Box(
        modifier = modifier
            .heightIn(min = height)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(50))
            .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** The prototype's sheet / dialog panel. */
internal fun Modifier.spritePanel(radius: Dp = 28.dp): Modifier = this
    .background(Brush.verticalGradient(listOf(Color(0xFF2B1870), Color(0xFF1A0E45))), RoundedCornerShape(radius))
    .border(2.dp, Line, RoundedCornerShape(radius))

// ─────────────────────────────── Cold-start intro ───────────────────────────────

// ─────────────────────────────── Welcome / what's new ───────────────────────────────

// ─────────────────────────────── Celebrations ───────────────────────────────

/** Drops a banner from the top for every achievement unlocked and when the daily challenge is done. */
@Composable
internal fun ProgressCelebrations() {
    var current by remember { mutableStateOf<ProgressEvent?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        Progress.events.collect { e ->
            current = e
            visible = true
            com.avfusionapps.game_2048.shared.game.SpriteSfx.play(com.avfusionapps.game_2048.shared.game.SpriteSfx.Sound.CHIME)
            if (e is ProgressEvent.Unlocked) com.avfusionapps.game_2048.shared.game.SpriteSfx.play(com.avfusionapps.game_2048.shared.game.SpriteSfx.Sound.COIN, delayMs = 420)
            delay(2600)
            visible = false
            delay(400)
        }
    }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(320, easing = EaseOut)) { -it } + fadeIn(),
            exit = slideOutVertically(tween(280)) { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            val e = current
            Box {
                Row(
                    Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .drawBehind { drawRoundRect(Color(0xFF8A4A08), topLeft = Offset(0f, 5.dp.toPx()), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx())) }
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFD25A), Color(0xFFFF9A1F))), RoundedCornerShape(22.dp))
                        .border(2.dp, Color(0xFFFFE7A6), RoundedCornerShape(22.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (e) {
                        is ProgressEvent.Unlocked -> {
                            Medal(e.achievement, unlocked = true, size = 50.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("ACHIEVEMENT UNLOCKED", color = Color(0xFF7A3A08), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(e.achievement.title, color = Color(0xFF3A1400), fontFamily = displayFont(), fontSize = 20.sp)
                                Text("+${e.achievement.tier.coins} coins", color = Color(0xFF5A2A06), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        is ProgressEvent.DailyDone -> {
                            SpriteTile(value = 8, modifier = Modifier.size(50.dp), showNumber = false, emotion = SpriteEmotion.PROUD)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("DAILY CHALLENGE DONE", color = Color(0xFF7A3A08), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(e.challenge.title, color = Color(0xFF3A1400), fontFamily = displayFont(), fontSize = 18.sp, maxLines = 2)
                                Text("Claim ${e.challenge.reward} coins on the home screen", color = Color(0xFF5A2A06), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        null -> Unit
                    }
                }
                if (visible) ConfettiBurst(18)
            }
        }
    }
}

private fun tierColors(t: Tier): Pair<Color, Color> = when (t) {
    Tier.BRONZE -> Color(0xFFF3B07A) to Color(0xFF9A5426)
    Tier.SILVER -> Color(0xFFF2F0FF) to Color(0xFF8C8AB4)
    Tier.GOLD -> Color(0xFFFFF3A6) to Color(0xFFE5850F)
}

/** An achievement medal: a ringed disc in its tier's metal with the sprite inside (a silhouette while locked). */
@Composable
internal fun Medal(a: Achievement, unlocked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val (light, dark) = tierColors(a.tier)
    Box(
        modifier
            .size(size)
            .drawBehind {
                val r = this.size.minDimension / 2f
                if (unlocked) drawCircle(light.copy(alpha = 0.35f), radius = r * 1.08f)
                drawCircle(Brush.verticalGradient(if (unlocked) listOf(light, dark) else listOf(Color(0xFF4A3E70), Color(0xFF221A40))), radius = r)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF3A1C80), Color(0xFF1A0E45))), radius = r * 0.8f)
                drawCircle(Color.White.copy(alpha = if (unlocked) 0.5f else 0.12f), radius = r * 0.8f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            },
        contentAlignment = Alignment.Center
    ) {
        SpriteTile(value = a.sprite, modifier = Modifier.size(size * 0.7f), showCard = false, showNumber = false, showAura = false, showEffects = false, locked = !unlocked, emotion = SpriteEmotion.HAPPY, still = true)
    }
}
