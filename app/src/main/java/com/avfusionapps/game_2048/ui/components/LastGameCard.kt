package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun LastGameCard(
    score: Int,
    bestTile: Int,
    grid: List<List<Int>>,
    onResumeClick: () -> Unit,
    accentColor: Color? = null
) {
    val theme = LocalGameTheme.current
    val cardBg = theme.surfaceColor
    val primary = accentColor ?: theme.primaryColor
    val cardBorder = primary.copy(alpha = 0.5f)
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

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
            grid = grid,
            modifier = Modifier.weight(1.2f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Details and Button
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.last_game),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.score),
                fontSize = 12.sp,
                color = textSecondary
            )
            Text(
                text = score.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.best_tile),
                fontSize = 12.sp,
                color = textSecondary
            )
            Text(
                text = bestTile.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onResumeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = theme.textColor
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.continue_label),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MiniGrid(
    grid: List<List<Int>>,
    modifier: Modifier = Modifier
) {
    // If grid is empty or not 4x4, use a mock grid
    val displayGrid = if (grid.isNotEmpty() && grid.size == 4 && grid[0].size == 4) {
        grid
    } else {
        listOf(
            listOf(2, 4, 8, 16),
            listOf(16, 32, 64, 128),
            listOf(128, 256, 512, 0),
            listOf(0, 0, 0, 0)
        )
    }
    
    val theme = LocalGameTheme.current

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f)
    ) {
        // We want 4 tiles and 3 spacers (of say 4.dp or 6.dp each)
        // Since it's aspect ratio 1f, we can just use columns and rows with weights.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until 4) {
                        val value = displayGrid[row][col]
                        val bgColor = if (value == 0) theme.backgroundColor else (theme.tileColors[value] ?: theme.surfaceColor)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (value > 0) {
                                Text(
                                    text = value.toString(),
                                    color = theme.textColor,
                                    fontSize = if (value >= 100) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
