package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun NeonCard(
    accentColor: Color,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = 1.4.dp,
    content: @Composable () -> Unit
) {
    val theme = LocalGameTheme.current
    val cardShape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isSelected) accentColor else accentColor.copy(alpha = 0.35f)
    val glowColor = accentColor.copy(alpha = if (isSelected) 0.22f else 0.08f)

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glowColor,
                        theme.backgroundColor.copy(alpha = 0.92f)
                    )
                )
            )
            .border(borderWidth, borderColor, cardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}
