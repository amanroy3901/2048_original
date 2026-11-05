package com.avfusionapps.game_2048.ui.screens

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
import com.avfusionapps.game_2048.data.progress.Achievement
import com.avfusionapps.game_2048.data.progress.Progress
import com.avfusionapps.game_2048.data.progress.ProgressEvent
import com.avfusionapps.game_2048.data.progress.Tier
import com.avfusionapps.game_2048.ui.sprites.ConfettiBurst
import com.avfusionapps.game_2048.ui.sprites.ElementalSprites
import com.avfusionapps.game_2048.ui.sprites.SpriteDisplayFont
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.ui.sprites.SpriteMotion
import com.avfusionapps.game_2048.ui.sprites.SpriteMotionKind
import com.avfusionapps.game_2048.ui.sprites.SpriteNumberFont
import com.avfusionapps.game_2048.ui.sprites.SpriteTile
import kotlinx.coroutines.delay

private val Muted = Color(0xFFC9B8F0)
private val Line = Color(0x52BE96FF)
private val Gold = Color(0xFFFFC93A)
private val Bounce = CubicBezierEasing(0.3f, 1.6f, 0.5f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/** What the app is showing over everything at start-up, so screens hold their own prompts until it is done. */
internal object StartupFlow {
    val introShowing = androidx.compose.runtime.mutableStateOf(false)
    val welcomeShowing = androidx.compose.runtime.mutableStateOf(false)
    val busy get() = introShowing.value || welcomeShowing.value
}

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

/**
 * A two-second hello on every cold start: a portal opens, Blaze pops out and hops, four friends pop up
 * beside him, and the gold title drops in. Tap to skip.
 */
@Composable
internal fun SpriteSplashIntro(onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    var finishing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        clock.animateTo(2300f, tween(2300, easing = LinearEasing))
        finishing = true
    }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(finishing) {
        if (!finishing) return@LaunchedEffect
        fade.animateTo(0f, tween(320))
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finishing = true }
    ) {
        SpriteNightBackground(Modifier.fillMaxSize(), scene = true, hillsTop = 480.dp)
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val ms = clock.value
            // title drops in at 900 ms with a bounce
            val tp = ((ms - 900f) / 520f).coerceIn(0f, 1f)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.16f)
                    .graphicsLayer {
                        translationY = -260f * (1f - Bounce.transform(tp))
                        alpha = (tp * 3f).coerceAtMost(1f)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GoldTitle("2048", 76f)
                Text(
                    "Elemental Sprites", fontFamily = SpriteDisplayFont, fontSize = 32.sp, color = Color.White,
                    style = TextStyle(shadow = Shadow(Color(0xFF4A1C9A), Offset(0f, 5f), 0f)),
                    modifier = Modifier.graphicsLayer { alpha = ((ms - 1150f) / 300f).coerceIn(0f, 1f) }
                )
            }
            // Blaze out of a portal at the centre
            Box(Modifier.align(Alignment.Center).offset(y = 40.dp).size(170.dp)) {
                SpriteTile(
                    value = 64, modifier = Modifier.fillMaxSize(), showCard = false, showNumber = false,
                    spawnKey = 1, spawnDelayMs = 150,
                    emotion = if (ms > 1300f) SpriteEmotion.HAPPY else SpriteEmotion.SURPRISED,
                    motion = if (ms > 1300f) SpriteMotion(SpriteMotionKind.HOP, 1, hop = 18f) else null
                )
            }
            // friends pop up in a row
            Row(
                Modifier.align(Alignment.Center).offset(y = 190.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(2, 4, 8, 16, 32).forEachIndexed { k, v ->
                    Box(Modifier.size(56.dp)) {
                        SpriteTile(value = v, modifier = Modifier.fillMaxSize(), showCard = false, showNumber = false, spawnKey = 1, spawnDelayMs = 650 + k * 110, emotion = SpriteEmotion.EXCITED)
                    }
                }
            }
        }
    }
}

@Composable
internal fun GoldTitle(text: String, size: Float, modifier: Modifier = Modifier) {
    Box(modifier) {
        Text(text, fontFamily = SpriteDisplayFont, fontSize = size.sp, letterSpacing = 1.sp, color = Color(0xFF7A2A08), modifier = Modifier.offset(y = (size / 18f).dp))
        Text(
            text, fontFamily = SpriteDisplayFont, fontSize = size.sp, letterSpacing = 1.sp,
            style = TextStyle(
                brush = Brush.verticalGradient(0.1f to Color(0xFFFFF6B0), 0.45f to Gold, 0.9f to Color(0xFFFF8A1F)),
                shadow = Shadow(Color(0x80FFAA3C), Offset.Zero, 24f)
            )
        )
    }
}

// ─────────────────────────────── Welcome / what's new ───────────────────────────────

/**
 * Shown once. Players coming from 2048 Neon Rush learn that their tiles have come alive (their scores and
 * saved game are kept); new players get the same tour as a welcome.
 */
@Composable
internal fun ElementalWelcome(updating: Boolean, onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(20.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .spritePanel()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (updating) "2048 NEON RUSH IS NOW" else "WELCOME TO",
                color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            GoldTitle("2048", 54f)
            Text("Elemental Sprites", fontFamily = SpriteDisplayFont, fontSize = 26.sp, color = Color.White, style = TextStyle(shadow = Shadow(Color(0xFF4A1C9A), Offset(0f, 4f), 0f)))
            Spacer(Modifier.height(14.dp))
            SpriteParade()
            Spacer(Modifier.height(14.dp))
            Text(
                if (updating) "Your tiles have come alive! Every number is now a sprite that grows up as you merge, from little Drip to the Ember King. Your best score and saved game are safe."
                else "Every tile is a sprite that grows up as you merge, from little Drip to the Ember King. Slide them together and watch them evolve.",
                color = Color(0xFFF6F0FF), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            listOf(
                Triple(64, "11 sprites to collect", "They blink, giggle when poked, and fall asleep if you wait."),
                Triple(8, "Daily challenge", "A new goal every day, with coins and a streak bonus."),
                Triple(512, "Achievements", "24 medals to earn across all modes."),
                Triple(16, "Power-ups", "Undo, Hammer, Shuffle and Freeze get you out of a jam.")
            ).forEach { (v, title, body) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color(0x800A041E), RoundedCornerShape(16.dp))
                        .border(1.dp, Line, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpriteTile(value = v, modifier = Modifier.size(44.dp), showNumber = false)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(body, color = Muted, fontSize = 13.sp, lineHeight = 17.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            SpriteGoldButton("Let's play!", onDone, Modifier.fillMaxWidth())
        }
    }
}

/** All eleven sprites hopping in a wave. */
@Composable
private fun SpriteParade() {
    var wave by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(2600); wave++ }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val size = (maxWidth / 6.2f).coerceAtMost(52.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            listOf(ElementalSprites.all.take(6), ElementalSprites.all.drop(6)).forEachIndexed { row, specs ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    specs.forEachIndexed { k, _ ->
                        val i = row * 6 + k
                        val value = 2 shl i
                        var hop by remember { mutableStateOf<SpriteMotion?>(null) }
                        LaunchedEffect(wave) {
                            delay(i * 90L)
                            hop = SpriteMotion(SpriteMotionKind.HOP, wave * 100 + i + 1, hop = 16f)
                        }
                        SpriteTile(value = value, modifier = Modifier.size(size), showCard = false, showNumber = false, motion = hop, emotion = SpriteEmotion.HAPPY)
                    }
                }
            }
        }
    }
}

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
                                Text(e.achievement.title, color = Color(0xFF3A1400), fontFamily = SpriteDisplayFont, fontSize = 20.sp)
                                Text("+${e.achievement.tier.coins} coins", color = Color(0xFF5A2A06), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        is ProgressEvent.DailyDone -> {
                            SpriteTile(value = 8, modifier = Modifier.size(50.dp), showNumber = false, emotion = SpriteEmotion.PROUD)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("DAILY CHALLENGE DONE", color = Color(0xFF7A3A08), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(e.challenge.title, color = Color(0xFF3A1400), fontFamily = SpriteDisplayFont, fontSize = 18.sp, maxLines = 2)
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
        SpriteTile(value = a.sprite, modifier = Modifier.size(size * 0.7f), showCard = false, showNumber = false, showAura = false, showEffects = false, locked = !unlocked, emotion = SpriteEmotion.HAPPY)
    }
}
