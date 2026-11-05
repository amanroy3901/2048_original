package com.avfusionapps.game_2048.shared.sprites

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.shared.ui.displayFont
import com.avfusionapps.game_2048.shared.ui.numberFont
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The prototype's display face (banners, "Combo ×3"). */
val SpriteDisplayFont: FontFamily
    @Composable get() = displayFont()

/** The prototype's number face: Baloo 2 ExtraBold (Latin subset, SIL OFL; licence in assets/licenses). */
val SpriteNumberFont: FontFamily
    @Composable get() = numberFont()

private val Outline = Color(0xFF2A0F4A)
private val FxEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/**
 * Text that pops up at [at] and floats away, like the prototype's .ft: it grows from 60 % to 110 % while rising
 * 90 % of its height, then drifts another (100 + [rise]) % and fades. Starts [startMs] after [clock] begins.
 */
@Composable
fun SpriteFloatText(
    text: String,
    at: Offset,
    clock: Animatable<Float, *>,
    startMs: Float,
    durationMs: Float,
    rise: Float,
    color: Color,
    fontSize: TextUnit,
    display: Boolean = false
) {
    var height by remember { mutableIntStateOf(0) }
    var width by remember { mutableIntStateOf(0) }
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontFamily = if (display) SpriteDisplayFont else SpriteNumberFont,
        fontWeight = if (display) FontWeight.Normal else FontWeight.ExtraBold,
        maxLines = 1,
        softWrap = false,
        style = TextStyle(shadow = Shadow(Outline, Offset(0f, 4f), 3f)),
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .onSizeChanged { height = it.height; width = it.width }
            .offset { IntOffset((at.x - width / 2f).toInt(), (at.y - height / 2f).toInt()) }
            .graphicsLayer {
                val p = ((clock.value - startMs) / durationMs).coerceIn(0f, 1f)
                if (clock.value < startMs || p >= 1f) {
                    alpha = 0f
                    return@graphicsLayer
                }
                val e = FxEaseOut.transform(p)
                val ty: Float
                val s: Float
                val a: Float
                if (e < 0.25f) {
                    val k = e / 0.25f
                    ty = -0.9f * k; s = 0.6f + 0.5f * k; a = k
                } else {
                    val k = (e - 0.25f) / 0.75f
                    ty = -0.9f - (0.1f + rise / 100f) * k; s = 1.1f - 0.1f * k; a = 1f - k
                }
                translationY = ty * height
                scaleX = s
                scaleY = s
                alpha = a
            }
    )
}

/**
 * The prototype's in-board banner: a glossy pill that bursts in at 38 % of the board's height, holds, then rises
 * and fades (1.8 s). Gold for unlocks and purges, red otherwise.
 */
@Composable
fun BoxScope.SpriteBanner(text: String, gold: Boolean = true) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(1800, easing = LinearEasing)) }
    var height by remember { mutableIntStateOf(0) }
    val top = if (gold) listOf(Color(0xFFFFD25A), Color(0xFFFF9A1F)) else listOf(Color(0xFFFF4D6D), Color(0xFFC21E45))
    val lip = if (gold) Color(0xFF8A4A08) else Color(0xFF7A0E28)
    val glow = if (gold) Color(0x99FFC93A) else Color(0x99FF4D6D)
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.matchParentSize()) {
        val y = maxHeight * 0.38f
        Text(
            text = text,
            color = if (gold) Color(0xFF3A1400) else Color.White,
            fontSize = 26.sp,
            fontFamily = SpriteDisplayFont,
            maxLines = 1,
            softWrap = false,
            style = if (gold) TextStyle.Default else TextStyle(shadow = Shadow(lip, Offset(0f, 4f), 0f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .wrapContentSize(unbounded = true)
                .onSizeChanged { height = it.height }
                .offset { IntOffset(0, (y.toPx() - height / 2f).toInt()) }
                .graphicsLayer {
                    val t = p.value
                    // scale .3 → 1.1 (20 %) → 1 (80 %), then up 20 % of its height and gone
                    val s = when {
                        t < 0.2f -> 0.3f + 0.8f * (t / 0.2f)
                        t < 0.8f -> 1.1f - 0.1f * ((t - 0.2f) / 0.6f)
                        else -> 1f
                    }
                    alpha = when {
                        t < 0.2f -> t / 0.2f
                        t < 0.8f -> 1f
                        else -> 1f - (t - 0.8f) / 0.2f
                    }
                    translationY = if (t > 0.8f) -0.2f * size.height * ((t - 0.8f) / 0.2f) else 0f
                    scaleX = s
                    scaleY = s
                }
                .drawBehind {
                    val r = CornerRadius(18.dp.toPx())
                    drawRoundRect(glow, topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()), size = Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx()), cornerRadius = CornerRadius(24.dp.toPx()), alpha = 0.35f)
                    drawRoundRect(lip, topLeft = Offset(0f, 6.dp.toPx()), size = size, cornerRadius = r)
                    drawRoundRect(Brush.verticalGradient(top), size = size, cornerRadius = r)
                }
                .padding(horizontal = 22.dp, vertical = 10.dp)
        )
    }
}

/** Confetti raining down the board (the prototype's 2048 / unlock celebration). */
@Composable
fun BoxScope.ConfettiBurst(pieces: Int = 40) {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(2500f, tween(2500, easing = LinearEasing)) }
    val seed = remember { Random.nextInt() }
    val colors = remember { listOf(Color(0xFFFFC93A), Color(0xFFFF5FA2), Color(0xFF47C6FF), Color(0xFF8CF05A), Color(0xFFB57BFF), Color.White) }
    val fall = remember { CubicBezierEasing(0.25f, 0.4f, 0.6f, 1f) }
    Canvas(Modifier.matchParentSize()) {
        val ms = clock.value
        if (ms >= 2500f) return@Canvas
        val rnd = Random(seed)
        val pw = 8.dp.toPx()
        val ph = 13.dp.toPx()
        repeat(pieces) { k ->
            val x = rnd.nextFloat() * size.width
            val drift = (rnd.nextFloat() * 2f - 1f) * 60.dp.toPx()
            val spin = 360f + rnd.nextFloat() * 540f
            val dur = 1300f + rnd.nextFloat() * 900f
            val d = rnd.nextFloat() * 250f
            val t = (ms - d) / dur
            if (t in 0f..1f) {
                val e = fall.transform(t)
                val cx = x + drift * e
                val cy = -12.dp.toPx() + (size.height + 42.dp.toPx()) * e
                rotate(spin * e, pivot = Offset(cx, cy)) {
                    drawRoundRect(colors[k % colors.size], topLeft = Offset(cx - pw / 2, cy - ph / 2), size = Size(pw, ph), cornerRadius = CornerRadius(2.dp.toPx()), alpha = if (t < 0.9f) 1f else 0.9f)
                }
            }
        }
    }
}

/**
 * The prototype's sparks: short bright strokes shooting out of [at] and shrinking away (360 ms).
 * [up] fans them upward (an impact); otherwise they ring all the way round (a merge).
 */
fun DrawScope.drawSparks(at: Offset, count: Int, length: Float, p: Float, up: Boolean, seed: Int) {
    if (p <= 0f || p >= 1f) return
    val rnd = Random(seed)
    val e = FxEaseOut.transform(p)
    // translate 0 → .9 len (40 %) → 1.5 len; scaleY .3 → 1 → .2; opacity 1 → 1 → 0
    val (dist, sc, a) = if (e < 0.4f) {
        val k = e / 0.4f
        Triple(0.9f * k, 0.3f + 0.7f * k, 1f)
    } else {
        val k = (e - 0.4f) / 0.6f
        Triple(0.9f + 0.6f * k, 1f - 0.8f * k, 1f - k)
    }
    for (k in 0 until count) {
        val ang = if (up) (-PI / 2 + (k / (count - 1f) - 0.5f) * PI * 1.1).toFloat() else (k.toFloat() / count) * 2f * PI.toFloat()
        val l = length * (0.6f + rnd.nextFloat() * 0.6f)
        val dir = Offset(cos(ang), sin(ang))
        val base = at + dir * (length * dist)
        val tip = base + dir * (l * sc)
        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFFFFD25A).copy(alpha = a), Color.White.copy(alpha = a)), start = base, end = tip),
            start = base,
            end = tip,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/** A full-size canvas that plays [drawSparks] once. */
@Composable
fun BoxScope.SparkBurst(at: Offset, count: Int, length: Float, up: Boolean, delayMs: Int = 0) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        p.animateTo(1f, tween(360, easing = LinearEasing))
    }
    val seed = remember { Random.nextInt() }
    Canvas(Modifier.matchParentSize()) { drawSparks(at, count, length, p.value, up, seed) }
}

/** Bits of [kind] bursting from [at] (merge, purge, freeze): the prototype's emit(). */
@Composable
fun BoxScope.ParticleBurst(
    at: Offset,
    kind: SpriteParticle,
    count: Int,
    spread: Float,
    up: Boolean = false,
    durationMs: Float = 600f,
    delayMs: Int = 0
) {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        clock.animateTo(durationMs + 400f, tween((durationMs + 400f).toInt(), easing = LinearEasing))
    }
    val seed = remember { Random.nextInt() }
    val burst = remember { CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f) }
    Canvas(Modifier.fillMaxSize()) {
        val ms = clock.value
        if (ms <= 0f || ms >= durationMs + 400f) return@Canvas
        val rnd = Random(seed)
        repeat(count) {
            val sz = spread * 0.11f * (0.6f + rnd.nextFloat() * 0.7f)
            val ang = if (up) (-PI / 2 + (rnd.nextFloat() - 0.5f) * 1.2f).toFloat() else rnd.nextFloat() * 2f * PI.toFloat()
            val dist = spread * (0.45f + rnd.nextFloat() * 0.55f)
            val dur = durationMs + rnd.nextFloat() * 300f
            val d = rnd.nextFloat() * 60f
            val rot = rnd.nextFloat() * 360f
            val t = (ms - d) / dur
            if (t in 0f..1f) {
                val e = burst.transform(t)
                drawParticle(kind, Offset(at.x + cos(ang) * dist * e, at.y + sin(ang) * dist * e), sz * (1f - 0.75f * e), rot * e, 1f - e)
            }
        }
    }
}

/** A warm glow streak from [from] to [to], [thickness] px tall, fading out over 320 ms. */
fun DrawScope.drawGlowStreak(from: Offset, to: Offset, thickness: Float, glow: Color, p: Float) {
    if (p >= 1f) return
    val len = kotlin.math.hypot(to.x - from.x, to.y - from.y)
    if (len < 4f) return
    val a = 0.75f * (1f - FxEaseOut.transform(p))
    val angle = (kotlin.math.atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble()) * 180.0 / PI).toFloat()
    rotate(angle, pivot = from) {
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), glow.copy(alpha = glow.alpha * a)), startX = from.x, endX = from.x + len),
            topLeft = Offset(from.x, from.y - thickness / 2),
            size = Size(len, thickness),
            cornerRadius = CornerRadius(thickness / 2)
        )
    }
}
