package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun GameModeCard(
    title: String,
    subtitle: String,
    tagText: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    accentColor: Color? = null
) {
    val theme = LocalGameTheme.current
    val primary = accentColor ?: theme.primaryColor
    val cardBg = theme.surfaceColor
    val cardBorder = primary.copy(alpha = 0.5f)
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // icon box
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(primary.copy(alpha = 0.15f))
                .border(1.dp, primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = theme.textColor,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color    = textSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tagText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = primary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(28.dp)
        )
    }
}
