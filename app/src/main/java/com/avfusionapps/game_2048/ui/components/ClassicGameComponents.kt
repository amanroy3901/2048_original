package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.components.SquareIconButton
import com.avfusionapps.game_2048.ui.components.NeonBadge

@Composable
fun ClassicTopBar(
    currentLevel: Int,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit
) {
    val theme = LocalGameTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Back Button
        SquareIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(id = R.string.desc_back_button),
            onClick = onBack
        )

        // Level Indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "—",
                    color = theme.textColor.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(id = R.string.level_label, currentLevel),
                    color = theme.textColor.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "—",
                    color = theme.textColor.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.textColor.copy(alpha = 0.2f)))
                Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.primaryColor))
                Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.textColor.copy(alpha = 0.2f)))
            }
        }

        // Settings Button
        SquareIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = stringResource(id = R.string.desc_settings_button),
            onClick = onSettingsClick
        )
    }
}

@Composable
fun ClassicBottomBar(
    onUndoClick: () -> Unit,
    onRestartClick: () -> Unit,
    onHintClick: () -> Unit
) {
    val theme = LocalGameTheme.current
    NeonCard(
        accentColor = theme.primaryColor,
        isSelected = true,
        onClick = null,
        cornerRadius = 16.dp,
        borderWidth = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarAction(
                icon = Icons.AutoMirrored.Rounded.Undo,
                label = stringResource(id = R.string.undo),
                badge = "3",
                tint = theme.primaryColor,
                onClick = onUndoClick
            )
            
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.textColor.copy(alpha = 0.1f)))

            // Center Hint (Bigger icon)
            BottomBarAction(
                icon = Icons.Rounded.Lightbulb,
                label = stringResource(id = R.string.hint),
                badge = "5",
                tint = theme.secondaryColor, // Yellow/Orange
                onClick = onHintClick,
                iconSize = 40.dp // Make it stand out
            )

            Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.textColor.copy(alpha = 0.1f)))

            BottomBarAction(
                icon = Icons.Rounded.Refresh,
                label = stringResource(id = R.string.restart),
                badge = null,
                tint = theme.primaryColor,
                onClick = onRestartClick
            )
        }
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    badge: String?,
    tint: Color,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 32.dp
) {
    val theme = LocalGameTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = theme.textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        if (badge != null) {
            Spacer(modifier = Modifier.height(4.dp))
            NeonBadge(
                text = badge,
                tint = tint
            )
        }
    }
}
