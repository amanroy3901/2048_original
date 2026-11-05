package com.avfusionapps.game_2048.shared.ui

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.shared.game.Achievements
import com.avfusionapps.game_2048.shared.game.DailyChallenge
import com.avfusionapps.game_2048.shared.game.GameMode
import com.avfusionapps.game_2048.shared.game.Progress
import com.avfusionapps.game_2048.shared.sprites.ElementalSprites
import com.avfusionapps.game_2048.shared.sprites.SpriteEmotion
import com.avfusionapps.game_2048.shared.sprites.SpriteTile
import kotlinx.coroutines.launch

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
/**
 * The Elemental Sprites home, laid out like a game menu: one fixed screen, never scrolls.
 * Top bar, the hero banner (it takes whatever height is left), today's challenge, the saved game,
 * the three modes as big tappable tiles, and a dock for the collection, medals and best score.
 * Landscape and wide tablets put the hero on the left and the menu on the right.
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
    // The hills sit behind Blaze's pedestal wherever the hero banner ends up.
    var hills by remember { mutableStateOf(160.dp) }
    var sideBySide by remember { mutableStateOf(false) }
    val heroPlaced = Modifier.onGloballyPositioned { c ->
        val top = c.positionInRoot().y
        val h = c.size.height
        hills = with(density) { (top + h * (if (h.toDp() < 168.dp) 0.7f else 0.28f)).toDp() }
    }
    Box(Modifier.fillMaxSize().testTag("MainScreen_Root")) {
        SpriteNightBackground(Modifier.fillMaxSize(), scene = true, hillsTop = hills, moon = !sideBySide)
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        // Tablets: draw the whole menu larger so tiles and text keep their phone proportions.
        val grow = if (maxWidth > maxHeight) minOf(maxWidth / 900.dp, maxHeight / 520.dp) else maxWidth / 440.dp
        val zoom = grow.coerceIn(1f, 1.4f)
        CompositionLocalProvider(LocalDensity provides Density(density.density * zoom, density.fontScale)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val side = maxWidth > maxHeight && maxWidth >= 560.dp
            SideEffect { sideBySide = side }
            val pad = if (maxWidth >= 720.dp) 28.dp else 14.dp
            Column(Modifier.fillMaxSize().padding(horizontal = pad, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                HomeTopBar(playerName, coins, actions)
                Spacer(Modifier.height(6.dp))
                if (side) {
                    Row(
                        Modifier.weight(1f).fillMaxWidth().widthIn(max = 1200.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val compact = this@BoxWithConstraints.maxHeight < 500.dp
                        // Left: the hero with the collection, medals and best score under it.
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                HeroArea(maxHeight.coerceAtMost(420.dp), heroPlaced)
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.widthIn(max = 520.dp)) { Dock(best, spritesFound, unlocked.size, actions, compact) }
                        }
                        // Right: today's challenge, the saved game and the modes.
                        Box(Modifier.weight(1.1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            MenuColumn(daily, hasSaved, savedScore, savedGrid, best, spritesFound, unlocked.size, actions, compact, Modifier.widthIn(max = 560.dp), withDock = false)
                        }
                    }
                } else {
                    Column(Modifier.weight(1f).widthIn(max = 600.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        // The hero gets the space the menu leaves; on tall screens the menu stays snug under it.
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            HeroArea(maxHeight.coerceAtMost(if (maxWidth >= 560.dp) 520.dp else 440.dp), heroPlaced)
                        }
                        Spacer(Modifier.height(8.dp))
                        val compact = this@BoxWithConstraints.maxHeight < 660.dp
                        MenuColumn(daily, hasSaved, savedScore, savedGrid, best, spritesFound, unlocked.size, actions, compact, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
        }
        }
    }
}

/** The banner, or just the title when the screen is too short for Blaze to stand under it. */
@Composable
private fun HeroArea(height: Dp, placed: Modifier) {
    if (height >= 168.dp) {
        SpriteHero(0, 0, SpriteEmotion.HAPPY, 0, 0, 0, height = height, showScores = false, modifier = placed, maxScale = (height.value / 300f).coerceIn(1.15f, 1.4f))
    } else {
        // Short screens: Blaze stands beside the title instead of under it.
        Row(placed.fillMaxWidth().height(height), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (height >= 84.dp) {
                SpriteTile(value = 64, modifier = Modifier.size((height * 0.72f).coerceAtMost(110.dp)), showCard = false, showNumber = false, showEffects = false, emotion = SpriteEmotion.HAPPY)
                Spacer(Modifier.width(10.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HomeTitle(fontSize = (height.value * 0.3f).coerceIn(26f, 46f).sp)
            }
        }
    }
}

@Composable
private fun HomeTitle(fontSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        "2048", fontFamily = displayFont(), fontSize = fontSize, lineHeight = fontSize * 0.92f,
        style = androidx.compose.ui.text.TextStyle(brush = Brush.verticalGradient(listOf(Color(0xFFFFF6B0), Gold, Color(0xFFFF8A1F))))
    )
    Text("Elemental Sprites", fontFamily = displayFont(), fontSize = fontSize * 0.46f, lineHeight = fontSize * 0.46f, color = Color.White)
}

@Composable
private fun MenuColumn(
    daily: DailyChallenge?,
    hasSaved: Boolean,
    savedScore: Int,
    savedGrid: List<List<Int>>,
    best: Int,
    spritesFound: Int,
    medals: Int,
    actions: HomeActions,
    compact: Boolean,
    modifier: Modifier,
    withDock: Boolean = true
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
        DailyStrip(daily, actions, compact)
        if (hasSaved) ContinueStrip(savedScore, savedGrid, actions, compact)
        ModeTiles(actions, highlightClassic = !hasSaved, compact = compact)
        if (withDock) Dock(best, spritesFound, medals, actions, compact)
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, border: Color = Line, radius: Dp = 20.dp, padding: Dp = 12.dp, content: @Composable () -> Unit) {
    Box(
        modifier
            .drawBehind { drawRoundRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(0f, 4.dp.toPx()), size = size, cornerRadius = CornerRadius(radius.toPx())) }
            .clip(RoundedCornerShape(radius))
            .background(Brush.verticalGradient(listOf(Color(0xCC2B1870), Color(0xE61A0E45))))
            .border(2.dp, border, RoundedCornerShape(radius))
            .padding(padding)
    ) { content() }
}

// ─────────────────────────────── Daily challenge ───────────────────────────────

@Composable
private fun DailyStrip(daily: DailyChallenge?, actions: HomeActions, compact: Boolean) {
    val d = daily ?: return
    val scope = rememberCoroutineScope()
    val clock = com.avfusionapps.game_2048.shared.sprites.spriteTime()
    val ready = d.done && !d.claimed
    // While a reward waits to be claimed the border glows brighter and dimmer. The glow is drawn on top in the
    // draw phase, so the pulse repaints one outline instead of rebuilding the card.
    val glow = if (ready) Modifier.drawWithContent {
        drawContent()
        val c = (clock.value % 2f) / 2f
        val k = if (c < 0.5f) c * 2f else (1f - c) * 2f
        val r = 20.dp.toPx()
        val sw = 2.dp.toPx()
        drawRoundRect(Gold.copy(alpha = 0.45f * k), topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw), cornerRadius = CornerRadius(r), style = Stroke(sw))
    } else Modifier
    Panel(Modifier.fillMaxWidth().then(glow), border = if (ready) Gold.copy(alpha = 0.45f) else Color(0x80FFC93A), padding = if (compact) 8.dp else 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(if (compact) 40.dp else 48.dp), contentAlignment = Alignment.Center) {
                SpriteTile(value = dailySprite(d), modifier = Modifier.fillMaxSize(), emotion = if (d.done) SpriteEmotion.PROUD else SpriteEmotion.EXCITED, showNumber = false, showEffects = false, still = true)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DAILY", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(
                        if (d.streak > 0) "  🔥 ${d.streak}  ·  ${untilMidnight()} left" else "  ·  ${untilMidnight()} left",
                        color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Text(d.title, color = Color.White, fontFamily = displayFont(), fontSize = if (compact) 15.sp else 17.sp, lineHeight = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                val shown = d.progress.coerceAtMost(d.target)
                val tile = d.kind.name.contains("TILE")
                val frac = if (tile) {
                    if (shown <= 0) 0f else (kotlin.math.log2(shown.toFloat()) / kotlin.math.log2(d.target.toFloat())).coerceIn(0f, 1f)
                } else shown.toFloat() / d.target
                ProgressBar(frac, label = if (tile) (if (d.done) "Done!" else "Best today: ${if (shown > 0) shown else "–"}") else "${spriteFmt(shown)} / ${spriteFmt(d.target)}")
            }
            Spacer(Modifier.width(10.dp))
            when {
                d.claimed -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                    Text("✓", color = Color(0xFF8BE38B), fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                    Text("Claimed", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                ready -> SpriteGoldButton("+${d.reward}", {
                    scope.launch { if (Progress.claimDaily() > 0) com.avfusionapps.game_2048.shared.game.SpriteSfx.play(com.avfusionapps.game_2048.shared.game.SpriteSfx.Sound.COIN) }
                }, Modifier.semantics { contentDescription = "Claim ${d.reward} coins" }, height = 40.dp)
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpriteGoldButton("Play", if (d.mode == GameMode.SHOOTER) actions.onShooter else actions.onNewClassic, height = 38.dp)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${d.reward}", color = Color.White, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 13.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────── Continue ───────────────────────────────

@Composable
private fun ContinueStrip(score: Int, grid: List<List<Int>>, actions: HomeActions, compact: Boolean) {
    Panel(Modifier.fillMaxWidth().clickable(onClick = actions.onContinue), border = Gold.copy(alpha = 0.7f), padding = if (compact) 8.dp else 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (grid.isNotEmpty()) MiniBoard(grid, Modifier.size(if (compact) 44.dp else 56.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("YOUR GAME", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("${spriteFmt(score)} points", color = Color.White, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, lineHeight = 21.sp, maxLines = 1)
                val top = grid.flatten().maxOrNull() ?: 0
                if (top > 0 && !compact) Text("Biggest: ${ElementalSprites.specFor(top).name}", color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SpriteGoldButton("Continue", actions.onContinue, height = if (compact) 40.dp else 46.dp)
        }
    }
}

// ─────────────────────────────── Modes ───────────────────────────────

@Composable
private fun ModeTiles(actions: HomeActions, highlightClassic: Boolean, compact: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeTile("Classic", "Swipe & merge", { ClassicArt() }, actions.onNewClassic, "MainScreen_Button_ClassicMode", if (highlightClassic) Gold.copy(alpha = 0.8f) else Line, compact, Modifier.weight(1f))
        ModeTile("Time Attack", "60 seconds", { TimeAttackArt() }, actions.onTimeAttack, "MainScreen_Button_TimeAttackMode", Color(0x9947C6FF), compact, Modifier.weight(1f))
        ModeTile("Shooter", "5 lanes", { ShooterArt() }, actions.onShooter, "MainScreen_Button_NeonDropMode", Color(0x99FF9A1F), compact, Modifier.weight(1f))
    }
}

@Composable
private fun ModeTile(
    title: String,
    tagline: String,
    art: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String,
    accent: Color,
    compact: Boolean,
    modifier: Modifier,
    withDock: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Panel(
        modifier
            .scale(if (pressed) 0.95f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .testTag(testTag)
            .semantics { contentDescription = "Play $title" },
        border = accent, radius = 22.dp, padding = 8.dp
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ArtSlot(if (compact) 54.dp else 72.dp, art)
            Spacer(Modifier.height(4.dp))
            Text(title, color = Color.White, fontFamily = displayFont(), fontSize = if (compact) 15.sp else 17.sp, lineHeight = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!compact) Text(tagline, color = Muted, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().height(if (compact) 26.dp else 30.dp)
                    .drawBehind { drawRoundRect(Color(0xFF8A4A08), topLeft = Offset(0f, 3.dp.toPx()), size = size, cornerRadius = CornerRadius(size.height / 2f)) }
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFD25A), Color(0xFFFF9A1F))), RoundedCornerShape(50))
                    .border(1.5.dp, Color(0xFFFFE7A6), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("PLAY", color = Color(0xFF5A2A00), fontFamily = displayFont(), fontSize = 14.sp, lineHeight = 15.sp, letterSpacing = 1.sp)
            }
        }
    }
}

/** Mode art is drawn at 92 dp; this scales it to fit the tile. */
@Composable
private fun ArtSlot(size: Dp, art: @Composable () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.requiredSize(92.dp).graphicsLayer { scaleX = size.value / 92f; scaleY = size.value / 92f }, contentAlignment = Alignment.Center) { art() }
    }
}

// ─────────────────────────────── Collection, medals, best ───────────────────────────────

@Composable
private fun Dock(best: Int, spritesFound: Int, medals: Int, actions: HomeActions, compact: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DockItem("Sprites", "$spritesFound/11", 64, actions.onCollection, compact, Modifier.weight(1f))
        DockItem("Medals", "$medals/${Achievements.all.size}", 512, actions.onAchievements, compact, Modifier.weight(1f))
        DockItem("Best", spriteFmt(best), 2048, actions.onProfile, compact, Modifier.weight(1f))
    }
}

@Composable
private fun DockItem(label: String, value: String, sprite: Int, onClick: () -> Unit, compact: Boolean, modifier: Modifier) {
    Row(
        modifier
            .height(if (compact) 46.dp else 54.dp)
            .clip(RoundedCornerShape(50))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 10.dp)
            .semantics { contentDescription = "$label $value" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteTile(value = sprite, modifier = Modifier.size(if (compact) 32.dp else 38.dp), showCard = false, showNumber = false, showEffects = false, still = true)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(value, color = Color.White, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = Muted, fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun HomeTopBar(playerName: String, coins: Int, actions: HomeActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
        Row(
            Modifier
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
            ) { SpriteTile(64, Modifier.size(34.dp), showCard = false, showNumber = false, showAura = false, showEffects = false, still = true) }
            Spacer(Modifier.width(8.dp))
            Text(playerName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        }
        Row(
            Modifier.height(36.dp).clip(RoundedCornerShape(50)).background(Color(0x8C0A041E))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50)).padding(start = 8.dp, end = 12.dp)
                .semantics { contentDescription = "$coins coins" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinIcon(Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(spriteFmt(coins), color = Color.White, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        RoundButton(size = 40.dp, description = "Settings and themes", onClick = actions.onSettings) { drawGear() }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGear() {
    val c = center
    val r = size.minDimension / 2f
    for (k in 0 until 8) {
        val a = ((k * 45.0) * kotlin.math.PI / 180.0)
        drawLine(Color.White, Offset(c.x + (r * 0.55f) * kotlin.math.cos(a).toFloat(), c.y + (r * 0.55f) * kotlin.math.sin(a).toFloat()), Offset(c.x + r * 0.95f * kotlin.math.cos(a).toFloat(), c.y + r * 0.95f * kotlin.math.sin(a).toFloat()), strokeWidth = r * 0.32f, cap = StrokeCap.Round)
    }
    drawCircle(Color.White, r * 0.62f)
    drawCircle(Color(0xFF4A2496), r * 0.28f)
}

private fun dailySprite(d: DailyChallenge): Int = when (d.kind.name) {
    "REACH_TILE", "SHOOTER_TILE" -> d.target
    "SCORE" -> 512
    "MERGES" -> 4
    "COMBO" -> 64
    else -> 16
}

private fun untilMidnight(): String {
    // No time-zone API in common code: counts down to the next UTC midnight.
    val day = 86_400_000L
    val left = day - (com.avfusionapps.game_2048.shared.platform.nowMillis() % day)
    val mins = left / 60_000L
    return "${mins / 60}h ${mins % 60}m"
}

@Composable
private fun ProgressBar(fraction: Float, label: String) {
    Box(
        Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.45f)).border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFF9A1F), Color(0xFFFFD25A))))
        )
        Text(label, color = Color.White, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 14.sp)
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
                        if (v != 0) SpriteTile(value = v, modifier = Modifier.fillMaxSize(), showNumber = n <= 4, fontSizeMultiplier = 0.8f, showEffects = false, still = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicArt() {
    Box(Modifier.size(92.dp)) {
        listOf(Triple(2, 0f, 0f), Triple(4, 1f, 0f), Triple(8, 0f, 1f), Triple(16, 1f, 1f)).forEachIndexed { k, (v, x, y) ->
            Box(Modifier.offset(x = (x * 46).dp, y = (y * 46).dp).size(44.dp).graphicsLayer { rotationZ = if (k % 2 == 0) -4f else 4f }) {
                SpriteTile(value = v, modifier = Modifier.fillMaxSize(), fontSizeMultiplier = 0.9f, showEffects = false, still = true)
            }
        }
    }
}

@Composable
private fun TimeAttackArt() {
    val clock = com.avfusionapps.game_2048.shared.sprites.spriteTime()
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF2E7BE0), Color(0xFF123E9A))), radius = r * 0.96f)
            drawCircle(Color(0xFFA6ECFF), radius = r * 0.96f, style = Stroke(4.dp.toPx()))
            drawRoundRect(Color(0xFFA6ECFF), topLeft = Offset(center.x - 7.dp.toPx(), -3.dp.toPx()), size = Size(14.dp.toPx(), 8.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()))
            for (k in 0 until 12) {
                val a = ((k * 30.0) * kotlin.math.PI / 180.0)
                drawLine(Color.White.copy(alpha = 0.6f), Offset(center.x + r * 0.78f * kotlin.math.sin(a).toFloat(), center.y - r * 0.78f * kotlin.math.cos(a).toFloat()), Offset(center.x + r * 0.88f * kotlin.math.sin(a).toFloat(), center.y - r * 0.88f * kotlin.math.cos(a).toFloat()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        // Only the hand redraws as it sweeps (once every 4 s).
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val a = (((clock.value % 4f) * kotlin.math.PI / 180.0) / 4f * 360.0)
            drawLine(Gold, center, Offset(center.x + r * 0.7f * kotlin.math.sin(a).toFloat(), center.y - r * 0.7f * kotlin.math.cos(a).toFloat()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        SpriteTile(value = 16, modifier = Modifier.size(58.dp).offset(y = 6.dp), showCard = false, showNumber = false, showEffects = false, emotion = SpriteEmotion.EXCITED, still = true)
    }
}

@Composable
private fun ShooterArt() {
    val clock = com.avfusionapps.game_2048.shared.sprites.spriteTime()
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
        Box(Modifier.offset(x = 4.dp, y = 4.dp).size(26.dp)) { SpriteTile(8, Modifier.fillMaxSize(), showCard = false, showNumber = false, showEffects = false, still = true) }
        Box(Modifier.offset(x = 60.dp, y = 4.dp).size(26.dp)) { SpriteTile(4, Modifier.fillMaxSize(), showCard = false, showNumber = false, showEffects = false, still = true) }
        // The shot rises in the layout's placement step, so the art never recomposes.
        Box(
            Modifier
                .offset { val t = (clock.value % 1.6f) / 1.6f; IntOffset(31.dp.roundToPx(), (58f - 44f * (t * 1.4f).coerceAtMost(1f)).dp.roundToPx()) }
                .size(30.dp)
        ) { SpriteTile(2, Modifier.fillMaxSize(), showCard = false, showNumber = false, showEffects = false, emotion = SpriteEmotion.EXCITED, still = true) }
    }
}
