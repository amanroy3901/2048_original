package com.ignitarium.game_2048.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ignitarium.game_2048.R
import com.ignitarium.game_2048.ui.theme.HighLighter
import com.ignitarium.game_2048.ui.theme.Purple80
import com.ignitarium.game_2048.ui.theme._2048OriginalTheme
import com.ignitarium.game_2048.viewmodel.Direction
import com.ignitarium.game_2048.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs


@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkModePreviewGame() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            GameScreen(navController = previewNavController)
        }}
}

//If you want to have a dynamic color preview:
@Preview(showSystemUi = true, apiLevel = 31)
@Composable
fun DynamicDarkModePreviewGame() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            GameScreen(navController = previewNavController)

        }
    }
}


@Composable
fun GameScreen(navController: NavController) {
    val viewModel: GameViewModel = viewModel()
    var showGridSizeDialog by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                var totalX = 0f
                var totalY = 0f
                detectDragGestures(
                    onDragEnd = {
                        when {
                            abs(totalX) > abs(totalY) -> {
                                if (abs(totalX) > 50) { // Minimum threshold for swipe
                                    if (totalX > 0) viewModel.move(Direction.RIGHT) else viewModel.move(Direction.LEFT)
                                }
                            }
                            else -> {
                                if (abs(totalY) > 50) { // Minimum threshold for swipe
                                    if (totalY > 0) viewModel.move(Direction.DOWN) else viewModel.move(Direction.UP)
                                }
                            }
                        }
                        totalX = 0f
                        totalY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalX += dragAmount.x
                    totalY += dragAmount.y
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_logo), contentDescription = "Game logo")
        Spacer(Modifier.height(32.dp))
        // Game Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Score: ${viewModel.gameState.score}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "High Score: ${viewModel.gameState.highScore}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Button(onClick = { showGridSizeDialog = true }, colors = ButtonDefaults.buttonColors
                (HighLighter)) {
                Text("New Game", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Game Board
        GameBoard(viewModel)
    }

    if (showGridSizeDialog) {
        GridSizeDialog(
            onSizeSelected = { size ->
                viewModel.updateGridSize(size)
                showGridSizeDialog = false
            },
            onDismiss = {
                if (!showGridSizeDialog) {
                    showGridSizeDialog = false
                } else {
                    navController.navigateUp()
                }
            }
        )
    }

    if (viewModel.gameState.isGameOver) {
        GameOverDialog(
            score = viewModel.gameState.score,
            onNewGame = { showGridSizeDialog = true },
            onExit = { navController.navigateUp() }
        )
    }
}

@Composable
fun GameBoard(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Purple80)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            viewModel.gameState.grid.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEachIndexed { j, cell ->
                        val cellPosition = Pair(i, j)
                        val tileInfo = viewModel.gameState.tileAnimationInfo[cellPosition]
                        
                        GameCell(
                            value = cell,
                            modifier = Modifier.weight(1f),
                            isNew = tileInfo?.isNew ?: false,
                            isMerged = tileInfo?.isMerged ?: false,
                            startPosition = tileInfo?.startPosition?.let { (startI, startJ) ->
                                // Convert grid coordinates to pixel offsets
                                val xOffset = (startJ - j) * 100f // Approximate cell width
                                val yOffset = (startI - i) * 100f // Approximate cell height
                                Pair(xOffset, yOffset)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Clear animation info after animations complete
    LaunchedEffect(viewModel.gameState.moveCount) {
        kotlinx.coroutines.delay(300) // Slightly longer than animation duration
        viewModel.clearAnimationInfo()
    }
}

@Composable
fun GameCell(
    value: Int, 
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    isMerged: Boolean = false,
    startPosition: Pair<Float, Float>? = null
) {
    val backgroundColor = when(value) {
        0 -> Color(0xFFCDC1B4)
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        512 -> Color(0xFFEDC850)
        1024 -> Color(0xFFEDC53F)
        2048 -> Color(0xFFEDC22E)
        else -> Color(0xFF3C3A32)
    }

    var scale by remember { mutableStateOf(if (isNew || isMerged) 0.6f else 1f) }
    var offsetX by remember { mutableStateOf(startPosition?.first ?: 0f) }
    var offsetY by remember { mutableStateOf(startPosition?.second ?: 0f) }

    LaunchedEffect(value, isNew, isMerged) {
        if (value > 0) {
            // Position animation - animate from start position to current position
            if (startPosition != null) {
                animate(
                    initialValue = startPosition.first,
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                ) { value, _ -> offsetX = value }
                
                animate(
                    initialValue = startPosition.second,
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                ) { value, _ -> offsetY = value }
            }

            // For new tiles or merged tiles
            if (isNew) {
                // Scale animation for new tiles (appear)
                animate(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 150)
                ) { value, _ -> scale = value }
            } else if (isMerged) {
                // Pulse animation for merged tiles
                animate(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = tween(durationMillis = 100)
                ) { value, _ -> scale = value }
                
                animate(
                    initialValue = 1.2f,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 100, delayMillis = 100)
                ) { value, _ -> scale = value }
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                color = if (value <= 4) Color(0xFF776E65) else Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GridSizeDialog(
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Grid Size",
                    style = MaterialTheme.typography.titleLarge
                )

                GridSizeButtons(onSizeSelected)
            }
        }
    }
}

@Composable
fun GameOverDialog(
    score: Int,
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Game Over!",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onNewGame) {
                        Text("New Game")
                    }
                    Button(onClick = onExit) {
                        Text("Exit")
                    }
                }
            }
        }
    }
}

@Composable
fun GridSizeButtons(onSizeSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center // Center vertically
    ) {
        // First Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onSizeSelected(3) },
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                shape = CutCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(HighLighter)
            ) {
                Text("3x3", color = Color.White)
            }
            Button(
                onClick = { onSizeSelected(4) },
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                shape = CutCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(HighLighter)
            ) {
                Text("4x4", color = Color.White)
            }
        }

        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onSizeSelected(5) },
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                shape = CutCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(HighLighter)
            ) {
                Text("5x5", color = Color.White)
            }
            Button(
                onClick = { onSizeSelected(6) },
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                shape = CutCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(HighLighter)
            ) {
                Text("6x6", color = Color.White)
            }
        }
    }
}