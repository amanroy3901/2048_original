package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

data class FloatingBonus(
    val id: Long,
    val text: String
)

@Composable
fun FloatingBonusText(
    text: String,
    onAnimationFinished: () -> Unit
) {
    val theme = LocalGameTheme.current
    val animatableY = remember { Animatable(40f) }
    val animatableAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.joinAll(
            launch {
                animatableY.animateTo(
                    targetValue = -50f,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutLinearInEasing)
                )
            },
            launch {
                animatableAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutLinearInEasing)
                )
            }
        )
        onAnimationFinished()
    }

    Text(
        text = text,
        color = theme.accentColor,
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(y = animatableY.value.dp)
            .graphicsLayer(alpha = animatableAlpha.value)
    )
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    badge: String?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(32.dp)
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    color = tint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
