package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

/** One playable mode inside a [GameHubCard] (e.g. Classic / Time Attack / Play). */
data class GameHubMode(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
    val testTag: String = "",
    /** Filled gradient chip (primary action) vs outlined chip (secondary). */
    val filled: Boolean = true
)

/**
 * A "game hub" card for the main menu: one card per GAME, with its playable
 * modes as chips inside. All sizing is fraction-of-card-height so the same
 * component works in portrait and landscape.
 */
@Composable
fun GameHubCard(
    title: String,
    subtitle: String,
    tagText: String,
    accentColor: Color,
    graphic: @Composable (Dp) -> Unit,
    modes: List<GameHubMode>,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

    NeonCard(
        accentColor = accentColor,
        isSelected = false,
        onClick = null,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardH = maxHeight
            val hPad = cardH * 0.10f
            val vPad = cardH * 0.09f
            val iconBoxSize = cardH * 0.42f
            val iconInnerSize = cardH * 0.26f
            val chipH = cardH * 0.27f
            val titleFontSize = (cardH * 0.135f).value.sp
            val subtitleFontSize = (cardH * 0.095f).value.sp
            val tagFontSize = (cardH * 0.09f).value.sp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hPad, vertical = vPad)
            ) {
                // ── Header: graphic + name/subtitle/tag ──
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .clip(RoundedCornerShape(cardH * 0.10f))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(cardH * 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        graphic(iconInnerSize)
                    }

                    Spacer(Modifier.width(cardH * 0.10f))

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = title,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(cardH * 0.015f))
                        Text(
                            text = subtitle,
                            fontSize = subtitleFontSize,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(cardH * 0.02f))
                        Text(
                            text = tagText,
                            fontSize = tagFontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(vPad * 0.9f))

                // ── Mode chips ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chipH),
                    horizontalArrangement = Arrangement.spacedBy(hPad * 0.7f)
                ) {
                    modes.forEach { mode ->
                        GameHubModeChip(
                            mode = mode,
                            accentColor = accentColor,
                            fontSize = (chipH * 0.38f).value.sp,
                            iconSize = chipH * 0.52f,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameHubModeChip(
    mode: GameHubMode,
    accentColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val shape = RoundedCornerShape(50)

    val background = if (mode.filled) {
        Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(accentColor, accentColor.copy(alpha = 0.65f))
            )
        )
    } else {
        Modifier
            .background(theme.backgroundColor.copy(alpha = 0.4f))
            .border(1.dp, accentColor.copy(alpha = 0.55f), shape)
    }

    val contentColor = when {
        !mode.filled -> accentColor
        accentColor.luminance() > 0.55f -> Color(0xFF1E1E2E)
        else -> Color.White
    }

    Row(
        modifier = modifier
            .clip(shape)
            .then(background)
            .then(if (mode.testTag.isNotEmpty()) Modifier.testTag(mode.testTag) else Modifier)
            .clickable(onClick = mode.onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        mode.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(Modifier.width(iconSize * 0.35f))
        }
        Text(
            text = mode.label,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
    }
}
