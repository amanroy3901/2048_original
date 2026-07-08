package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun BestScoreCard(
    score: Int,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    val theme = LocalGameTheme.current
    val textSecondary = theme.textColor.copy(alpha = 0.6f)
    val iconColor = accentColor ?: theme.primaryColor

    NeonCard(
        accentColor = iconColor,
        isSelected = false,
        onClick = null,
        cornerRadius = 16.dp,
        borderWidth = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardH = maxHeight
            val cardW = maxWidth
            val hPad        = cardW * 0.05f
            val vPad        = cardH * 0.15f
            val badgeSizeDp = cardH * 0.62f
            val iconSizeDp  = cardH * 0.32f
            val spacerW     = cardW * 0.04f
            val labelSize   = (cardH * 0.15f).value.sp
            val scoreSize   = (cardH * 0.38f).value.sp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hPad, vertical = vPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // star badge
                Box(
                    modifier = Modifier
                        .size(badgeSizeDp)
                        .clip(RoundedCornerShape(badgeSizeDp / 2))
                        .background(theme.backgroundColor)
                        .border(
                            width = 1.dp,
                            color = iconColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(badgeSizeDp / 2)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(iconSizeDp)
                    )
                }

                Spacer(Modifier.width(spacerW))

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.best_score),
                        fontSize = labelSize,
                        letterSpacing = 1.5.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = score.toString(),
                        fontSize = scoreSize,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
