package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

// Steps for Classic 2048
data class TutorialStep(
    val titleResId: Int,
    val descriptionResId: Int,
    val graphic: @Composable BoxScope.() -> Unit
)

@Composable
fun GameTutorialOverlay(
    isTimeAttack: Boolean,
    onDismiss: () -> Unit
) {
    val theme = LocalGameTheme.current
    var currentStepIndex by remember { mutableStateOf(0) }

    // Define steps
    val steps = remember(isTimeAttack) {
        if (isTimeAttack) {
            listOf(
                TutorialStep(
                    titleResId = com.avfusionapps.game_2048.R.string.tutorial_ta_title_1,
                    descriptionResId = com.avfusionapps.game_2048.R.string.tutorial_ta_desc_1,
                    graphic = { TimeAttackTimerGraphic() }
                ),
                TutorialStep(
                    titleResId = com.avfusionapps.game_2048.R.string.tutorial_ta_title_2,
                    descriptionResId = com.avfusionapps.game_2048.R.string.tutorial_ta_desc_2,
                    graphic = { TimeAttackBonusGraphic() }
                ),
                TutorialStep(
                    titleResId = com.avfusionapps.game_2048.R.string.tutorial_ta_title_3,
                    descriptionResId = com.avfusionapps.game_2048.R.string.tutorial_ta_desc_3,
                    graphic = { TimeAttackMultiplierGraphic() }
                ),
                TutorialStep(
                    titleResId = com.avfusionapps.game_2048.R.string.tutorial_ta_title_4,
                    descriptionResId = com.avfusionapps.game_2048.R.string.tutorial_ta_desc_4,
                    graphic = { TimeAttackControlsGraphic() }
                )
            )
        } else {
            listOf(
                TutorialStep(
                    titleResId = com.avfusionapps.game_2048.R.string.tutorial_classic_title_1,
                    descriptionResId = com.avfusionapps.game_2048.R.string.tutorial_classic_desc_1,
                    graphic = { ClassicMergeGraphic() }
                ),
                TutorialStep(
                    titleResId = com.avfusionapps.game_2048.R.string.tutorial_classic_title_2,
                    descriptionResId = com.avfusionapps.game_2048.R.string.tutorial_classic_desc_2,
                    graphic = { ClassicGoalGraphic() }
                ),
                TutorialStep(
                    titleResId = com.avfusionapps.game_2048.R.string.tutorial_classic_title_3,
                    descriptionResId = com.avfusionapps.game_2048.R.string.tutorial_classic_desc_3,
                    graphic = { ClassicControlsGraphic() }
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = false) {} // block click propagation
            .testTag("TutorialOverlay_Root"),
        contentAlignment = Alignment.Center
    ) {
        // Skip button in top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .clickable { onDismiss() }
                .testTag("Tutorial_Button_Skip")
                .padding(8.dp)
        ) {
            Text(
                text = stringResource(id = com.avfusionapps.game_2048.R.string.tutorial_skip),
                color = theme.textColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Main Tutorial Card
        NeonCard(
            accentColor = theme.accentColor,
            isSelected = true,
            onClick = null,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp)
                .testTag("Tutorial_Card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Graphic/Illustration Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.surfaceColor.copy(alpha = 0.5f))
                        .border(1.dp, theme.textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    steps[currentStepIndex].graphic(this)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = stringResource(id = steps[currentStepIndex].titleResId),
                    color = theme.textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("Tutorial_Text_Title")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = stringResource(id = steps[currentStepIndex].descriptionResId),
                    color = theme.textColor.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .height(88.dp)
                        .testTag("Tutorial_Text_Description")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Indicators and Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Progress Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        steps.forEachIndexed { index, _ ->
                            val isSelected = index == currentStepIndex
                            val dotColor = if (isSelected) theme.primaryColor else theme.textColor.copy(alpha = 0.2f)
                            val dotScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.2f else 1.0f,
                                label = "dotScale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(dotScale)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .testTag("Tutorial_Indicator_Step_$index")
                            )
                        }
                    }

                    // Next / Got It Button
                    val isLastStep = currentStepIndex == steps.size - 1
                    NeonRoundedButton(
                        text = stringResource(id = if (isLastStep) com.avfusionapps.game_2048.R.string.tutorial_got_it else com.avfusionapps.game_2048.R.string.tutorial_next),
                        onClick = {
                            if (isLastStep) {
                                onDismiss()
                            } else {
                                currentStepIndex++
                            }
                        },
                        buttonColor = theme.primaryColor,
                        glowColor = theme.primaryColor.copy(alpha = 0.8f),
                        cornerRadius = 8.dp,
                        glowRadius = 8.dp,
                        modifier = Modifier.testTag(if (isLastStep) "Tutorial_Button_GotIt" else "Tutorial_Button_Next")
                    )
                }
            }
        }
    }
}

// Graphic Composables for Classic mode
@Composable
private fun BoxScope.ClassicMergeGraphic() {
    val theme = LocalGameTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "mergeAnimation")
    
    val slideOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slideOffset"
    )

    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.0f at 0
                1.0f at 600
                1.2f at 800
                1.0f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scaleFactor"
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tile 1
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.tileColors[2] ?: theme.surfaceColor)
                .border(1.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Moving Arrow/Merge indicator
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = theme.primaryColor,
            modifier = Modifier
                .size(24.dp)
                .offset(x = slideOffset.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Tile 2
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.tileColors[2] ?: theme.surfaceColor)
                .border(1.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text("=", color = theme.textColor.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.width(16.dp))

        // Merged Tile
        Box(
            modifier = Modifier
                .size(50.dp)
                .scale(scaleFactor)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.tileColors[4] ?: theme.surfaceColor)
                .border(1.5.dp, theme.accentColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("4", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun BoxScope.ClassicGoalGraphic() {
    val theme = LocalGameTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scaleFactor)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(theme.tileColors[2048] ?: theme.primaryColor, theme.secondaryColor)
                )
            )
            .border(2.dp, Color.White, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("2048", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
    }
}

@Composable
private fun BoxScope.ClassicControlsGraphic() {
    val theme = LocalGameTheme.current
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlIconWithLabel(icon = Icons.AutoMirrored.Rounded.Undo, label = "Undo", tint = theme.primaryColor)
        ControlIconWithLabel(icon = Icons.Rounded.Lightbulb, label = "Hint", tint = theme.secondaryColor)
        ControlIconWithLabel(icon = Icons.Rounded.Refresh, label = "Restart", tint = theme.accentColor)
    }
}

@Composable
private fun ControlIconWithLabel(icon: ImageVector, label: String, tint: Color) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surfaceColor)
                .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = theme.textColor.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// Graphic Composables for Time Attack mode
@Composable
private fun BoxScope.TimeAttackTimerGraphic() {
    val theme = LocalGameTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Timer,
            contentDescription = null,
            tint = theme.primaryColor,
            modifier = Modifier
                .size(48.dp)
                .scale(scaleFactor)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "00:48.25",
            color = theme.primaryColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun BoxScope.TimeAttackBonusGraphic() {
    val theme = LocalGameTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "bonusAnimation")
    
    val textYOffset by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Tile merge representation in background
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.tileColors[128] ?: theme.primaryColor)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("128", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = theme.textColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.tileColors[128] ?: theme.primaryColor)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("128", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Rising float text
        Text(
            text = "+15s Time Bonus!",
            color = Color(0xFF00FF66), // Green glow
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            modifier = Modifier
                .offset(y = textYOffset.dp)
                .scale(1.1f)
                .background(Color.Black.copy(alpha = 0.6f * textAlpha), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun BoxScope.TimeAttackMultiplierGraphic() {
    val theme = LocalGameTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "multAnimation")
    
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scaleFactor)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(theme.primaryColor, theme.accentColor)
                    )
                )
                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "x3.5 MULTIPLIER",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Speed up merges to lock high multiplier!",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BoxScope.TimeAttackControlsGraphic() {
    val theme = LocalGameTheme.current
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlIconWithLabel(icon = Icons.Rounded.Pause, label = "Pause", tint = theme.primaryColor)
        ControlIconWithLabel(icon = Icons.AutoMirrored.Rounded.HelpOutline, label = "Rules", tint = theme.textColor)
        ControlIconWithLabel(icon = Icons.AutoMirrored.Rounded.Undo, label = "Undo", tint = theme.secondaryColor)
    }
}
