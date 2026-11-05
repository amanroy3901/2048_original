package com.avfusionapps.game_2048.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.shared.game.DropLevelUp
import com.avfusionapps.game_2048.shared.game.DropNotice
import com.avfusionapps.game_2048.shared.game.DropPowerUps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * The Neon Shooter's controls from the Elemental Sprites design: the level bar with its
 * reward chest, the "tap a lane" line, the power-up bar (Undo, Shuffle, NEXT, Remove,
 * Freeze) and the notice that pops over the board. Icons are drawn from the design's SVGs.
 */

private val Muted = Color(0xFFC9B8F0)
private val Gold = Color(0xFFFFC93A)
private val GoldDeep = Color(0xFFFF9A1F)
private val PanelLine = Color(0x52BE96FF)

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private val undoArc by lazy { svg("M13 12 A12 12 0 1 1 9 24") }
private val undoHead by lazy { svg("M5 9 L15 8 L13 18 Z") }
private val shuffleLines by lazy { svg("M5 12 H12 Q18 12 21 20 Q24 28 30 28 H33 M5 28 H12 Q16 28 18 24 M23 16 Q25 12 30 12 H33") }
private val shuffleHeads by lazy { svg("M31 7 L37 12 L31 17 Z M31 23 L37 28 L31 33 Z") }
private val flakeArms by lazy { svg("M20 4 V36 M6.1 12 L33.9 28 M6.1 28 L33.9 12") }
private val flakeTips by lazy {
    svg("M15 6 L20 10 L25 6 M15 34 L20 30 L25 34 M5 17.5 L10.5 15 L9 9.5 M35 22.5 L29.5 25 L31 30.5 M5 22.5 L10.5 25 L9 30.5 M35 17.5 L29.5 15 L31 9.5")
}
private val chestLid by lazy { svg("M3 12 Q3 4 17 4 Q31 4 31 12 Z") }
private val badgeStar by lazy { svg("M15 6 L17.6 11.6 L23.6 12.2 L19 16.2 L20.4 22.2 L15 19 L9.6 22.2 L11 16.2 L6.4 12.2 L12.4 11.6 Z") }
private val sparkle by lazy { svg("M12 1 Q13.5 10.5 23 12 Q13.5 13.5 12 23 Q10.5 13.5 1 12 Q10.5 10.5 12 1 Z") }

/** Draws a path given in a [box]-unit viewBox scaled to this canvas. */
private inline fun DrawScope.inBox(box: Float, block: DrawScope.() -> Unit) {
    val s = size.minDimension / box
    translate((size.width - box * s) / 2f, (size.height - box * s) / 2f) {
        scale(s, s, pivot = Offset.Zero) { block() }
    }
}

internal fun DrawScope.drawUndoIcon() = inBox(40f) {
    drawPath(undoArc, Color.White, style = Stroke(width = 5f, cap = StrokeCap.Round))
    drawPath(undoHead, Color.White)
}

internal fun DrawScope.drawShuffleIcon() = inBox(40f) {
    drawPath(shuffleLines, Color.White, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(shuffleHeads, Color.White)
}

internal fun DrawScope.drawFreezeIcon(color: Color = Color.White) = inBox(40f) {
    val stroke = Stroke(width = 3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(flakeArms, color, style = stroke)
    drawPath(flakeTips, color, style = stroke)
}

/** The Remove hammer, in the design's 40×40 box. Also swung over the board when it hits. */
internal fun DrawScope.drawHammerIcon() = inBox(40f) {
    drawLine(Color(0xFF7A4A22), Offset(19f, 17f), Offset(31f, 33f), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(Color(0xFFC88A4E), Offset(19f, 17f), Offset(31f, 33f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    rotate(-38f, pivot = Offset(17f, 13f)) {
        drawRoundRect(Color(0xFFE9E3FF), Offset(7f, 7f), Size(20f, 12f), CornerRadius(3f))
        drawRoundRect(Color(0xFF4A2E8A), Offset(7f, 7f), Size(20f, 12f), CornerRadius(3f), style = Stroke(2f))
        drawRoundRect(Color(0xFFB9A4FF), Offset(8f, 9f), Size(6f, 8f), CornerRadius(1.5f))
    }
}

internal fun DrawScope.drawChest() = inBox(34f) {
    val outline = Color(0xFF6A3A10)
    drawPath(chestLid, Color(0xFFC8792A))
    drawPath(chestLid, outline, style = Stroke(1.4f))
    drawRoundRect(Color(0xFFE8973A), Offset(3f, 12f), Size(28f, 15f), CornerRadius(2f))
    drawRoundRect(outline, Offset(3f, 12f), Size(28f, 15f), CornerRadius(2f), style = Stroke(1.4f))
    drawRect(Gold, Offset(3f, 11f), Size(28f, 3f))
    drawRoundRect(Color(0xFFFFE07A), Offset(14f, 10f), Size(6f, 8f), CornerRadius(1.5f))
    drawRoundRect(outline, Offset(14f, 10f), Size(6f, 8f), CornerRadius(1.5f), style = Stroke(1.2f))
}

private fun DrawScope.drawLevelBadge() = inBox(30f) {
    drawCircle(Color(0xFF5A2CB8), radius = 14f, center = Offset(15f, 15f))
    drawCircle(Gold, radius = 14f, center = Offset(15f, 15f), style = Stroke(2f))
    drawPath(badgeStar, Gold)
}

// ─────────────────────────────── Level bar ──────────────────────────────────

/** Star badge, level, XP bar and the reward chest that shakes open on each level-up. */
@Composable
internal fun ShooterLevelBar(
    level: Int,
    xp: Int,
    xpNeeded: Int,
    levelUp: DropLevelUp?,
    onChest: () -> Unit,
    modifier: Modifier = Modifier,
    /** Narrow columns (landscape): badge, level and chest on one row, the XP bar under them. */
    compact: Boolean = false
) {
    val fill by animateFloatAsState(
        targetValue = (xp.toFloat() / xpNeeded).coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "xpFill"
    )
    val chestTilt = remember { Animatable(0f) }
    val chestGlow = remember { Animatable(0f) }
    LaunchedEffect(levelUp?.key) {
        if (levelUp == null) return@LaunchedEffect
        launch {
            chestGlow.snapTo(1f)
            chestGlow.animateTo(0f, tween(1400))
        }
        repeat(2) {
            chestTilt.animateTo(0f, keyframes {
                durationMillis = 600
                -10f at 150
                10f at 450
            })
        }
    }

    val description = "Level $level, ${formatXp(xp)} of ${formatXp(xpNeeded)} XP"
    val badge: @Composable () -> Unit = { Canvas(Modifier.size(32.dp)) { drawLevelBadge() } }
    val label: @Composable () -> Unit = {
        Text("Lv. $level", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
    val bar: @Composable (Modifier) -> Unit = { m ->
        Box(
            modifier = m
                .height(18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(fill)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(GoldDeep, Color(0xFFFFD25A))))
            )
            Text(
                "${formatXp(xp)} / ${formatXp(xpNeeded)}",
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1
            )
        }
    }
    val chest: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(38.dp, 34.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onChest)
                .semantics { contentDescription = "Level reward chest" }
                .testTag("DropMerge_Chest"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                Modifier
                    .size(34.dp, 30.dp)
                    .graphicsLayer {
                        rotationZ = chestTilt.value
                        val s = 1f + kotlin.math.abs(chestTilt.value) / 100f
                        scaleX = s
                        scaleY = s
                    }
                    .drawBehind {
                        if (chestGlow.value > 0f) {
                            drawCircle(
                                Brush.radialGradient(listOf(Gold.copy(alpha = 0.75f * chestGlow.value), Color.Transparent)),
                                radius = size.maxDimension
                            )
                        }
                    }
            ) { drawChest() }
        }
    }

    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                badge()
                label()
                Spacer(Modifier.weight(1f))
                chest()
            }
            bar(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 2.dp)
                .semantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            badge()
            label()
            bar(Modifier.weight(1f))
            chest()
        }
    }
}

private fun formatXp(n: Int): String = commas(n)

// ─────────────────────────────── Tap line ───────────────────────────────────

@Composable
internal fun ShooterTapLine(removeArmed: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val star = if (removeArmed) Color(0xFFFF9FB0) else Color(0xFFFFE58A)
        Canvas(Modifier.size(14.dp)) { inBox(24f) { drawPath(sparkle, star) } }
        Spacer(Modifier.width(8.dp))
        Text(
            if (removeArmed) "TAP A SPRITE TO REMOVE IT" else "TAP A LANE TO LAUNCH",
            color = if (removeArmed) Color(0xFFFFC2CE) else Muted,
            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp, maxLines = 1
        )
        Spacer(Modifier.width(8.dp))
        Canvas(Modifier.size(14.dp)) { inBox(24f) { drawPath(sparkle, star) } }
    }
}

// ─────────────────────────────── Power-up bar ───────────────────────────────

private enum class PowerStyle(val label: String, val top: Color, val bottom: Color, val glow: Color) {
    UNDO("Undo", Color(0xFFB57BFF), Color(0xFF6B35D6), Color(0xB3B57BFF)),
    SHUFFLE("Shuffle", Color(0xFFFFB25A), Color(0xFFF2601E), Color(0xB3FF963C)),
    REMOVE("Remove", Color(0xFFFF9FB0), Color(0xFFE0337A), Color(0x99FF5FA2)),
    FREEZE("Freeze", Color(0xFF7FD8FF), Color(0xFF2E7BE0), Color(0xB36EC8FF))
}

/** Everything the bar needs to know, gathered once by the screen. */
internal data class ShooterPowerState(
    val powerUps: DropPowerUps,
    val canUndo: Boolean,
    val canShuffle: Boolean,
    val canRemove: Boolean,
    val canSwap: Boolean,
    val removeArmed: Boolean,
    val freezeShots: Int,
    val nextValue: Int,
    val enabled: Boolean
)

/**
 * Undo · Shuffle · NEXT · Remove · Freeze, five equal slots in a rounded tray.
 * [vertical] stacks NEXT over a 2×2 grid for the landscape side column.
 */
@Composable
internal fun ShooterPowerBar(
    state: ShooterPowerState,
    onUndo: () -> Unit,
    onShuffle: () -> Unit,
    onNext: () -> Unit,
    onRemove: () -> Unit,
    onFreeze: () -> Unit,
    nextTile: @Composable (Dp) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    val tray = RoundedCornerShape(24.dp)
    BoxWithConstraints(
        modifier = modifier
            .clip(tray)
            .background(Color(0x990A041E))
            .border(1.dp, PanelLine, tray)
            .padding(8.dp)
            .semantics { contentDescription = "Power-ups" }
    ) {
        val gap = 6.dp
        if (!vertical) {
            val slotW = (maxWidth - gap * 4) / 5
            val slotH = maxHeight.coerceAtLeast(64.dp)
            val sq = minOf(slotW * 0.86f, slotH * 0.68f, 60.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                val slot = Modifier.width(slotW).height(slotH)
                PowerSlot(PowerStyle.UNDO, state.powerUps.undo, state.enabled && state.canUndo, false, sq, slot.tutorialAnchor(TutorialAnchor.UNDO), onUndo) { drawUndoIcon() }
                PowerSlot(PowerStyle.SHUFFLE, state.powerUps.shuffle, state.enabled && state.canShuffle, false, sq, slot, onShuffle) { drawShuffleIcon() }
                NextSlot(state, sq, slot.tutorialAnchor(TutorialAnchor.SKIP), onNext, nextTile)
                PowerSlot(PowerStyle.REMOVE, state.powerUps.remove, (state.enabled && state.canRemove) || state.removeArmed, state.removeArmed, sq, slot, onRemove) { drawHammerIcon() }
                PowerSlot(PowerStyle.FREEZE, state.powerUps.freeze, state.enabled, state.freezeShots > 0, sq, slot, onFreeze) { drawFreezeIcon() }
            }
        } else {
            val slotW = (maxWidth - gap) / 2
            val sq = minOf(slotW * 0.8f, 56.dp)
            val slotH = sq + 26.dp
            Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
                NextSlot(state, sq, Modifier.fillMaxWidth().height(slotH).tutorialAnchor(TutorialAnchor.SKIP), onNext, nextTile)
                val slot = Modifier.width(slotW).height(slotH)
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    PowerSlot(PowerStyle.UNDO, state.powerUps.undo, state.enabled && state.canUndo, false, sq, slot.tutorialAnchor(TutorialAnchor.UNDO), onUndo) { drawUndoIcon() }
                    PowerSlot(PowerStyle.SHUFFLE, state.powerUps.shuffle, state.enabled && state.canShuffle, false, sq, slot, onShuffle) { drawShuffleIcon() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    PowerSlot(PowerStyle.REMOVE, state.powerUps.remove, (state.enabled && state.canRemove) || state.removeArmed, state.removeArmed, sq, slot, onRemove) { drawHammerIcon() }
                    PowerSlot(PowerStyle.FREEZE, state.powerUps.freeze, state.enabled, state.freezeShots > 0, sq, slot, onFreeze) { drawFreezeIcon() }
                }
            }
        }
    }
}

@Composable
private fun PowerSlot(
    style: PowerStyle,
    charges: Int,
    enabled: Boolean,
    active: Boolean,
    sq: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
    icon: DrawScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "pwPress")
    val pulse = rememberInfiniteTransition(label = "pwPulse")
    val p by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pwPulseP")
    val buy = charges <= 0
    val shape = RoundedCornerShape(sq * 0.3f)
    val description = when {
        buy -> "${style.label}, buy one for ${DropPowerUps.COST} coins"
        else -> "${style.label}, $charges left"
    }
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag("DropMerge_Power_${style.label}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(sq)
                .scale(press)
                .drawBehind {
                    // Coloured glow; it breathes while the power is armed or running.
                    val spread = if (active) 0.62f + 0.22f * p else 0.62f
                    drawCircle(
                        Brush.radialGradient(listOf(style.glow, Color.Transparent), radius = size.minDimension * (spread + 0.18f)),
                        radius = size.minDimension * (spread + 0.18f)
                    )
                }
                .clip(shape)
                .drawBehind {
                    // The design's 160° gradient, then an inset bevel: light along the top, shade along the bottom.
                    drawRect(
                        Brush.linearGradient(
                            listOf(style.top, style.bottom),
                            start = Offset(size.width * 0.32f, 0f),
                            end = Offset(size.width * 0.68f, size.height)
                        )
                    )
                    drawRect(Color.White.copy(alpha = 0.3f), size = Size(size.width, 3.dp.toPx()))
                    drawRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(0f, size.height - 5.dp.toPx()), size = Size(size.width, 5.dp.toPx()))
                }
                .border(
                    width = if (active) 3.dp else 2.dp,
                    color = Color.White.copy(alpha = if (active) 0.55f + 0.45f * p else 0.4f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(sq * 0.6f)) { icon() }
        }
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(style.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            if (buy) {
                CoinIcon(Modifier.size(11.dp))
                Text("${DropPowerUps.COST}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            } else {
                Text("($charges)", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun NextSlot(
    state: ShooterPowerState,
    sq: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
    nextTile: @Composable (Dp) -> Unit
) {
    val used = !state.canSwap || state.freezeShots > 0
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "nextPress")
    Column(
        modifier = modifier
            .scale(press)
            .drawBehind {
                if (!used) {
                    drawRoundRect(
                        Brush.radialGradient(listOf(Gold.copy(alpha = 0.35f), Color.Transparent), radius = size.maxDimension * 0.75f),
                        topLeft = Offset(-8.dp.toPx(), -8.dp.toPx()),
                        size = Size(size.width + 16.dp.toPx(), size.height + 16.dp.toPx()),
                        cornerRadius = CornerRadius(26.dp.toPx())
                    )
                }
            }
            .clip(shape)
            .background(Brush.radialGradient(listOf(Gold.copy(alpha = 0.25f), Color(0x990C0522))))
            .border(2.dp, if (used) Color.White.copy(alpha = 0.25f) else Gold, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = state.enabled, onClick = onClick)
            .semantics { contentDescription = "Swap the current sprite with the next one" }
            .testTag("DropMerge_Button_Skip"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        key(state.nextValue) {
            val appear = remember { Animatable(0.4f) }
            LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f)) }
            Box(Modifier.size(sq * 0.95f).scale(appear.value).alpha(if (used) 0.7f else 1f), contentAlignment = Alignment.Center) {
                nextTile(sq * 0.95f)
            }
        }
        Text(
            "NEXT", color = if (used) Muted else Color.White,
            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, maxLines = 1
        )
    }
}

// ─────────────────────────────── Notice ─────────────────────────────────────

/** A short message that drops in over the top of the board and fades after a couple of seconds. */
@Composable
internal fun ShooterNotice(notice: DropNotice?, modifier: Modifier = Modifier) {
    if (notice == null) return
    key(notice.key) {
        var visible by remember { mutableStateOf(true) }
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            enter.animateTo(1f, tween(200))
            delay(2_000)
            enter.animateTo(0f, tween(220))
            visible = false
        }
        if (visible) {
            Box(
                modifier = modifier
                    .graphicsLayer {
                        alpha = enter.value
                        translationY = (1f - enter.value) * -14.dp.toPx()
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF00F0628))
                    .border(1.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 9.dp)
                    .testTag("DropMerge_Notice")
            ) {
                Text(notice.text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Snowflake badge for a frozen launcher, with the shots left. */
@Composable
internal fun FrozenBadge(shots: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF7FD8FF), Color(0xFF2E7BE0))))
            .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(3.dp).alpha(0.35f)) { drawFreezeIcon() }
        Text("$shots", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}
