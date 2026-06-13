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

    NeonCard(
        accentColor = primary,
        isSelected = true,
        onClick = null,
        cornerRadius = 20.dp,
        borderWidth = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
}

@Composable
fun MiniGrid(
    grid: List<List<Int>>,
    modifier: Modifier = Modifier
) {
    // If grid is empty or doesn't have valid elements, use a mock grid
    val displayGrid = if (grid.isNotEmpty() && grid[0].isNotEmpty()) {
        grid
    } else {
        listOf(
            listOf(2, 4, 8, 16),
            listOf(16, 32, 64, 128),
            listOf(128, 256, 512, 0),
            listOf(0, 0, 0, 0)
        )
    }
    
    val rows = displayGrid.size
    val cols = displayGrid[0].size
    val theme = LocalGameTheme.current

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until cols) {
                        val value = displayGrid[row][col]
                        GameCell(
                            value = value,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            fontSizeMultiplier = if (rows > 4) 0.4f else 0.5f
                        )
                    }
                }
            }
        }
    }
}
