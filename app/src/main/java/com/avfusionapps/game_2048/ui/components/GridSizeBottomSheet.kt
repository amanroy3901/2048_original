package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Preview
@Composable
fun GridSizeBottomSheetPreview() {
    GridSizeBottomSheet(
        currentSize = 4,
        onSizeSelected = {},
        onDismiss = {}
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridSizeBottomSheet(
    currentSize: Int,
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalGameTheme.current
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val scrollState = rememberScrollState()

    val options = listOf(
        GridSizeOption(
            size = 3,
            title = stringResource(R.string.grid_size_3_label),
            badge = stringResource(R.string.grid_size_easy),
            description = stringResource(R.string.grid_size_3_description),
            accentColor = theme.primaryColor
        ),
        GridSizeOption(
            size = 4,
            title = stringResource(R.string.grid_size_4_label),
            badge = stringResource(R.string.grid_size_standard),
            description = stringResource(R.string.grid_size_4_description),
            accentColor = theme.secondaryColor
        ),
        GridSizeOption(
            size = 5,
            title = stringResource(R.string.grid_size_5_label),
            badge = stringResource(R.string.grid_size_challenging),
            description = stringResource(R.string.grid_size_5_description),
            accentColor = theme.secondaryColor
        ),
        GridSizeOption(
            size = 6,
            title = stringResource(R.string.grid_size_6_label),
            badge = stringResource(R.string.grid_size_expert),
            description = stringResource(R.string.grid_size_6_description),
            accentColor = theme.secondaryColor
        )
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        modifier = modifier.testTag("GameScreen_Dialog_GridSize")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(sheetShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            theme.surfaceColor.copy(alpha = 0.98f),
                            theme.backgroundColor.copy(alpha = 0.99f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = theme.textColor.copy(alpha = 0.12f),
                    shape = sheetShape
                )
                .verticalScroll(scrollState)
                .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 18.dp)
        ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(theme.textColor.copy(alpha = 0.35f))
                        .size(width = 64.dp, height = 5.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.choose_grid_size),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 48.dp),
                        color = theme.textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .background(theme.textColor.copy(alpha = 0.12f))
                            .border(1.dp, theme.textColor.copy(alpha = 0.1f), CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close_grid_size_picker),
                            tint = theme.textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowOptions.forEach { option ->
                            GridSizeOptionCard(
                                option = option,
                                isSelected = option.size == currentSize,
                                onClick = { onSizeSelected(option.size) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
}

@Composable
private fun GridSizeOptionCard(
    option: GridSizeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalGameTheme.current

    NeonCard(
        accentColor = option.accentColor,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1.18f)
            .testTag("GameScreen_Button_GridSize${option.size}x${option.size}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    GridPreviewIcon(
                        size = option.size,
                        tint = option.accentColor
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = option.title,
                            color = theme.textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(option.accentColor.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = option.badge,
                                color = option.accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Text(
                text = option.description,
                color = theme.textColor.copy(alpha = 0.68f),
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun GridPreviewIcon(
    size: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val squareSize = when (size) {
        3 -> 10.dp
        4 -> 8.5.dp
        5 -> 7.dp
        else -> 5.5.dp
    }
    val spacing = when (size) {
        3 -> 3.dp
        4 -> 2.5.dp
        5 -> 2.dp
        else -> 1.4.dp
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(size) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                repeat(size) {
                    Box(
                        modifier = Modifier
                            .size(squareSize)
                            .clip(RoundedCornerShape(2.dp))
                            .background(tint)
                    )
                }
            }
        }
    }
}

private data class GridSizeOption(
    val size: Int,
    val title: String,
    val badge: String,
    val description: String,
    val accentColor: Color,
)
