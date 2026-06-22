package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.drawNeonGlow
import android.media.MediaPlayer
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LevelUnlockDialog(
    unlockedTileValue: Int,
    soundEnabled: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val mediaPlayer = remember { if (isPreview) null else MediaPlayer() }
    val assetFileName = "next_level_sound.mp3"

    // ── Staggered element visibility triggers ──
    var showCrown by remember { mutableStateOf(false) }
    var showCongrats by remember { mutableStateOf(false) }
    var showMilestone by remember { mutableStateOf(false) }
    var showTile by remember { mutableStateOf(false) }
    var showRibbon by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(false) }
    var showNextTarget by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    // ── Tile bounce-in scale (with spring overshoot) ──
    val tileScale = remember { Animatable(0f) }

    // ── Staggered reveal sequence ──
    LaunchedEffect(Unit) {
        // Play sound
        if (soundEnabled && mediaPlayer != null) {
            try {
                val afd = context.assets.openFd(assetFileName)
                mediaPlayer.reset()
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (e: Exception) {
                // handle error if needed
            }
        }

        // Staggered reveal timing
        showCrown = true
        delay(120)
        showCongrats = true
        delay(100)
        showMilestone = true
        delay(180)
        showTile = true
        // Launch tile bounce animation
        tileScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // These trigger during the spring animation settling
    }

    // Separate effect for elements after tile (so they don't wait for spring to finish)
    LaunchedEffect(showTile) {
        if (showTile) {
            delay(300)
            showRibbon = true
            delay(150)
            showDescription = true
            delay(150)
            showNextTarget = true
            delay(200)
            showButton = true
        }
    }

    // ── Continuous ambient animations ──
    val infiniteTransition = rememberInfiniteTransition(label = "ambientAnims")

    // Slow ray rotation
    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rayRotation"
    )

    // Glow pulse (breathing effect)
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Star twinkle
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.stop()
                    }
                } catch (e: Exception) {
                    // ignore
                }
                it.release()
            }
        }
    }

    val theme = LocalGameTheme.current
    val primary = theme.primaryColor
    val secondary = theme.secondaryColor
    val textSecondary = theme.textColor.copy(alpha = 0.6f)
    // Derive golden/amber from theme tile colors for warm accents
    val gold = theme.tileColors[256] ?: Color(0xFFFFD54F)
    val amber = theme.tileColors[128] ?: Color(0xFFFFA000)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LevelCelebrationEffect(
            isVisible = true,
            level = unlockedTileValue
        )

        Dialog(onDismissRequest = { /* Prevent dismissal by clicking outside */ }) {
            NeonCard(
                accentColor = primary,
                isSelected = true,
                onClick = null,
                cornerRadius = 28.dp,
                modifier = Modifier
                    .testTag("LevelUnlockDialog_Root")
                    .fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.backgroundColor.copy(alpha = 0.96f))
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ─── Crown Header (fade + slide down) ───
                    StaggeredSlideIn(visible = showCrown, delayMs = 0, fromTop = true) {
                        CrownHeader(crownColor = amber)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ─── ★ CONGRATULATIONS! ★ (fade in) ───
                    StaggeredSlideIn(visible = showCongrats, delayMs = 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "★",
                                color = primary.copy(alpha = starAlpha),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONGRATULATIONS!",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "★",
                                color = primary.copy(alpha = starAlpha),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ─── NEW MILESTONE (scale up + fade) ───
                    StaggeredSlideIn(visible = showMilestone, delayMs = 0) {
                        Text(
                            text = "NEW MILESTONE",
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    listOf(gold, amber, primary)
                                ),
                                shadow = Shadow(
                                    color = primary.copy(alpha = 0.5f),
                                    blurRadius = 16f
                                )
                            ),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── Hero Tile with animated multi-layer neon glow ───
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Continuously rotating radiating light rays
                        if (showTile) {
                            Canvas(modifier = Modifier
                                .fillMaxSize()
                                .alpha(tileScale.value.coerceIn(0f, 1f))
                            ) {
                                rotate(degrees = rayRotation) {
                                    val w = size.width
                                    val h = size.height
                                    val center = Offset(w / 2f, h / 2f)
                                    val numRays = 24
                                    for (i in 0 until numRays) {
                                        val angle =
                                            Math.toRadians((i * (360.0 / numRays)))
                                        val rayLen = w / 2f
                                        val endX =
                                            center.x + cos(angle).toFloat() * rayLen
                                        val endY =
                                            center.y + sin(angle).toFloat() * rayLen
                                        drawLine(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    primary.copy(alpha = 0.6f),
                                                    primary.copy(alpha = 0.15f),
                                                    Color.Transparent
                                                ),
                                                start = center,
                                                end = Offset(endX, endY)
                                            ),
                                            start = center,
                                            end = Offset(endX, endY),
                                            strokeWidth = 2.5f
                                        )
                                    }
                                }
                            }
                        }

                        // Multi-layer neon glow + tile (bounce in with pulsing glow)
                        val tileSize = 130.dp
                        val tileCorner = 22.dp
                        Box(
                            modifier = Modifier
                                .size(tileSize)
                                .scale(tileScale.value)
                                .drawBehind {
                                    if (tileScale.value > 0.01f) {
                                        val s = size
                                        val cr = tileCorner.toPx()
                                        val pulse = glowPulse

                                        // Layer 4: Outermost faint purple haze (pulsing)
                                        drawRoundRect(
                                            color = secondary.copy(alpha = 0.12f * pulse),
                                            topLeft = Offset(
                                                -32.dp.toPx(),
                                                -32.dp.toPx()
                                            ),
                                            size = Size(
                                                s.width + 64.dp.toPx(),
                                                s.height + 64.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(cr + 32.dp.toPx())
                                        )

                                        // Layer 3: Outer pink glow (pulsing)
                                        drawRoundRect(
                                            color = primary.copy(alpha = 0.20f * pulse),
                                            topLeft = Offset(
                                                -22.dp.toPx(),
                                                -22.dp.toPx()
                                            ),
                                            size = Size(
                                                s.width + 44.dp.toPx(),
                                                s.height + 44.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(cr + 22.dp.toPx())
                                        )

                                        // Layer 2: Mid pink glow ring (pulsing)
                                        drawRoundRect(
                                            color = primary.copy(alpha = 0.35f * pulse),
                                            topLeft = Offset(
                                                -14.dp.toPx(),
                                                -14.dp.toPx()
                                            ),
                                            size = Size(
                                                s.width + 28.dp.toPx(),
                                                s.height + 28.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(cr + 14.dp.toPx())
                                        )

                                        // Layer 1: Inner warm amber/orange border glow
                                        drawRoundRect(
                                            color = amber.copy(alpha = 0.45f),
                                            topLeft = Offset(
                                                -7.dp.toPx(),
                                                -7.dp.toPx()
                                            ),
                                            size = Size(
                                                s.width + 14.dp.toPx(),
                                                s.height + 14.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(cr + 7.dp.toPx())
                                        )

                                        // Inner glow stroke border (orange → pink gradient)
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    amber.copy(alpha = 0.9f),
                                                    primary.copy(alpha = 0.8f)
                                                )
                                            ),
                                            topLeft = Offset(
                                                -3.dp.toPx(),
                                                -3.dp.toPx()
                                            ),
                                            size = Size(
                                                s.width + 6.dp.toPx(),
                                                s.height + 6.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(cr + 3.dp.toPx()),
                                            style = Stroke(width = 2.5.dp.toPx())
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            TileView(
                                value = unlockedTileValue,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(tileCorner))
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.4f),
                                                amber.copy(alpha = 0.3f),
                                                primary.copy(alpha = 0.5f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(tileCorner)
                                    ),
                                fontSizeMultiplier = 2.4f
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ─── Ribbon Badge: YOU MADE 2048! (scale-X expand) ───
                    StaggeredSlideIn(visible = showRibbon, delayMs = 0, scaleEffect = true) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(50.dp)
                                .drawBehind {
                                    val w = size.width
                                    val h = size.height
                                    val arrowIn = 16.dp.toPx()

                                    val path = Path().apply {
                                        moveTo(arrowIn, 0f)
                                        lineTo(w - arrowIn, 0f)
                                        lineTo(w, h / 2f)
                                        lineTo(w - arrowIn, h)
                                        lineTo(arrowIn, h)
                                        lineTo(0f, h / 2f)
                                        close()
                                    }

                                    // Fill background
                                    drawPath(
                                        path = path,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                secondary.copy(alpha = 0.4f),
                                                secondary.copy(alpha = 0.15f)
                                            )
                                        )
                                    )

                                    // Draw gradient neon border (primary → secondary)
                                    drawPath(
                                        path = path,
                                        brush = Brush.horizontalGradient(
                                            listOf(primary, secondary, primary)
                                        ),
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            join = StrokeJoin.Round
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "≡",
                                    color = primary.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "YOU MADE",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$unlockedTileValue!",
                                    color = gold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = gold.copy(alpha = 0.8f),
                                            blurRadius = 10f
                                        )
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "≡",
                                    color = primary.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ─── Muted Description Text (fade in) ───
                    StaggeredSlideIn(visible = showDescription, delayMs = 0) {
                        Text(
                            text = "The legendary tile has been reached.\nKeep merging to beat your high score!",
                            color = textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ─── Next Target Card (slide in from right) ───
                    StaggeredSlideIn(
                        visible = showNextTarget,
                        delayMs = 0,
                        fromRight = true
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            secondary.copy(alpha = 0.12f),
                                            theme.backgroundColor.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    Brush.horizontalGradient(
                                        listOf(
                                            primary.copy(alpha = 0.3f),
                                            secondary.copy(alpha = 0.3f)
                                        )
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TargetIconCanvas(tint = primary)

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "NEXT TARGET",
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(1.dp)
                                    .background(primary.copy(alpha = 0.25f))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "${unlockedTileValue * 2}",
                                color = primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = primary.copy(alpha = 0.6f),
                                        blurRadius = 10f
                                    )
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ─── Continue Playing Button (slide up from bottom) ───
                    StaggeredSlideIn(
                        visible = showButton,
                        delayMs = 0,
                        fromBottom = true
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .testTag("LevelUnlockDialog_Button_Continue")
                                .fillMaxWidth(0.9f)
                                .height(54.dp)
                                .drawBehind {
                                    // Neon glow behind the button (pulsing)
                                    drawRoundRect(
                                        color = primary.copy(alpha = 0.25f * glowPulse),
                                        topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()),
                                        size = Size(
                                            size.width + 12.dp.toPx(),
                                            size.height + 12.dp.toPx()
                                        ),
                                        cornerRadius = CornerRadius(33.dp.toPx())
                                    )
                                    drawRoundRect(
                                        color = primary.copy(alpha = 0.12f * glowPulse),
                                        topLeft = Offset(
                                            -12.dp.toPx(),
                                            -12.dp.toPx()
                                        ),
                                        size = Size(
                                            size.width + 24.dp.toPx(),
                                            size.height + 24.dp.toPx()
                                        ),
                                        cornerRadius = CornerRadius(39.dp.toPx())
                                    )
                                },
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(primary, secondary)
                                        ),
                                        shape = RoundedCornerShape(27.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CONTINUE PLAYING",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        style = TextStyle(
                                            shadow = Shadow(
                                                color = Color.White.copy(alpha = 0.5f),
                                                blurRadius = 8f
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Reusable staggered slide-in animation wrapper ───

@Composable
private fun StaggeredSlideIn(
    visible: Boolean,
    delayMs: Int = 0,
    fromTop: Boolean = false,
    fromBottom: Boolean = false,
    fromRight: Boolean = false,
    scaleEffect: Boolean = false,
    content: @Composable () -> Unit
) {
    val targetAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        ),
        label = "staggerAlpha"
    )

    val slideOffset = when {
        fromTop -> -20
        fromBottom -> 20
        fromRight -> 0 // horizontal slide handled separately
        else -> 0
    }

    val targetOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else slideOffset.toFloat(),
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        ),
        label = "staggerOffsetY"
    )

    val targetOffsetX by animateFloatAsState(
        targetValue = if (visible) 0f else if (fromRight) 40f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        ),
        label = "staggerOffsetX"
    )

    val targetScale by animateFloatAsState(
        targetValue = if (visible) 1f else if (scaleEffect) 0.3f else 1f,
        animationSpec = if (scaleEffect) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "staggerScale"
    )

    Box(
        modifier = Modifier
            .alpha(targetAlpha)
            .offset { IntOffset(targetOffsetX.roundToInt(), targetOffsetY.roundToInt()) }
            .scale(targetScale)
    ) {
        content()
    }
}

// ─── Crown Header ───

@Composable
private fun CrownHeader(crownColor: Color = Color(0xFFFFB300)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Left horizontal lines
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, crownColor)
                        )
                    )
            )
            Spacer(modifier = Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(2.dp)
                    .background(crownColor)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Crown Canvas
        Canvas(modifier = Modifier.size(width = 38.dp, height = 24.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(0f, h * 0.9f)
                lineTo(w, h * 0.9f)
                lineTo(w * 0.92f, h * 0.35f)
                lineTo(w * 0.72f, h * 0.65f)
                lineTo(w * 0.5f, h * 0.15f)
                lineTo(w * 0.28f, h * 0.65f)
                lineTo(w * 0.08f, h * 0.35f)
                close()
            }
            drawPath(
                path = path,
                color = crownColor,
                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
            )

            // Draw tiny circles at tips
            drawCircle(
                color = crownColor,
                radius = 2.5.dp.toPx(),
                center = Offset(w * 0.08f, h * 0.35f)
            )
            drawCircle(
                color = crownColor,
                radius = 2.5.dp.toPx(),
                center = Offset(w * 0.5f, h * 0.15f)
            )
            drawCircle(
                color = crownColor,
                radius = 2.5.dp.toPx(),
                center = Offset(w * 0.92f, h * 0.35f)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right horizontal lines
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(2.dp)
                    .background(crownColor)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(crownColor, Color.Transparent)
                        )
                    )
            )
        }
    }
}

// ─── Target Icon ───

@Composable
private fun TargetIconCanvas(tint: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height

        // Draw target circles
        drawCircle(
            color = tint,
            radius = w * 0.4f,
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawCircle(
            color = tint,
            radius = w * 0.22f,
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawCircle(color = tint, radius = w * 0.08f)

        // Draw diagonal arrow cutting from top-right to center
        drawLine(
            color = tint,
            start = Offset(w * 0.82f, h * 0.18f),
            end = Offset(w * 0.38f, h * 0.62f),
            strokeWidth = 1.8.dp.toPx()
        )

        // Draw arrow tip at top-right
        drawPath(
            path = Path().apply {
                moveTo(w * 0.62f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.38f)
            },
            color = tint,
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )
    }
}

private fun getTileTargetForLevel(level: Int): String {
    return "${level * 2}"
}

@Composable
fun AnimatedLevelUnlockDialog(
    unlockedTileValue: Int?,
    soundEnabled: Boolean = true,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = unlockedTileValue != null,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        if (unlockedTileValue != null) {
            LevelUnlockDialog(
                unlockedTileValue = unlockedTileValue,
                soundEnabled = soundEnabled,
                onDismiss = onDismiss
            )
        }
    }
}

// ─── Previews: All Themes ───

@Preview(showBackground = true, name = "Neon Pink — 2048")
@Composable
private fun PreviewNeonPink2048() {
    _2048OriginalTheme(theme = GameTheme.NeonPink) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Neon Pink — 128")
@Composable
private fun PreviewNeonPink128() {
    _2048OriginalTheme(theme = GameTheme.NeonPink) {
        LevelUnlockDialog(unlockedTileValue = 128, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Cyber Blue — 2048")
@Composable
private fun PreviewCyberBlue2048() {
    _2048OriginalTheme(theme = GameTheme.CyberBlue) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Cyber Blue — 512")
@Composable
private fun PreviewCyberBlue512() {
    _2048OriginalTheme(theme = GameTheme.CyberBlue) {
        LevelUnlockDialog(unlockedTileValue = 512, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Emerald — 2048")
@Composable
private fun PreviewEmerald2048() {
    _2048OriginalTheme(theme = GameTheme.Emerald) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Emerald — 256")
@Composable
private fun PreviewEmerald256() {
    _2048OriginalTheme(theme = GameTheme.Emerald) {
        LevelUnlockDialog(unlockedTileValue = 256, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Sunset — 2048")
@Composable
private fun PreviewSunset2048() {
    _2048OriginalTheme(theme = GameTheme.Sunset) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Sunset — 1024")
@Composable
private fun PreviewSunset1024() {
    _2048OriginalTheme(theme = GameTheme.Sunset) {
        LevelUnlockDialog(unlockedTileValue = 1024, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Royal Purple — 2048")
@Composable
private fun PreviewRoyalPurple2048() {
    _2048OriginalTheme(theme = GameTheme.RoyalPurple) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Royal Purple — 512")
@Composable
private fun PreviewRoyalPurple512() {
    _2048OriginalTheme(theme = GameTheme.RoyalPurple) {
        LevelUnlockDialog(unlockedTileValue = 512, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Minimal White — 2048")
@Composable
private fun PreviewMinimalWhite2048() {
    _2048OriginalTheme(theme = GameTheme.MinimalWhite) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Minimal White — 256")
@Composable
private fun PreviewMinimalWhite256() {
    _2048OriginalTheme(theme = GameTheme.MinimalWhite) {
        LevelUnlockDialog(unlockedTileValue = 256, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "AMOLED Black — 2048")
@Composable
private fun PreviewAmoledBlack2048() {
    _2048OriginalTheme(theme = GameTheme.AmoledBlack) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "AMOLED Black — 1024")
@Composable
private fun PreviewAmoledBlack1024() {
    _2048OriginalTheme(theme = GameTheme.AmoledBlack) {
        LevelUnlockDialog(unlockedTileValue = 1024, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Ocean Teal — 2048")
@Composable
private fun PreviewOceanTeal2048() {
    _2048OriginalTheme(theme = GameTheme.OceanTeal) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Ocean Teal — 512")
@Composable
private fun PreviewOceanTeal512() {
    _2048OriginalTheme(theme = GameTheme.OceanTeal) {
        LevelUnlockDialog(unlockedTileValue = 512, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Golden Hour — 2048")
@Composable
private fun PreviewGoldenHour2048() {
    _2048OriginalTheme(theme = GameTheme.GoldenHour) {
        LevelUnlockDialog(unlockedTileValue = 2048, soundEnabled = false, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Golden Hour — 128")
@Composable
private fun PreviewGoldenHour128() {
    _2048OriginalTheme(theme = GameTheme.GoldenHour) {
        LevelUnlockDialog(unlockedTileValue = 128, soundEnabled = false, onDismiss = {})
    }
}
