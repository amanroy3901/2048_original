package com.avfusionapps.game_2048.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.NeonCutCornerButton
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.components.ClassicTopBar
import com.avfusionapps.game_2048.ui.components.ClassicBottomBar
import com.avfusionapps.game_2048.ui.components.GameScoreBoard
import com.avfusionapps.game_2048.ui.components.GameSwipeIndicator
import com.avfusionapps.game_2048.ui.components.GridSizeBottomSheet
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.ui.components.AnimatedLevelUnlockDialog
import com.avfusionapps.game_2048.viewmodel.Direction
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.log2
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Game Screen Dark Preview"
)
@Composable
fun DarkModePreviewGame() {
    rememberNavController()
    val theme = LocalGameTheme.current
    _2048OriginalTheme {
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(theme.backgroundColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                "Preview Player",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text("Score: 128", color = Color.White)
                            Text("High Score: 1024", color = Color.White)
                        }
                        Button(onClick = {}) { Text("New Game") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .background(theme.primaryColor)
                    ) {
                        Text(
                            "4x4 Grid Preview",
                            Modifier.align(Alignment.Center),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    navController: NavController,
    viewModel: GameViewModel = viewModel(),
    ) {

    val theme = LocalGameTheme.current
    val gameState = viewModel.gameState
    val persistentHighScore by viewModel.persistentHighScore.collectAsState()
    val persistentPlayerName by viewModel.persistentPlayerName.collectAsState()
    val newlyUnlockedTileValue by viewModel.newlyUnlockedTileValue.collectAsState()

    var showGridSizeDialog by remember { mutableStateOf(false) }
    val canUndo by viewModel.canUndo.collectAsState()
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = viewModel) {
        viewModel.mergeEvent.collectLatest {
            println("Merge event received. Attempting direct vibration.")

            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) { // Check if vibrator exists
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val vibrationEffect = VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                        vibrator.vibrate(vibrationEffect)
                        println("Direct vibration (Oreo+) attempted.")
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50) // 50ms vibration
                        println("Direct vibration (Legacy) attempted.")
                    }
                } catch (e: Exception) {
                    println("Error during direct vibration: ${e.message}")
                }
            } else {
                println("Device does not have a vibrator or service not found.")
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ -> /* no-op */ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasProgress = viewModel.gameState.moveCount > 0 ||
            viewModel.gameState.grid.any { row -> row.any { it != 0 } }

    BackHandler(enabled = true) {
        if (hasProgress) {
            showExitDialog = true
        } else {
            navController.navigateUp()
        }
    }

    if (showExitDialog) {
        Dialog(onDismissRequest = { showExitDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = theme.surfaceColor,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Exit current game?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Progress will be cleared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NeonRoundedButton(
                            modifier = Modifier.testTag("GameScreen_Button_CancelExit"),
                            text = "Cancel",
                            onClick = { showExitDialog = false },
                        )
                        NeonRoundedButton(
                            modifier = Modifier.testTag("GameScreen_Button_SaveAndExit"),
                            text = "Save & Exit",
                            onClick = {
                                viewModel.markResumableWithoutMove()
                                showExitDialog = false
                                navController.navigateUp()
                            },
                        )
                    }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("GameScreen_Root")
            .background(theme.backgroundColor)
            .padding(start = 16.dp, end = 16.dp, top = 50.dp)
            .pointerInput(Unit) {
                var totalX = 0f
                var totalY = 0f
                detectDragGestures(
                    onDragEnd = {
                        if (!gameState.isGameOver) {
                            val minSwipeDistance = 50
                            when {
                                abs(totalX) > abs(totalY) && abs(totalX) > minSwipeDistance -> {
                                    viewModel.move(if (totalX > 0) Direction.RIGHT else Direction.LEFT)
                                }

                                abs(totalY) > abs(totalX) && abs(totalY) > minSwipeDistance -> {
                                    viewModel.move(if (totalY > 0) Direction.DOWN else Direction.UP)
                                }
                            }
                        }
                        totalX = 0f; totalY = 0f
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
        ClassicTopBar(
            currentLevel = gameState.currentLevel,
            onSettingsClick = { showExitDialog = true },
            onBack = {
                if (hasProgress) {
                    showExitDialog = true
                } else {
                    navController.navigateUp()
                }
            }
        )

        GameScoreBoard(
            score = gameState.score,
            highScore = persistentHighScore,
            scoreIcon = Icons.Rounded.EmojiEvents, // Trophy
            highScoreIcon = Icons.Rounded.WorkspacePremium, // Ribbon/Crown
            highScoreTint = Color(0xFFFFD700) // Gold color for Best Score
        )

        Spacer(modifier = Modifier.height(16.dp))

        GameBoard(viewModel = viewModel)

        Spacer(modifier = Modifier.weight(1f))

        GameSwipeIndicator()

        ClassicBottomBar(
            onUndoClick = { viewModel.undoMove(context) },
            onRestartClick = { showGridSizeDialog = true },
            onHintClick = { /* TODO: Implement hint logic */ }
        )
    }

    if (showGridSizeDialog) {
        GridSizeBottomSheet(
            currentSize = gameState.gridSize,
            onSizeSelected = { size ->
                viewModel.updateGridSize(size)
                showGridSizeDialog = false
            },
            onDismiss = {
                if (gameState.grid.all { r -> r.all { it == 0 } }) {
                    navController.navigateUp()
                } else {
                    showGridSizeDialog = false
                }
            }
        )
    }

    if (gameState.isGameOver) {
        GameOverDialog(
            score = gameState.score,
            onNewGame = {
                showGridSizeDialog = true
            },
            onExit = { navController.navigateUp() }
        )
    }

    newlyUnlockedTileValue?.let { unlockedTileValue ->
        AnimatedLevelUnlockDialog(
            unlockedTileValue = unlockedTileValue,
            onDismiss = {
                viewModel.consumeNewlyUnlockedLevel()
            }
        )
    }
}

@Composable
fun GameBoard(viewModel: GameViewModel) {
    val theme = LocalGameTheme.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surfaceColor)
            .border(2.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            viewModel.gameState.grid.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEachIndexed { j, cellValue ->
                        val tileInfo = viewModel.gameState.tileAnimationInfo[Pair(i, j)]
                        GameCell(
                            value = cellValue,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("GameScreen_Item_GameCell_${i}_${j}")
                                .semantics {
                                    contentDescription = if (cellValue == 0) "Empty Tile at row $i column $j" else "Tile $cellValue at row $i column $j"
                                },
                            isNew = tileInfo?.isNew ?: false,
                            isMerged = tileInfo?.isMerged ?: false
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel.gameState.moveCount) {
        if (viewModel.gameState.tileAnimationInfo.isNotEmpty()) {
            delay(300) // Adjust delay based on longest animation
            viewModel.clearAnimationInfo()
        }
    }
}

@Composable
fun GameCell(
    value: Int,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    isMerged: Boolean = false
) {
    val theme = LocalGameTheme.current
    val backgroundColor = theme.tileColors[value] ?: theme.tileColors[0]!!
    val textColor = if (value <= 4) theme.textColor else Color.White

    val textSize = remember(value) {
        when {
            value >= 10000 -> 14.sp
            value >= 1000 -> 20.sp
            value >= 100 -> 24.sp
            else -> 28.sp
        }
    }

    val cellShadowModifier = if (value in 2..32) {
        Modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(6.dp),
            clip = false,
            ambientColor = Color.White.copy(alpha = 0.25f),
            spotColor = Color.White.copy(alpha = 0.25f)
        )
    } else {
        Modifier
    }

    val textShadow = remember(value) {
        if (value > 0) {
            val logValue = log2(value.toFloat()).coerceAtLeast(1f)
            val baseBlur = 2f
            val scaleFactor = 1.5f
            val blurRadius = (baseBlur + (logValue * scaleFactor)).coerceIn(2f, 25f)
            Shadow(
                color = if (value > 128) textColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.6f),
                offset = Offset.Zero,
                blurRadius = blurRadius
            )
        } else {
            null
        }
    }

    val borderModifier = if (value in 2..32) {
        Modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.3f),
            shape = RoundedCornerShape(6.dp)
        )
    } else {
        Modifier
    }

    var scale by remember { mutableStateOf(1f) }
    LaunchedEffect(key1 = value, key2 = isNew, key3 = isMerged) {
        scale = 1f // Reset scale
        if (isNew) {
            scale = 0.1f
            animate(0.1f, 1f, animationSpec = tween(150, 50)) { v, _ -> scale = v }
        } else if (isMerged) {
            animate(1f, 1.2f, animationSpec = tween(80)) { v, _ -> scale = v }
            animate(1.2f, 1f, animationSpec = tween(80)) { v, _ -> scale = v }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(cellShadowModifier) // Apply glow
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .then(borderModifier), // Apply border
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                color = textColor,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = textShadow
                ),
                modifier = Modifier.drawBehind {
                    val paint = Paint()
                        .asFrameworkPaint().apply {
                            this.style = android.graphics.Paint.Style.STROKE
                            this.strokeWidth = 2f  // Increased stroke width
                            this.color = Color.White.copy(alpha = 0.7f).hashCode()
                            this.textSize = textSize.toPx()
                            this.isAntiAlias = true  // Enable anti-aliasing
                            this.flags = this.flags or android.graphics.Paint.SUBPIXEL_TEXT_FLAG
                        }

                    val textBounds = android.graphics.Rect()
                    val text = value.toString()
                    paint.getTextBounds(text, 0, text.length, textBounds)

                    drawIntoCanvas { canvas ->
                        val x = (size.width - textBounds.width()) / 2f
                        val y = (size.height + textBounds.height()) / 2f
                        canvas.nativeCanvas.drawText(
                            text,
                            x,
                            y,
                            paint
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun GameOverDialog(score: Int, onNewGame: () -> Unit, onExit: () -> Unit) {
    val theme = LocalGameTheme.current
    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .testTag("GameScreen_Dialog_GameOver")
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = theme.surfaceColor,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Game Over!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HighLighter
                )
                Text(
                    "Final Score: $score",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NeonRoundedButton(
                        modifier = Modifier.testTag("GameScreen_Button_GameOverNewGame"),
                        onClick = onNewGame,
                        text = "New Game"
                    )
                    NeonRoundedButton(
                        modifier = Modifier.testTag("GameScreen_Button_GameOverExit"),
                        onClick = onExit,
                        text = "Exit"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun GameCellPreview() {
    val values = listOf(0, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048)

    _2048OriginalTheme {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(values) { value ->
                Box(
                    modifier = Modifier.padding(4.dp)
                ) {
                    GameCell(value, Modifier.size(75.dp))
                }
            }
        }
    }
}
