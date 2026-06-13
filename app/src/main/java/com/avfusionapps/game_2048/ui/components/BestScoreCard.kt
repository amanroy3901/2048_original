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
    accentColor: Color? = null
) {
    val theme = LocalGameTheme.current
    val cardBg = theme.surfaceColor
    val cardBorder = theme.surfaceColor.copy(alpha = 0.5f)
    val textSecondary = theme.textColor.copy(alpha = 0.6f)
    val iconColor = accentColor ?: theme.primaryColor

    NeonCard(
        accentColor = iconColor,
        isSelected = false,
        onClick = null,
        cornerRadius = 16.dp,
        borderWidth = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // star badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(theme.backgroundColor)
                    .border(
                        width = 1.dp,
                        color = iconColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.best_score),
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = score.toString(),
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = theme.textColor
                )
            }
        }
    }
}
