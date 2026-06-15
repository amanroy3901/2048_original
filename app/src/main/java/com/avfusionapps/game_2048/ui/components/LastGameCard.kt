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
fun LastGameCard(
    score: Int,
    bestTile: Int,
    grid: List<List<Int>>,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    val theme = LocalGameTheme.current
    val primary = accentColor ?: theme.primaryColor
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

    NeonCard(
        accentColor = primary,
        isSelected = true,
        onClick = null,
        cornerRadius = 20.dp,
        borderWidth = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardH = maxHeight
            val cardW = maxWidth
            // All sizes as fractions of card height or width
            val hPad          = cardW * 0.045f   // ~16dp
            val vPad          = cardH * 0.07f    // ~12dp
            val spacerSmall   = cardH * 0.04f    // ~7dp
            val spacerMediumW = cardW * 0.045f   // ~16dp
            val btnHeight     = cardH * 0.20f    // ~35dp
            val labelFontSize = (cardH * 0.07f).value.sp
            val metaFontSize  = (cardH * 0.06f).value.sp
            val valueFontSize = (cardH * 0.11f).value.sp
            val btnFontSize   = (cardH * 0.07f).value.sp
            val iconSize      = cardH * 0.09f

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hPad, vertical = vPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Grid
                MiniGrid(
                    grid = grid,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.width(spacerMediumW))

                // Details and Button
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(R.string.last_game),
                            fontSize = labelFontSize,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(spacerSmall))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.score),
                                    fontSize = metaFontSize,
                                    color = textSecondary,
                                    maxLines = 1
                                )
                                Text(
                                    text = score.toString(),
                                    fontSize = valueFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textColor,
                                    maxLines = 1
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.best_tile),
                                    fontSize = metaFontSize,
                                    color = textSecondary,
                                    maxLines = 1
                                )
                                Text(
                                    text = bestTile.toString(),
                                    fontSize = valueFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onResumeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(btnHeight)
                            .testTag("LastGameCard_Button_Resume"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = theme.textColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.width(cardW * 0.015f))
                        Text(
                            text = stringResource(R.string.continue_label),
                            fontWeight = FontWeight.Bold,
                            fontSize = btnFontSize,
                            maxLines = 1
                        )
                    }
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
