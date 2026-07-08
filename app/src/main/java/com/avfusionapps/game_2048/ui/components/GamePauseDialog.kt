package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun GamePauseDialog(
    currentScore: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalGameTheme.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("GamePauseDialog_Root")
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
                // Pause Icon
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
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = stringResource(id = R.string.desc_pause_button),
                        tint = theme.primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PAUSED text
                Text(
                    text = stringResource(id = R.string.paused),
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

                // Current Score
                Text(
                    text = stringResource(id = R.string.current_score),
                    fontSize = 14.sp,
                    color = theme.textColor.copy(alpha = 0.7f)
                )
                Text(
                    text = currentScore.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Resume Button
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
                                    theme.secondaryColor // Using secondaryColor instead of hardcoded hot pink
                                )
                            )
                        )
                        .testTag("GamePauseDialog_Button_Resume")
                        .clickable { onResume() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(id = R.string.desc_resume_button),
                            tint = contentColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.resume),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Restart Button
                SecondaryActionButton(
                    icon = Icons.Rounded.Refresh,
                    title = stringResource(id = R.string.restart),
                    subtitle = stringResource(id = R.string.restart_subtitle),
                    modifier = Modifier.testTag("GamePauseDialog_Button_Restart"),
                    onClick = onRestart
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Home Button
                SecondaryActionButton(
                    icon = Icons.Rounded.Home,
                    title = stringResource(id = R.string.home),
                    subtitle = stringResource(id = R.string.home_subtitle),
                    modifier = Modifier.testTag("GamePauseDialog_Button_Home"),
                    onClick = onQuit
                )
            }
        }
    }
}

@Composable
private fun SecondaryActionButton(
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
