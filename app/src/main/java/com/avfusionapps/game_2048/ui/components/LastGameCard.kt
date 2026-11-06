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
import androidx.compose.ui.platform.testTag
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
        modifier = modifier
    ) {
        val cellSpacing = maxHeight * 0.05f
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(cellSpacing)
        ) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing)
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
