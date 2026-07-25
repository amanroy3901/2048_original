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
    /** Optional neon sweep for the outline; falls back to a flat [accentColor] border. */
    borderBrush: Brush? = null,
    content: @Composable () -> Unit
) {
    val theme = LocalGameTheme.current
    val cardShape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isSelected) accentColor else accentColor.copy(alpha = 0.35f)
    // A whisper of accent along the top edge only — enough to catch the light, not
    // enough to tint the card. The body settles onto the page colour so the card
    // reads as part of the screen rather than a coloured panel sitting on it.
    val glowColor = accentColor.copy(alpha = if (isSelected) 0.09f else 0.04f)
    val bodyColor = theme.backgroundColor.copy(alpha = 0.92f)

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to glowColor,
                        0.45f to bodyColor,
                        1f to bodyColor
                    )
                )
            )
            .then(
                if (borderBrush != null) Modifier.border(borderWidth, borderBrush, cardShape)
                else Modifier.border(borderWidth, borderColor, cardShape)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}
