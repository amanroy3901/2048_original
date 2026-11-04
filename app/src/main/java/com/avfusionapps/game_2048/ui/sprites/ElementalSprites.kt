package com.avfusionapps.game_2048.ui.sprites

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.key
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** What a sprite is feeling. The board picks one from the game state; merges and spawns override it briefly. */
enum class SpriteEmotion { IDLE, HAPPY, EXCITED, THINKING, SURPRISED, SLEEPY, SAD, WORRIED, DIZZY, GRUMPY, LOVE, PROUD }

enum class SpriteElement { WATER, LEAF, SUN, ICE, COSMIC, FIRE, FLOWER, WAVE, STAR, CRYSTAL, KING, ROCK }

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
    val particle: Color
)

object ElementalSprites {
    val all = listOf(
        SpriteSpec("Drip", SpriteElement.WATER, Color(0xFFE2FAFF), Color(0xFF47C6FF), Color(0xFF1667D9), Color(0xFF2A86E8), Color(0xFF123E9A), Color(0xFF47C6FF), Color(0xFF8FD8FF)),
        SpriteSpec("Sprig", SpriteElement.LEAF, Color(0xFFEAFFB8), Color(0xFF86E04A), Color(0xFF2E8F1F), Color(0xFF3FA52C), Color(0xFF1A5418), Color(0xFF8CF05A), Color(0xFFB6F57A)),
        SpriteSpec("Sunny", SpriteElement.SUN, Color(0xFFFFF8C2), Color(0xFFFFCF3A), Color(0xFFF08A12), Color(0xFFF0A01E), Color(0xFFA8500C), Color(0xFFFFD25A), Color(0xFFFFE58A)),
        SpriteSpec("Frosty", SpriteElement.ICE, Color(0xFFF4FDFF), Color(0xFFA8E8FF), Color(0xFF4A9FE0), Color(0xFF3C8EDB), Color(0xFF1B4C9E), Color(0xFFA6ECFF), Color(0xFFE8FBFF)),
        SpriteSpec("Orbi", SpriteElement.COSMIC, Color(0xFFEDD6FF), Color(0xFF9A5BFF), Color(0xFF4B1FB0), Color(0xFF6B35D6), Color(0xFF2E1270), Color(0xFFB57BFF), Color(0xFFEBD3FF)),
        SpriteSpec("Blaze", SpriteElement.FIRE, Color(0xFFFFF6B0), Color(0xFFFF9A2E), Color(0xFFE5381B), Color(0xFFD9481C), Color(0xFF7A1A10), Color(0xFFFF8A3D), Color(0xFFFFB347)),
        SpriteSpec("Bloom", SpriteElement.FLOWER, Color(0xFFEEFFC2), Color(0xFF8BE04E), Color(0xFF2F9A2A), Color(0xFF3FA532), Color(0xFF185418), Color(0xFF9BF26A), Color(0xFFFF8FC0)),
        SpriteSpec("Splash", SpriteElement.WAVE, Color(0xFFD8F2FF), Color(0xFF5A96FF), Color(0xFF2338C9), Color(0xFF2F52D8), Color(0xFF151F78), Color(0xFF6E9BFF), Color(0xFF9FC2FF)),
        SpriteSpec("Starla", SpriteElement.STAR, Color(0xFFFFF9CC), Color(0xFFFFC53D), Color(0xFFE2770F), Color(0xFFE58A16), Color(0xFF8F420A), Color(0xFFFFD45A), Color(0xFFFFE58A)),
        SpriteSpec("Prism", SpriteElement.CRYSTAL, Color(0xFFFCF6FF), Color(0xFFCDB8FF), Color(0xFF7A62D9), Color(0xFF7E5BD8), Color(0xFF3A2386), Color(0xFFD4C2FF), Color(0xFFF1E9FF)),
        SpriteSpec("Ember King", SpriteElement.KING, Color(0xFFFFF3A6), Color(0xFFFF8A2A), Color(0xFFD72A16), Color(0xFFF2A01A), Color(0xFFB5361A), Color(0xFFFFD050), Color(0xFFFFC94D))
    )

    /** The Shadow Rock penalty tile (grid value -1). */
    val rock = SpriteSpec("Shadow Rock", SpriteElement.ROCK, Color(0xFF9A90B8), Color(0xFF5B5470), Color(0xFF2E2840), Color(0xFF3E3560), Color(0xFF1C1736), Color(0xFF8A5BFF), Color(0xFF8A80A8))

    /** 2 → 0 … 2048 → 10; anything bigger stays the Ember King. */
    fun indexFor(value: Int): Int =
        if (value <= 1) 0 else (log2(value.toFloat()) - 1f).roundToInt().coerceIn(0, all.lastIndex)

    fun specFor(value: Int): SpriteSpec = if (value < 0) rock else all[indexFor(value)]
}

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
    val stem by lazy { parse("M50 32 Q50 24 52 18") }
    val sideLeaf by lazy { parse("M38 32 C30 22 20 26 20 32 C27 37 34 37 38 32 Z") }
    val crest by lazy { parse("M22 46 C20 28 38 16 55 20 C68 23 73 34 64 41 C60 33 50 31 46 39 C40 33 29 37 22 46 Z") }
    val iceMid by lazy { parse("M50 6 L58 22 L50 35 L42 22 Z") }
    val iceMidFacet by lazy { parse("M50 6 L58 22 L50 35 Z") }
    val iceL by lazy { parse("M33 18 L40 28 L37 39 L28 31 Z") }
    val iceLFacet by lazy { parse("M33 18 L40 28 L37 39 Z") }
    val iceR by lazy { parse("M67 18 L72 31 L63 39 L60 28 Z") }
    val iceRFacet by lazy { parse("M67 18 L72 31 L63 39 Z") }
    val crystals by lazy { listOf("M30 34 L33 22 L38 33 Z", "M38 32 L42 14 L47 31 Z", "M45 31 L50 6 L55 31 Z", "M53 31 L58 14 L62 32 Z", "M62 33 L67 22 L70 34 Z").map(::parse) }
    val crown by lazy { parse("M31 30 L30 10 L40 19 L50 3 L60 19 L70 10 L69 30 Z") }
    val wingL by lazy { parse("M30 58 C16 44 3 44 4 54 C-1 57 2 65 10 65 C7 71 15 76 24 69 C27 66 29 62 30 58 Z") }
    val wingR by lazy { parse("M70 58 C84 44 97 44 96 54 C101 57 98 65 90 65 C93 71 85 76 76 69 C73 66 71 62 70 58 Z") }
    val heart by lazy { parse("M0 4.6 C-7.6 -0.8 -5.4 -7.6 0 -3.6 C5.4 -7.6 7.6 -0.8 0 4.6 Z") }
    val smallHeart by lazy { parse("M0 3.5 C-6 -1 -4 -6 0 -3 C4 -6 6 -1 0 3.5 Z") }
    val starEye by lazy { parse("M0 -6.4 L1.9 -2 L6.4 -2 L2.8 1.2 L4.1 6 L0 3.3 L-4.1 6 L-2.8 1.2 L-6.4 -2 L-1.9 -2 Z") }
    val sparkle by lazy { parse("M0 -6 Q1.2 -1.2 6 0 Q1.2 1.2 0 6 Q-1.2 1.2 -6 0 Q-1.2 -1.2 0 -6 Z") }
    val sweat by lazy { parse("M76 38 Q81 46 78 50 Q74 52.5 72.5 48.5 Q72 44 76 38 Z") }
    val tear by lazy { parse("M0 0 Q3 5 0.5 7 Q-2 5 0 0 Z") }
    val vein by lazy { parse("M-5 -1 Q-1 -1 -1 -5 M1 -5 Q1 -1 5 -1 M5 1 Q1 1 1 5 M-1 5 Q-1 1 -5 1") }
    val rock by lazy { parse("M18 88 Q10 70 20 56 Q21 38 38 35 Q49 25 63 32 Q80 35 83 54 Q92 70 83 88 Z") }
    val rockCracks by lazy { parse("M33 46 L40 54 L36 61 M64 60 L71 66 L68 74 M50 36 L53 43") }
    val spiral by lazy { parse("M0 0 m-1 0 a1 1 0 1 1 2 0 a2 2 0 1 1 -4 0 a3 3 0 1 1 6 0 a4 4 0 1 1 -8 0") }

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
 * the fixed 100-unit box, so one per sprite is enough; card brushes are keyed by tile width. Main thread only.
 */
private object BrushCache {
    private val map = HashMap<String, Brush>()
    fun get(key: String, make: () -> Brush): Brush = map.getOrPut(key, make)
}

private val labelPaint by lazy {
    android.graphics.Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
}

private val Ink = Color(0xFF2B1442)
private val MouthDark = Color(0xFF6A1E3E)
private val Tongue = Color(0xFFFF7A9A)
private val Gold = Color(0xFFFFC93A)
private val GoldLight = Color(0xFFFFF3A6)
private val GoldDark = Color(0xFFE5850F)
private val Blush = Color(0xFFFF6FA0)

private fun twoPi(t: Float, period: Float) = (2.0 * PI * t / period).toFloat()

// ---------------------------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------------------------

/**
 * One elemental sprite, optionally on its glowing tile card with the number badge.
 *
 * @param charScaleX/charScaleY squash and stretch for the character only (the card and number stay square).
 * @param mergeKey a new non-null value plays the merge burst (swirl, flash, particles) and a happy face.
 * @param spawnKey a new non-null value plays the spawn portal and a surprised face.
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
    badgeText: String? = null
) {
    val rock = value < 0
    val spec = ElementalSprites.specFor(value)
    val index = if (rock) 5 else ElementalSprites.indexFor(value)
    val transition = rememberInfiniteTransition(label = "sprite")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "spritePhase"
    )
    val phaseOffset = remember { Random.nextInt(0, 8) * 0.5f }
    val blink = remember { Animatable(1f) }
    val mergeFx = remember { Animatable(1f) }
    val spawnFx = remember { Animatable(1f) }
    val wings = remember { Animatable(0f) }
    val breakFx = remember { Animatable(1f) }
    val pokeFx = remember { Animatable(1f) }
    var nameTag by remember { mutableStateOf<String?>(null) }
    val pokeTimes = remember { ArrayList<Long>() }
    var override by remember { mutableStateOf<SpriteEmotion?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2400, 5600))
            blink.animateTo(0.08f, tween(70))
            blink.animateTo(1f, tween(90))
        }
    }
    // Effects run in the composable's own scope so a later move can't freeze them half-way.
    LaunchedEffect(mergeKey) {
        if (mergeKey == null) return@LaunchedEffect
        scope.launch {
            override = if (value >= 128) SpriteEmotion.PROUD else SpriteEmotion.HAPPY
            delay(1100)
            override = null
        }
        scope.launch {
            mergeFx.snapTo(0f)
            delay(120)
            mergeFx.animateTo(1f, tween(650, easing = LinearOutSlowInEasing))
        }
        if (value >= 1024) scope.launch {
            wings.snapTo(1f)
            delay(1500)
            wings.animateTo(0f, tween(250))
        }
    }
    LaunchedEffect(breaking) {
        if (!breaking) return@LaunchedEffect
        override = SpriteEmotion.SURPRISED
        breakFx.snapTo(0f)
        delay(160)
        breakFx.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))
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
                else -> "No.${(index + 1).toString().padStart(2, '0')} · ${spec.name}"
            }
            delay(if (annoyed) 2000 else 1200)
            override = null
            nameTag = null
        }
        scope.launch {
            pokeFx.snapTo(0f)
            pokeFx.animateTo(1f, tween(460))
        }
    }
    LaunchedEffect(spawnKey) {
        if (spawnKey == null) return@LaunchedEffect
        scope.launch {
            override = SpriteEmotion.SURPRISED
            delay(900)
            if (override == SpriteEmotion.SURPRISED) override = null
        }
        scope.launch {
            spawnFx.snapTo(0f)
            spawnFx.animateTo(1f, tween(600, easing = LinearOutSlowInEasing))
        }
    }

    val particleSeed = (mergeKey ?: 0) * 31 + index
    BoxWithConstraints(modifier = modifier) {
        val sizeDp = maxWidth
        // The card and the sprite's body never change shape, so each is drawn once into a bitmap;
        // only the face, sparkles and effects are drawn per frame.
        key(value, locked, showCard) {
            Spacer(modifier = Modifier.fillMaxSize().drawWithCache {
                val w = size.width
                val h = size.height
                val left = if (showCard) w * 0.05f else 0f
                val top = if (showCard) -h * 0.06f else 0f
                val u = if (showCard) w * 0.009f else w / 100f
                val legend = index == 10 && !rock && !locked
                val cardBitmap = if (showCard) renderBitmap((w * 1.28f).toInt(), (h * 1.28f).toInt(), this) {
                    withTransform({ translate(w * 0.14f, h * 0.14f) }) { drawCardStatic(spec, legend, locked, w, h) }
                } else null
                val bodyPx = (120f * u).toInt().coerceAtLeast(1)
                val bodyBitmap = renderBitmap(bodyPx, bodyPx, this) {
                    withTransform({
                        scale(bodyPx / 120f, bodyPx / 120f, pivot = Offset.Zero)
                        translate(10f, 10f)
                    }) { drawSpriteBody(spec) }
                    if (locked) drawRect(Color(0xFF0A0420).copy(alpha = 0.75f), blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn)
                }
                val drawBody: DrawScope.() -> Unit = {
                    drawImage(bodyBitmap, dstOffset = IntOffset(-10, -10), dstSize = IntSize(120, 120))
                }
                onDrawBehind {
                    val t = (phase.value + phaseOffset) % 4f
                    val emo = if (rock) SpriteEmotion.GRUMPY else (override ?: emotion)
                    val pk = pokeFx.value
                    val (pokeX, pokeY) = if (pk < 1f) pokeSquash(pk) else (1f to 1f)
                    val m = mergeFx.value
                    val sp = spawnFx.value

                    if (cardBitmap == null && !locked) {
                        // Card-less sprites (the shooter, mascots) get a soft aura instead of a card.
                        drawCircle(
                            brush = Brush.radialGradient(listOf(spec.glow.copy(alpha = 0.4f), Color.Transparent), center = Offset(w / 2, h * 0.55f), radius = w * 0.5f),
                            radius = w * 0.5f,
                            center = Offset(w / 2, h * 0.55f)
                        )
                    }
                    if (cardBitmap != null) {
                        drawImage(cardBitmap, topLeft = Offset(-w * 0.14f, -h * 0.14f))
                        drawCardLive(legend, t, if (m < 0.3f) (1f - m / 0.3f) else 0f)
                    }
                    if (sp < 1f) drawPortal(spec.glow, sp)
                    withTransform({
                        translate(left, top)
                        scale(u, u, pivot = Offset.Zero)
                    }) {
                        if (locked) {
                            drawCharacter(spec, index, SpriteEmotion.IDLE, t, 1f, 0f, 0f, 1f, 1f, 0f, drawBody, faceless = true)
                        } else {
                            drawCharacter(spec, index, emo, t, blink.value, lookX, lookY, charScaleX * pokeX, charScaleY * pokeY, wings.value, drawBody)
                        }
                    }
                    if (m < 1f) drawMergeBurst(spec, m, particleSeed)
                    if (breakFx.value < 1f) drawShards(spec, breakFx.value)
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
                    .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        if (showNumber) {
            NumberBadge(
                value = value,
                text = badgeText ?: if (locked) "?" else value.toString(),
                tileWidthPx = with(LocalDensity.current) { sizeDp.toPx() },
                fontSizeMultiplier = fontSizeMultiplier,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
    val fontSize = with(density) { (tileWidthPx * factor).toSp() }
    val outline = with(density) { (tileWidthPx * 0.05f).coerceAtLeast(2f) }
    val base = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black, lineHeight = fontSize)
    Box(modifier = modifier.offset(y = 1.dp)) {
        Text(
            text = text,
            color = Color(0xFF2A0F4A),
            style = base.copy(
                drawStyle = Stroke(width = outline, join = StrokeJoin.Round),
                shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, outline * 0.9f), 0f)
            )
        )
        Text(text = text, color = if (value < 0) Color(0xFFE9DDFF) else Color.White, style = base)
    }
}

// ---------------------------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------------------------

/** Quick squish when poked: flat and wide, then tall, then settle. */
private fun pokeSquash(p: Float): Pair<Float, Float> {
    val keys = listOf(0f to (1f to 1f), 0.2f to (1.22f to 0.76f), 0.5f to (0.88f to 1.14f), 0.75f to (1.05f to 0.96f), 1f to (1f to 1f))
    val k = keys.indexOfLast { it.first <= p }.coerceIn(0, keys.size - 2)
    val (p0, a) = keys[k]
    val (p1, b) = keys[k + 1]
    val f = ((p - p0) / (p1 - p0)).coerceIn(0f, 1f)
    return (a.first + (b.first - a.first) * f) to (a.second + (b.second - a.second) * f)
}

/** Draws the whole tile card into an image once: [w]×[h] is the card, the glow spills 14% past it. */
private fun DrawScope.drawCardStatic(spec: SpriteSpec, legend: Boolean, locked: Boolean, w: Float, h: Float) {
    val radius = CornerRadius(w * 0.2f)
    val cardSize = Size(w, h)
    drawRoundRect(
        brush = Brush.radialGradient(
            0f to spec.glow.copy(alpha = if (locked) 0f else if (legend) 0.7f else 0.42f),
            1f to Color.Transparent,
            center = Offset(w / 2, h / 2),
            radius = w * 0.72f
        ),
        topLeft = Offset(-w * 0.14f, -h * 0.14f),
        size = Size(w * 1.28f, h * 1.28f),
        cornerRadius = CornerRadius(w * 0.36f)
    )
    val t0 = if (locked) Color(0xFF3A2E5C) else spec.t0
    val t1 = if (locked) Color(0xFF1C1436) else spec.t1
    drawRoundRect(
        brush = Brush.radialGradient(
            0f to lerp(t0, Color.White, 0.25f),
            0.52f to t0,
            1f to t1,
            center = Offset(w * 0.32f, h * 0.18f),
            radius = w * 1.1f
        ),
        size = cardSize,
        cornerRadius = radius
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.Transparent), startY = 0f, endY = h * 0.22f),
        topLeft = Offset(w * 0.04f, h * 0.02f),
        size = Size(w * 0.92f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.16f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.26f)), startY = h * 0.6f, endY = h),
        size = cardSize,
        cornerRadius = radius
    )
    drawRoundRect(
        color = if (legend) Color(0xFFFFE27A) else Color.White.copy(alpha = 0.4f),
        size = cardSize,
        style = Stroke(width = (w * 0.022f).coerceAtLeast(1.5f)),
        cornerRadius = radius
    )
}

/** The moving parts of the card: the 2048 shine band and the merge flash. */
private fun DrawScope.drawCardLive(legend: Boolean, t: Float, flash: Float) {
    val w = size.width
    val h = size.height
    val radius = CornerRadius(w * 0.2f)
    if (legend) {
        val x = (t / 4f) * w * 2.6f - w * 0.8f
        drawRoundRect(
            brush = Brush.linearGradient(
                0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.45f), 1f to Color.Transparent,
                start = Offset(x - w * 0.25f, 0f), end = Offset(x + w * 0.25f, h)
            ),
            cornerRadius = radius
        )
    }
    if (flash > 0f) drawRoundRect(color = Color.White.copy(alpha = flash * 0.6f), cornerRadius = radius)
}

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

private fun DrawScope.drawPortal(glow: Color, p: Float) {
    val w = size.width
    val h = size.height
    val bell = sin(PI * p).toFloat()
    val rw = w * 0.85f * (0.3f + 0.8f * p)
    val rh = h * 0.24f * (0.3f + 0.8f * p)
    drawOval(
        color = glow.copy(alpha = bell),
        topLeft = Offset(w / 2 - rw / 2, h * 0.84f - rh / 2),
        size = Size(rw, rh),
        style = Stroke(width = w * 0.035f)
    )
    drawRect(
        brush = Brush.verticalGradient(listOf(Color.Transparent, glow.copy(alpha = 0.8f * bell)), startY = -h * 0.2f, endY = h * 0.84f),
        topLeft = Offset(w * 0.27f, -h * 0.2f),
        size = Size(w * 0.46f, h * 1.04f)
    )
}

private fun DrawScope.drawMergeBurst(spec: SpriteSpec, p: Float, seed: Int) {
    val w = size.width
    val c = center
    val bell = sin(PI * p).toFloat()
    // flash
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 1f - p), 0.35f to spec.glow.copy(alpha = (1f - p) * 0.8f), 1f to Color.Transparent,
            center = c, radius = w * 0.8f * (0.25f + p)
        ),
        radius = w * 0.8f * (0.25f + p),
        center = c
    )
    // swirl
    val r = w * 0.62f * (0.4f + 0.85f * p)
    val rot = 420f * p
    for (k in 0..1) {
        drawArc(
            color = (if (k == 0) Color.White else spec.glow).copy(alpha = bell),
            startAngle = rot + k * 180f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = w * 0.06f, cap = StrokeCap.Round)
        )
    }
    // particles
    val rnd = Random(seed)
    repeat(14) { k ->
        val ang = rnd.nextFloat() * 2f * PI.toFloat()
        val dist = w * (0.45f + rnd.nextFloat() * 0.6f) * (1f - (1f - p) * (1f - p))
        val pr = w * (0.035f + rnd.nextFloat() * 0.035f) * (1f - 0.7f * p)
        val col = if (k % 3 == 0) Color.White else spec.particle
        drawCircle(color = col.copy(alpha = (1f - p).coerceIn(0f, 1f)), radius = pr, center = Offset(c.x + cos(ang) * dist, c.y + sin(ang) * dist))
    }
}

private fun DrawScope.drawShards(spec: SpriteSpec, p: Float) {
    val w = size.width
    val c = center
    val rnd = Random(7)
    repeat(16) { k ->
        val ang = rnd.nextFloat() * 2f * PI.toFloat()
        val dist = w * (0.3f + rnd.nextFloat() * 0.7f) * p
        val r = w * (0.05f + rnd.nextFloat() * 0.05f) * (1f - 0.6f * p)
        val pos = Offset(c.x + cos(ang) * dist, c.y + sin(ang) * dist + w * 0.25f * p * p)
        val col = if (k % 2 == 0) spec.c2 else Color.White
        withTransform({ rotate(ang * 57f + p * 300f, pivot = pos) }) {
            drawRect(col.copy(alpha = 1f - p), topLeft = Offset(pos.x - r, pos.y - r * 0.6f), size = Size(r * 2, r * 1.2f))
        }
    }
}

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

private fun DrawScope.label(text: String, x: Float, y: Float, sizeU: Float, fill: Color, stroke: Color?) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = labelPaint
    paint.textSize = sizeU
    if (stroke != null) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = sizeU * 0.14f
        paint.color = stroke.toArgb()
        canvas.drawText(text, x, y, paint)
    }
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = fill.toArgb()
    canvas.drawText(text, x, y, paint)
}

private fun DrawScope.drawCharacter(
    spec: SpriteSpec,
    index: Int,
    emo: SpriteEmotion,
    t: Float,
    blink: Float,
    lookX: Float,
    lookY: Float,
    squashX: Float,
    squashY: Float,
    wings: Float,
    body: DrawScope.() -> Unit,
    faceless: Boolean = false
) {
    val e = spec.element
    val fiery = e == SpriteElement.FIRE || e == SpriteElement.KING
    val L = lerp(spec.c3, Color(0xFF1A0830), 0.42f)
    val s = 0.86f + index * 0.014f

    // mood motion
    var mx = 0f
    var my = 0f
    var rot = 0f
    var moodSx = 1f
    var moodSy = 1f
    when (emo) {
        SpriteEmotion.HAPPY, SpriteEmotion.PROUD -> {
            val b = sin(twoPi(t, 0.8f)).coerceAtLeast(0f)
            my = -8f * b; moodSx = 1f - 0.03f * b; moodSy = 1f + 0.04f * b
        }
        SpriteEmotion.EXCITED -> {
            val b = sin(twoPi(t, 1f)).coerceAtLeast(0f)
            my = -7f * b
        }
        SpriteEmotion.WORRIED -> mx = 1.4f * sin(twoPi(t, 0.25f))
        SpriteEmotion.DIZZY -> rot = 7f * sin(twoPi(t, 1f))
        SpriteEmotion.SAD -> { my = 2.5f; moodSx = 1.04f; moodSy = 0.94f }
        SpriteEmotion.LOVE -> rot = 5f * sin(twoPi(t, 2f))
        SpriteEmotion.THINKING -> rot = -3f - 3f * sin(twoPi(t, 2f))
        SpriteEmotion.GRUMPY -> { val b = if ((t % 2f) > 1.7f) 1f else 0f; moodSx = 1f + 0.05f * b; moodSy = 1f - 0.04f * b }
        else -> Unit
    }
    val breath = if (emo == SpriteEmotion.SLEEPY) sin(twoPi(t, 4f)) * 1.3f else sin(twoPi(t, 2f))
    val bx = 1f - 0.018f * breath
    val by = 1f + 0.035f * breath
    val flick = if (fiery) sin(twoPi(t, 0.5f)) else 0f

    withTransform({
        translate(mx, my)
        rotate(rot, pivot = Offset(50f, 88f))
        scale(squashX * bx * moodSx, squashY * by * moodSy, pivot = Offset(50f, 90f))
        scale(s, s, pivot = Offset(50f, 89f))
    }) {
        // wings, only for a moment after a 1024+ merge
        if (wings > 0f) {
            val flap = sin(twoPi(t, 0.4f)) * 22f
            val wingBrush = BrushCache.get("wing") { Brush.linearGradient(listOf(Color.White, Color(0xFFFFE7A6)), start = Offset(30f, 44f), end = Offset(5f, 70f)) }
            withTransform({ rotate(-flap, pivot = Offset(30f, 60f)) }) {
                drawPath(Shapes.wingL, wingBrush, alpha = wings)
                line(Shapes.wingL, Color(0xFFC99A2E), 1.4f, wings)
            }
            withTransform({ rotate(flap, pivot = Offset(70f, 60f)) }) {
                drawPath(Shapes.wingR, wingBrush, alpha = wings)
                line(Shapes.wingR, Color(0xFFC99A2E), 1.4f, wings)
            }
        }
        withTransform({ if (fiery) scale(1f + 0.015f * flick, 1f + 0.04f * flick, pivot = Offset(50f, 89f)) }) { body() }
        if (faceless) return@withTransform

        // face (drawn live: blinks, looks around, changes with emotion)
        val fy = when (e) {
            SpriteElement.STAR -> -1f
            SpriteElement.WATER -> 3f
            else -> 0f
        }
        withTransform({ translate(0f, fy) }) { drawFace(spec, emo, t, blink, lookX, lookY) }

        if (fiery) {
            listOf(Triple(34f, 26f, 0f), Triple(66f, 22f, 0.66f), Triple(58f, 12f, 1.33f)).forEach { (x, y, d) ->
                val p = ((t + d) % 2f) / 2f
                drawCircle(if (d == 0f) Color(0xFFFFD25A) else Color(0xFFFF9A2E), 2f * (1f - 0.7f * p), Offset(x, y - 18f * p), alpha = sin(PI * p).toFloat())
            }
        }
        if (e == SpriteElement.STAR || e == SpriteElement.KING || e == SpriteElement.CRYSTAL || e == SpriteElement.SUN) {
            twinkle(14f, 30f, 1f, t, 0f, Color.White)
            twinkle(88f, 44f, 0.7f, t, 0.33f, Color.White)
            twinkle(84f, 16f, 0.5f, t, 0.66f, Color.White)
        }

        drawEmotionFx(emo, t)
    }
}

/** Everything about a sprite that doesn't animate by itself, in the 100-unit box. Drawn once into a bitmap. */
private fun DrawScope.drawSpriteBody(spec: SpriteSpec) {
    val e = spec.element
    val fiery = e == SpriteElement.FIRE || e == SpriteElement.KING
    val L = lerp(spec.c3, Color(0xFF1A0830), 0.42f)
    run {
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
                leaf(Shapes.leafL, Offset(25f, 10f), Offset(50f, 28f))
                leaf(Shapes.leafR, Offset(77f, 6f), Offset(52f, 24f))
            }
            else -> Unit
        }
        // body
        val bodyBrush = BrushCache.get("body${spec.name}") { if (fiery) {
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
        } }
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
        }
        if (e == SpriteElement.ROCK) {
            line(Shapes.rockCracks, Color(0xFFB57BFF), 2f, 0.9f)
        }
        if (e == SpriteElement.WAVE) {
            drawPath(Shapes.crest, spec.c1, alpha = 0.95f)
            line(Shapes.crest, L, 1.4f)
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
                val ice = BrushCache.get("ice") { Brush.linearGradient(listOf(Color.White, Color(0xFF8FD8FF)), start = Offset(30f, 6f), end = Offset(70f, 40f)) }
                listOf(Shapes.iceMid to Shapes.iceMidFacet, Shapes.iceL to Shapes.iceLFacet, Shapes.iceR to Shapes.iceRFacet).forEach { (shard, facet) ->
                    drawPath(shard, ice)
                    drawPath(facet, Color(0xFF9EDCFF), alpha = 0.8f)
                    line(shard, L, 1.2f)
                }
            }
            SpriteElement.FLOWER -> {
                leaf(Shapes.sideLeaf, Offset(20f, 24f), Offset(38f, 36f))
                val petal = BrushCache.get("petal") { Brush.linearGradient(listOf(Color(0xFFFFE3F1), Color(0xFFFF6FA8)), start = Offset(-5f, -13f), end = Offset(5f, 0f)) }
                at(63f, 26f) {
                    for (k in 0 until 5) {
                        withTransform({ rotate(k * 72f, pivot = Offset.Zero) }) {
                            drawOval(petal, topLeft = Offset(-5f, -13.5f), size = Size(10f, 13f))
                            drawOval(Color(0xFFB0306A), topLeft = Offset(-5f, -13.5f), size = Size(10f, 13f), style = Stroke(1.1f))
                        }
                    }
                    drawCircle(Color(0xFFFFD84D), 4.2f, Offset.Zero)
                    drawCircle(Color(0xFFC08A10), 4.2f, Offset.Zero, style = Stroke(1f))
                }
            }
            SpriteElement.CRYSTAL -> {
                val lilac = BrushCache.get("lilac") { Brush.linearGradient(listOf(Color.White, Color(0xFFB9A4FF)), start = Offset(30f, 6f), end = Offset(70f, 34f)) }
                Shapes.crystals.forEach { drawPath(it, lilac); line(it, L, 1.1f) }
                drawCircle(Color(0xFFFF6FB8), 3.2f, Offset(50f, 33f))
            }
            SpriteElement.KING -> {
                drawPath(Shapes.crown, BrushCache.get("crown") { Brush.verticalGradient(listOf(GoldLight, Gold, GoldDark), startY = 3f, endY = 30f) })
                line(Shapes.crown, Color(0xFF8A4A08), 1.5f)
                drawRoundRect(GoldDark, topLeft = Offset(31f, 27f), size = Size(38f, 6f), cornerRadius = CornerRadius(2f))
                drawCircle(Color(0xFFFF3D6E), 3.2f, Offset(50f, 20f))
                drawCircle(Color(0xFF47C6FF), 2f, Offset(39f, 24f))
                drawCircle(Color(0xFF47C6FF), 2f, Offset(61f, 24f))
            }
            else -> Unit
        }
    }
}

private fun ringBrush() = BrushCache.get("ring") { Brush.horizontalGradient(listOf(Color(0xFFFFB6F0), Color.White, Color(0xFFB57BFF)), startX = 8f, endX = 92f) }

private fun DrawScope.leaf(path: Path, from: Offset, to: Offset) {
    drawPath(path, BrushCache.get("leaf${from.x},${from.y}") { Brush.linearGradient(listOf(Color(0xFFD4FF8A), Color(0xFF3FA52C)), start = from, end = to) })
    line(path, Color(0xFF2E6E1A), 1.5f)
}

private fun DrawScope.twinkle(x: Float, y: Float, scale: Float, t: Float, delay: Float, color: Color) {
    val p = ((t + delay) % 1f)
    val k = 0.5f + 0.65f * sin(PI * p).toFloat()
    val a = 0.2f + 0.8f * sin(PI * p).toFloat()
    at(x, y, scale * k) { drawPath(Shapes.sparkle, color, alpha = a) }
}

private fun DrawScope.drawFace(spec: SpriteSpec, emo: SpriteEmotion, t: Float, blink: Float, lookX: Float, lookY: Float) {
    val strongCheeks = emo == SpriteEmotion.HAPPY || emo == SpriteEmotion.LOVE || emo == SpriteEmotion.PROUD || emo == SpriteEmotion.EXCITED
    for (cx in listOf(32.5f, 67.5f)) {
        drawOval(
            brush = BrushCache.get("blush$strongCheeks$cx") { Brush.radialGradient(listOf(Blush.copy(alpha = if (strongCheeks) 0.9f else 0.5f), Color.Transparent), center = Offset(cx, 68f), radius = 7f) },
            topLeft = Offset(cx - 7f, 64f),
            size = Size(14f, 8f)
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
    for (ex in listOf(41f, 59f)) {
        at(ex + lx, 60f + ly) {
            when (emo) {
                SpriteEmotion.HAPPY -> line(EyeCache.happy, Ink, 2.4f)
                SpriteEmotion.SLEEPY, SpriteEmotion.SAD -> line(EyeCache.closed, Ink, 2.4f)
                SpriteEmotion.DIZZY -> withTransform({ rotate(t * 180f, pivot = Offset.Zero) }) { drawPath(Shapes.spiral, Ink, style = Stroke(1.5f)) }
                SpriteEmotion.LOVE -> { drawPath(Shapes.heart, Color(0xFFFF4D6D)); drawPath(Shapes.heart, Ink, style = Stroke(1f)) }
                SpriteEmotion.PROUD -> { drawPath(Shapes.starEye, Color(0xFFFFE27A)); drawPath(Shapes.starEye, Ink, style = Stroke(1f, join = StrokeJoin.Round)) }
                else -> {
                    val big = if (emo == SpriteEmotion.SURPRISED) 1.25f else 1f
                    val open = if (emo == SpriteEmotion.SURPRISED) 1f else blink
                    withTransform({ scale(big, big * open, pivot = Offset.Zero) }) {
                        oval(Ink, 0f, 0f, 4.6f, 5.9f)
                        drawCircle(Color.White, 2.05f, Offset(1.5f, -2.3f))
                        drawCircle(Color.White, 0.95f, Offset(-1.5f, 2.4f))
                        if (emo == SpriteEmotion.EXCITED) at(1.5f, -2.3f, 0.55f) { drawPath(Shapes.sparkle, Color(0xFFFFF6B0)) }
                    }
                    if (emo == SpriteEmotion.GRUMPY) drawPath(EyeCache.lid, lerp(spec.c2, Color.Black, 0.15f))
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
            drawPath(MouthCache.smallTongue, Tongue)
        }
        SpriteEmotion.SURPRISED -> oval(MouthDark, 50f, 71f, 2.5f, 3.2f)
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
    when (emo) {
        SpriteEmotion.THINKING -> {
            drawCircle(Color.White, 1.6f, Offset(72f, 36f))
            drawCircle(Color.White, 2.4f, Offset(76f, 31f))
            val pop = 1f + 0.1f * sin(twoPi(t, 1f))
            at(84f, 20f, pop) {
                drawCircle(Color.White, 9f, Offset.Zero)
                drawCircle(Color(0xFF6B35D6), 9f, Offset.Zero, style = Stroke(1.4f))
                label("?", 0f, 5f, 14f, Color(0xFF6B35D6), null)
            }
        }
        SpriteEmotion.SURPRISED -> at(84f, 20f, 1f + 0.12f * sin(twoPi(t, 0.5f))) {
            drawCircle(Color(0xFFFF4D6D), 9f, Offset.Zero)
            drawCircle(Color.White, 9f, Offset.Zero, style = Stroke(1.6f))
            label("!", 0f, 5f, 14f, Color.White, null)
        }
        SpriteEmotion.SLEEPY -> listOf(Triple(70f, 34f, 11f), Triple(77f, 25f, 13f), Triple(84f, 15f, 15f)).forEachIndexed { k, (x, y, sz) ->
            val p = ((t + k * 0.66f) % 2f) / 2f
            withTransform({ translate(8f * p, -12f * p) }) {
                label(if (k == 2) "Z" else "z", x, y, sz, Color.White.copy(alpha = sin(PI * p).toFloat()), Ink.copy(alpha = sin(PI * p).toFloat()))
            }
        }
        SpriteEmotion.WORRIED -> {
            val p = (t % 1.3f) / 1.3f
            withTransform({ translate(0f, 6f * p) }) {
                drawPath(Shapes.sweat, Color(0xFF8FD8FF), alpha = sin(PI * p).toFloat())
                drawPath(Shapes.sweat, Ink, alpha = sin(PI * p).toFloat(), style = Stroke(1f))
            }
        }
        SpriteEmotion.SAD -> for ((x, d) in listOf(38f to 0f, 62f to 0.5f)) {
            val p = (t + d) % 1f
            at(x, 64f + 14f * p) { drawPath(Shapes.tear, Color(0xFF9FE4FF), alpha = 1f - p) }
        }
        SpriteEmotion.LOVE -> for ((pos, d) in listOf(Offset(22f, 36f) to 0f, Offset(80f, 32f) to 1f)) {
            val p = ((t + d) % 2f) / 2f
            at(pos.x, pos.y - 14f * p, 0.5f + 0.6f * p) { drawPath(Shapes.smallHeart, Color(0xFFFF4D6D), alpha = sin(PI * p).toFloat()) }
        }
        SpriteEmotion.DIZZY -> withTransform({
            translate(50f, 30f)
            scale(1f, 0.45f, pivot = Offset.Zero)
            rotate((t % 1f) * 360f, pivot = Offset.Zero)
        }) {
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 2f
                drawCircle(if (k % 2 == 0) Color(0xFFFFE27A) else Color.White, 3f, Offset(cos(a) * 20f, sin(a) * 20f))
            }
        }
        SpriteEmotion.GRUMPY -> at(76f, 36f, 1f + 0.25f * abs(sin(twoPi(t, 0.8f)))) { line(Shapes.vein, Color(0xFFFF4D6D), 2.2f) }
        SpriteEmotion.EXCITED, SpriteEmotion.PROUD -> {
            twinkle(16f, 46f, 1f, t, 0f, Color(0xFFFFF6B0))
            twinkle(86f, 52f, 0.8f, t, 0.4f, Color(0xFFFFF6B0))
            twinkle(22f, 78f, 0.6f, t, 0.75f, Color(0xFFFFF6B0))
        }
        else -> Unit
    }
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
