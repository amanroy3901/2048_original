package com.avfusionapps.game_2048.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.data.progress.Achievements
import com.avfusionapps.game_2048.data.progress.DailyChallenge
import com.avfusionapps.game_2048.data.progress.GameMode
import com.avfusionapps.game_2048.data.progress.Progress
import com.avfusionapps.game_2048.ui.components.CoinIcon
import com.avfusionapps.game_2048.ui.sprites.ElementalSprites
import com.avfusionapps.game_2048.ui.sprites.SpriteDisplayFont
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.ui.sprites.SpriteNumberFont
import com.avfusionapps.game_2048.ui.sprites.SpriteTile
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

private val Muted = Color(0xFFC9B8F0)
private val Line = Color(0x52BE96FF)
private val Panel = Color(0xB80C0522)
private val Gold = Color(0xFFFFC93A)

/** Everything the sprite home screen needs from the app. */
internal class HomeActions(
    val onContinue: () -> Unit,
    val onNewClassic: () -> Unit,
    val onTimeAttack: () -> Unit,
    val onShooter: () -> Unit,
    val onProfile: () -> Unit,
    val onSettings: () -> Unit,
    val onCollection: () -> Unit,
    val onAchievements: () -> Unit
)

/**
 * The Elemental Sprites home: the night sky and hero banner from the prototype, today's challenge, the saved game,
 * the three modes as illustrated cards, and the collection, achievements and best score. Two columns on wide screens.
 */
@Composable
internal fun SpriteHomeScreen(
    playerName: String,
    coins: Int,
    best: Int,
    hasSaved: Boolean,
    savedScore: Int,
    savedGrid: List<List<Int>>,
    spritesFound: Int,
    actions: HomeActions
) {
    val daily by Progress.daily.collectAsState()
    val unlocked by Progress.unlocked.collectAsState()
    val density = LocalDensity.current
    val safeTop = with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
    Box(Modifier.fillMaxSize().testTag("MainScreen_Root")) {
        SpriteNightBackground(Modifier.fillMaxSize(), scene = true, hillsTop = safeTop + 8.dp + 106.dp)
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val wide = maxWidth >= 720.dp
            val pad = if (wide) 28.dp else 14.dp
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = pad, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HomeTopBar(playerName, coins, actions)
                Spacer(Modifier.height(8.dp))
                if (wide) {
                    // Wide screens: two columns, centred in the space below the top bar.
                    Box(Modifier.fillMaxWidth().heightIn(min = this@BoxWithConstraints.maxHeight - 76.dp), contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.widthIn(max = 1100.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SpriteHero(0, 0, SpriteEmotion.HAPPY, 0, 0, 0, height = if (this@BoxWithConstraints.maxHeight < 600.dp) 210.dp else 300.dp, showScores = false)
                            DailyCard(daily, actions)
                            ContinueCard(hasSaved, savedScore, savedGrid, actions)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModeCards(actions)
                            ExtrasRow(best, spritesFound, unlocked.size, actions)
                        }
                    }
                    }
                } else {
                    Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpriteHero(0, 0, SpriteEmotion.HAPPY, 0, 0, 0, height = 214.dp, showScores = false)
                        DailyCard(daily, actions)
                        ContinueCard(hasSaved, savedScore, savedGrid, actions)
                        ModeCards(actions)
                        ExtrasRow(best, spritesFound, unlocked.size, actions)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(playerName: String, coins: Int, actions: HomeActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .weight(1f, fill = false)
                .widthIn(max = 220.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x8C0A041E))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                .clickable(onClick = actions.onProfile)
                .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp)
                .semantics { contentDescription = "Profile, $playerName" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF5A2CB8), Color(0xFF2A1366))))
                    .border(2.dp, Color(0xFFFFB547), CircleShape),
                contentAlignment = Alignment.Center
            ) { SpriteTile(64, Modifier.size(34.dp), showCard = false, showNumber = false, showAura = false, showEffects = false) }
            Spacer(Modifier.width(8.dp))
            Text(playerName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.height(36.dp).clip(RoundedCornerShape(50)).background(Color(0x8C0A041E))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50)).padding(start = 8.dp, end = 12.dp)
                .semantics { contentDescription = "$coins coins" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinIcon(Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("%,d".format(coins), color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        RoundButton(size = 40.dp, description = "Settings and themes", onClick = actions.onSettings) { drawGear() }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGear() {
    val c = center
    val r = size.minDimension / 2f
    for (k in 0 until 8) {
        val a = Math.toRadians(k * 45.0)
        drawLine(Color.White, Offset(c.x + (r * 0.55f) * kotlin.math.cos(a).toFloat(), c.y + (r * 0.55f) * kotlin.math.sin(a).toFloat()), Offset(c.x + r * 0.95f * kotlin.math.cos(a).toFloat(), c.y + r * 0.95f * kotlin.math.sin(a).toFloat()), strokeWidth = r * 0.32f, cap = StrokeCap.Round)
    }
    drawCircle(Color.White, r * 0.62f)
    drawCircle(Color(0xFF4A2496), r * 0.28f)
}

@Composable
private fun SectionPanel(modifier: Modifier = Modifier, border: Color = Line, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind { drawRoundRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(0f, 5.dp.toPx()), size = size, cornerRadius = CornerRadius(22.dp.toPx())) }
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xCC2B1870), Color(0xE61A0E45))))
            .border(2.dp, border, RoundedCornerShape(22.dp))
            .padding(14.dp),
        content = content
    )
}

// ─────────────────────────────── Daily challenge ───────────────────────────────

@Composable
private fun DailyCard(daily: DailyChallenge?, actions: HomeActions) {
    val d = daily ?: return
    val scope = rememberCoroutineScope()
    val pulse by rememberInfiniteTransition(label = "daily").animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "dailyT")
    SectionPanel(border = if (d.done && !d.claimed) Gold.copy(alpha = 0.5f + 0.4f * pulse) else Color(0x80FFC93A)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                SpriteTile(value = dailySprite(d), modifier = Modifier.fillMaxSize(), emotion = if (d.done) SpriteEmotion.PROUD else SpriteEmotion.EXCITED, showNumber = false)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DAILY CHALLENGE", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    if (d.streak > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("🔥 ${d.streak}-day streak", color = Color(0xFFFFB25A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(d.title, color = Color.White, fontFamily = SpriteDisplayFont, fontSize = 20.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(6.dp))
                val shown = d.progress.coerceAtMost(d.target)
                val frac = if (d.kind.name.contains("TILE")) {
                    if (shown <= 0) 0f else (kotlin.math.log2(shown.toFloat()) / kotlin.math.log2(d.target.toFloat())).coerceIn(0f, 1f)
                } else shown.toFloat() / d.target
                ProgressBar(frac, label = if (d.kind.name.contains("TILE")) (if (d.done) "Done!" else "Best today: ${if (shown > 0) shown else "–"}") else "${"%,d".format(shown)} / ${"%,d".format(d.target)}")
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            d.claimed -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("✓ Claimed. Next challenge in ${untilMidnight()}", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            d.done -> SpriteGoldButton("Claim ${d.reward} coins", {
                scope.launch { if (Progress.claimDaily() > 0) com.avfusionapps.game_2048.utils.SpriteSfx.play(com.avfusionapps.game_2048.utils.SpriteSfx.Sound.COIN) }
            }, Modifier.fillMaxWidth())
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${d.reward} coins", color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text("  ·  ${untilMidnight()} left", color = Muted, fontSize = 13.sp)
                }
                SpriteGoldButton("Play", if (d.mode == GameMode.SHOOTER) actions.onShooter else actions.onNewClassic, height = 44.dp)
            }
        }
    }
}

private fun dailySprite(d: DailyChallenge): Int = when (d.kind.name) {
    "REACH_TILE", "SHOOTER_TILE" -> d.target
    "SCORE" -> 512
    "MERGES" -> 4
    "COMBO" -> 64
    else -> 16
}

private fun untilMidnight(): String {
    val now = LocalDateTime.now()
    val d = Duration.between(now, now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT))
    return "${d.toHours()}h ${d.toMinutesPart()}m"
}

@Composable
private fun ProgressBar(fraction: Float, label: String) {
    Box(
        Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.45f)).border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFF9A1F), Color(0xFFFFD25A))))
        )
        Text(label, color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 14.sp)
    }
}

// ─────────────────────────────── Continue ───────────────────────────────

@Composable
private fun ContinueCard(hasSaved: Boolean, score: Int, grid: List<List<Int>>, actions: HomeActions) {
    SectionPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasSaved && grid.isNotEmpty()) {
                MiniBoard(grid, Modifier.size(104.dp))
            } else {
                Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                    SpriteTile(value = 2, modifier = Modifier.size(84.dp), emotion = SpriteEmotion.EXCITED, showNumber = false)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (hasSaved) "YOUR GAME" else "NEW GAME", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                if (hasSaved) {
                    val top = grid.flatten().maxOrNull() ?: 0
                    Text("%,d points".format(score), color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 24.sp)
                    if (top > 0) Text("Biggest: ${ElementalSprites.specFor(top).name} ($top)", color = Muted, fontSize = 13.sp)
                } else {
                    Text("Drip is ready to grow up", color = Color.White, fontFamily = SpriteDisplayFont, fontSize = 19.sp)
                    Text("3×3, 4×4 or 5×5 board", color = Muted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                SpriteGoldButton(if (hasSaved) "Continue" else "Start", if (hasSaved) actions.onContinue else actions.onNewClassic, height = 44.dp)
            }
        }
    }
}

@Composable
private fun MiniBoard(grid: List<List<Int>>, modifier: Modifier) {
    val n = grid.size
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xBF3C1E82)).border(1.5.dp, Color(0x66BE96FF), RoundedCornerShape(14.dp)).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        grid.forEach { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { v ->
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Color(0xA6140834)), contentAlignment = Alignment.Center) {
                        if (v != 0) SpriteTile(value = v, modifier = Modifier.fillMaxSize(), showNumber = n <= 4, fontSizeMultiplier = 0.8f, showEffects = false)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────── Modes ───────────────────────────────

@Composable
private fun ModeCards(actions: HomeActions) {
    Text("GAME MODES", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
    ModeCard(
        title = "Classic",
        body = "Swipe to slide every sprite. Twins merge into the next element.",
        art = { ClassicArt() },
        primary = "Play" to actions.onNewClassic,
        testTag = "MainScreen_Button_ClassicMode"
    )
    ModeCard(
        title = "Time Attack",
        body = "One minute on the clock. Big merges buy you more time.",
        art = { TimeAttackArt() },
        primary = "Play" to actions.onTimeAttack,
        testTag = "MainScreen_Button_TimeAttackMode",
        accent = Color(0x8047C6FF)
    )
    ModeCard(
        title = "Neon Shooter",
        body = "Launch sprites up five lanes and chain the merges.",
        art = { ShooterArt() },
        primary = "Play" to actions.onShooter,
        testTag = "MainScreen_Button_NeonDropMode",
        accent = Color(0x80FF9A1F)
    )
}

@Composable
private fun ModeCard(
    title: String,
    body: String,
    art: @Composable () -> Unit,
    primary: Pair<String, () -> Unit>,
    testTag: String,
    accent: Color = Line
) {
    SectionPanel(border = accent, modifier = Modifier.clickable(onClick = primary.second).testTag(testTag)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) { art() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontFamily = SpriteDisplayFont, fontSize = 24.sp, lineHeight = 26.sp)
                Text(body, color = Muted, fontSize = 13.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(10.dp))
                SpriteGoldButton(primary.first, primary.second, height = 42.dp)
            }
        }
    }
}

@Composable
private fun ClassicArt() {
    Box(Modifier.size(92.dp)) {
        listOf(Triple(2, 0f, 0f), Triple(4, 1f, 0f), Triple(8, 0f, 1f), Triple(16, 1f, 1f)).forEachIndexed { k, (v, x, y) ->
            Box(Modifier.offset(x = (x * 46).dp, y = (y * 46).dp).size(44.dp).graphicsLayer { rotationZ = if (k % 2 == 0) -4f else 4f }) {
                SpriteTile(value = v, modifier = Modifier.fillMaxSize(), fontSizeMultiplier = 0.9f, showEffects = false)
            }
        }
    }
}

@Composable
private fun TimeAttackArt() {
    val t by rememberInfiniteTransition(label = "clock").animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "clockT")
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF2E7BE0), Color(0xFF123E9A))), radius = r * 0.96f)
            drawCircle(Color(0xFFA6ECFF), radius = r * 0.96f, style = Stroke(4.dp.toPx()))
            drawRoundRect(Color(0xFFA6ECFF), topLeft = Offset(center.x - 7.dp.toPx(), -3.dp.toPx()), size = Size(14.dp.toPx(), 8.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()))
            for (k in 0 until 12) {
                val a = Math.toRadians(k * 30.0)
                drawLine(Color.White.copy(alpha = 0.6f), Offset(center.x + r * 0.78f * kotlin.math.sin(a).toFloat(), center.y - r * 0.78f * kotlin.math.cos(a).toFloat()), Offset(center.x + r * 0.88f * kotlin.math.sin(a).toFloat(), center.y - r * 0.88f * kotlin.math.cos(a).toFloat()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            val a = Math.toRadians(t.toDouble())
            drawLine(Gold, center, Offset(center.x + r * 0.7f * kotlin.math.sin(a).toFloat(), center.y - r * 0.7f * kotlin.math.cos(a).toFloat()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        SpriteTile(value = 16, modifier = Modifier.size(58.dp).offset(y = 6.dp), showCard = false, showNumber = false, showEffects = false, emotion = SpriteEmotion.EXCITED)
    }
}

@Composable
private fun ShooterArt() {
    val t by rememberInfiniteTransition(label = "shot").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "shotT")
    Box(Modifier.size(92.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            for (k in 0 until 3) {
                val x = size.width * (0.08f + k * 0.3f)
                drawRoundRect(Color(0x8C140834), topLeft = Offset(x, 0f), size = Size(size.width * 0.26f, size.height), cornerRadius = CornerRadius(8.dp.toPx()))
            }
            val cx = size.width * 0.08f + size.width * 0.3f + size.width * 0.13f
            val beam = Path().apply {
                moveTo(cx - size.width * 0.07f, size.height * 0.85f); lineTo(cx + size.width * 0.07f, size.height * 0.85f)
                lineTo(cx + size.width * 0.13f, size.height * 0.2f); lineTo(cx - size.width * 0.13f, size.height * 0.2f); close()
            }
            drawPath(beam, Brush.verticalGradient(listOf(Color(0x08FF9A2E), Color(0xB8FFC04A)), startY = size.height * 0.2f, endY = size.height * 0.85f))
        }
        Box(Modifier.offset(x = 4.dp, y = 4.dp).size(26.dp)) { SpriteTile(8, Modifier.fillMaxSize(), showCard = false, showNumber = false, showEffects = false) }
        Box(Modifier.offset(x = 60.dp, y = 4.dp).size(26.dp)) { SpriteTile(4, Modifier.fillMaxSize(), showCard = false, showNumber = false, showEffects = false) }
        val y = 58f - 44f * ((t * 1.4f).coerceAtMost(1f))
        Box(Modifier.offset(x = 31.dp, y = y.dp).size(30.dp)) { SpriteTile(2, Modifier.fillMaxSize(), showCard = false, showNumber = false, showEffects = false, emotion = SpriteEmotion.EXCITED) }
    }
}

// ─────────────────────────────── Collection, achievements, best ───────────────────────────────

@Composable
private fun ExtrasRow(best: Int, spritesFound: Int, achievements: Int, actions: HomeActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ExtraTile("Collection", "$spritesFound/11", 64, actions.onCollection, Modifier.weight(1f))
        ExtraTile("Medals", "$achievements/${Achievements.all.size}", 512, actions.onAchievements, Modifier.weight(1f))
        ExtraTile("Best score", "%,d".format(best), 2048, actions.onProfile, Modifier.weight(1f))
    }
}

@Composable
private fun ExtraTile(label: String, value: String, sprite: Int, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .semantics { contentDescription = "$label $value" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpriteTile(value = sprite, modifier = Modifier.size(44.dp), showCard = false, showNumber = false, showEffects = false)
        Text(value, color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 19.sp, maxLines = 1)
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
