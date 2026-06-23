package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun StartJourneyCard(
    onNewGameClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    val theme = LocalGameTheme.current
    val primary = accentColor ?: theme.primaryColor
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

    val previewGrid = listOf(
        listOf(2, 4, 8, 16),
        listOf(4, 8, 16, 32),
        listOf(8, 16, 32, 64),
        listOf(16, 32, 64, 128)
    )

    NeonCard(
        accentColor = primary,
        isSelected = true,
        onClick = null,
        cornerRadius = 20.dp,
        borderWidth = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardH = maxHeight
            val cardW = maxWidth
            // Derive sizes as fractions of the card height or width
            val hPad          = cardW * 0.045f   // ~16dp
            val vPad          = cardH * 0.07f    // ~12dp
            val iconSizeDp    = cardH * 0.22f
            val btnHeightDp   = cardH * 0.19f
            val spacerSmall   = cardH * 0.03f
            val spacerMedium  = cardH * 0.05f
            val spacerMediumW = cardW * 0.045f
            val titleFontSize = (cardH * 0.06f).value.sp
            val headFontSize  = (cardH * 0.09f).value.sp
            val bodyFontSize  = (cardH * 0.05f).value.sp
            val btnFontSize   = (cardH * 0.06f).value.sp
            val iconBtnSize   = cardH * 0.08f

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hPad, vertical = vPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Grid
                MiniGrid(
                    grid = previewGrid,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.width(spacerMediumW))

                // Details and Button
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(iconSizeDp)
                        )
                        Spacer(modifier = Modifier.height(spacerSmall))

                        Text(
                            text = "START YOUR",
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor,
                            maxLines = 1
                        )
                        Text(
                            text = "JOURNEY",
                            fontSize = headFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = primary,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(spacerSmall))

                        Text(
                            text = "Merge tiles, reach 2048 and beat your high score!",
                            fontSize = bodyFontSize,
                            color = textSecondary,
                            lineHeight = (bodyFontSize.value * 1.4f).sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }

                    val isLight = (primary.red * 0.2126f + primary.green * 0.7152f + primary.blue * 0.0722f) > 0.5f
                    val buttonContentColor = if (isLight) Color(0xFF1F1F1F) else Color.White

                    Button(
                        onClick = onNewGameClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(btnHeightDp)
                            .testTag("StartJourneyCard_Button_NewGame"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = buttonContentColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(iconBtnSize)
                            )
                            Spacer(modifier = Modifier.width(cardW * 0.015f))
                            Text(
                                text = "NEW GAME",
                                fontWeight = FontWeight.Bold,
                                fontSize = btnFontSize,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
