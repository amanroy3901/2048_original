package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun GameOverDialog(
    score: Int,
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    val theme = LocalGameTheme.current

    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("GameOverDialog_Root")
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surfaceColor)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            theme.primaryColor.copy(alpha = 0.8f),
                            theme.primaryColor.copy(alpha = 0.2f),
                            theme.secondaryColor.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    theme.primaryColor.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = theme.primaryColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = theme.primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // GAME OVER text
                Text(
                    text = "GAME OVER",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                theme.primaryColor,
                                theme.secondaryColor
                            )
                        )
                    ),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Small dot/divider
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Final Score
                Text(
                    text = "FINAL SCORE",
                    fontSize = 14.sp,
                    color = theme.textColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = score.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Play Again Button
                val isLight = ((theme.primaryColor.red * 0.2126f + theme.primaryColor.green * 0.7152f + theme.primaryColor.blue * 0.0722f) +
                               (theme.secondaryColor.red * 0.2126f + theme.secondaryColor.green * 0.7152f + theme.secondaryColor.blue * 0.0722f)) / 2f > 0.5f
                val contentColor = if (isLight) Color(0xFF1F1F1F) else Color.White

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    theme.primaryColor,
                                    theme.secondaryColor
                                )
                            )
                        )
                        .testTag("GameOverDialog_Button_PlayAgain")
                        .clickable { onNewGame() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play Again",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exit Button
                SecondaryActionExitButton(
                    icon = Icons.Rounded.Home,
                    title = "Exit",
                    subtitle = "Return to menu",
                    modifier = Modifier.testTag("GameOverDialog_Button_Exit"),
                    onClick = onExit
                )
            }
        }
    }
}

@Composable
fun TimeAttackGameOverDialog(
    finalScore: Int,
    timeSurvived: Long,
    isTimeUp: Boolean = true,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val theme = LocalGameTheme.current
    val minutes = (timeSurvived / 60000).toInt()
    val seconds = ((timeSurvived % 60000) / 1000).toInt()

    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("TimeAttackGameOverDialog_Root")
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surfaceColor)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            theme.primaryColor.copy(alpha = 0.8f),
                            theme.primaryColor.copy(alpha = 0.2f),
                            theme.secondaryColor.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timer / Trophy Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    theme.primaryColor.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = theme.primaryColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTimeUp) Icons.Rounded.Timer else Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = theme.primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title text
                Text(
                    text = if (isTimeUp) "TIME'S UP!" else "NO MOVES LEFT!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                theme.primaryColor,
                                theme.secondaryColor
                            )
                        )
                    ),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Small dot/divider
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Score and Time details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FINAL SCORE",
                            fontSize = 11.sp,
                            color = theme.textColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = finalScore.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(theme.textColor.copy(alpha = 0.1f))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SURVIVED",
                            fontSize = 11.sp,
                            color = theme.textColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.secondaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Play Again Button
                val isLight = ((theme.primaryColor.red * 0.2126f + theme.primaryColor.green * 0.7152f + theme.primaryColor.blue * 0.0722f) +
                               (theme.secondaryColor.red * 0.2126f + theme.secondaryColor.green * 0.7152f + theme.secondaryColor.blue * 0.0722f)) / 2f > 0.5f
                val contentColor = if (isLight) Color(0xFF1F1F1F) else Color.White

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    theme.primaryColor,
                                    theme.secondaryColor
                                )
                            )
                        )
                        .testTag("TimeAttackGameOverDialog_Button_PlayAgain")
                        .clickable { onPlayAgain() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play Again",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exit Button
                SecondaryActionExitButton(
                    icon = Icons.Rounded.Home,
                    title = "Exit",
                    subtitle = "Return to menu",
                    modifier = Modifier.testTag("TimeAttackGameOverDialog_Button_Exit"),
                    onClick = onExit
                )
            }
        }
    }
}

@Composable
private fun SecondaryActionExitButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.backgroundColor.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = theme.primaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = theme.secondaryColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textColor
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = theme.textColor.copy(alpha = 0.6f)
            )
        }
    }
}
