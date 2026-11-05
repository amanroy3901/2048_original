package com.avfusionapps.game_2048.shared.ui

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

/** A centred sprite-panel dialog. */
@Composable
internal fun SpriteDialog(onDismiss: () -> Unit, dismissable: Boolean = true, testTag: String = "", content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (dismissable) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // The screen behind holds still while the dialog's own sprites keep moving.
        com.avfusionapps.game_2048.shared.sprites.SpriteCover {
        Column(
            Modifier
                .padding(22.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .spritePanel()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp)
                .testTag(testTag),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
    }
}

@Composable
private fun Title(text: String, size: Float = 34f) {
    Text(text, color = Color.White, fontFamily = displayFont(), fontSize = size.sp, textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(Color(0xFF4A1C9A), Offset(0f, 4f), 0f)))
}

@Composable
private fun Body(text: String) {
    Text(text, color = Muted, fontSize = 15.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
}

@Composable
private fun ScoreLine(label: String, value: Int) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xB80C0522)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(commas(value), color = Color.White, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
    }
}

/** A sprite standing on a small glowing pedestal. */
@Composable
private fun PedestalSprite(value: Int, size: Dp, emotion: SpriteEmotion, motion: SpriteMotion? = null, spawnKey: Int? = null) {
    Box(Modifier.size(size * 1.25f, size * 1.12f)) {
        Canvas(Modifier.align(Alignment.BottomCenter).size(size * 1.1f, size * 0.26f)) {
            drawOval(Color(0x59FF8C3C), topLeft = Offset(-12f, -10f), size = Size(this.size.width + 24f, this.size.height + 20f))
            drawOval(Brush.radialGradient(0f to Color(0xFF7C6AA8), 0.7f to Color(0xFF3B2D63), center = Offset(this.size.width / 2f, this.size.height * 0.3f), radius = this.size.width * 0.6f))
        }
        SpriteTile(value = value, modifier = Modifier.align(Alignment.TopCenter).size(size), showCard = false, showNumber = false, emotion = emotion, motion = motion, spawnKey = spawnKey)
    }
}

// ─────────────────────────────── Pause ───────────────────────────────

@Composable
internal fun SpritePauseDialog(score: Int, onResume: () -> Unit, onRestart: () -> Unit, onQuit: () -> Unit, onDismiss: () -> Unit) {
    SpriteDialog(onDismiss = onDismiss, testTag = "GamePauseDialog_Root") {
        PedestalSprite(64, 96.dp, SpriteEmotion.SLEEPY)
        Spacer(Modifier.height(6.dp))
        Title("Paused")
        Body("Blaze is taking a nap. Your sprites will wait right here.")
        Spacer(Modifier.height(14.dp))
        ScoreLine("Score", score)
        Spacer(Modifier.height(16.dp))
        SpriteGoldButton("Resume", onResume, Modifier.fillMaxWidth().testTag("GamePauseDialog_Button_Resume"))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SpriteGhostButton("Restart", onRestart, Modifier.weight(1f).testTag("GamePauseDialog_Button_Restart"))
            SpriteGhostButton("Home", onQuit, Modifier.weight(1f).testTag("GamePauseDialog_Button_Quit"))
        }
    }
}

// ─────────────────────────────── Game over ───────────────────────────────

@Composable
internal fun SpriteGameOverDialog(
    score: Int,
    title: String = "No moves left",
    body: String = "Nobody can slide anywhere. Use a power-up to keep going, or start again.",
    primary: String = "New game",
    onNewGame: () -> Unit,
    onExit: () -> Unit,
    rescue: (@Composable ColumnScope.() -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    SpriteDialog(onDismiss = {}, dismissable = false, testTag = "GameOverDialog_Root") {
        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp), verticalAlignment = Alignment.Bottom) {
            SpriteTile(2, Modifier.size(56.dp), showCard = false, showNumber = false, emotion = SpriteEmotion.SAD)
            PedestalSprite(64, 92.dp, SpriteEmotion.SAD)
            SpriteTile(8, Modifier.size(56.dp), showCard = false, showNumber = false, emotion = SpriteEmotion.SAD)
        }
        Spacer(Modifier.height(4.dp))
        Title(title, 32f)
        Body(body)
        Spacer(Modifier.height(14.dp))
        ScoreLine("Final score", score)
        extra?.invoke(this)
        if (rescue != null) {
            Spacer(Modifier.height(12.dp))
            rescue()
        }
        Spacer(Modifier.height(14.dp))
        SpriteGoldButton(primary, onNewGame, Modifier.fillMaxWidth().testTag("GameOverDialog_Button_PlayAgain"))
        Spacer(Modifier.height(10.dp))
        SpriteGhostButton("Home", onExit, Modifier.fillMaxWidth().testTag("GameOverDialog_Button_Exit"))
    }
}

// ─────────────────────────────── Milestone (a new biggest sprite) ───────────────────────────────

/**
 * A new biggest sprite: it evolves on a pedestal inside a gold laurel, with a ribbon and confetti.
 * The Ember King (2048) gets a crown and the win wording.
 */
@Composable
internal fun SpriteMilestoneDialog(value: Int, onDismiss: () -> Unit) {
    val spec = ElementalSprites.specFor(value)
    val king = value >= 2048
    var hop by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1600); hop++ } }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(700, easing = Bounce)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // The screen behind holds still while the dialog's own sprites keep moving.
        com.avfusionapps.game_2048.shared.sprites.SpriteCover {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .padding(22.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .spritePanel()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .testTag("LevelUnlockDialog_Root"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (king) "YOU WIN!" else "NEW SPRITE", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Box(Modifier.size(210.dp).graphicsLayer { scaleX = 0.6f + 0.4f * grow.value; scaleY = scaleX }, contentAlignment = Alignment.Center) {
                    Laurel(Modifier.fillMaxSize())
                    PedestalSprite(value, 128.dp, SpriteEmotion.PROUD, motion = if (hop > 0) SpriteMotion(SpriteMotionKind.HOP, hop, hop = 14f) else null, spawnKey = 1)
                }
                Ribbon(if (king) "2048!" else "$value!")
                Spacer(Modifier.height(10.dp))
                Title(if (king) "Long live the Ember King!" else "You made ${spec.name}!", 28f)
                Spacer(Modifier.height(4.dp))
                Body(if (king) "You raised the legendary Ember King. Keep playing to go even higher." else "That's your biggest sprite yet. ${nextLine(value)}")
                Spacer(Modifier.height(18.dp))
                SpriteGoldButton("Keep playing", onDismiss, Modifier.fillMaxWidth().testTag("LevelUnlockDialog_Button_Continue"))
            }
            ConfettiBurst(44)
        }
    }
    }
}

private fun nextLine(value: Int): String {
    val next = value * 2
    return if (next <= 2048) "Next up: ${ElementalSprites.specFor(next).name} ($next)." else ""
}

@Composable
private fun Laurel(modifier: Modifier) {
    val spin by rememberInfiniteTransition(label = "laurel").animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "laurelT")
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.44f
        rotate(spin, c) {
            drawCircle(Brush.radialGradient(listOf(Color(0x55FFD050), Color.Transparent), center = c, radius = r * 1.1f), radius = r * 1.1f, center = c)
            for (k in 0 until 12) {
                val a = toRadiansCompat(k * 30.0)
                val p = Offset(c.x + r * 1.02f * kotlin.math.cos(a).toFloat(), c.y + r * 1.02f * kotlin.math.sin(a).toFloat())
                drawLine(Color(0x33FFE07A), c, p, strokeWidth = r * 0.14f)
            }
        }
        // two gold laurel branches, like the prototype's win art
        for (side in listOf(-1, 1)) {
            for (k in 0 until 7) {
                val deg = if (side < 0) 200 - k * 22 else -20 + k * 22
                val a = toRadiansCompat(deg.toDouble())
                val p = Offset(c.x + r * 1.0f * kotlin.math.cos(a).toFloat(), c.y + r * 0.92f * kotlin.math.sin(a).toFloat() + r * 0.1f)
                rotate(deg + if (side < 0) 70f else 110f, p) {
                    drawOval(Brush.verticalGradient(listOf(Color(0xFFFFF3A6), Gold, Color(0xFFE5850F)), startY = p.y - r * 0.08f, endY = p.y + r * 0.08f), topLeft = Offset(p.x - r * 0.16f, p.y - r * 0.077f), size = Size(r * 0.32f, r * 0.154f))
                    drawOval(Color(0xFF8A4A08), topLeft = Offset(p.x - r * 0.16f, p.y - r * 0.077f), size = Size(r * 0.32f, r * 0.154f), style = Stroke(1.5f))
                }
            }
        }
    }
}

@Composable
private fun Ribbon(text: String) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp, 46.dp)) {
            val w = size.width
            val h = size.height
            for (side in listOf(-1f, 1f)) {
                val x0 = if (side < 0) 0f else w
                val tail = Path().apply {
                    moveTo(x0, h * 0.25f); lineTo(x0 + side * -w * 0.16f, h * 0.25f); lineTo(x0 + side * -w * 0.16f, h); lineTo(x0, h); lineTo(x0 + side * -w * 0.05f, h * 0.62f); close()
                }
                drawPath(tail, Color(0xFFA3163A))
            }
            drawRoundRect(Color(0xFF7A0E28), topLeft = Offset(w * 0.1f, 4.dp.toPx()), size = Size(w * 0.8f, h * 0.8f), cornerRadius = CornerRadius(6.dp.toPx()))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF4D6D), Color(0xFFC21E45))), topLeft = Offset(w * 0.1f, 0f), size = Size(w * 0.8f, h * 0.8f), cornerRadius = CornerRadius(6.dp.toPx()))
        }
        Text(text, color = Color.White, fontFamily = displayFont(), fontSize = 26.sp, modifier = Modifier.offset(y = (-4).dp), style = TextStyle(shadow = Shadow(Color(0xFF7A0E28), Offset(0f, 4f), 0f)))
    }
}

// ─────────────────────────────── Grid size ───────────────────────────────

/** Pick a board: each option shows a mini sprite board of that size. */
@Composable
internal fun SpriteGridSizeDialog(currentSize: Int, onSizeSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    SpriteDialog(onDismiss = onDismiss, testTag = "GridSizeBottomSheet_Root") {
        Title("Choose a board", 30f)
        Body("Bigger boards give your sprites more room to grow.")
        Spacer(Modifier.height(14.dp))
        val options = listOf(
            Triple(3, "3 × 3", "Easy · quick games"),
            Triple(4, "4 × 4", "Classic · the real deal"),
            Triple(5, "5 × 5", "Roomy · long runs"),
            Triple(6, "6 × 6", "Huge · marathon")
        )
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { (n, title, sub) ->
                    val selected = n == currentSize
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) Color(0xCC3A1C80) else Color(0xB80C0522))
                            .border(if (selected) 2.dp else 1.dp, if (selected) Gold else Line, RoundedCornerShape(18.dp))
                            .clickable { onSizeSelected(n) }
                            .padding(10.dp)
                            .testTag("GridSize_Option_$n"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MiniGrid(n, Modifier.size(86.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(title, color = Color.White, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(sub, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        SpriteGhostButton("Cancel", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MiniGrid(n: Int, modifier: Modifier) {
    val values = remember(n) {
        val r = kotlin.random.Random(n * 31)
        List(n * n) { if (r.nextFloat() < 0.45f) 2 shl r.nextInt(0, if (n <= 3) 3 else 5) else 0 }
    }
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xBF3C1E82)).padding(3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until n) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (col in 0 until n) {
                    val v = values[row * n + col]
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color(0xA6140834))) {
                        if (v > 0) SpriteTile(v, Modifier.fillMaxSize(), showNumber = false, showEffects = false)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────── Tutorial ───────────────────────────────

private class TipStep(val title: String, val body: String, val art: @Composable () -> Unit)

/** The how-to-play cards, told with the sprites themselves. */
@Composable
internal fun SpriteTutorial(isTimeAttack: Boolean, onDismiss: () -> Unit) {
    val steps = remember(isTimeAttack) {
        if (isTimeAttack) listOf(
            TipStep("Beat the clock", "You start with 60 seconds. Every swipe counts, so keep the sprites moving.") { ClockArt() },
            TipStep("Merges buy time", "Each merge adds seconds back to the clock, and bigger sprites add more.") { MergeArt(8) },
            TipStep("Build a multiplier", "Merge on back-to-back moves to raise your score multiplier, up to x5.") { ComboArt() },
            TipStep("Take a breather", "Pause stops the timer. Help shows these tips again any time.") { SleepArt() }
        ) else listOf(
            TipStep("Swipe to merge", "Swipe to slide every sprite. When two twins touch, they merge and evolve into the next element.") { MergeArt(2) },
            TipStep("Raise the Ember King", "Drip grows into Sprig, Sunny, Frosty and on up to the Ember King at 2048.") { ChainArt() },
            TipStep("Power-ups", "Undo a move, smash a sprite with the Hammer, Shuffle the board, Freeze new arrivals, or ask for a Hint.") { PowerArt() },
            TipStep("Make friends", "Tap a sprite to poke it. Leave them alone for a while and they doze off.") { SleepArt() }
        )
    }
    var step by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE60A041E))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding()
            .testTag("TutorialOverlay_Root"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Skip", color = Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(50)).clickable(onClick = onDismiss).padding(horizontal = 14.dp, vertical = 8.dp).testTag("Tutorial_Button_Skip")
        )
        Column(
            Modifier.padding(22.dp).widthIn(max = 420.dp).fillMaxWidth().spritePanel().padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val s = steps[step]
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xB80C0522)).border(1.dp, Line, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                androidx.compose.runtime.key(step) { s.art() }
            }
            Spacer(Modifier.height(16.dp))
            Title(s.title, 28f)
            Spacer(Modifier.height(4.dp))
            Body(s.body)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.indices.forEach { k ->
                        Box(Modifier.size(if (k == step) 22.dp else 8.dp, 8.dp).clip(RoundedCornerShape(50)).background(if (k == step) Gold else Color.White.copy(alpha = 0.25f)))
                    }
                }
                SpriteGoldButton(if (step == steps.lastIndex) "Let's go!" else "Next", { if (step == steps.lastIndex) onDismiss() else step++ }, height = 46.dp, modifier = Modifier.testTag("Tutorial_Button_Next"))
            }
        }
    }
}

@Composable
private fun MergeArt(v: Int) {
    val t by rememberInfiniteTransition(label = "merge").animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "mergeT")
    var key by remember { mutableIntStateOf(0) }
    val merged = t > 0.45f
    LaunchedEffect(merged) { if (merged) key++ }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!merged) {
            val slide = (t / 0.45f).coerceIn(0f, 1f)
            Box(Modifier.size(70.dp).graphicsLayer { translationX = 70.dp.toPx() * 0.5f * slide }) { SpriteTile(v, Modifier.fillMaxSize(), emotion = SpriteEmotion.EXCITED, lookX = 2f) }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(70.dp).graphicsLayer { translationX = -70.dp.toPx() * 0.5f * slide }) { SpriteTile(v, Modifier.fillMaxSize(), emotion = SpriteEmotion.EXCITED, lookX = -2f) }
        } else {
            Box(Modifier.size(84.dp)) { SpriteTile(v * 2, Modifier.fillMaxSize(), mergeKey = key, emotion = SpriteEmotion.HAPPY) }
        }
    }
}

@Composable
private fun ChainArt() {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(2, 4, 8, 16, 32, 64).forEach { SpriteTile(it, Modifier.size(38.dp), showCard = false, showNumber = false, showEffects = false) }
        Text("…", color = Muted, fontSize = 22.sp)
        SpriteTile(2048, Modifier.size(60.dp), showCard = false, showNumber = false, showEffects = false, emotion = SpriteEmotion.PROUD)
    }
}

@Composable
private fun PowerArt() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SpriteTile(16, Modifier.size(66.dp), emotion = SpriteEmotion.SURPRISED, wantHitDeg = 4f)
        SpriteTile(4, Modifier.size(66.dp), emotion = SpriteEmotion.DIZZY)
        SpriteTile(8, Modifier.size(66.dp), emotion = SpriteEmotion.WORRIED)
    }
}

@Composable
private fun SleepArt() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
        SpriteTile(4, Modifier.size(72.dp), emotion = SpriteEmotion.LOVE)
        SpriteTile(32, Modifier.size(72.dp), emotion = SpriteEmotion.SLEEPY)
    }
}

@Composable
private fun ComboArt() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SpriteTile(64, Modifier.size(80.dp), emotion = SpriteEmotion.EXCITED)
        Text("x5", color = Gold, fontFamily = displayFont(), fontSize = 44.sp, style = TextStyle(shadow = Shadow(Color(0xFF7A2A08), Offset(0f, 4f), 0f)))
    }
}

@Composable
private fun ClockArt() {
    val t by rememberInfiniteTransition(label = "tick").animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "tickT")
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF2E7BE0), Color(0xFF123E9A))), radius = r * 0.96f)
            drawCircle(Color(0xFFA6ECFF), radius = r * 0.96f, style = Stroke(5.dp.toPx()))
            val a = toRadiansCompat(t.toDouble())
            drawLine(Gold, center, Offset(center.x + r * 0.7f * kotlin.math.sin(a).toFloat(), center.y - r * 0.7f * kotlin.math.cos(a).toFloat()), strokeWidth = 4.dp.toPx())
        }
        SpriteTile(16, Modifier.size(70.dp).offset(y = 8.dp), showCard = false, showNumber = false, emotion = SpriteEmotion.WORRIED)
    }
}

// ─────────────────────────────── Collection ───────────────────────────────

/** The sprite collection: all eleven, found ones in colour with their names, the rest as "???" silhouettes. */
@Composable
internal fun SpriteCollectionDialog(seen: Set<Int>, onDismiss: () -> Unit) {
    val found = ElementalSprites.all.indices.count { (2 shl it) in seen }
    SpriteDialog(onDismiss = onDismiss, testTag = "SpriteCollection_Root") {
        Title("Collection", 30f)
        Text("$found of ${ElementalSprites.all.size} sprites found", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        ElementalSprites.all.chunked(3).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEachIndexed { k, spec ->
                    val i = r * 3 + k
                    val v = 2 shl i
                    val have = v in seen
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xB80C0522)).border(1.dp, if (have) Gold.copy(alpha = 0.5f) else Line, RoundedCornerShape(16.dp)).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SpriteTile(v, Modifier.size(64.dp), locked = !have, emotion = SpriteEmotion.HAPPY, showEffects = false)
                        Spacer(Modifier.height(4.dp))
                        Text("No." + pad2(i + 1), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(if (have) spec.name else "???", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        SpriteGoldButton("Close", onDismiss, Modifier.fillMaxWidth())
    }
}
