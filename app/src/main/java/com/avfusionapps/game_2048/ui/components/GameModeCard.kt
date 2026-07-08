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

import androidx.compose.ui.unit.Dp

@Composable
fun GameModeCard(
    title: String,
    subtitle: String,
    tagText: String,
    icon: @Composable (Dp) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    val theme = LocalGameTheme.current
    val primary = accentColor ?: theme.primaryColor
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

    NeonCard(
        accentColor = primary,
        isSelected = false,
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardH = maxHeight
            val cardW = maxWidth

            val vPad = cardH * 0.14f
            val hPad = cardW * 0.045f
            val iconBoxSize = cardH * 0.58f
            val iconInnerSize = cardH * 0.30f
            val arrowSize = cardH * 0.28f
            val spacerW = cardW * 0.04f
            val titleFontSize = (cardH * 0.18f).value.sp
            val subtitleFontSize = (cardH * 0.13f).value.sp
            val tagFontSize = (cardH * 0.12f).value.sp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hPad, vertical = vPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // icon box
                Box(
                    modifier = Modifier
                        .size(iconBoxSize)
                        .clip(RoundedCornerShape(cardH * 0.14f))
                        .background(primary.copy(alpha = 0.15f))
                        .border(1.dp, primary.copy(alpha = 0.3f), RoundedCornerShape(cardH * 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    icon(iconInnerSize)
                }

                Spacer(Modifier.width(spacerW))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        title,
                        fontSize   = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color      = theme.textColor,
                        letterSpacing = 0.5.sp,
                        maxLines   = 1
                    )
                    Spacer(Modifier.height(cardH * 0.02f))
                    Text(
                        subtitle,
                        fontSize = subtitleFontSize,
                        color    = textSecondary,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(cardH * 0.04f))
                    Text(
                        text = tagText,
                        fontSize = tagFontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = primary,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(arrowSize)
                )
            }
        }
    }
}
