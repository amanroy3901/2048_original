package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun StartJourneyCard(
    onNewGameClick: () -> Unit,
    accentColor: Color? = null
) {
    val theme = LocalGameTheme.current
    val cardBg = theme.surfaceColor
    val primary = accentColor ?: theme.primaryColor
    val cardBorder = primary.copy(alpha = 0.5f)
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

    val previewGrid = listOf(
        listOf(2, 4, 8, 16),
        listOf(4, 8, 16, 32),
        listOf(8, 16, 32, 64),
        listOf(16, 32, 64, 128)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.2f), cardBg),
                    radius = 600f
                )
            )
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini Grid
        MiniGrid(
            grid = previewGrid,
            modifier = Modifier.weight(1.2f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Details and Button
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "START YOUR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textColor
            )
            Text(
                text = "JOURNEY",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Merge tiles, reach 2048 and beat your high score!",
                fontSize = 10.sp,
                color = textSecondary,
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNewGameClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NEW GAME",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
