package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    val theme = LocalGameTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(theme.primaryColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = theme.textColor.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SettingsCard(onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.surfaceColor)
            .border(1.dp, theme.textColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun SupportItem(icon: ImageVector, label: String, sub: String) {
    val theme = LocalGameTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
        Icon(icon, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(sub, color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}
