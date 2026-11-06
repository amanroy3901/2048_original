package com.avfusionapps.game_2048.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.components.CoinIcon
import com.avfusionapps.game_2048.ui.sprites.SpriteDisplayFont
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.ui.sprites.SpriteMotion
import com.avfusionapps.game_2048.ui.sprites.SpriteMotionKind
import com.avfusionapps.game_2048.ui.sprites.SpriteNumberFont
import com.avfusionapps.game_2048.ui.sprites.SpriteTile
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.random.Random

// The prototypes' night-magic palette.
private val Muted = Color(0xFFC9B8F0)
private val Line = Color(0x52BE96FF)
private val PanelDark = Color(0xB80C0522)
private val Outline = Color(0xFF2A0F4A)
private val GoldTop = Color(0xFFFFF3A6)
private val Gold = Color(0xFFFFC93A)
private val GoldBottom = Color(0xFFE5850F)
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()
private val crownPath by lazy { svg("M3 20 L2 6 L8 11 L14 2 L20 11 L26 6 L25 20 Z") }
private val laurelArcs by lazy { svg("M2 6 Q10 1 20 5 M42 5 Q52 1 60 6") }
private val laurelTicks by lazy { svg("M8 4 l2 -3 M14 3.5 l2 -3 M48 3.5 l-2 -3 M54 4 l-2 -3") }
private val hillBack by lazy { svg("M0 90 Q60 40 120 80 T240 70 T400 60 V140 H0 Z") }
private val hillFront by lazy { svg("M0 110 Q80 70 170 105 T400 95 V140 H0 Z") }
private val soundBody by lazy { svg("M4 9 H8 L13 5 V19 L8 15 H4 Z") }
private val soundWaves by lazy { svg("M16 8.5 Q18.5 12 16 15.5 M18.5 6 Q22.5 12 18.5 18") }
private val soundOff by lazy { svg("M16 9 L21 15 M21 9 L16 15") }
private val giftBox by lazy { svg("M5 9 H19 A2 2 0 0 1 21 11 V19 A2 2 0 0 1 19 21 H5 A2 2 0 0 1 3 19 V11 A2 2 0 0 1 5 9 Z") }
private val giftLid by lazy { svg("M3.5 6.5 H20.5 A1.5 1.5 0 0 1 22 8 V9.5 A1.5 1.5 0 0 1 20.5 11 H3.5 A1.5 1.5 0 0 1 2 9.5 V8 A1.5 1.5 0 0 1 3.5 6.5 Z") }
private val giftBow by lazy { svg("M12 6.5 C9 2 5 4 8 6.5 M12 6.5 C15 2 19 4 16 6.5") }
private val trophyCup by lazy { svg("M9 4 H23 V12 Q23 19 16 19 Q9 19 9 12 Z") }
private val trophyHandles by lazy { svg("M9 7 H4 Q4 13 9 13 M23 7 H28 Q28 13 23 13") }
private val leafIcon by lazy { svg("M16 28 Q15 18 6 10 Q18 6 24 14 Q28 20 16 28 Z") }
private val leafVein by lazy { svg("M16 27 Q17 18 10 12") }
private val starIcon by lazy { svg("M16 8 L18.4 13.2 L24 13.6 L19.7 17.2 L21 22.6 L16 19.6 L11 22.6 L12.3 17.2 L8 13.6 L13.6 13.2 Z") }
private val bulbIcon by lazy { svg("M20 5 C13 5 9 10 9 15.5 C9 19.5 11.5 22 13.5 24 V28 H26.5 V24 C28.5 22 31 19.5 31 15.5 C31 10 27 5 20 5 Z") }

/** Draws an SVG-unit drawing ([box] units square) fitted into this scope. */
private inline fun DrawScope.fit(box: Float, block: DrawScope.() -> Unit) {
    val s = size.minDimension / box
    translate((size.width - box * s) / 2f, (size.height - box * s) / 2f) { scale(s, s, pivot = Offset.Zero) { block() } }
}

private fun DrawScope.drawCrown() = fit(28f) {
    drawPath(crownPath, Brush.verticalGradient(listOf(GoldTop, Gold, GoldBottom), startY = 2f, endY = 20f))
    drawPath(crownPath, Color(0xFF8A4A08), style = Stroke(1.4f, join = StrokeJoin.Round))
}

private fun cycle(t: Float, period: Float, delay: Float = 0f): Float {
    val x = (t - delay) / period
    return x - floor(x)
}

private fun pingPong(c: Float): Float = EaseInOut.transform(if (c < 0.5f) c * 2f else (1f - c) * 2f)

// ─────────────────────────────── Background ───────────────────────────────

/** The prototypes' phone background as a brush, for screens that just need the colour. */
internal val NightBrush = Brush.verticalGradient(0f to Color(0xFF2B1870), 0.45f to Color(0xFF1A0E45), 1f to Color(0xFF140A33))

/** A title for the player from the biggest sprite they have raised. */
internal fun spriteRank(biggest: Int): String = when {
    biggest >= 2048 -> "Ember Royalty"
    biggest >= 1024 -> "Crystal Sage"
    biggest >= 256 -> "Star Herder"
    biggest >= 64 -> "Blaze Keeper"
    biggest >= 8 -> "Sprite Tamer"
    else -> "Drip Sitter"
}

/**
 * The prototypes' phone background: a deep violet gradient. With [scene], the v3 night sky too:
 * thirty twinkling stars in the top half, dark hills and a crescent moon behind the hero banner.
 */
@Composable
internal fun SpriteNightBackground(modifier: Modifier = Modifier, scene: Boolean, hillsTop: Dp = 106.dp, moon: Boolean = true) {
    val stars = remember {
        val rnd = Random(2048)
        List(30) { Triple(rnd.nextFloat(), rnd.nextFloat() * 0.45f, 1f + rnd.nextFloat() * 2.2f) to rnd.nextFloat() * 3f }
    }
    val clock = com.avfusionapps.game_2048.ui.sprites.spriteTime()
    // Sky, hills and moon are drawn once; only the small star layer redraws as the stars twinkle.
    Box(modifier) {
    Canvas(Modifier.matchParentSize()) {
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF2B1870), 0.45f to Color(0xFF1A0E45), 1f to Color(0xFF140A33)
            )
        )
        if (!scene) return@Canvas
        // hills and moon (the prototype's 400 × 140 scene, stretched across the width)
        val top = hillsTop.toPx()
        val sx = size.width / 400f
        val sy = size.width * 0.35f / 140f
        translate(0f, top) {
            scale(sx, sy, pivot = Offset.Zero) {
                drawPath(hillBack, Color(0xFF241356), alpha = 0.9f)
                drawPath(hillFront, Color(0xFF1A0D42))
            }
            if (moon) {
                drawCircle(Color(0xFFFFF2C6).copy(alpha = 0.85f), radius = 18f * sx, center = Offset(330f * sx, 28f * sy))
                drawCircle(Color(0xFF2B1870), radius = 16f * sx, center = Offset(338f * sx, 22f * sy))
            }
        }
    }
    Canvas(Modifier.matchParentSize()) {
        // stars twinkle .15 ↔ .9 every 3 s
        val t = clock.value
        stars.forEach { (s, d) ->
            val a = 0.15f + 0.75f * pingPong(cycle(t, 3f, -d))
            drawCircle(Color.White.copy(alpha = a), radius = s.third.dp.toPx() / 2f, center = Offset(s.first * size.width, s.second * size.height))
        }
    }
    }
}

// ─────────────────────────────── Classic: header ───────────────────────────────

/** v3 header: Blaze avatar with level and XP bar, the coin purse, and a round sound button. */
@Composable
internal fun SpriteClassicHeader(
    level: Int,
    progress: Float,
    coins: Int,
    soundOn: Boolean,
    onSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .drawBehind { drawCircle(Color(0x80FFB547), radius = size.minDimension / 2f + 6.dp.toPx()) }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF5A2CB8), Color(0xFF2A1366))))
                .border(2.dp, Color(0xFFFFB547), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            SpriteTile(value = 64, modifier = Modifier.size(42.dp), showCard = false, showNumber = false, showAura = false, showEffects = false, still = true)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Lv. $level", color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 15.sp)
            Box(
                modifier = Modifier
                    .widthIn(max = 110.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFB547), Color(0xFFFFE07A))))
                )
            }
        }
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x8C0A041E))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                .padding(start = 8.dp, end = 12.dp)
                .semantics { contentDescription = "$coins coins" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinIcon(Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text("%,d".format(coins), color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        RoundButton(size = 40.dp, description = if (soundOn) "Sound on" else "Sound off", onClick = onSound, modifier = Modifier.graphicsLayer { alpha = if (soundOn) 1f else 0.55f }) {
            fit(24f) {
                drawPath(soundBody, Color.White)
                if (soundOn) drawPath(soundWaves, Color.White, style = Stroke(2f, cap = StrokeCap.Round))
                else drawPath(soundOff, Color.White, style = Stroke(2f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
internal fun RoundButton(size: Dp, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: DrawScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(size)
            .scale(if (pressed) 0.9f else 1f)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF6A3BD0), Color(0xFF341A86))))
            .border(2.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.5f)) { icon() }
    }
}

// ─────────────────────────────── Classic: hero banner ───────────────────────────────

/**
 * v3 hero: the gold "2048 / Elemental Sprites" title, Blaze on a glowing stone pedestal with four sprites
 * floating around him, and the Score and Best panels with their crowns. [heroEmotion] and [hopKey] let
 * the narration drive Blaze; [reactValue]/[reactKey] make the matching side sprite hop after a merge.
 */
@Composable
internal fun SpriteHero(
    score: Int,
    best: Int,
    heroEmotion: SpriteEmotion,
    hopKey: Int,
    reactValue: Int,
    reactKey: Int,
    height: Dp,
    modifier: Modifier = Modifier,
    showScores: Boolean = true,
    maxScale: Float = 1.15f
) {
    val clock = com.avfusionapps.game_2048.ui.sprites.spriteTime()
    // Side sprites: Drip, Sprig, Starla, Orbi (left to right), floating on a 3.2 s loop.
    val sides = remember { listOf(SideSprite(2, 48.dp, 0.02f, true, 86.dp, 0f), SideSprite(4, 44.dp, 0.14f, true, 100.dp, -1f), SideSprite(512, 44.dp, 0.14f, false, 100.dp, -2f), SideSprite(32, 48.dp, 0.02f, false, 86.dp, -0.5f)) }
    // Side fidgets: now and then one of them wiggles or hops, like on the board.
    var sideMotion by remember { mutableStateOf<Pair<Int, SpriteMotion>?>(null) }
    var motionKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1900)
            if (Random.nextFloat() < 0.35f) sideMotion = Random.nextInt(4) to SpriteMotion(SpriteMotionKind.FIDGET, ++motionKey + 10_000, variant = Random.nextInt(4))
        }
    }
    LaunchedEffect(reactKey) {
        if (reactKey == 0) return@LaunchedEffect
        val k = sides.indexOfFirst { it.value == reactValue }
        if (k >= 0) sideMotion = k to SpriteMotion(SpriteMotionKind.HOP, ++motionKey + 10_000, hop = 30f)
    }
    var sideHappy by remember { mutableIntStateOf(-1) }
    LaunchedEffect(reactKey) {
        val k = sides.indexOfFirst { it.value == reactValue }
        if (reactKey == 0 || k < 0) return@LaunchedEffect
        sideHappy = k
        delay(900)
        sideHappy = -1
    }
    val scale = (height.value / 224f).coerceIn(0.75f, maxScale)
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(height)) {
        val w = maxWidth
        // title (Blaze stands below it; on tall banners there is room to spare)
        Column(Modifier.align(Alignment.TopCenter).offset(y = if (height > 240.dp) (-6).dp else 0.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Text("2048", fontFamily = SpriteDisplayFont, fontSize = (54 * scale).sp, lineHeight = (49 * scale).sp, letterSpacing = 1.sp, color = Color(0xFF7A2A08), modifier = Modifier.offset(y = 3.dp))
                Text(
                    "2048", fontFamily = SpriteDisplayFont, fontSize = (54 * scale).sp, lineHeight = (49 * scale).sp, letterSpacing = 1.sp,
                    style = TextStyle(
                        brush = Brush.verticalGradient(0.1f to Color(0xFFFFF6B0), 0.45f to Gold, 0.9f to Color(0xFFFF8A1F)),
                        shadow = Shadow(Color(0x80FFAA3C), Offset.Zero, 20f)
                    )
                )
            }
            Text(
                "Elemental Sprites", fontFamily = SpriteDisplayFont, fontSize = (25 * scale).sp, lineHeight = (23 * scale).sp, color = Color.White,
                style = TextStyle(shadow = Shadow(Color(0xFF4A1C9A), Offset(0f, 4f), 0f))
            )
        }
        // pedestal
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-8).dp)
                .size(150.dp * scale, 34.dp * scale)
                .drawBehind {
                    drawOval(Color(0x73FF8C3C), topLeft = Offset(-18.dp.toPx(), -14.dp.toPx()), size = Size(size.width + 36.dp.toPx(), size.height + 28.dp.toPx()), alpha = 0.5f)
                    drawOval(Brush.radialGradient(0f to Color(0xFF7C6AA8), 0.7f to Color(0xFF3B2D63), center = Offset(size.width / 2f, size.height * 0.3f), radius = size.width * 0.6f))
                    drawArc(Color.White.copy(alpha = 0.18f), 200f, 140f, false, topLeft = Offset(size.width * 0.06f, 2.dp.toPx()), size = Size(size.width * 0.88f, size.height * 0.9f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
        )
        // side sprites
        sides.forEachIndexed { k, s ->
            val x = if (s.left) w * s.x else w - w * s.x - s.size * scale
            Box(
                Modifier
                    .offset(x = x, y = height - s.bottom * scale - s.size * scale)
                    // read the clock in the layer, so the bob moves the layer without recomposing the banner
                    .graphicsLayer { translationY = -7f * pingPong(cycle(clock.value, 3.2f, s.delay)) * density }
                    .size(s.size * scale)
            ) {
                SpriteTile(
                    value = s.value, modifier = Modifier.fillMaxSize(), showCard = false, showNumber = false, showEffects = false,
                    emotion = if (sideHappy == k) SpriteEmotion.HAPPY else SpriteEmotion.IDLE,
                    motion = sideMotion?.takeIf { it.first == k }?.second
                )
            }
        }
        // Blaze
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-22).dp * scale)
                .size(124.dp * scale)
        ) {
            SpriteTile(
                value = 64, modifier = Modifier.fillMaxSize(), showCard = false, showNumber = false, showEffects = false,
                emotion = heroEmotion,
                motion = if (hopKey > 0) SpriteMotion(SpriteMotionKind.HOP, hopKey + 20_000, hop = 10f) else null
            )
        }
        // score panels
        if (showScores) {
            ScoreBox("Score", score, Modifier.align(Alignment.BottomStart))
            ScoreBox("Best", best, Modifier.align(Alignment.BottomEnd))
        }
    }
}

private class SideSprite(val value: Int, val size: Dp, val x: Float, val left: Boolean, val bottom: Dp, val delay: Float)

@Composable
internal fun ScoreBox(label: String, value: Int, modifier: Modifier) {
    Box(modifier = modifier.width(104.dp)) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .drawBehind { drawRoundRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(0f, 6.dp.toPx()), size = size, cornerRadius = CornerRadius(18.dp.toPx())) }
                .clip(RoundedCornerShape(18.dp))
                .background(PanelDark)
                .border(2.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                .padding(top = 16.dp, start = 6.dp, end = 6.dp, bottom = 6.dp)
                .semantics { contentDescription = "$label $value" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
            Text("%,d".format(value), color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 24.sp, maxLines = 1)
        }
        Canvas(Modifier.align(Alignment.TopCenter).size(28.dp, 22.dp)) { drawCrown() }
    }
}

// ─────────────────────────────── Classic: narrator row ───────────────────────────────

/** v3 narrator: Blaze's speech bubble, then the combo tag (pink when hot) and the freeze / rock tags. */
@Composable
internal fun SpriteNarratorRow(text: String, textKey: Int, combo: Int, freezeMoves: Int, rockTtl: Int, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(textKey) {
        pop.snapTo(0f)
        pop.animateTo(1f, tween(240, easing = LinearEasing))
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = text,
            color = Color(0xFFF6F0FF),
            fontSize = 14.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    val p = pop.value
                    val s = if (p < 0.5f) 0.96f + 0.12f * p else 1.02f - 0.04f * (p - 0.5f)
                    scaleX = s
                    scaleY = s
                }
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PanelDark)
                .border(1.dp, Line, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )
        val hot = combo >= 2
        Tag(
            background = if (hot) Brush.verticalGradient(listOf(Color(0xFFFF7AB6), Color(0xFFE0337A))) else Brush.verticalGradient(listOf(PanelDark, PanelDark)),
            border = if (hot) Color(0xFFFFB0D2) else Line
        ) {
            Text("x${combo.coerceIn(1, 5)}", color = if (hot) Color.White else Muted, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        if (freezeMoves > 0) {
            Tag(background = Brush.verticalGradient(listOf(Color(0xFFA6ECFF), Color(0xFF3C8EDB))), border = Color(0xFFE8FBFF)) {
                Canvas(Modifier.size(14.dp)) { drawFreezeIcon(Color(0xFF0E2A5A)) }
                Spacer(Modifier.width(4.dp))
                Text("$freezeMoves", color = Color(0xFF0E2A5A), fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
        if (rockTtl > 0) {
            Tag(background = Brush.verticalGradient(listOf(Color(0xFF5B5470), Color(0xFF2E2840))), border = Color(0xFF8A80A8)) {
                Text("Rock $rockTtl", color = Color(0xFFE9DDFF), fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun Tag(background: Brush, border: Color, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

// ─────────────────────────────── Classic: power-ups ───────────────────────────────

internal class SpritePowerButton(
    val label: String,
    val charges: Int,
    val top: Color,
    val bottom: Color,
    val glow: Color,
    val active: Boolean = false,
    val onClick: () -> Unit,
    val icon: DrawScope.() -> Unit
)

/** v3 power-ups: big glossy squares with a glow, a light rim, and their charges (or price) on a badge. */
@Composable
internal fun SpritePowerRow(buttons: List<SpritePowerButton>, cost: Int, modifier: Modifier = Modifier) {
    val clock = com.avfusionapps.game_2048.ui.sprites.spriteTime()
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
        buttons.forEach { b ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val buy = b.charges <= 0
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .testTag("PowerUp_${b.label}")
                    .semantics { contentDescription = if (buy) "${b.label}, buy one for $cost coins" else "${b.label}, ${b.charges} left" }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(if (pressed) 0.9f else 1f)
                        .drawBehind {
                            val k = if (b.active) pingPong(cycle(clock.value, 1f)) else 0f
                            val blur = (14f + 12f * k).dp.toPx()
                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = b.glow.toArgb()
                                maskFilter = android.graphics.BlurMaskFilter(blur * 0.5f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                            }
                            drawContext.canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, 18.dp.toPx(), 18.dp.toPx(), paint)
                            if (k > 0f) drawRoundRect(Color.White.copy(alpha = 0.4f * k), topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()), size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), cornerRadius = CornerRadius(22.dp.toPx()), style = Stroke(4.dp.toPx() * k))
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(b.top, b.bottom), start = Offset(0f, 0f), end = Offset(120f, 190f)))
                        .drawBehind {
                            drawRect(Color.White.copy(alpha = 0.35f), size = Size(size.width, 3.dp.toPx()))
                            drawRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(0f, size.height - 5.dp.toPx()), size = Size(size.width, 5.dp.toPx()))
                        }
                        .border(2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .clickable(interactionSource = interaction, indication = null, onClick = b.onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(34.dp)) {
                        translate(0f, 2.dp.toPx()) { b.icon(this) }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                        .widthIn(min = 26.dp)
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(if (buy) Gold else Color.White)
                        .border(2.dp, Outline, CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (buy) "$cost" else "${b.charges}", color = Outline, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = if (buy) 12.sp else 15.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

internal fun DrawScope.drawHintIcon() = fit(40f) {
    drawPath(bulbIcon, Color.White)
    drawRoundRect(Color.White, topLeft = Offset(14f, 30f), size = Size(12f, 3.4f), cornerRadius = CornerRadius(1.7f))
    drawRoundRect(Color.White, topLeft = Offset(15.5f, 34.6f), size = Size(9f, 3f), cornerRadius = CornerRadius(1.5f))
}

// ─────────────────────────────── Classic: bottom menu ───────────────────────────────

internal class SpriteNavItem(val label: String, val onClick: () -> Unit, val icon: DrawScope.() -> Unit)

/** v3 bottom menu: five icon buttons on a dark rounded bar. */
@Composable
internal fun SpriteBottomNav(items: List<SpriteNavItem>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x990A041E))
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = item.onClick)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Canvas(Modifier.size(26.dp)) { item.icon(this) }
                Spacer(Modifier.height(2.dp))
                Text(item.label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

internal fun DrawScope.navMenuIcon() = fit(32f) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFF8A5BFF), Color(0xFF5A2CB8))), radius = 12f, center = Offset(16f, 16f))
    drawCircle(Gold, radius = 12f, center = Offset(16f, 16f), style = Stroke(1.6f))
    drawRoundRect(Color.White, topLeft = Offset(11.5f, 10f), size = Size(3.4f, 12f), cornerRadius = CornerRadius(1.4f))
    drawRoundRect(Color.White, topLeft = Offset(17.1f, 10f), size = Size(3.4f, 12f), cornerRadius = CornerRadius(1.4f))
}

internal fun DrawScope.navHelpIcon() = fit(32f) {
    drawCircle(Color(0xFFFF8A2A), radius = 12f, center = Offset(16f, 16f))
    drawCircle(Color(0xFF8A2A08), radius = 12f, center = Offset(16f, 16f), style = Stroke(1.4f))
    drawPath(starIcon, Color(0xFFFFF3A6))
}

internal fun DrawScope.navCollectionIcon() = fit(32f) {
    drawPath(leafIcon, Brush.linearGradient(listOf(Color(0xFFD4FF8A), Color(0xFF3FA52C)), start = Offset(6f, 6f), end = Offset(28f, 28f)))
    drawPath(leafIcon, Color(0xFF2E6E1A), style = Stroke(1.4f))
    drawPath(leafVein, Color(0xFF2E6E1A), style = Stroke(1.4f))
}

internal fun DrawScope.navProfileIcon() = fit(32f) {
    drawRoundRect(Color(0xFFB57BFF), topLeft = Offset(5f, 16f), size = Size(5f, 11f), cornerRadius = CornerRadius(1.5f))
    drawRoundRect(Color(0xFFD8B9FF), topLeft = Offset(13.5f, 8f), size = Size(5f, 19f), cornerRadius = CornerRadius(1.5f))
    drawRoundRect(Color(0xFF9A5BFF), topLeft = Offset(22f, 12f), size = Size(5f, 15f), cornerRadius = CornerRadius(1.5f))
}

internal fun DrawScope.navTrophyIcon() = fit(32f) {
    drawPath(trophyCup, Brush.verticalGradient(listOf(GoldTop, Gold, GoldBottom), startY = 4f, endY = 19f))
    drawPath(trophyCup, Color(0xFF8A4A08), style = Stroke(1.4f))
    drawPath(trophyHandles, Gold, style = Stroke(2f))
    drawRect(GoldBottom, topLeft = Offset(13f, 19f), size = Size(6f, 5f))
    drawRoundRect(Color(0xFFB5650C), topLeft = Offset(9f, 24f), size = Size(14f, 4f), cornerRadius = CornerRadius(1.5f))
}

// ─────────────────────────────── Neon Shooter header ───────────────────────────────

/**
 * v4 header: a pause button, then Score (with a gold laurel), Best (with a crown), Combo (gold gradient
 * number, glowing when hot) and Next goal (a gift and the target, pulsing gold when it is within reach).
 */
@Composable
internal fun SpriteShooterHud(
    score: Int,
    best: Int,
    combo: Int,
    goalTarget: Int,
    goalNear: Boolean,
    height: Dp,
    onPause: () -> Unit,
    goalModifier: Modifier = Modifier
) {
    val clock = com.avfusionapps.game_2048.ui.sprites.spriteTime()
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF6A3BD0), Color(0xFF341A86))))
                .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .clickable(interactionSource = interaction, indication = null, onClick = onPause)
                .semantics { contentDescription = "Pause and settings" }
                .testTag("DropMerge_Button_Pause"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(22.dp)) {
                fit(24f) {
                    drawRoundRect(Color.White, topLeft = Offset(6f, 5f), size = Size(4f, 14f), cornerRadius = CornerRadius(1.5f))
                    drawRoundRect(Color.White, topLeft = Offset(14f, 5f), size = Size(4f, 14f), cornerRadius = CornerRadius(1.5f))
                }
            }
        }
        HudBox(Modifier.weight(1f)) {
            HudLabel("Score")
            HudNumber("%,d".format(score))
            Canvas(Modifier.size(62.dp, 9.dp)) {
                scale(size.width / 62f, size.height / 9f, pivot = Offset.Zero) {
                    drawPath(laurelArcs, Gold, style = Stroke(1.6f, cap = StrokeCap.Round))
                    drawCircle(Gold, 2f, Offset(31f, 5f))
                    drawPath(laurelTicks, Gold, style = Stroke(1.4f, cap = StrokeCap.Round))
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            HudBox(Modifier.fillMaxSize(), topPad = 10.dp) {
                HudLabel("Best")
                HudNumber("%,d".format(best))
            }
            Canvas(Modifier.align(Alignment.TopCenter).offset(y = (-11).dp).size(24.dp, 18.dp)) { drawCrown() }
        }
        val hot = combo >= 2
        HudBox(Modifier.weight(1f), border = if (hot) Color(0xFFFFB25A) else Color(0x80FF9A1F), glow = if (hot) Color(0xB3FF9A1F) else null) {
            HudLabel("Combo")
            Box {
                Text("×${combo.coerceAtLeast(1)}", fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 26.sp, color = Color(0xFF7A2A08), modifier = Modifier.offset(y = 2.dp))
                Text(
                    "×${combo.coerceAtLeast(1)}", fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 26.sp,
                    style = TextStyle(brush = Brush.verticalGradient(listOf(Color(0xFFFFE07A), Color(0xFFFF8A1F))))
                )
            }
        }
        val k = if (goalNear) pingPong(cycle(clock.value, 1f)) else 0f
        HudBox(goalModifier.weight(1f), border = Color(0x80FFC93A), glow = if (k > 0.01f) Gold.copy(alpha = 0.6f * k) else null, ring = 4f * k) {
            HudLabel("Next goal")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Canvas(Modifier.size(20.dp)) {
                    fit(24f) {
                        drawPath(giftBox, Color(0xFFFF5FA2)); drawPath(giftBox, Color(0xFF7A0E28), style = Stroke(1.2f))
                        drawPath(giftLid, Color(0xFFFF8AC0)); drawPath(giftLid, Color(0xFF7A0E28), style = Stroke(1.2f))
                        drawRect(Gold, topLeft = Offset(10.5f, 6.5f), size = Size(3f, 14.5f))
                        drawPath(giftBow, Gold, style = Stroke(1.8f))
                    }
                }
                HudNumber("%,d".format(goalTarget))
            }
        }
    }
}

@Composable
private fun HudBox(
    modifier: Modifier,
    border: Color = Color.White.copy(alpha = 0.16f),
    glow: Color? = null,
    ring: Float = 0f,
    topPad: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .drawBehind {
                if (glow != null) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = glow.toArgb()
                        maskFilter = android.graphics.BlurMaskFilter(9.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }
                    drawContext.canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, 16.dp.toPx(), 16.dp.toPx(), paint)
                }
                if (ring > 0.1f) drawRoundRect(Gold.copy(alpha = 0.55f), topLeft = Offset(-ring.dp.toPx() / 2, -ring.dp.toPx() / 2), size = Size(size.width + ring.dp.toPx(), size.height + ring.dp.toPx()), cornerRadius = CornerRadius(18.dp.toPx()), style = Stroke(ring.dp.toPx()))
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xB80C0522))
            .border(2.dp, border, RoundedCornerShape(16.dp))
            .padding(top = topPad, bottom = 5.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun HudLabel(text: String) {
    Text(text.uppercase(), color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1, lineHeight = 12.sp)
}

@Composable
private fun HudNumber(text: String) {
    Text(text, color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 22.sp, maxLines = 1)
}
