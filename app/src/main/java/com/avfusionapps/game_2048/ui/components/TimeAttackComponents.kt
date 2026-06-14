package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

data class FloatingBonus(
    val id: Long,
    val text: String
)

@Composable
fun FloatingBonusText(
    text: String,
    onAnimationFinished: () -> Unit
) {
    val theme = LocalGameTheme.current
    val animatableY = remember { Animatable(40f) }
    val animatableAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.joinAll(
            launch {
                animatableY.animateTo(
                    targetValue = -50f,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutLinearInEasing)
                )
            },
            launch {
                animatableAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutLinearInEasing)
                )
            }
        )
        onAnimationFinished()
    }

    Text(
        text = text,
        color = theme.accentColor,
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(y = animatableY.value.dp)
            .graphicsLayer(alpha = animatableAlpha.value)
    )
}

@Composable
fun TimeAttackTopBar(
    timeRemainingMillis: Long,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onHelpClick: () -> Unit,
    onBack: () -> Unit,
    floatingBonuses: List<FloatingBonus> = emptyList(),
    onBonusAnimationFinished: (Long) -> Unit = {}
) {
    val theme = LocalGameTheme.current
    val minutes = (timeRemainingMillis / 60000).toInt()
    val seconds = ((timeRemainingMillis % 60000) / 1000).toInt()
    val millis = ((timeRemainingMillis % 1000) / 10).toInt()

    val timerColor by animateColorAsState(
        targetValue = when {
            timeRemainingMillis < 10_000L -> Color(0xFFFF3378) // Red/Pink from theme
            timeRemainingMillis < 30_000L -> Color(0xFFFFD700) // Yellow
            else -> Color(0xFF00FF66) // Green
        },
        label = "timerColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surfaceColor)
                .border(1.dp, theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .testTag("TimeAttackTopBar_Button_Back")
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(id = R.string.desc_back_button),
                tint = theme.textColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Timer
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = timerColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = String.format("%02d:%02d.%02d", minutes, seconds, millis),
                    color = timerColor,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = stringResource(id = R.string.time_left),
                    color = theme.textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            floatingBonuses.forEach { bonus ->
                key(bonus.id) {
                    FloatingBonusText(
                        text = bonus.text,
                        onAnimationFinished = { onBonusAnimationFinished(bonus.id) }
                    )
                }
            }
        }

        // Action Row (Help & Pause)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Help Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.surfaceColor)
                    .border(1.dp, theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .testTag("TimeAttackTopBar_Button_Help")
                    .clickable { onHelpClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = "Help",
                    tint = theme.textColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Pause Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.surfaceColor)
                    .border(1.dp, theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .testTag("TimeAttackTopBar_Button_Pause")
                    .clickable { onPauseToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = stringResource(id = R.string.desc_pause_button),
                    tint = theme.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun GameScoreBoard(
    score: Int,
    highScore: Int,
    scoreIcon: ImageVector = Icons.Rounded.Star,
    scoreTint: Color? = null,
    highScoreIcon: ImageVector = Icons.Rounded.EmojiEvents,
    highScoreTint: Color? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val actualScoreTint = scoreTint ?: theme.primaryColor
    val actualHighScoreTint = highScoreTint ?: theme.secondaryColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Score Card
        NeonCard(
            accentColor = actualScoreTint,
            isSelected = true,
            onClick = null,
            cornerRadius = 12.dp,
            borderWidth = 1.dp,
            modifier = Modifier.weight(1f).testTag("GameScoreBoard_Card_Score")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(actualScoreTint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = scoreIcon,
                        contentDescription = stringResource(id = R.string.desc_score_icon),
                        tint = actualScoreTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.score),
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = score.toString(),
                        color = theme.textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Best Score Card
        NeonCard(
            accentColor = actualHighScoreTint,
            isSelected = true,
            onClick = null,
            cornerRadius = 12.dp,
            borderWidth = 1.dp,
            modifier = Modifier.weight(1f).testTag("GameScoreBoard_Card_BestScore")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(actualHighScoreTint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = highScoreIcon,
                        contentDescription = stringResource(id = R.string.desc_high_score_icon),
                        tint = actualHighScoreTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.best_score),
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = highScore.toString(),
                        color = theme.textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GameSwipeIndicator() {
    val theme = LocalGameTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "<<<",
            color = theme.primaryColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.swipe_to_move),
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = ">>>",
            color = theme.secondaryColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun TimeAttackBottomBar(
    onUndoClick: () -> Unit,
    onNewGameClick: () -> Unit,
    onHintClick: () -> Unit
) {
    val theme = LocalGameTheme.current
    NeonCard(
        accentColor = theme.primaryColor,
        isSelected = true,
        onClick = null,
        cornerRadius = 16.dp,
        borderWidth = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarAction(
                icon = Icons.AutoMirrored.Rounded.Undo,
                label = stringResource(id = R.string.undo),
                badge = "3",
                tint = theme.primaryColor,
                onClick = onUndoClick,
                modifier = Modifier.testTag("TimeAttackBottomBar_Button_Undo")
            )
            
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.textColor.copy(alpha = 0.1f)))

            BottomBarAction(
                icon = Icons.Rounded.Refresh,
                label = stringResource(id = R.string.new_game),
                badge = null,
                tint = theme.primaryColor, // Pinkish like the image
                onClick = onNewGameClick,
                modifier = Modifier.testTag("TimeAttackBottomBar_Button_NewGame")
            )

            Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.textColor.copy(alpha = 0.1f)))

            BottomBarAction(
                icon = Icons.Rounded.Lightbulb,
                label = stringResource(id = R.string.hint),
                badge = "5",
                tint = theme.secondaryColor, // Purpleish like the image
                onClick = onHintClick,
                modifier = Modifier.testTag("TimeAttackBottomBar_Button_Hint")
            )
        }
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    badge: String?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = theme.textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        if (badge != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    color = tint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
