package com.avfusionapps.game_2048.ui.sprites

import android.graphics.BlurMaskFilter
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** What a sprite is feeling. The board picks one from the game state; merges and spawns override it briefly. */
enum class SpriteEmotion { IDLE, HAPPY, EXCITED, THINKING, SURPRISED, SLEEPY, SAD, WORRIED, DIZZY, GRUMPY, LOVE, PROUD }

enum class SpriteElement { WATER, LEAF, SUN, ICE, COSMIC, FIRE, FLOWER, WAVE, STAR, CRYSTAL, KING, ROCK }

/** The little bits a sprite throws out when it merges or breaks. */
enum class SpriteParticle { DROP, LEAF, STAR, SHARD, DUST, EMBER, PETAL, ROCKBIT }

/** One character in the 2 → 2048 chain: body gradient (c1 light → c3 dark), tile card colours and glow. */
data class SpriteSpec(
    val name: String,
    val element: SpriteElement,
    val c1: Color,
    val c2: Color,
    val c3: Color,
    val t0: Color,
    val t1: Color,
    val glow: Color,
    val particle: Color,
    val bits: SpriteParticle = SpriteParticle.DUST
)

object ElementalSprites {
    val all = listOf(
        SpriteSpec("Drip", SpriteElement.WATER, Color(0xFFE2FAFF), Color(0xFF47C6FF), Color(0xFF1667D9), Color(0xFF2A86E8), Color(0xFF123E9A), Color(0xFF47C6FF), Color(0xFF8FD8FF), SpriteParticle.DROP),
        SpriteSpec("Sprig", SpriteElement.LEAF, Color(0xFFEAFFB8), Color(0xFF86E04A), Color(0xFF2E8F1F), Color(0xFF3FA52C), Color(0xFF1A5418), Color(0xFF8CF05A), Color(0xFFB6F57A), SpriteParticle.LEAF),
        SpriteSpec("Sunny", SpriteElement.SUN, Color(0xFFFFF8C2), Color(0xFFFFCF3A), Color(0xFFF08A12), Color(0xFFF0A01E), Color(0xFFA8500C), Color(0xFFFFD25A), Color(0xFFFFE58A), SpriteParticle.STAR),
        SpriteSpec("Frosty", SpriteElement.ICE, Color(0xFFF4FDFF), Color(0xFFA8E8FF), Color(0xFF4A9FE0), Color(0xFF3C8EDB), Color(0xFF1B4C9E), Color(0xFFA6ECFF), Color(0xFFE8FBFF), SpriteParticle.SHARD),
        SpriteSpec("Orbi", SpriteElement.COSMIC, Color(0xFFEDD6FF), Color(0xFF9A5BFF), Color(0xFF4B1FB0), Color(0xFF6B35D6), Color(0xFF2E1270), Color(0xFFB57BFF), Color(0xFFEBD3FF), SpriteParticle.DUST),
        SpriteSpec("Blaze", SpriteElement.FIRE, Color(0xFFFFF6B0), Color(0xFFFF9A2E), Color(0xFFE5381B), Color(0xFFD9481C), Color(0xFF7A1A10), Color(0xFFFF8A3D), Color(0xFFFFB347), SpriteParticle.EMBER),
        SpriteSpec("Bloom", SpriteElement.FLOWER, Color(0xFFEEFFC2), Color(0xFF8BE04E), Color(0xFF2F9A2A), Color(0xFF3FA532), Color(0xFF185418), Color(0xFF9BF26A), Color(0xFFFF8FC0), SpriteParticle.PETAL),
        SpriteSpec("Splash", SpriteElement.WAVE, Color(0xFFD8F2FF), Color(0xFF5A96FF), Color(0xFF2338C9), Color(0xFF2F52D8), Color(0xFF151F78), Color(0xFF6E9BFF), Color(0xFF9FC2FF), SpriteParticle.DROP),
        SpriteSpec("Starla", SpriteElement.STAR, Color(0xFFFFF9CC), Color(0xFFFFC53D), Color(0xFFE2770F), Color(0xFFE58A16), Color(0xFF8F420A), Color(0xFFFFD45A), Color(0xFFFFE58A), SpriteParticle.STAR),
        SpriteSpec("Prism", SpriteElement.CRYSTAL, Color(0xFFFCF6FF), Color(0xFFCDB8FF), Color(0xFF7A62D9), Color(0xFF7E5BD8), Color(0xFF3A2386), Color(0xFFD4C2FF), Color(0xFFF1E9FF), SpriteParticle.SHARD),
        SpriteSpec("Ember King", SpriteElement.KING, Color(0xFFFFF3A6), Color(0xFFFF8A2A), Color(0xFFD72A16), Color(0xFFF2A01A), Color(0xFFB5361A), Color(0xFFFFD050), Color(0xFFFFC94D), SpriteParticle.EMBER)
    )

    /** The Shadow Rock penalty tile (grid value -1). */
    val rock = SpriteSpec("Shadow Rock", SpriteElement.ROCK, Color(0xFF9A90B8), Color(0xFF5B5470), Color(0xFF2E2840), Color(0xFF3E3560), Color(0xFF1C1736), Color(0xFF8A5BFF), Color(0xFF8A80A8), SpriteParticle.ROCKBIT)

    /** 2 → 0 … 2048 → 10; anything bigger stays the Ember King. */
    fun indexFor(value: Int): Int =
        if (value <= 1) 0 else (log2(value.toFloat()) - 1f).roundToInt().coerceIn(0, all.lastIndex)

    fun specFor(value: Int): SpriteSpec = if (value < 0) rock else all[indexFor(value)]
}

// ---------------------------------------------------------------------------------------------
// One-shot body motions the board asks for (slide stretch, landing, bonk, idle fidgets…)
// ---------------------------------------------------------------------------------------------

enum class SpriteDir { LEFT, RIGHT, UP, DOWN }

enum class SpriteMotionKind {
    /** Stretched along the slide while moving. */
    STRETCH,
    /** Squashed against the side it slid into. */
    LAND,
    /** Hit a wall that would not move: squash and knock. */
    BONK,
    /** One of four idle fidgets, picked by [SpriteMotion.variant]. */
    FIDGET,
    /** A little hop off the ground. */
    HOP,
    /** Fading back in after an undo. */
    REWIND,
    /** A full spin while shuffled to a new spot. */
    SPIN
}

/** A new [key] replays the motion. [variant] picks the fidget; [hop] is the hop height in % of the sprite. */
data class SpriteMotion(
    val kind: SpriteMotionKind,
    val key: Int,
    val dir: SpriteDir = SpriteDir.DOWN,
    val variant: Int = 0,
    val durationMs: Int = 0,
    val hop: Float = 22f
) {
    val totalMs: Int
        get() = when (kind) {
            SpriteMotionKind.STRETCH -> if (durationMs > 0) durationMs else 140
            SpriteMotionKind.LAND -> 300
            SpriteMotionKind.BONK -> 340
            SpriteMotionKind.FIDGET -> when (variant) { 0 -> 720; 1 -> 1000; 2 -> 460; else -> 640 }
            SpriteMotionKind.HOP -> 460
            SpriteMotionKind.REWIND -> 320
            SpriteMotionKind.SPIN -> 460
        }
}

// ---------------------------------------------------------------------------------------------
// Timing helpers: CSS easings and keyframes
// ---------------------------------------------------------------------------------------------

private val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val CssEaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val CssEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val CssEaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val Burst = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

/** Position (0–1) in a repeating cycle, like an infinite CSS animation with a delay. */
private fun cycle(t: Float, period: Float, delay: Float = 0f): Float {
    val x = (t - delay) / period
    return x - floor(x)
}

/** Keyframe interpolation. [leg] eases each leg like CSS @keyframes; null keeps legs linear (Web Animations). */
private fun kf(p: Float, at: FloatArray, v: FloatArray, leg: Easing? = null): Float {
    if (p <= at[0]) return v[0]
    for (k in 0 until at.size - 1) {
        if (p <= at[k + 1]) {
            val span = at[k + 1] - at[k]
            val f = if (span <= 0f) 1f else (p - at[k]) / span
            return v[k] + (v[k + 1] - v[k]) * (leg?.transform(f) ?: f)
        }
    }
    return v[v.size - 1]
}

private fun f(vararg v: Float) = v

private fun clamp01(x: Float) = x.coerceIn(0f, 1f)

private fun bell(p: Float) = sin(PI * p).toFloat()

// Mood loops (CSS keyframes from the prototype).
private val A3 = f(0f, 0.5f, 1f)
private val BOUNCE_AT = f(0f, 0.15f, 0.45f, 0.75f, 1f)
private val BOUNCE_TY = f(0f, 0f, -9f, 0f, 0f)
private val BOUNCE_SX = f(1f, 1.06f, 0.96f, 1.04f, 1f)
private val BOUNCE_SY = f(1f, 0.94f, 1.05f, 0.96f, 1f)
private val HOPL_AT = f(0f, 0.2f, 0.38f, 0.6f, 1f)
private val HOPL_TY = f(0f, 0f, -7f, 0f, 0f)
private val HOPL_SX = f(1f, 1.06f, 0.96f, 1f, 1f)
private val HOPL_SY = f(1f, 0.94f, 1.05f, 1f, 1f)
private val SWAY = f(-5f, 5f, -5f)
private val SHIVER_AT = f(0f, 0.25f, 0.75f, 1f)
private val SHIVER = f(0f, -1.6f, 1.6f, 0f)
private val WOBBLE = f(-7f, 7f, -7f)
private val DROOP_TY = f(2f, 3f, 2f)
private val DROOP_SX = f(1.03f, 1.05f, 1.03f)
private val DROOP_SY = f(0.95f, 0.93f, 0.95f)
private val PROUD_TY = f(0f, -5f, 0f)
private val PROUD_S = f(1f, 1.04f, 1f)
private val HUFF_AT = f(0f, 0.8f, 0.88f, 1f)
private val HUFF_SX = f(1f, 1f, 1.06f, 1f)
private val HUFF_SY = f(1f, 1f, 0.95f, 1f)
private val TILT = f(0f, -6f, 0f)
private val BLINK_AT = f(0f, 0.93f, 0.96f, 1f)
private val BLINK = f(1f, 1f, 0.08f, 1f)

// One-shot motions (Web Animations from the prototype).
private val STRETCH_AT = f(0f, 0.45f, 1f)
private val STRETCH_LONG = f(1f, 1.22f, 1f)
private val STRETCH_SHORT = f(1f, 0.84f, 1f)
private val LAND_AT = f(0f, 0.3f, 0.65f, 1f)
private val LAND_H_X = f(1f, 0.84f, 1.05f, 1f)
private val LAND_H_Y = f(1f, 1.1f, 0.97f, 1f)
private val LAND_V_X = f(1f, 1.12f, 0.97f, 1f)
private val LAND_V_Y = f(1f, 0.84f, 1.04f, 1f)
private val BONK_AT = f(0f, 0.25f, 0.6f, 1f)
private val BONK_H_X = f(1f, 0.76f, 1.08f, 1f)
private val BONK_H_Y = f(1f, 1.16f, 0.95f, 1f)
private val BONK_JUMP_AT = f(0f, 0.2f, 0.5f, 1f)
private val BONK_JUMP = f(0f, 5f, -3f, 0f)
private val EVEN5 = f(0f, 0.25f, 0.5f, 0.75f, 1f)
private val EVEN4 = f(0f, 1f / 3f, 2f / 3f, 1f)
private val FIDGET0_X = f(1f, 1.12f, 0.92f, 1.06f, 1f)
private val FIDGET0_Y = f(1f, 0.89f, 1.09f, 0.95f, 1f)
private val FIDGET1_R = f(0f, -9f, 8f, 0f)
private val FIDGET3_R = f(0f, 7f, -7f, 0f)
private val FIDGET3_S = f(1f, 1.05f, 1f, 1f)
private val HOP_AT = f(0f, 0.15f, 0.5f, 0.82f, 1f)
private val HOP_SQ_AT = f(0f, 0.15f, 0.35f, 0.6f, 0.86f, 1f)
private val HOP_SQ_X = f(1f, 1.14f, 0.9f, 1f, 1.14f, 1f)
private val HOP_SQ_Y = f(1f, 0.84f, 1.14f, 1f, 0.86f, 1f)
private val HOP_SH_AT = f(0f, 0.5f, 0.82f, 1f)
private val HOP_SH_S = f(1f, 0.55f, 1f, 1f)
private val HOP_SH_A = f(1f, 0.45f, 1f, 1f)
private val REWIND_AT = f(0f, 0.7f, 1f)
private val REWIND_A = f(0f, 1f, 1f)
private val REWIND_S = f(0.7f, 1.05f, 1f)
private val REWIND_R = f(-14f, 3f, 0f)
private val SPIN_S = f(1f, 0.7f, 1f)
private val POKE_AT = f(0f, 0.2f, 0.5f, 0.75f, 1f)
private val POKE_X = f(1f, 1.22f, 0.88f, 1.05f, 1f)
private val POKE_Y = f(1f, 0.76f, 1.14f, 0.96f, 1f)
private val EVOLVE_AT = f(0f, 0.3f, 0.55f, 0.8f, 1f)
private val EVOLVE_X = f(0.3f, 1.34f, 0.86f, 1.06f, 1f)
private val EVOLVE_Y = f(0.3f, 0.76f, 1.2f, 0.96f, 1f)
private val EVOLVE_B = f(2.6f, 1.7f, 1.15f, 1.0667f, 1f)
private val EVOLVE_JUMP = f(0f, -14f, 0f)
private val EVOLVE_JUMP_AT = f(0f, 0.45f, 1f)
private val CARD_POP_S = f(1.25f, 0.95f, 1f)
private val CARD_POP_B = f(1.9f, 1.45f, 1f)
private val SPAWN_AT = f(0f, 0.55f, 0.8f, 1f)
private val SPAWN_X = f(0f, 1.2f, 0.94f, 1f)
private val SPAWN_Y = f(0f, 0.82f, 1.08f, 1f)
private val SPAWN_TY = f(20f, 0f, 0f, 0f)
private val NUM_POP_AT = f(0f, 0.7f, 1f)
private val NUM_POP = f(0f, 1.25f, 1f)
private val VANISH_AT = f(0f, 0.4f, 0.7f, 1f)
private val VANISH_X = f(1f, 1f, 1.2f, 0.2f)
private val VANISH_Y = f(1f, 1f, 0.5f, 0.1f)
private val VANISH_TY = f(0f, 0f, 30f, 80f)
private val VANISH_A = f(1f, 1f, 0.6f, 0f)
private val VANISH_SHAKE = f(0f, -4f, 4f, -3f, 0f)
private val FLAP = f(0f, -22f, 0f)
private val WANT = f(-1f, 1f, -1f)
private val BREATH_X = f(1f, 0.98f, 1f)
private val BREATH_Y = f(1f, 1.04f, 1f)
private val FLICK_X = f(1f, 1.02f, 0.99f)
private val FLICK_Y = f(1f, 1.04f, 1.06f)
private val FLICK_SKEW = f(0f, -2f, 2f)
private val TWINKLE_S = f(0.5f, 1.15f, 0.5f)
private val TWINKLE_A = f(0.2f, 1f, 0.2f)
private val FADE_AT_20 = f(0f, 0.2f, 1f)
private val FADE_AT_25 = f(0f, 0.25f, 1f)
private val FADE_AT_30 = f(0f, 0.3f, 1f)
private val FADE_IN_OUT = f(0f, 1f, 0f)
private val BUB_S = f(1f, 1.12f, 1f)
private val VEIN_S = f(1f, 1.3f, 1f)
private val CHEEKS = f(32.5f, 67.5f)
private val EYES = f(41f, 59f)
private val EMBER_X = f(34f, 66f, 58f)
private val EMBER_Y = f(26f, 22f, 12f)
private val EMBER_R = f(2f, 1.6f, 1.4f)
private val EMBER_D = f(0f, 0.6f, 1.2f)
private val EMBER_C = arrayOf(Color(0xFFFFD25A), Color(0xFFFF9A2E), Color(0xFFFFF3A6))

/** Mutable pose, reused every frame on the main thread to avoid allocations. */
private class Pose {
    var tx = 0f; var ty = 0f; var rot = 0f; var sx = 1f; var sy = 1f
    var ox = 0.5f; var oy = 0f
    var alpha = 1f
    fun reset(originY: Float) { tx = 0f; ty = 0f; rot = 0f; sx = 1f; sy = 1f; ox = 0.5f; oy = originY; alpha = 1f }
}

private val moodPose = Pose()
private val jumpPose = Pose()
private val squashPose = Pose()
private val motionPose = Pose()

// ---------------------------------------------------------------------------------------------
// Geometry. Everything is drawn in a 100 × 100 box; the character stands on y ≈ 89.
// ---------------------------------------------------------------------------------------------

private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

private fun starPath(cx: Float, cy: Float, ro: Float, ri: Float, n: Int, round: Boolean): String {
    val pts = (0 until n * 2).map { k ->
        val r = if (k % 2 == 1) ri else ro
        val a = -PI / 2 + k * PI / n
        Pair(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
    }
    if (!round) return "M" + pts.joinToString(" L") { "${it.first} ${it.second}" } + " Z"
    val last = pts.last()
    val sb = StringBuilder("M${(last.first + pts[0].first) / 2} ${(last.second + pts[0].second) / 2}")
    pts.forEachIndexed { k, p ->
        val q = pts[(k + 1) % pts.size]
        sb.append(" Q${p.first} ${p.second} ${(p.first + q.first) / 2} ${(p.second + q.second) / 2}")
    }
    return sb.append(" Z").toString()
}

private object Shapes {
    val drop by lazy { parse("M50 12 C58 28 82 42 82 63 C82 79 68 89 50 89 C32 89 18 79 18 63 C18 42 42 28 50 12 Z") }
    val blob by lazy { parse("M50 30 C71 30 85 42 85 61 C85 79 70 89 50 89 C30 89 15 79 15 61 C15 42 29 30 50 30 Z") }
    val orb by lazy { parse("M20 60 A30 30 0 1 0 80 60 A30 30 0 1 0 20 60 Z") }
    val sun by lazy { parse("M24 60 A26 26 0 1 0 76 60 A26 26 0 1 0 24 60 Z") }
    val flame by lazy { parse("M50 8 C55 20 61 22 65 15 C71 26 68 33 75 33 C83 43 85 54 85 63 C85 79 70 89 50 89 C30 89 15 79 15 63 C15 51 21 42 28 35 C30 41 34 43 37 39 C36 28 44 20 50 8 Z") }
    val flameCore by lazy { parse("M50 40 C57 49 66 55 66 66 C66 77 59 83 50 83 C41 83 34 77 34 66 C34 56 43 50 50 40 Z") }
    val star by lazy { parse(starPath(50f, 58f, 40f, 25f, 5, true)) }
    val rays by lazy { parse(starPath(50f, 60f, 40f, 30f, 12, false)) }
    val ringBack by lazy { parse("M8 72 A42 10 0 0 1 92 72") }
    val ringFront by lazy { parse("M8 72 A42 10 0 0 0 92 72") }
    val leafL by lazy { parse("M51 22 C40 9 25 13 25 22 C33 29 44 29 51 22 Z") }
    val leafR by lazy { parse("M52 20 C60 5 77 7 77 16 C70 24 60 26 52 20 Z") }
    val leafVeins by lazy { parse("M48 21 C42 17 35 17 30 20 M55 18 C61 13 68 12 73 14") }
    val stem by lazy { parse("M50 32 Q50 24 52 18") }
    val sideLeaf by lazy { parse("M38 32 C30 22 20 26 20 32 C27 37 34 37 38 32 Z") }
    val crest by lazy { parse("M22 46 C20 28 38 16 55 20 C68 23 73 34 64 41 C60 33 50 31 46 39 C40 33 29 37 22 46 Z") }
    val cosmicShine by lazy { parse("M56 49 Q63 50 66 56") }
    val iceMid by lazy { parse("M50 6 L58 22 L50 35 L42 22 Z") }
    val iceMidFacet by lazy { parse("M50 6 L58 22 L50 35 Z") }
    val iceL by lazy { parse("M33 18 L40 28 L37 39 L28 31 Z") }
    val iceLFacet by lazy { parse("M33 18 L40 28 L37 39 Z") }
    val iceR by lazy { parse("M67 18 L72 31 L63 39 L60 28 Z") }
    val iceRFacet by lazy { parse("M67 18 L72 31 L63 39 Z") }
    val crystals by lazy { listOf("M30 34 L33 22 L38 33 Z", "M38 32 L42 14 L47 31 Z", "M45 31 L50 6 L55 31 Z", "M53 31 L58 14 L62 32 Z", "M62 33 L67 22 L70 34 Z").map(::parse) }
    val crystalFacet by lazy { parse("M50 6 L55 31 L50 31 Z") }
    val crown by lazy { parse("M31 30 L30 10 L40 19 L50 3 L60 19 L70 10 L69 30 Z") }
    val wingL by lazy { parse("M30 58 C16 44 3 44 4 54 C-1 57 2 65 10 65 C7 71 15 76 24 69 C27 66 29 62 30 58 Z") }
    val wingR by lazy { parse("M70 58 C84 44 97 44 96 54 C101 57 98 65 90 65 C93 71 85 76 76 69 C73 66 71 62 70 58 Z") }
    val wingLVeins by lazy { parse("M27 60 C18 55 11 54 8 56 M26 64 C19 63 14 64 12 66") }
    val wingRVeins by lazy { parse("M73 60 C82 55 89 54 92 56 M74 64 C81 63 86 64 88 66") }
    val heart by lazy { parse("M0 4.6 C-7.6 -0.8 -5.4 -7.6 0 -3.6 C5.4 -7.6 7.6 -0.8 0 4.6 Z") }
    val smallHeart by lazy { parse("M0 3.5 C-6 -1 -4 -6 0 -3 C4 -6 6 -1 0 3.5 Z") }
    val starEye by lazy { parse("M0 -6.4 L1.9 -2 L6.4 -2 L2.8 1.2 L4.1 6 L0 3.3 L-4.1 6 L-2.8 1.2 L-6.4 -2 L-1.9 -2 Z") }
    val sparkle by lazy { parse("M0 -6 Q1.2 -1.2 6 0 Q1.2 1.2 0 6 Q-1.2 1.2 -6 0 Q-1.2 -1.2 0 -6 Z") }
    val eyeSparkle by lazy { parse("M1.5 -5.5 Q2 -2.8 4.6 -2.3 Q2 -1.8 1.5 0.9 Q1 -1.8 -1.6 -2.3 Q1 -2.8 1.5 -5.5 Z") }
    val sweat by lazy { parse("M76 38 Q81 46 78 50 Q74 52.5 72.5 48.5 Q72 44 76 38 Z") }
    val tearL by lazy { parse("M38 64 Q41 69 38.5 71 Q36 69 38 64 Z") }
    val tearR by lazy { parse("M62 64 Q65 69 62.5 71 Q60 69 62 64 Z") }
    val vein by lazy { parse("M-5 -1 Q-1 -1 -1 -5 M1 -5 Q1 -1 5 -1 M5 1 Q1 1 1 5 M-1 5 Q-1 1 -5 1") }
    val diamondGoldL by lazy { parse("M-22 0 l2.4 -4.4 l2.4 4.4 l-2.4 4.4 Z") }
    val diamondGoldR by lazy { parse("M17 0 l2.4 -4.4 l2.4 4.4 l-2.4 4.4 Z") }
    val diamondTop by lazy { parse("M-2.4 -20 l2.4 -4.4 l2.4 4.4 l-2.4 4.4 Z") }
    val diamondBottom by lazy { parse("M-2.4 20 l2.4 -4.4 l2.4 4.4 l-2.4 4.4 Z") }
    val rock by lazy { parse("M18 88 Q10 70 20 56 Q21 38 38 35 Q49 25 63 32 Q80 35 83 54 Q92 70 83 88 Z") }
    val rockCracks by lazy { parse("M33 46 L40 54 L36 61 M64 60 L71 66 L68 74 M50 36 L53 43") }
    val spiral by lazy { parse("M0 0 m-1 0 a1 1 0 1 1 2 0 a2 2 0 1 1 -4 0 a3 3 0 1 1 6 0 a4 4 0 1 1 -8 0") }
    // particles, in a 1 × 1 box
    val pStar by lazy { parse("M0.5 0 L0.61 0.39 L1 0.5 L0.61 0.61 L0.5 1 L0.39 0.61 L0 0.5 L0.39 0.39 Z") }
    val pDiamond by lazy { parse("M0.5 0 L1 0.5 L0.5 1 L0 0.5 Z") }
    val pDrop by lazy { parse("M0 0 H0.5 A0.5 0.5 0 0 1 1 0.5 A0.5 0.5 0 0 1 0.5 1 A0.5 0.5 0 0 1 0 0.5 Z") }
    val pLeaf by lazy { parse("M0 0 H0.5 A0.5 0.5 0 0 1 1 0.5 V1 H0.5 A0.5 0.5 0 0 1 0 0.5 Z") }

    fun body(e: SpriteElement): Path = when (e) {
        SpriteElement.WATER -> drop
        SpriteElement.SUN -> sun
        SpriteElement.COSMIC -> orb
        SpriteElement.FIRE, SpriteElement.KING -> flame
        SpriteElement.STAR -> star
        SpriteElement.ROCK -> rock
        else -> blob
    }
}

/**
 * Brushes are rebuilt on every frame otherwise (each tile animates continuously). Character brushes live in
 * the fixed 100-unit box, so one per sprite is enough. Main thread only.
 */
private object BrushCache {
    private val map = HashMap<String, Brush>()
    fun get(key: String, make: () -> Brush): Brush = map.getOrPut(key, make)
}

/** The parts of a sprite that never change shape, rendered once per size and shared by every tile. */
private class BodyArt(val body: ImageBitmap, val glow: ImageBitmap?, val glowX: Float, val glowY: Float)

/**
 * Rendered cards and bodies, reused by every sprite of the same kind and size. Least recently used art goes first
 * once the cache passes [BUDGET] bytes (instead of dropping everything at once), and [trim] frees it all when
 * Android is short of memory.
 */
private object ArtCache {
    private const val BUDGET = 16L * 1024 * 1024
    private val cards = LinkedHashMap<String, ImageBitmap>(64, 0.75f, true)
    private val bodies = LinkedHashMap<String, BodyArt>(64, 0.75f, true)
    private var bytes = 0L

    private fun ImageBitmap.bytes(): Long = asAndroidBitmap().allocationByteCount.toLong()
    private fun BodyArt.bytes(): Long = body.bytes() + (glow?.bytes() ?: 0L)

    fun card(key: String, make: () -> ImageBitmap): ImageBitmap {
        cards[key]?.let { return it }
        return make().also { cards[key] = it; bytes += it.bytes(); evict() }
    }
    fun body(key: String, make: () -> BodyArt): BodyArt {
        bodies[key]?.let { return it }
        return make().also { bodies[key] = it; bytes += it.bytes(); evict() }
    }
    private fun evict() {
        while (bytes > BUDGET && (cards.size > 1 || bodies.size > 1)) {
            // drop the oldest entry of the larger map
            if (cards.size >= bodies.size) cards.entries.iterator().let { val e = it.next(); bytes -= e.value.bytes(); it.remove() }
            else bodies.entries.iterator().let { val e = it.next(); bytes -= e.value.bytes(); it.remove() }
        }
    }
    /** Frees all cached art (it is redrawn on demand). */
    fun trim() { cards.clear(); bodies.clear(); bytes = 0L }
}

/** Frees the sprites' cached art; call when Android reports low memory. */
fun trimSpriteArt() = ArtCache.trim()

private val labelPaint by lazy {
    android.graphics.Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
}

/** Gives the canvas labels (z, ?, !) the prototype's Baloo 2 once a context is around. */
private object LabelFont {
    private var loaded = false
    fun ensure(context: android.content.Context) {
        if (loaded) return
        loaded = true
        runCatching { context.resources.getFont(com.avfusionapps.game_2048.R.font.baloo2_extrabold) }
            .onSuccess { labelPaint.typeface = it }
    }
}

private val Ink = Color(0xFF2B1442)
private val MouthDark = Color(0xFF6A1E3E)
private val Tongue = Color(0xFFFF7A9A)
private val Gold = Color(0xFFFFC93A)
private val GoldLight = Color(0xFFFFF3A6)
private val GoldDark = Color(0xFFE5850F)
private val Blush = Color(0xFFFF6FA0)
private val GroundShadow = Color(0x73050014)

private const val CLOCK_SECONDS = 3600f
/** The moment a still sprite is drawn at: mid-breath, eyes open. */
private const val STILL_T = 0.8f
private const val DONE = 1_000_000f
private const val MERGE_MS = 1000f
private const val SPAWN_MS = 640f
private const val SPAWN_SPRITE_AT = 160f
private const val BREAK_HIT_MS = 340f
private const val VANISH_MS = 560f

// ---------------------------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------------------------

/**
 * One elemental sprite, optionally on its glowing tile card with the number badge.
 *
 * @param charScaleX/charScaleY extra squash and stretch for the character only (the card and number stay square).
 * @param mergeKey a new non-null value plays the evolve pop, swirl, flash and particles, after [mergeDelayMs].
 * @param spawnKey a new non-null value opens a portal and pops the sprite out of it, after [spawnDelayMs].
 * @param motion a one-shot body motion (stretch, land, bonk, fidget, hop, rewind, spin).
 * @param breaking the sprite is hit (hammer): it flinches, then crumbles at [breakHitMs] and stays gone.
 * @param wingsFrom merges at or above this value show flapping wings for a moment.
 * @param chainGlow a pink chain glow around the character (Neon Shooter, just after a chain merge).
 */
@Composable
fun SpriteTile(
    value: Int,
    modifier: Modifier = Modifier,
    emotion: SpriteEmotion = SpriteEmotion.IDLE,
    lookX: Float = 0f,
    lookY: Float = 0f,
    charScaleX: Float = 1f,
    charScaleY: Float = 1f,
    mergeKey: Int? = null,
    spawnKey: Int? = null,
    showCard: Boolean = true,
    showNumber: Boolean = true,
    fontSizeMultiplier: Float = 1f,
    breaking: Boolean = false,
    /** A new non-null value is a poke: squish, giggle, and a name tag. Four quick pokes make it grumpy. */
    pokeKey: Int? = null,
    /** Collection silhouette for a sprite not found yet. */
    locked: Boolean = false,
    /** Replaces the number on the badge (the rock shows its countdown). */
    badgeText: String? = null,
    motion: SpriteMotion? = null,
    mergeDelayMs: Int = 0,
    spawnDelayMs: Int = 0,
    breakHitMs: Int = BREAK_HIT_MS.toInt(),
    wingsFrom: Int = 512,
    chainGlow: Boolean = false,
    /** Card-less sprites stand in a soft glow of their colour. */
    showAura: Boolean = true,
    /** Draws the merge swirl, flash and particles (off for ghost copies). */
    showEffects: Boolean = true,
    /** Remove/Hammer armed: the sprite rocks ±this many degrees, asking to be picked. */
    wantHitDeg: Float = 0f,
    /** Icons and previews: drawn once in a calm pose, no idle loop (one-shot motions still play). */
    still: Boolean = false
) {
    val rock = value < 0
    val spec = ElementalSprites.specFor(value)
    val index = if (rock) 5 else ElementalSprites.indexFor(value)
    LabelFont.ensure(androidx.compose.ui.platform.LocalContext.current)
    // Idle loops run on the shared, throttled sprite clock; a still sprite never reads it, so it never redraws.
    val time = spriteTime()
    val stillState = rememberUpdatedState(still)
    val timeOffset = remember { Random.nextFloat() * 5f }
    val blinkPeriod = remember { 3f + Random.nextFloat() * 3.5f }

    val mergeT = remember { Animatable(if (mergeKey != null) -mergeDelayMs.toFloat() else DONE) }
    val spawnT = remember { Animatable(if (spawnKey != null) -spawnDelayMs.toFloat() else DONE) }
    val motionT = remember { Animatable(DONE) }
    val pokeT = remember { Animatable(DONE) }
    val breakT = remember { Animatable(if (breaking) 0f else DONE) }
    var playing by remember { mutableStateOf<SpriteMotion?>(null) }
    // Keys already started; a key that is newer than this is pending (its sprite waits hidden or small).
    val mergeSeen = remember { mutableStateOf(mergeKey) }
    val spawnSeen = remember { mutableStateOf(spawnKey) }
    var wingsUntil by remember { mutableStateOf(0L) }
    var nameTag by remember { mutableStateOf<String?>(null) }
    val pokeTimes = remember { ArrayList<Long>() }
    var override by remember { mutableStateOf<SpriteEmotion?>(null) }
    var fidgetLook by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()

    val lookTx by animateFloatAsState(lookX + fidgetLook.x, tween(220, easing = CssEaseOut), label = "lookX")
    val lookTy by animateFloatAsState(lookY + fidgetLook.y, tween(220, easing = CssEaseOut), label = "lookY")
    val lookXState = rememberUpdatedState(lookTx)
    val lookYState = rememberUpdatedState(lookTy)
    val emotionState = rememberUpdatedState(emotion)
    val scaleXState = rememberUpdatedState(charScaleX)
    val scaleYState = rememberUpdatedState(charScaleY)
    val chainState = rememberUpdatedState(chainGlow)
    val mergeKeyState = rememberUpdatedState(mergeKey)
    val spawnKeyState = rememberUpdatedState(spawnKey)
    val mergeDelayState = rememberUpdatedState(mergeDelayMs)
    val spawnDelayState = rememberUpdatedState(spawnDelayMs)
    val breakingState = rememberUpdatedState(breaking)
    val wantHitState = rememberUpdatedState(wantHitDeg)
    val overrideState = rememberUpdatedState(override)
    val playingState = rememberUpdatedState(playing)
    val wingsState = rememberUpdatedState(wingsUntil)

    // Effects run in the composable's own scope so a later move can't freeze them half-way.
    LaunchedEffect(mergeKey) {
        if (mergeKey == null) return@LaunchedEffect
        mergeSeen.value = mergeKey
        scope.launch {
            mergeT.snapTo(-mergeDelayMs.toFloat())
            mergeT.animateTo(MERGE_MS, tween(MERGE_MS.toInt() + mergeDelayMs, easing = LinearEasing))
        }
        scope.launch {
            delay(mergeDelayMs.toLong())
            override = if (value >= 128) SpriteEmotion.PROUD else SpriteEmotion.HAPPY
            if (value >= wingsFrom) wingsUntil = System.currentTimeMillis() + 1500
            delay(1000)
            if (override == SpriteEmotion.PROUD || override == SpriteEmotion.HAPPY) override = null
        }
    }
    LaunchedEffect(spawnKey) {
        if (spawnKey == null) return@LaunchedEffect
        spawnSeen.value = spawnKey
        scope.launch {
            spawnT.snapTo(-spawnDelayMs.toFloat())
            spawnT.animateTo(SPAWN_MS, tween(SPAWN_MS.toInt() + spawnDelayMs, easing = LinearEasing))
        }
        scope.launch {
            delay(spawnDelayMs.toLong() + SPAWN_SPRITE_AT.toLong())
            override = SpriteEmotion.SURPRISED
            delay(900)
            if (override == SpriteEmotion.SURPRISED) override = null
        }
    }
    LaunchedEffect(motion) {
        val m = motion ?: return@LaunchedEffect
        playing = m
        if (m.kind == SpriteMotionKind.FIDGET) {
            fidgetLook = Offset((Random.nextFloat() * 2f - 1f) * 2.4f, (Random.nextFloat() * 2f - 1f) * 1.8f)
            scope.launch { delay(900); fidgetLook = Offset.Zero }
        }
        scope.launch {
            motionT.snapTo(0f)
            motionT.animateTo(m.totalMs.toFloat(), tween(m.totalMs, easing = LinearEasing))
        }
    }
    LaunchedEffect(breaking) {
        if (!breaking) {
            breakT.snapTo(DONE)
            if (override == SpriteEmotion.SURPRISED) override = null
            return@LaunchedEffect
        }
        override = SpriteEmotion.SURPRISED
        breakT.snapTo(0f)
        breakT.animateTo(breakHitMs + VANISH_MS + 400f, tween(breakHitMs + VANISH_MS.toInt() + 400, easing = LinearEasing))
    }
    LaunchedEffect(pokeKey) {
        if (pokeKey == null) return@LaunchedEffect
        val now = System.currentTimeMillis()
        pokeTimes.removeAll { now - it > 2600 }
        pokeTimes.add(now)
        val annoyed = pokeTimes.size >= 4
        scope.launch {
            override = when {
                rock || annoyed -> SpriteEmotion.GRUMPY
                pokeTimes.size == 1 -> SpriteEmotion.HAPPY
                else -> SpriteEmotion.LOVE
            }
            nameTag = when {
                rock -> "Shadow Rock"
                annoyed -> "Hey! Stop poking!"
                else -> "No.${(index + 1).toString().padStart(2, '0')} · ${spec.name} · $value"
            }
            delay(if (annoyed) 2200 else 800)
            override = null
            delay(if (annoyed) 0 else 500)
            nameTag = null
        }
        scope.launch {
            pokeT.snapTo(0f)
            pokeT.animateTo(460f, tween(460, easing = LinearEasing))
        }
    }

    fun mergeMs(): Float {
        val k = mergeKeyState.value
        return if (k != null && k != mergeSeen.value) -mergeDelayState.value.toFloat() else mergeT.value
    }
    fun spawnMs(): Float {
        val k = spawnKeyState.value
        return if (k != null && k != spawnSeen.value) -spawnDelayState.value.toFloat() else spawnT.value
    }
    fun vanishMs(): Float = if (breakingState.value) breakT.value - breakHitMs else -1f

    val particleSeed = (mergeKey ?: 0) * 31 + index
    val tiled = showCard

    BoxWithConstraints(modifier = modifier) {
        val sizeDp = maxWidth
        key(value, locked, showCard) {
            Spacer(modifier = Modifier.fillMaxSize().drawWithCache {
                val w = size.width
                val h = size.height
                // The character's 100-unit box inside the tile (the prototype's .squash box).
                val u = if (tiled) w * 0.009f else w * 0.0104f
                val left = if (tiled) w * 0.05f else -w * 0.02f
                val top = if (tiled) -h * 0.06f else -h * 0.04f
                val legend = index == 10 && !rock && !locked
                val card = if (tiled) ArtCache.card("${spec.name}|${w.toInt()}|${h.toInt()}|$legend|$locked") {
                    renderCard(spec, legend, locked, w, h, this)
                } else null
                val bodyPx = (120f * u).toInt().coerceAtLeast(1)
                val art = ArtCache.body("${spec.name}|$bodyPx|$locked") { renderBody(spec, bodyPx, u, locked, this) }
                val drawBody: DrawScope.(ColorFilter?) -> Unit = { filter ->
                    withTransform({
                        translate(-10f, -10f)
                        scale(1f / u, 1f / u, pivot = Offset.Zero)
                    }) {
                        drawImage(art.body, colorFilter = filter)
                    }
                }
                val drawGlow: DrawScope.(Color, Float) -> Unit = { color, grow ->
                    art.glow?.let { g ->
                        withTransform({
                            if (grow != 1f) scale(grow, grow, pivot = Offset(50f, 60f))
                            translate(-10f, -10f)
                            scale(1f / u, 1f / u, pivot = Offset.Zero)
                        }) {
                            drawImage(g, topLeft = Offset(art.glowX, art.glowY), colorFilter = ColorFilter.tint(color, BlendMode.SrcIn))
                        }
                    }
                }
                val layerPaint = Paint()
                // glow hugging the character (the prototype's .squash drop-shadow)
                val glowFor: DrawScope.() -> Unit = {
                    if (chainState.value) {
                        drawGlow(Color(0xFFFF5FA2), 1.08f)
                        drawGlow(Color.White.copy(alpha = 0.8f), 1f)
                    } else {
                        drawGlow(spec.glow.copy(alpha = 0.6f), 1f)
                    }
                }
                onDrawBehind {
                    val t = if (stillState.value) STILL_T else time.value + timeOffset
                    val emo = if (rock) SpriteEmotion.GRUMPY else (overrideState.value ?: emotionState.value)
                    val mm = mergeMs()
                    val sm = spawnMs()
                    val vm = vanishMs()
                    val pm = pokeT.value
                    val mo = playingState.value
                    val mt = motionT.value

                    // ── spawn: hidden until the portal opens, then pops out ──
                    val spawning = sm < SPAWN_MS
                    val spriteSpawnP = if (spawning) clamp01((sm - SPAWN_SPRITE_AT) / 380f) else 1f
                    val cardSpawnP = if (spawning) clamp01((sm - SPAWN_SPRITE_AT) / 240f) else 1f
                    val shadowSpawnP = if (spawning) clamp01((sm - SPAWN_SPRITE_AT) / 300f) else 1f
                    if (spawning && sm < SPAWN_SPRITE_AT && sm < 0f) return@onDrawBehind

                    // ── card ──
                    if (card != null) {
                        var cs = 1f
                        var cb = 1f
                        var ca = 1f
                        if (mm in 0f..400f) {
                            val p = mm / 400f
                            cs = kf(p, A3, CARD_POP_S); cb = kf(p, A3, CARD_POP_B)
                        } else if (mm < 0f && mergeKeyState.value != null) {
                            cs = 1.25f; cb = 1.9f
                        }
                        if (spawning) {
                            val e = CssEaseOut.transform(cardSpawnP)
                            cs *= 0.5f + 0.5f * e; ca *= e
                        }
                        if (vm >= 0f) {
                            val p = CssEaseIn.transform(clamp01(vm / VANISH_MS))
                            ca *= kf(p, f(0f, 0.5f, 1f), f(1f, 1f, 0f)); cs *= kf(p, f(0f, 0.5f, 1f), f(1f, 1f, 0.6f))
                        }
                        if (ca > 0.01f) {
                            withTransform({ scale(cs, cs, pivot = Offset(w / 2, h / 2)) }) {
                                val m = cardMargin(w)
                                drawImage(
                                    card,
                                    topLeft = Offset(-m, -m),
                                    alpha = ca,
                                    colorFilter = when {
                                        locked -> LockedCardFilter
                                        cb > 1.001f -> brightness(cb)
                                        else -> null
                                    }
                                )
                                if (legend) drawLegendShine(w, h, t)
                            }
                        }
                    } else if (!locked && showAura) {
                        // .aura: a soft glow behind card-less sprites
                        drawOval(
                            brush = Brush.radialGradient(
                                0f to spec.glow.copy(alpha = 0.6f), 0.68f to Color.Transparent,
                                center = Offset(w / 2, h * 0.52f), radius = w * 0.58f
                            ),
                            topLeft = Offset(w * 0.1f, h * 0.1f),
                            size = Size(w * 0.8f, h * 0.84f)
                        )
                    }
                    if (spawning && sm < SPAWN_SPRITE_AT) return@onDrawBehind

                    // ── ground shadow ──
                    var shS = shadowSpawnP
                    var shA = 1f
                    if (mo != null && mt < mo.totalMs && (mo.kind == SpriteMotionKind.HOP || (mo.kind == SpriteMotionKind.FIDGET && mo.variant == 2))) {
                        val p = mt / mo.totalMs
                        shS *= kf(p, HOP_SH_AT, HOP_SH_S); shA *= kf(p, HOP_SH_AT, HOP_SH_A)
                    }
                    if (vm >= 0f) shA *= 1f - clamp01(vm / VANISH_MS)
                    if (!locked && shS > 0.01f) {
                        val cy = if (tiled) h * 0.81f else h * 0.94f
                        val rx = w * 0.24f * shS
                        val ry = h * 0.03f * shS
                        drawOval(GroundShadow, topLeft = Offset(w / 2 - rx, cy - ry), size = Size(rx * 2, ry * 2), alpha = shA)
                    }

                    // ── jump layer (hops, evolve leap, bonk knock, fidget rock) ──
                    val jp = jumpPose
                    jp.reset(0.92f)
                    if (mm in 0f..540f) jp.ty += kf(CssEaseOut.transform(mm / 540f), EVOLVE_JUMP_AT, EVOLVE_JUMP)
                    if (mo != null && mt < mo.totalMs) motionJump(mo, mt / mo.totalMs, jp)
                    if (vm in 0f..240f) jp.tx += kf(vm / 240f, EVEN5, VANISH_SHAKE)
                    val want = wantHitState.value
                    if (want != 0f) jp.rot += want * kf(cycle(t, 0.5f), A3, WANT, CssEaseInOut)

                    // ── mood layer (the emotion's own loop) ──
                    val mp = moodPose
                    moodLoop(emo, t, mp)

                    // ── squash layer ──
                    val sp = squashPose
                    sp.reset(if (tiled) 0.92f else 0.9f)
                    sp.sx = scaleXState.value
                    sp.sy = scaleYState.value
                    var bright = 1f
                    if (pm < 460f) {
                        val p = CssEaseOut.transform(pm / 460f)
                        sp.sx *= kf(p, POKE_AT, POKE_X); sp.sy *= kf(p, POKE_AT, POKE_Y)
                    }
                    if (mm < 0f && mergeKeyState.value != null) {
                        sp.sx *= 0.3f; sp.sy *= 0.3f; bright = 2.6f
                    } else if (mm < 580f) {
                        val p = CssEaseOut.transform(clamp01(mm / 580f))
                        sp.sx *= kf(p, EVOLVE_AT, EVOLVE_X); sp.sy *= kf(p, EVOLVE_AT, EVOLVE_Y); bright = kf(p, EVOLVE_AT, EVOLVE_B)
                    }
                    if (spawning) {
                        val p = CssEaseOut.transform(spriteSpawnP)
                        sp.sx *= kf(p, SPAWN_AT, SPAWN_X); sp.sy *= kf(p, SPAWN_AT, SPAWN_Y); sp.ty += kf(p, SPAWN_AT, SPAWN_TY)
                    }
                    if (vm >= 0f) {
                        val p = CssEaseIn.transform(clamp01(vm / VANISH_MS))
                        sp.sx *= kf(p, VANISH_AT, VANISH_X); sp.sy *= kf(p, VANISH_AT, VANISH_Y)
                        sp.ty += kf(p, VANISH_AT, VANISH_TY); sp.alpha *= kf(p, VANISH_AT, VANISH_A)
                    }
                    val mop = motionPose
                    mop.reset(sp.oy)
                    if (mo != null && mt < mo.totalMs) motionSquash(mo, mt / mo.totalMs, mop)
                    if (sp.alpha * mop.alpha < 0.01f) return@onDrawBehind

                    // squash box in tile pixels
                    val boxL = left
                    val boxT = top
                    val boxW = 100f * u
                    withTransform({
                        translate(jp.tx / 100f * w, jp.ty / 100f * h)
                        if (jp.rot != 0f) rotate(jp.rot, pivot = Offset(w * 0.5f, h * jp.oy))
                        translate(mp.tx / 100f * w, mp.ty / 100f * h)
                        if (mp.rot != 0f) rotate(mp.rot, pivot = Offset(w * 0.5f, h * 0.88f))
                        scale(mp.sx, mp.sy, pivot = Offset(w * 0.5f, h * 0.88f))
                        // motion squash with its own origin
                        val mPivot = Offset(boxL + boxW * mop.ox, boxT + boxW * mop.oy)
                        translate(0f, mop.ty / 100f * boxW)
                        if (mop.rot != 0f) rotate(mop.rot, pivot = mPivot)
                        scale(mop.sx, mop.sy, pivot = mPivot)
                        // everything else squashes from the feet
                        val pivot = Offset(boxL + boxW * 0.5f, boxT + boxW * sp.oy)
                        translate(0f, sp.ty / 100f * boxW)
                        scale(sp.sx, sp.sy, pivot = pivot)
                        translate(boxL, boxT)
                        scale(u, u, pivot = Offset.Zero)
                    }) {
                        val alpha = sp.alpha * mop.alpha
                        val layered = alpha < 0.999f || bright > 1.001f
                        if (layered) {
                            layerPaint.alpha = alpha
                            layerPaint.colorFilter = if (bright > 1.001f) brightness(bright) else null
                            drawContext.canvas.saveLayer(Rect(-40f, -60f, 140f, 140f), layerPaint)
                        }
                        val winged = System.currentTimeMillis() < wingsState.value
                        drawCharacter(
                            spec = spec,
                            index = index,
                            emo = emo,
                            t = t,
                            blink = kf(cycle(t, blinkPeriod), BLINK_AT, BLINK, CssEase),
                            lookX = lookXState.value,
                            lookY = lookYState.value,
                            wings = winged,
                            body = drawBody,
                            glow = if (locked) null else glowFor,
                            faceless = locked
                        )
                        if (layered) drawContext.canvas.restore()
                    }
                }
            })
        }
        if (nameTag != null) {
            Text(
                text = nameTag!!,
                color = Color(0xFF2A0F4A),
                style = TextStyle(fontSize = with(LocalDensity.current) { (maxWidth.toPx() * 0.13f).coerceAtLeast(22f).toSp() }, fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .wrapContentWidth(unbounded = true)
                    .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        if (showNumber) {
            NumberBadge(
                value = value,
                text = badgeText ?: if (locked) "?" else value.toString(),
                tileWidthPx = with(LocalDensity.current) { sizeDp.toPx() },
                fontSizeMultiplier = fontSizeMultiplier,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val sm = spawnMs()
                        val vm = vanishMs()
                        var s = 1f
                        var a = 1f
                        if (sm < SPAWN_MS) {
                            val p = clamp01((sm - SPAWN_SPRITE_AT - 80f) / 320f)
                            s = kf(p, NUM_POP_AT, NUM_POP)
                        }
                        if (vm >= 0f) a = 1f - clamp01(vm / 260f)
                        scaleX = s
                        scaleY = s
                        alpha = a
                    }
            )
        }
        if (showEffects) {
            // Effects sit above the number, like the prototype's effects layer.
            Spacer(modifier = Modifier.fillMaxSize().drawBehind {
                val sm = spawnMs()
                if (sm < SPAWN_MS && sm >= 0f) drawPortal(spec.glow, sm)
                val mm = mergeMs()
                if (mm in 0f..MERGE_MS) drawMergeBurst(spec, mm, particleSeed)
                val vm = vanishMs()
                if (vm in 0f..1000f) drawBreakBits(spec, vm, index)
            })
        }
    }
}

@Composable
private fun NumberBadge(value: Int, text: String, tileWidthPx: Float, fontSizeMultiplier: Float, modifier: Modifier) {
    val density = LocalDensity.current
    val digits = text.length
    val factor = when {
        digits >= 5 -> 0.2f
        digits == 4 -> 0.245f
        digits == 3 -> 0.29f
        else -> 0.34f
    }
    val fontSize = with(density) { (tileWidthPx * factor * fontSizeMultiplier.coerceIn(0.6f, 1.4f)).toSp() }
    val outline = with(density) { (tileWidthPx * 0.044f).coerceAtLeast(2f) }
    val base = TextStyle(
        fontSize = fontSize,
        fontFamily = SpriteNumberFont,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = fontSize,
        letterSpacing = (-0.01f).em,
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
            alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
            trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None
        )
    )
    // Baloo 2 sits high in its line box; this puts the digits where the prototype has them (bottom ~94 %).
    val drop = with(density) { (tileWidthPx * 0.095f).toDp() }
    Box(modifier = modifier.offset(y = drop)) {
        Text(
            text = text,
            color = Color(0xFF2A0F4A),
            style = base.copy(
                drawStyle = Stroke(width = outline, join = StrokeJoin.Round),
                shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, tileWidthPx * 0.053f), 0f)
            )
        )
        Text(text = text, color = if (value < 0) Color(0xFFE9DDFF) else Color.White, style = base)
    }
}

// ---------------------------------------------------------------------------------------------
// Motion
// ---------------------------------------------------------------------------------------------

/** The emotion's looping body language, in % of the sprite (translate) and degrees. */
private fun moodLoop(emo: SpriteEmotion, t: Float, p: Pose) {
    p.reset(0.88f)
    val io = CssEaseInOut
    when (emo) {
        SpriteEmotion.HAPPY -> {
            val c = cycle(t, 0.8f)
            p.ty = kf(c, BOUNCE_AT, BOUNCE_TY, io); p.sx = kf(c, BOUNCE_AT, BOUNCE_SX, io); p.sy = kf(c, BOUNCE_AT, BOUNCE_SY, io)
        }
        SpriteEmotion.EXCITED -> {
            val c = cycle(t, 1f)
            p.ty = kf(c, HOPL_AT, HOPL_TY, io); p.sx = kf(c, HOPL_AT, HOPL_SX, io); p.sy = kf(c, HOPL_AT, HOPL_SY, io)
        }
        SpriteEmotion.LOVE -> p.rot = kf(cycle(t, 1.6f), A3, SWAY, io)
        SpriteEmotion.WORRIED -> p.tx = kf(cycle(t, 0.22f), SHIVER_AT, SHIVER)
        SpriteEmotion.DIZZY -> p.rot = kf(cycle(t, 1f), A3, WOBBLE, io)
        SpriteEmotion.SAD -> {
            val c = cycle(t, 2.2f)
            p.ty = kf(c, A3, DROOP_TY, io); p.sx = kf(c, A3, DROOP_SX, io); p.sy = kf(c, A3, DROOP_SY, io)
        }
        SpriteEmotion.PROUD -> {
            val c = cycle(t, 0.9f)
            p.ty = kf(c, A3, PROUD_TY, io); val s = kf(c, A3, PROUD_S, io); p.sx = s; p.sy = s
        }
        SpriteEmotion.GRUMPY -> {
            val c = cycle(t, 2.6f)
            p.sx = kf(c, HUFF_AT, HUFF_SX, io); p.sy = kf(c, HUFF_AT, HUFF_SY, io)
        }
        SpriteEmotion.THINKING -> p.rot = kf(cycle(t, 2.4f), A3, TILT, io)
        else -> Unit
    }
}

private fun dirSign(d: SpriteDir): Pair<Float, Float> = when (d) {
    SpriteDir.LEFT -> -1f to 0f
    SpriteDir.RIGHT -> 1f to 0f
    SpriteDir.UP -> 0f to -1f
    SpriteDir.DOWN -> 0f to 1f
}

private fun SpriteDir.horizontal() = this == SpriteDir.LEFT || this == SpriteDir.RIGHT

/** Squash-side origins in the prototype: the wall the sprite pressed against. */
private fun Pose.originFor(d: SpriteDir) {
    when (d) {
        SpriteDir.LEFT -> { ox = 0f; oy = 0.6f }
        SpriteDir.RIGHT -> { ox = 1f; oy = 0.6f }
        SpriteDir.UP -> { ox = 0.5f; oy = 0f }
        SpriteDir.DOWN -> { ox = 0.5f; oy = 1f }
    }
}

private fun motionJump(m: SpriteMotion, p: Float, out: Pose) {
    when (m.kind) {
        SpriteMotionKind.BONK -> {
            val (kx, ky) = dirSign(m.dir)
            val e = CssEaseOut.transform(p)
            val v = kf(e, BONK_JUMP_AT, BONK_JUMP)
            out.tx += kx * v; out.ty += ky * v
        }
        SpriteMotionKind.HOP -> hopJump(p, m.hop, out)
        SpriteMotionKind.FIDGET -> when (m.variant) {
            1 -> out.rot += kf(CssEaseInOut.transform(p), EVEN4, FIDGET1_R)
            2 -> hopJump(p, 12f, out)
            else -> Unit
        }
        else -> Unit
    }
}

private fun hopJump(p: Float, height: Float, out: Pose) {
    val e = CssEaseOut.transform(p)
    // the leg from the top back down eases in, like the prototype's keyframe
    out.ty += when {
        e < 0.15f -> 0f
        e < 0.5f -> -height * (e - 0.15f) / 0.35f
        e < 0.82f -> -height * (1f - CssEaseIn.transform((e - 0.5f) / 0.32f))
        else -> 0f
    }
}

private fun motionSquash(m: SpriteMotion, p: Float, out: Pose) {
    when (m.kind) {
        SpriteMotionKind.STRETCH -> {
            val e = CssEaseOut.transform(p)
            val long = kf(e, STRETCH_AT, STRETCH_LONG)
            val short = kf(e, STRETCH_AT, STRETCH_SHORT)
            if (m.dir.horizontal()) { out.sx = long; out.sy = short } else { out.sx = short; out.sy = long }
        }
        SpriteMotionKind.LAND -> {
            val e = CssEaseOut.transform(p)
            out.originFor(m.dir)
            if (m.dir.horizontal()) { out.sx = kf(e, LAND_AT, LAND_H_X); out.sy = kf(e, LAND_AT, LAND_H_Y) }
            else { out.sx = kf(e, LAND_AT, LAND_V_X); out.sy = kf(e, LAND_AT, LAND_V_Y) }
        }
        SpriteMotionKind.BONK -> {
            val e = CssEaseOut.transform(p)
            out.originFor(m.dir)
            if (m.dir.horizontal()) { out.sx = kf(e, BONK_AT, BONK_H_X); out.sy = kf(e, BONK_AT, BONK_H_Y) }
            else { out.sx = kf(e, BONK_AT, BONK_H_Y); out.sy = kf(e, BONK_AT, BONK_H_X) }
        }
        SpriteMotionKind.FIDGET -> {
            val e = CssEaseInOut.transform(p)
            when (m.variant) {
                0 -> { out.sx = kf(e, EVEN5, FIDGET0_X); out.sy = kf(e, EVEN5, FIDGET0_Y) }
                2 -> hopSquash(p, out)
                3 -> { out.rot = kf(e, EVEN4, FIDGET3_R); val s = kf(e, EVEN4, FIDGET3_S); out.sx = s; out.sy = s }
                else -> Unit
            }
        }
        SpriteMotionKind.HOP -> hopSquash(p, out)
        SpriteMotionKind.REWIND -> {
            val e = CssEaseOut.transform(p)
            out.alpha = kf(e, REWIND_AT, REWIND_A)
            val s = kf(e, REWIND_AT, REWIND_S); out.sx = s; out.sy = s
            out.rot = kf(e, REWIND_AT, REWIND_R)
        }
        SpriteMotionKind.SPIN -> {
            val e = CssEaseInOut.transform(p)
            out.rot = 360f * e
            val s = kf(e, A3, SPIN_S); out.sx = s; out.sy = s
        }
    }
}

private fun hopSquash(p: Float, out: Pose) {
    out.sx = kf(p, HOP_SQ_AT, HOP_SQ_X)
    out.sy = kf(p, HOP_SQ_AT, HOP_SQ_Y)
}

// ---------------------------------------------------------------------------------------------
// Static art (rendered once per sprite and size)
// ---------------------------------------------------------------------------------------------

private fun renderBitmap(widthPx: Int, heightPx: Int, density: Density, block: DrawScope.() -> Unit): ImageBitmap {
    val bitmap = ImageBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1))
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = androidx.compose.ui.graphics.Canvas(bitmap),
        size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        block = block
    )
    return bitmap
}

/** Room around the card for its outer glow. */
private fun cardMargin(w: Float) = w * 0.3f

/** Gaussian blur for a CSS blur length, as an Android mask filter. */
private fun cssBlur(blurPx: Float): BlurMaskFilter? {
    val sigma = blurPx / 2f
    val radius = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.5f)
    return BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
}

/**
 * The tile card from the prototype's CSS: radial fill, a soft outer glow in the sprite's colour,
 * a crisp light rim along the top, a soft shade along the bottom, and a translucent border.
 */
private fun renderCard(spec: SpriteSpec, legend: Boolean, locked: Boolean, w: Float, h: Float, density: Density): ImageBitmap {
    val m = cardMargin(w)
    return renderBitmap((w + 2 * m).toInt(), (h + 2 * m).toInt(), density) {
        withTransform({ translate(m, m) }) {
            val nc = drawContext.canvas.nativeCanvas
            val r = w * 0.2f
            // outer glow (box-shadow 0 0 14% glow/.6; the Ember King gets a bigger gold one)
            val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = (if (legend) Color(0xD9FFC83C) else spec.glow.copy(alpha = 0.6f)).toArgb()
                maskFilter = cssBlur(w * if (legend) 0.22f else 0.14f)
            }
            nc.drawRoundRect(0f, 0f, w, h, r, r, glowPaint)
            val cardPath = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r))) }
            clipPath(cardPath) {
                val t2 = lerp(spec.t0, Color.White, 0.38f)
                drawRect(
                    brush = Brush.radialGradient(
                        0f to t2, 0.52f to spec.t0, 1f to spec.t1,
                        center = Offset(w * 0.32f, h * 0.18f),
                        radius = sqrt((w * 0.68f) * (w * 0.68f) + (h * 0.82f) * (h * 0.82f))
                    ),
                    size = Size(w, h)
                )
                // inset 0 -6% 10% black/.28: soft shade along the bottom
                val shade = Path().apply {
                    addRect(Rect(-w, -h, 2 * w, 2 * h))
                    val hole = Path().apply { addRoundRect(RoundRect(0f, -w * 0.06f, w, h - w * 0.06f, CornerRadius(r))) }
                    op(this, hole, PathOperation.Difference)
                }
                val shadePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.Black.copy(alpha = 0.28f).toArgb()
                    maskFilter = cssBlur(w * 0.1f)
                }
                nc.drawPath(shade.asAndroidPath(), shadePaint)
                // inset 0 3% 0 white/.28: a crisp light rim along the top edge
                val rim = Path().apply {
                    val shifted = Path().apply { addRoundRect(RoundRect(0f, w * 0.03f, w, h + w * 0.03f, CornerRadius(r))) }
                    op(cardPath, shifted, PathOperation.Difference)
                }
                drawPath(rim, Color.White.copy(alpha = if (legend) 0.35f else 0.28f))
            }
            val bw = (w * 0.022f).coerceAtLeast(1.5f)
            drawRoundRect(
                color = if (legend) Color(0xFFFFE27A) else Color.White.copy(alpha = 0.4f),
                topLeft = Offset(bw / 2, bw / 2),
                size = Size(w - bw, h - bw),
                style = Stroke(width = bw),
                cornerRadius = CornerRadius(r - bw / 2)
            )
        }
    }
}


/** The character's body, accessories and gloss, plus a blurred silhouette for its glow. */
private fun renderBody(spec: SpriteSpec, bodyPx: Int, u: Float, locked: Boolean, density: Density): BodyArt {
    val body = renderBitmap(bodyPx, bodyPx, density) {
        withTransform({
            scale(bodyPx / 120f, bodyPx / 120f, pivot = Offset.Zero)
            translate(10f, 10f)
        }) { drawSpriteBody(spec) }
        if (locked) drawRect(Color(0xFF0A0420).copy(alpha = 0.75f), blendMode = BlendMode.SrcIn)
    }
    if (locked) return BodyArt(body, null, 0f, 0f)
    // drop-shadow(0 0 4% glow): blur radius ≈ 2.1 character units
    val paint = android.graphics.Paint().apply { maskFilter = cssBlur(u * 4.2f) }
    val offset = IntArray(2)
    val glow = body.asAndroidBitmap().extractAlpha(paint, offset).asImageBitmap()
    return BodyArt(body, glow, offset[0].toFloat(), offset[1].toFloat())
}

private val LockedCardFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }.also { it.timesAssign(ColorMatrix().apply { setToScale(0.6f, 0.6f, 0.6f, 1f) }) })
}

private fun brightness(b: Float): ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(b, b, b, 1f) })

/** The Ember King's card shine: a light band sweeping across every 2.8 s. */
private fun DrawScope.drawLegendShine(w: Float, h: Float, t: Float) {
    val p = cycle(t, 2.8f)
    val x = -0.91f * w + p * 4.16f * w
    val dx = 0.906f * w * 0.42f
    val dy = 0.423f * w * 0.42f
    drawRoundRect(
        brush = Brush.linearGradient(
            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.5f), 1f to Color.Transparent,
            start = Offset(x - dx, h / 2 - dy), end = Offset(x + dx, h / 2 + dy)
        ),
        size = Size(w, h),
        cornerRadius = CornerRadius(w * 0.2f)
    )
}

// ---------------------------------------------------------------------------------------------
// Effects
// ---------------------------------------------------------------------------------------------

/** A ring opens under the sprite and a beam of its colour drops in (620 ms). */
private fun DrawScope.drawPortal(glow: Color, ms: Float) {
    val w = size.width
    val h = size.height
    val p = clamp01(ms / 620f)
    val e = CssEaseOut.transform(p)
    // ring: scale 0 → 1 (40%) → 1.1, fading in then out
    val rs = kf(e, f(0f, 0.4f, 1f), f(0f, 1f, 1.1f))
    val ra = kf(e, f(0f, 0.4f, 1f), f(0f, 1f, 0f))
    val rw = w * 0.85f * rs
    val rh = h * 0.26f * rs
    val cy = h * 0.5f + h * 0.36f
    if (ra > 0.01f) {
        drawOval(glow.copy(alpha = 0.35f * ra), topLeft = Offset(w / 2 - rw / 2 - 4f, cy - rh / 2 - 4f), size = Size(rw + 8f, rh + 8f), style = Stroke(width = w * 0.05f))
        drawOval(glow.copy(alpha = ra), topLeft = Offset(w / 2 - rw / 2, cy - rh / 2), size = Size(rw, rh), style = Stroke(width = 3.dp.toPx()))
    }
    // beam: grows up from the ring, then fades
    val bs = kf(e, f(0f, 0.35f, 1f), f(0f, 1f, 1f))
    val ba = kf(e, f(0f, 0.35f, 1f), f(0f, 0.85f, 0f))
    if (ba > 0.01f && bs > 0.01f) {
        val bw = w * 0.5f
        val bh = h * 1.1f * bs
        val bottom = h * 0.5f - h * 0.1f + h * 0.55f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0f), glow.copy(alpha = ba)), startY = bottom - bh, endY = bottom),
            topLeft = Offset(w / 2 - bw / 2, bottom - bh),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(bw * 0.4f)
        )
    }
}

private val SwirlColors = arrayOf(
    0f to Color.Transparent, 0.10f to Color.Transparent, 0.25f to Color.Unspecified, 0.32f to Color.White,
    0.45f to Color.Transparent, 0.55f to Color.Transparent, 0.72f to Color.Unspecified, 0.80f to Color.White,
    0.92f to Color.Transparent, 1f to Color.Transparent
)

/** Merge: a bright flash, two comet arcs swirling outwards, and element bits flying out. */
private fun DrawScope.drawMergeBurst(spec: SpriteSpec, ms: Float, seed: Int) {
    val w = size.width
    val c = center
    // flash (480 ms): white → glow → clear, growing 0.2 → 1.2
    if (ms < 480f) {
        val e = CssEaseOut.transform(ms / 480f)
        val s = 0.2f + e
        val r = w * 1.06f * s
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color.White.copy(alpha = 1f - e), 0.3f to spec.glow.copy(alpha = 1f - e), 0.68f to Color.Transparent,
                center = c, radius = r
            ),
            radius = r * 0.68f,
            center = c
        )
    }
    // swirl (620 ms): rotate 0 → 200° → 420°, scale 0.4 → 1 → 1.25, opacity 0 → 1 → 0
    if (ms < 620f) {
        val e = CssEaseOut.transform(ms / 620f)
        val rot = kf(e, A3, f(0f, 200f, 420f))
        val s = kf(e, A3, f(0.4f, 1f, 1.25f))
        val a = kf(e, A3, f(0f, 1f, 0f))
        val radius = w * 0.533f * s
        val stops = SwirlColors.map { (pos, col) -> pos to (if (col == Color.Unspecified) spec.glow else col).let { it.copy(alpha = it.alpha * a) } }.toTypedArray()
        withTransform({ rotate(rot - 90f, pivot = c) }) {
            drawCircle(Brush.sweepGradient(*stops, center = c), radius = radius, center = c, style = Stroke(width = w * 0.1f * s))
        }
    }
    // particles (600–900 ms each, staggered up to 60 ms)
    val rnd = Random(seed)
    repeat(12) {
        val sz = w * 0.11f * (0.6f + rnd.nextFloat() * 0.7f)
        val ang = rnd.nextFloat() * 2f * PI.toFloat()
        val dist = w * (0.45f + rnd.nextFloat() * 0.55f)
        val dur = 600f + rnd.nextFloat() * 300f
        val delayMs = rnd.nextFloat() * 60f
        val rot = rnd.nextFloat() * 360f
        val p = (ms - delayMs) / dur
        if (p in 0f..1f) {
            val e = Burst.transform(p)
            drawParticle(spec.bits, Offset(c.x + cos(ang) * dist * e, c.y + sin(ang) * dist * e), sz * (1f - 0.75f * e), rot * e, 1f - e)
        }
    }
}

/** Hammer hit: diamond shards plus a few of the sprite's own bits. */
private fun DrawScope.drawBreakBits(spec: SpriteSpec, ms: Float, index: Int) {
    val w = size.width
    val c = center
    val rnd = Random(97 + index)
    repeat(26) { k ->
        val shard = k < 18
        val sz = w * 0.11f * (0.6f + rnd.nextFloat() * 0.7f)
        val ang = rnd.nextFloat() * 2f * PI.toFloat()
        val dist = w * (0.45f + rnd.nextFloat() * 0.55f)
        val dur = 600f + rnd.nextFloat() * 300f
        val delayMs = rnd.nextFloat() * 60f
        val rot = rnd.nextFloat() * 360f
        val p = (ms - delayMs) / dur
        if (p in 0f..1f) {
            val e = Burst.transform(p)
            drawParticle(if (shard) SpriteParticle.SHARD else spec.bits, Offset(c.x + cos(ang) * dist * e, c.y + sin(ang) * dist * e), sz * (1f - 0.75f * e), rot * e, 1f - e)
        }
    }
}

/** One element bit, [s] px across, centred on [at]. */
internal fun DrawScope.drawParticle(kind: SpriteParticle, at: Offset, s: Float, rotation: Float, alpha: Float) {
    if (alpha <= 0.01f || s <= 0.5f) return
    withTransform({
        translate(at.x - s / 2, at.y - s / 2)
        rotate(rotation, pivot = Offset(s / 2, s / 2))
        scale(s, s, pivot = Offset.Zero)
    }) {
        val diag = Offset(0f, 0f) to Offset(1f, 1f)
        when (kind) {
            SpriteParticle.EMBER -> drawCircle(
                Brush.radialGradient(0f to Color(0xFFFFF3A6), 0.55f to Color(0xFFFF8A2A), 0.72f to Color(0x00FF5014), center = Offset(0.5f, 0.5f), radius = 0.707f),
                radius = 0.6f, center = Offset(0.5f, 0.5f), alpha = alpha
            )
            SpriteParticle.DROP -> drawPath(Shapes.pDrop, Brush.linearGradient(listOf(Color(0xFFE2FAFF), Color(0xFF2E8BFF)), diag.first, diag.second), alpha = alpha)
            SpriteParticle.LEAF -> drawPath(Shapes.pLeaf, Brush.linearGradient(listOf(Color(0xFFD4FF8A), Color(0xFF3FA52C)), diag.first, diag.second), alpha = alpha)
            SpriteParticle.PETAL -> drawPath(Shapes.pLeaf, Brush.linearGradient(listOf(Color(0xFFFFE0EF), Color(0xFFFF6FA8)), diag.first, diag.second), alpha = alpha)
            SpriteParticle.SHARD -> drawPath(Shapes.pDiamond, Brush.linearGradient(listOf(Color.White, Color(0xFF8FD8FF)), diag.first, diag.second), alpha = alpha)
            SpriteParticle.STAR -> drawPath(Shapes.pStar, Color(0xFFFFE58A), alpha = alpha)
            SpriteParticle.DUST -> {
                drawCircle(Color(0xFFB57BFF).copy(alpha = 0.45f * alpha), radius = 0.85f, center = Offset(0.5f, 0.5f))
                drawCircle(Color(0xFFEBD3FF), radius = 0.5f, center = Offset(0.5f, 0.5f), alpha = alpha)
            }
            SpriteParticle.ROCKBIT -> drawRoundRect(Color(0xFF5B5470), size = Size(1f, 1f), cornerRadius = CornerRadius(0.3f), alpha = alpha)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Drawing the character (in its 100-unit box)
// ---------------------------------------------------------------------------------------------

private fun DrawScope.oval(color: Color, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Float = 1f) =
    drawOval(color = color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2), alpha = alpha)

private fun DrawScope.line(path: Path, color: Color, width: Float, alpha: Float = 1f) =
    drawPath(path, color, alpha = alpha, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))

private fun DrawScope.at(x: Float, y: Float, scale: Float = 1f, rotate: Float = 0f, block: DrawScope.() -> Unit) =
    withTransform({
        translate(x, y)
        if (rotate != 0f) rotate(rotate, pivot = Offset.Zero)
        if (scale != 1f) scale(scale, scale, pivot = Offset.Zero)
    }, block)

private fun DrawScope.label(text: String, x: Float, y: Float, sizeU: Float, fill: Color, stroke: Color?, strokeW: Float = sizeU * 0.14f) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = labelPaint
    paint.textSize = sizeU
    if (stroke != null) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = strokeW
        paint.color = stroke.toArgb()
        canvas.drawText(text, x, y, paint)
    }
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = fill.toArgb()
    canvas.drawText(text, x, y, paint)
}

private val flickMatrix = Matrix()

private fun DrawScope.drawCharacter(
    spec: SpriteSpec,
    index: Int,
    emo: SpriteEmotion,
    t: Float,
    blink: Float,
    lookX: Float,
    lookY: Float,
    wings: Boolean,
    body: DrawScope.(ColorFilter?) -> Unit,
    glow: (DrawScope.() -> Unit)? = null,
    faceless: Boolean = false
) {
    val e = spec.element
    val fiery = e == SpriteElement.FIRE || e == SpriteElement.KING
    val s = if (e == SpriteElement.ROCK) 0.94f else 0.86f + index * 0.014f
    // breathe (2.6 s, slower when asleep): scale(.98, 1.04) from the feet
    val breathP = cycle(t, if (emo == SpriteEmotion.SLEEPY) 4.2f else 2.6f)
    val bx = kf(breathP, A3, BREATH_X, CssEaseInOut)
    val by = kf(breathP, A3, BREATH_Y, CssEaseInOut)
    val feet = if (e == SpriteElement.SUN) 100f else 89f

    withTransform({
        scale(s, s, pivot = Offset(50f, 89f))
        scale(bx, by, pivot = Offset(50f, feet))
    }) {
        // wings, only for a moment after a big merge (flap 0.35 s)
        if (wings) {
            val flap = kf(cycle(t, 0.35f), A3, FLAP, CssEaseInOut)
            val wingBrush = BrushCache.get("wing") { Brush.linearGradient(listOf(Color.White, Color(0xFFFFE7A6)), start = Offset(30f, 44f), end = Offset(2f, 76f)) }
            val wingBrushR = BrushCache.get("wingR") { Brush.linearGradient(listOf(Color.White, Color(0xFFFFE7A6)), start = Offset(98f, 44f), end = Offset(70f, 76f)) }
            withTransform({ rotate(flap, pivot = Offset(30f, 62.8f)) }) {
                drawPath(Shapes.wingL, wingBrush)
                line(Shapes.wingL, Color(0xFFC99A2E), 1.4f)
                line(Shapes.wingLVeins, Color(0xFFE5C26A), 1.2f)
            }
            withTransform({ rotate(-flap, pivot = Offset(70f, 62.8f)) }) {
                drawPath(Shapes.wingR, wingBrushR)
                line(Shapes.wingR, Color(0xFFC99A2E), 1.4f)
                line(Shapes.wingRVeins, Color(0xFFE5C26A), 1.2f)
            }
        }
        glow?.invoke(this)
        if (fiery) {
            // flick (0.9 s, alternate): stretch and lean from side to side
            val raw = cycle(t, 1.8f)
            val p = if (raw < 0.5f) raw * 2f else (1f - raw) * 2f
            val fx = kf(p, A3, FLICK_X, CssEaseInOut)
            val fy = kf(p, A3, FLICK_Y, CssEaseInOut)
            val skew = kf(p, A3, FLICK_SKEW, CssEaseInOut)
            flickMatrix.reset()
            flickMatrix[1, 0] = kotlin.math.tan(Math.toRadians(skew.toDouble())).toFloat()
            withTransform({
                translate(50f, 89f)
                transform(flickMatrix)
                scale(fx, fy, pivot = Offset.Zero)
                translate(-50f, -89f)
            }) { body(null) }
        } else {
            body(null)
        }
        if (faceless) return@withTransform

        // face (drawn live: blinks, looks around, changes with emotion)
        val fy = when (e) {
            SpriteElement.STAR -> -1f
            SpriteElement.WATER -> 3f
            else -> 0f
        }
        withTransform({ translate(0f, fy) }) { drawFace(spec, emo, t, blink, lookX, lookY) }

        if (fiery) {
            // embers rise and fade (1.8 s)
            for (k in 0 until 3) {
                val p = cycle(t, 1.8f, EMBER_D[k])
                val e1 = CssEaseOut.transform(p)
                val a = kf(p, FADE_AT_20, FADE_IN_OUT, CssEaseOut)
                drawCircle(EMBER_C[k], EMBER_R[k] * (1f - 0.7f * e1), Offset(EMBER_X[k], EMBER_Y[k] - 18f * e1), alpha = a)
            }
        }
        if (e == SpriteElement.STAR || e == SpriteElement.KING || e == SpriteElement.CRYSTAL || e == SpriteElement.SUN) {
            twinkle(14f, 30f, 1f, t, 0f, Color.White)
            twinkle(88f, 44f, 0.7f, t, 0.5f, Color.White)
            twinkle(84f, 16f, 0.5f, t, 0.9f, Color.White)
        }

        drawEmotionFx(emo, t)
    }
}

/** Everything about a sprite that doesn't animate by itself, in the 100-unit box. Drawn once into a bitmap. */
private fun DrawScope.drawSpriteBody(spec: SpriteSpec) {
    val e = spec.element
    val fiery = e == SpriteElement.FIRE || e == SpriteElement.KING
    val L = lerp(spec.c3, Color(0xFF1A0830), 0.42f)
    // behind the body
    when (e) {
        SpriteElement.SUN -> {
            drawPath(Shapes.rays, BrushCache.get("rays${spec.name}") { Brush.radialGradient(listOf(spec.c2, spec.c3), center = Offset(50f, 60f), radius = 40f) })
            line(Shapes.rays, L, 1.6f)
        }
        SpriteElement.COSMIC -> withTransform({ rotate(-10f, pivot = Offset(50f, 72f)) }) {
            drawPath(Shapes.ringBack, ringBrush(), style = Stroke(4.5f, cap = StrokeCap.Round), alpha = 0.9f)
        }
        SpriteElement.LEAF -> {
            line(Shapes.stem, Color(0xFF2E6E1A), 3f)
            leaf(Shapes.leafL, Offset(25f, 10f), Offset(51f, 28f))
            leaf(Shapes.leafR, Offset(52f, 6f), Offset(77f, 26f))
            line(Shapes.leafVeins, Color(0xFFF2FFD0), 1f, 0.7f)
        }
        else -> Unit
    }
    // body
    val bodyBrush = BrushCache.get("body${spec.name}") {
        if (fiery) {
            Brush.radialGradient(0f to spec.c1, 0.5f to spec.c2, 1f to spec.c3, center = Offset(50f, 78f), radius = 72f)
        } else {
            val (cx, cy, r) = when (e) {
                SpriteElement.WATER -> Triple(40f, 48f, 52f)
                SpriteElement.SUN -> Triple(40f, 50f, 36f)
                SpriteElement.COSMIC -> Triple(40f, 48f, 42f)
                SpriteElement.STAR -> Triple(44f, 44f, 46f)
                else -> Triple(40f, 46f, 54f)
            }
            Brush.radialGradient(0f to spec.c1, 0.5f to spec.c2, 1f to spec.c3, center = Offset(cx, cy), radius = r)
        }
    }
    val bodyPath = Shapes.body(e)
    drawPath(bodyPath, bodyBrush)
    line(bodyPath, L, 1.8f)
    if (fiery) drawPath(Shapes.flameCore, spec.c1, alpha = 0.55f)
    oval(spec.c3, 50f, 83f, 25f, 5.5f, 0.35f)

    // surface detail
    if (e == SpriteElement.COSMIC) {
        drawCircle(Color.White, 1.3f, Offset(35f, 47f), alpha = 0.9f)
        drawCircle(Color.White, 1f, Offset(64f, 44f), alpha = 0.8f)
        drawCircle(Color.White, 1.5f, Offset(70f, 62f), alpha = 0.7f)
        drawCircle(Color.White, 1f, Offset(28f, 66f), alpha = 0.8f)
        line(Shapes.cosmicShine, Color.White, 1.4f, 0.35f)
    }
    if (e == SpriteElement.ROCK) {
        line(Shapes.rockCracks, Color(0xFFB57BFF), 2f, 0.9f)
    }
    if (e == SpriteElement.WAVE) {
        drawPath(Shapes.crest, spec.c1, alpha = 0.95f)
        line(Shapes.crest, L, 1.4f, 0.95f)
        drawCircle(Color.White, 3.2f, Offset(29f, 26f), alpha = 0.85f)
        drawCircle(Color.White, 2.6f, Offset(73f, 30f), alpha = 0.8f)
        drawCircle(Color.White, 2f, Offset(67f, 18f), alpha = 0.7f)
    }

    // gloss
    val (hx, hy) = when (e) {
        SpriteElement.STAR -> 44f to 38f
        SpriteElement.WATER -> 38f to 48f
        SpriteElement.SUN -> 38f to 46f
        SpriteElement.FIRE, SpriteElement.KING -> 36f to 48f
        else -> 36f to 44f
    }
    at(hx, hy, rotate = -35f) { oval(Color.White, 0f, 0f, 7.5f, 4.2f, 0.75f) }
    drawCircle(Color.White, 1.7f, Offset(hx + 8f, hy - 5f), alpha = 0.85f)

    // front accessories
    when (e) {
        SpriteElement.COSMIC -> withTransform({ rotate(-10f, pivot = Offset(50f, 72f)) }) {
            drawPath(Shapes.ringFront, ringBrush(), style = Stroke(4.5f, cap = StrokeCap.Round))
        }
        SpriteElement.ICE -> {
            val ice = BrushCache.get("ice") { Brush.linearGradient(listOf(Color.White, Color(0xFF8FD8FF)), start = Offset(28f, 6f), end = Offset(72f, 39f)) }
            listOf(Shapes.iceMid to Shapes.iceMidFacet, Shapes.iceL to Shapes.iceLFacet, Shapes.iceR to Shapes.iceRFacet).forEach { (shard, facet) ->
                drawPath(shard, ice)
                line(shard, L, 1.2f)
                drawPath(facet, Color(0xFF9EDCFF), alpha = 0.8f)
                line(facet, L, 1.2f, 0.8f)
            }
        }
        SpriteElement.FLOWER -> {
            leaf(Shapes.sideLeaf, Offset(20f, 24f), Offset(38f, 37f), 1.3f)
            val petal = BrushCache.get("petal") { Brush.linearGradient(listOf(Color(0xFFFFE3F1), Color(0xFFFF6FA8)), start = Offset(-5f, -13.5f), end = Offset(5f, -0.5f)) }
            at(63f, 26f) {
                for (k in 0 until 5) {
                    withTransform({ rotate(k * 72f, pivot = Offset.Zero) }) {
                        drawOval(petal, topLeft = Offset(-5f, -13.5f), size = Size(10f, 13f))
                        drawOval(Color(0xFFB0306A), topLeft = Offset(-5f, -13.5f), size = Size(10f, 13f), style = Stroke(1.1f))
                    }
                }
                drawCircle(Color(0xFFFFD84D), 4.2f, Offset.Zero)
                drawCircle(Color(0xFFC08A10), 4.2f, Offset.Zero, style = Stroke(1.1f))
            }
        }
        SpriteElement.CRYSTAL -> {
            val lilac = BrushCache.get("lilac") { Brush.linearGradient(listOf(Color.White, Color(0xFFB9A4FF)), start = Offset(30f, 6f), end = Offset(70f, 34f)) }
            Shapes.crystals.forEach { drawPath(it, lilac); line(it, L, 1.1f) }
            drawPath(Shapes.crystalFacet, Color(0xFFA994FF), alpha = 0.7f)
            line(Shapes.crystalFacet, L, 1.1f, 0.7f)
            drawCircle(Color(0xFFFF6FB8), 3.2f, Offset(50f, 33f))
            drawCircle(L, 3.2f, Offset(50f, 33f), style = Stroke(1f))
        }
        SpriteElement.KING -> {
            drawPath(Shapes.crown, BrushCache.get("crown") { Brush.verticalGradient(listOf(GoldLight, Gold, GoldDark), startY = 3f, endY = 30f) })
            line(Shapes.crown, Color(0xFF8A4A08), 1.5f)
            drawRoundRect(GoldDark, topLeft = Offset(31f, 27f), size = Size(38f, 6f), cornerRadius = CornerRadius(2f))
            drawRoundRect(Color(0xFF8A4A08), topLeft = Offset(31f, 27f), size = Size(38f, 6f), cornerRadius = CornerRadius(2f), style = Stroke(1.5f))
            drawCircle(Color(0xFFFF3D6E), 3.2f, Offset(50f, 20f))
            drawCircle(Color(0xFF8A0E30), 3.2f, Offset(50f, 20f), style = Stroke(1f))
            drawCircle(Color(0xFF47C6FF), 2f, Offset(39f, 24f))
            drawCircle(Color(0xFF47C6FF), 2f, Offset(61f, 24f))
        }
        else -> Unit
    }
}

private fun ringBrush() = BrushCache.get("ring") { Brush.horizontalGradient(listOf(Color(0xFFFFB6F0), Color.White, Color(0xFFB57BFF)), startX = 8f, endX = 92f) }

private fun DrawScope.leaf(path: Path, from: Offset, to: Offset, stroke: Float = 1.5f) {
    drawPath(path, BrushCache.get("leaf${from.x},${from.y}") { Brush.linearGradient(listOf(Color(0xFFD4FF8A), Color(0xFF3FA52C)), start = from, end = to) })
    line(path, Color(0xFF2E6E1A), stroke)
}

/** Sparkle twinkle (1.4 s): fades .2 → 1 and grows .5 → 1.15. */
private fun DrawScope.twinkle(x: Float, y: Float, scale: Float, t: Float, delay: Float, color: Color) {
    val p = cycle(t, 1.4f, delay)
    val k = kf(p, A3, TWINKLE_S, CssEaseInOut)
    val a = kf(p, A3, TWINKLE_A, CssEaseInOut)
    at(x, y, scale * k) { drawPath(Shapes.sparkle, color, alpha = a) }
}

private fun DrawScope.drawFace(spec: SpriteSpec, emo: SpriteEmotion, t: Float, blink: Float, lookX: Float, lookY: Float) {
    val strongCheeks = emo == SpriteEmotion.HAPPY || emo == SpriteEmotion.LOVE || emo == SpriteEmotion.PROUD || emo == SpriteEmotion.EXCITED
    for (cx in CHEEKS) {
        drawOval(
            brush = BrushCache.get("blush$strongCheeks$cx") {
                Brush.radialGradient(
                    0f to Blush.copy(alpha = if (strongCheeks) 0.9f else 0.5f),
                    0.55f to Blush.copy(alpha = if (strongCheeks) 0.7f else 0.38f),
                    1f to Color.Transparent,
                    center = Offset(cx, 68f), radius = 8f
                )
            },
            topLeft = Offset(cx - 8f, 63.5f),
            size = Size(16f, 9f)
        )
    }
    // brows
    val brow = when (emo) {
        SpriteEmotion.SURPRISED -> "M34 50 Q40 45.5 46 49 M54 49 Q60 45.5 66 50"
        SpriteEmotion.WORRIED, SpriteEmotion.SAD -> "M34 52 Q40 51.5 46 48 M54 48 Q60 51.5 66 52"
        SpriteEmotion.GRUMPY -> "M34 48 L46 52.5 M54 52.5 L66 48"
        SpriteEmotion.THINKING -> "M35 51 Q40 50 45 51 M54 49 Q60 45 66 47.5"
        else -> null
    }
    if (brow != null) line(BrowCache.get(brow), Ink, if (emo == SpriteEmotion.GRUMPY) 2.8f else 2.2f)

    // eyes
    val lx = if (emo == SpriteEmotion.THINKING) 1.8f else lookX
    val ly = if (emo == SpriteEmotion.THINKING) -2.2f else lookY
    for (ex in EYES) {
        at(ex + lx, 60f + ly) {
            when (emo) {
                SpriteEmotion.HAPPY -> line(EyeCache.happy, Ink, 2.4f)
                SpriteEmotion.SLEEPY, SpriteEmotion.SAD -> line(EyeCache.closed, Ink, 2.4f)
                SpriteEmotion.DIZZY -> drawPath(Shapes.spiral, Ink, style = Stroke(1.5f))
                SpriteEmotion.LOVE -> { drawPath(Shapes.heart, Color(0xFFFF4D6D)); drawPath(Shapes.heart, Ink, style = Stroke(1f)) }
                SpriteEmotion.PROUD -> { drawPath(Shapes.starEye, Color(0xFFFFE27A)); drawPath(Shapes.starEye, Ink, style = Stroke(1f, join = StrokeJoin.Round)) }
                else -> {
                    val big = if (emo == SpriteEmotion.SURPRISED) 1.25f else 1f
                    val open = if (emo == SpriteEmotion.SURPRISED) 1f else blink
                    withTransform({ scale(big, big * open, pivot = Offset.Zero) }) {
                        oval(Ink, 0f, 0f, 4.6f, 5.9f)
                        drawCircle(Color.White, 2.05f, Offset(1.5f, -2.3f))
                        drawCircle(Color.White, 0.95f, Offset(-1.5f, 2.4f))
                        if (emo == SpriteEmotion.EXCITED) drawPath(Shapes.eyeSparkle, Color(0xFFFFF6B0))
                        if (emo == SpriteEmotion.GRUMPY) drawPath(EyeCache.lid, lerp(spec.c2, Color.Black, 0.15f))
                    }
                }
            }
        }
    }

    // mouth
    when (emo) {
        SpriteEmotion.HAPPY, SpriteEmotion.PROUD -> {
            drawPath(MouthCache.open, MouthDark)
            line(MouthCache.open, Ink, 1.5f)
            drawPath(MouthCache.tongue, Tongue)
        }
        SpriteEmotion.EXCITED -> {
            oval(MouthDark, 50f, 70.5f, 3.8f, 4.3f)
            drawOval(Ink, topLeft = Offset(46.2f, 66.2f), size = Size(7.6f, 8.6f), style = Stroke(1.4f))
            drawPath(MouthCache.smallTongue, Tongue)
        }
        SpriteEmotion.SURPRISED -> {
            oval(MouthDark, 50f, 71f, 2.5f, 3.2f)
            drawOval(Ink, topLeft = Offset(47.5f, 67.8f), size = Size(5f, 6.4f), style = Stroke(1.3f))
        }
        SpriteEmotion.SLEEPY -> oval(MouthDark, 50f, 70.5f, 1.8f, 2.2f)
        SpriteEmotion.THINKING -> line(MouthCache.side, Ink, 2f)
        SpriteEmotion.SAD -> line(MouthCache.frown, Ink, 2.2f)
        SpriteEmotion.WORRIED, SpriteEmotion.DIZZY -> line(MouthCache.wobble, Ink, 1.9f)
        SpriteEmotion.GRUMPY -> line(MouthCache.flat, Ink, 2.2f)
        SpriteEmotion.LOVE -> line(MouthCache.cat, Ink, 1.9f)
        SpriteEmotion.IDLE -> line(MouthCache.smile, Ink, 2f)
    }
}

private fun DrawScope.drawEmotionFx(emo: SpriteEmotion, t: Float) {
    val io = CssEaseInOut
    when (emo) {
        SpriteEmotion.THINKING -> {
            drawCircle(Color.White, 1.6f, Offset(72f, 36f))
            drawCircle(Color.White, 2.4f, Offset(76f, 31f))
            bubble(t) {
                drawCircle(Color.White, 9f, Offset.Zero)
                drawCircle(Color(0xFF6B35D6), 9f, Offset.Zero, style = Stroke(1.4f))
                label("?", 0f, 4.8f, 14f, Color(0xFF6B35D6), null)
            }
        }
        SpriteEmotion.SURPRISED -> bubble(t) {
            drawCircle(Color(0xFFFF4D6D), 9f, Offset.Zero)
            drawCircle(Color.White, 9f, Offset.Zero, style = Stroke(1.6f))
            label("!", 0f, 5f, 14f, Color.White, null)
        }
        SpriteEmotion.SLEEPY -> listOf(Triple(70f, 34f, 11f), Triple(77f, 25f, 13f), Triple(84f, 15f, 15f)).forEachIndexed { k, (x, y, sz) ->
            // zfloat 2.4 s: drift up and right, fading in then out
            val p = cycle(t, 2.4f, k * 0.8f)
            val e = CssEaseOut.transform(p)
            val a = kf(p, FADE_AT_25, FADE_IN_OUT, CssEaseOut)
            withTransform({ translate(8f * e, -16f * e) }) {
                label(if (k == 2) "Z" else "z", x, y, sz, Color.White.copy(alpha = a), Ink.copy(alpha = a), 0.8f)
            }
        }
        SpriteEmotion.WORRIED -> {
            // drip 1.3 s ease-in: slides down 6 and fades in then out
            val p = cycle(t, 1.3f)
            val dy = 6f * CssEaseIn.transform(p)
            val a = kf(p, FADE_AT_20, FADE_IN_OUT, CssEaseIn)
            withTransform({ translate(0f, dy) }) {
                drawPath(Shapes.sweat, Color(0xFF8FD8FF), alpha = a)
                drawPath(Shapes.sweat, Ink, alpha = a, style = Stroke(1f))
            }
        }
        SpriteEmotion.SAD -> listOf(Shapes.tearL to 0f, Shapes.tearR to 0.5f).forEach { (tear, d) ->
            val p = cycle(t, 1f, d)
            val e = CssEaseIn.transform(p)
            withTransform({ translate(0f, 14f * e) }) {
                drawPath(tear, Color(0xFF9FE4FF), alpha = 1f - e)
                drawPath(tear, Color(0xFF2E6FD8), alpha = 1f - e, style = Stroke(0.6f))
            }
        }
        SpriteEmotion.LOVE -> for ((pos, d) in listOf(Offset(22f, 36f) to 0f, Offset(80f, 32f) to 0.8f)) {
            // heartUp 1.6 s: float up 14, grow .5 → 1.1, fade in then out
            val p = cycle(t, 1.6f, d)
            val e = CssEaseOut.transform(p)
            val a = kf(p, FADE_AT_30, FADE_IN_OUT, CssEaseOut)
            at(pos.x, pos.y - 14f * e, 0.5f + 0.6f * e) { drawPath(Shapes.smallHeart, Color(0xFFFF4D6D), alpha = a) }
        }
        SpriteEmotion.DIZZY -> withTransform({
            translate(50f, 30f)
            scale(1f, 0.45f, pivot = Offset.Zero)
            rotate(cycle(t, 1.1f) * 360f, pivot = Offset.Zero)
        }) {
            drawPath(Shapes.diamondGoldL, Color(0xFFFFE27A))
            drawPath(Shapes.diamondGoldR, Color(0xFFFFE27A))
            drawPath(Shapes.diamondTop, Color.White)
            drawPath(Shapes.diamondBottom, Color.White)
        }
        SpriteEmotion.GRUMPY -> at(76f, 36f, kf(cycle(t, 0.8f), A3, VEIN_S, io)) { line(Shapes.vein, Color(0xFFFF4D6D), 2.2f) }
        SpriteEmotion.EXCITED, SpriteEmotion.PROUD -> {
            twinkle(16f, 46f, 1f, t, 0f, Color(0xFFFFF6B0))
            twinkle(86f, 52f, 0.8f, t, 0.5f, Color(0xFFFFF6B0))
            twinkle(22f, 78f, 0.6f, t, 0.9f, Color(0xFFFFF6B0))
        }
        else -> Unit
    }
}

/** The ? and ! bubbles at the top right, puffing from their lower-left corner (1.6 s). */
private fun DrawScope.bubble(t: Float, block: DrawScope.() -> Unit) {
    val s = kf(cycle(t, 1.6f), A3, BUB_S, CssEaseInOut)
    withTransform({
        translate(84f, 20f)
        scale(s, s, pivot = Offset(-9f, 9f))
    }, block)
}

private object BrowCache {
    private val cache = HashMap<String, Path>()
    fun get(d: String): Path = cache.getOrPut(d) { parse(d) }
}

private object EyeCache {
    val happy by lazy { parse("M-5 1.5 Q0 -4.8 5 1.5") }
    val closed by lazy { parse("M-5 -0.8 Q0 3.6 5 -0.8") }
    val lid by lazy { parse("M-6.2 -7.4 H6.2 V-1.6 Q0 0.6 -6.2 -1.6 Z") }
}

private object MouthCache {
    val smile by lazy { parse("M46.5 68.5 Q50 71.8 53.5 68.5") }
    val open by lazy { parse("M44.5 67 Q50 68 55.5 67 Q54.6 75.5 50 75.5 Q45.4 75.5 44.5 67 Z") }
    val tongue by lazy { parse("M46.6 72.2 Q50 69.8 53.4 72.2 Q50 75 46.6 72.2 Z") }
    val smallTongue by lazy { parse("M47.6 72.6 Q50 71 52.4 72.6 Q50 74.6 47.6 72.6 Z") }
    val side by lazy { parse("M47 70.5 Q50.5 69 54 71") }
    val frown by lazy { parse("M46 73 Q50 68.5 54 73") }
    val wobble by lazy { parse("M44.5 71 Q46.3 68.6 48.1 71 Q49.9 73.4 51.7 71 Q53.5 68.6 55.5 71") }
    val flat by lazy { parse("M46 71 Q50 69.2 54 71") }
    val cat by lazy { parse("M45.5 68.5 Q47.8 71.6 50 68.5 Q52.2 71.6 54.5 68.5") }
}
