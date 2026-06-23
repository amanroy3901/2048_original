package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun CylinderActionButton(
    modifier: Modifier = Modifier
        .width(220.dp)
        .height(60.dp),
    text: String,
    onClick: () -> Unit,
    leadingIcon: Int? = null,
) {
    val theme = LocalGameTheme.current
    val buttonColor = theme.primaryColor
    val isLight = (buttonColor.red * 0.2126f + buttonColor.green * 0.7152f + buttonColor.blue * 0.0722f) > 0.5f
    val contentColor = if (isLight) Color(0xFF1F1F1F) else Color.White

    Button(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = text },
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(percent = 50),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (leadingIcon != null) {
                Icon(painter = painterResource(leadingIcon), contentDescription = null, tint = contentColor)
            }
            Text(text, color = contentColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalGameTheme.current.textColor
) {
    val theme = LocalGameTheme.current
    val cardBorder = theme.textColor.copy(alpha = 0.1f)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surfaceColor)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SquareIconButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalGameTheme.current.textColor
) {
    val theme = LocalGameTheme.current
    val cardBorder = theme.textColor.copy(alpha = 0.1f)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surfaceColor)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
