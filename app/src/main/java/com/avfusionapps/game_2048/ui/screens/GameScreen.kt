package com.avfusionapps.game_2048.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel // Standard viewModel import
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.avfusionapps.game_2048.R // Assuming logo is here
import com.avfusionapps.game_2048.ui.NeonCutCornerButton // Your custom button
import com.avfusionapps.game_2048.ui.NeonRoundedButton // Your custom button
import com.avfusionapps.game_2048.ui.theme.HighLighter // Your theme color
import com.avfusionapps.game_2048.ui.theme.Purple80 // Your theme color
import com.avfusionapps.game_2048.ui.theme.PurpleDarkBackground // Your theme color
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme // Your theme
import com.avfusionapps.game_2048.viewmodel.Direction
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset // *** Import Offset ***
import androidx.compose.ui.graphics.Shadow // *** Import Shadow ***
import androidx.compose.ui.text.TextStyle // *** Import TextStyle ***
import kotlin.math.log2 // *** Import log2 ***

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text // Use Material 3 Text
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas


@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Game Screen Dark Preview")
@Composable
fun DarkModePreviewGame() {
    val previewNavController = rememberNavController()
    // Provide a dummy ViewModel for preview or display static structure
    _2048OriginalTheme { // Apply your theme
        Scaffold { padding -> // Use Scaffold for basic layout structure
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(PurpleDarkBackground)) {
                // Static preview content simulating the screen layout
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Image(painter = painterResource(id = R.drawable.ic_logo), contentDescription = "Logo", Modifier.height(100.dp))
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Preview Player", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text("Score: 128", color = Color.White)
                            Text("High Score: 1024", color = Color.White)
                        }
                        Button(onClick = {}) { Text("New Game") } // Use standard button for preview
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.aspectRatio(1f).fillMaxWidth().background(Purple80)) {
                        Text("4x4 Grid Preview", Modifier.align(Alignment.Center), color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun GameScreen(navController: NavController, viewModel: GameViewModel = viewModel()) {

    val gameState = viewModel.gameState
    // Collect persistent values, reacting to changes from DataStore
    val persistentHighScore by viewModel.persistentHighScore.collectAsState()
    val persistentPlayerName by viewModel.persistentPlayerName.collectAsState()

    var showGridSizeDialog by remember { mutableStateOf(false) }
    // ... (other state collection) ...
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current // *** Get context ***

    LaunchedEffect(key1 = viewModel) {
        viewModel.mergeEvent.collectLatest {
            println("Merge event received. Attempting direct vibration.")

            // --- Direct Vibrator Test ---
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) { // Check if vibrator exists
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // For Android Oreo (API 26) and above
                        val vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE) // 50ms vibration
                        vibrator.vibrate(vibrationEffect)
                        println("Direct vibration (Oreo+) attempted.")
                    } else {
                        // For older versions (deprecated)
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
            // --- End Direct Vibrator Test ---

            // You can comment out the compose haptic call while testing direct vibration
            // hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Use DisposableEffect for lifecycle-aware setup and cleanup
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Show dialog on resume only if game is uninitialized and dialog isn't already showing
                if (!showGridSizeDialog && gameState.grid.all { r -> r.all { it == 0 } } && gameState.moveCount == 0) {
                    showGridSizeDialog = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Cleanup: remove observer when effect leaves composition
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Main UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleDarkBackground) // Use theme background
            .padding(16.dp)
            // Swipe gesture detection
            .pointerInput(Unit) {
                var totalX = 0f
                var totalY = 0f
                detectDragGestures(
                    onDragEnd = {
                        if (!gameState.isGameOver) { // Process moves only if game is active
                            val minSwipeDistance = 50 // Adjust sensitivity
                            when {
                                abs(totalX) > abs(totalY) && abs(totalX) > minSwipeDistance -> {
                                    viewModel.move(if (totalX > 0) Direction.RIGHT else Direction.LEFT)
                                }
                                abs(totalY) > abs(totalX) && abs(totalY) > minSwipeDistance -> {
                                    viewModel.move(if (totalY > 0) Direction.DOWN else Direction.UP)
                                }
                            }
                        }
                        totalX = 0f; totalY = 0f // Reset trackers
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
        // Game Logo
        Image(
            painter = painterResource(id = R.drawable.ic_logo), // Ensure this resource exists
            contentDescription = "Game Logo",
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .scale(1.2f) // Adjust scale/size as needed
        )
        Spacer(Modifier.height(32.dp))

        // Header Section: Scores and New Game Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = persistentPlayerName, // Display persistent name
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Score: ${gameState.score}", // Display current score
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "High Score: $persistentHighScore", // Display persistent high score
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            // Use your custom Neon button
            NeonRoundedButton(
                onClick = { showGridSizeDialog = true }, // Trigger size selection
                text = "New Game"
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Game Board Area
        GameBoard(viewModel = viewModel) // Pass the ViewModel down

    } // End Main Column

    // --- Dialogs ---

    // Grid Size Selection Dialog
    if (showGridSizeDialog) {
        GridSizeDialog(
            onSizeSelected = { size ->
                viewModel.updateGridSize(size) // Let ViewModel handle grid update/init
                showGridSizeDialog = false
            },
            onDismiss = {
                // If dismissing initial dialog without starting, navigate back
                if (gameState.grid.all { r -> r.all { it == 0 } }) {
                    navController.navigateUp()
                } else {
                    showGridSizeDialog = false // Just close dialog if game was in progress
                }
            }
        )
    }

    // Game Over Dialog
    if (gameState.isGameOver) {
        GameOverDialog(
            score = gameState.score, // Show final score
            onNewGame = {
                showGridSizeDialog = true // Start new game flow via size selection
            },
            onExit = { navController.navigateUp() } // Exit the game screen
        )
    }
}


// ==================================================
// Game Board and Cell Components
// ==================================================

@Composable
fun GameBoard(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Maintain square aspect ratio
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Purple80.copy(alpha = 0.8f)) // Slightly transparent grid background
            .padding(4.dp) // Padding between border and cells
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp) // Space between rows
        ) {
            viewModel.gameState.grid.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Space between cells
                ) {
                    row.forEachIndexed { j, cellValue ->
                        val tileInfo = viewModel.gameState.tileAnimationInfo[Pair(i, j)]
                        GameCell(
                            value = cellValue,
                            modifier = Modifier.weight(1f),
                            isNew = tileInfo?.isNew ?: false,
                            isMerged = tileInfo?.isMerged ?: false
                        )
                    }
                }
            }
        }
    }

    // Animation Cleanup Effect
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
    // Background color logic (kept as you provided)
    val backgroundColor = remember(value) {
        when (value) {
            0 -> Color(0xFF79002A).copy(0.45f)
            2 -> Color(0xFF7E002B)
            4 -> Color(0xFFCC0143)
            8 -> Color(0xFFE8004E)
            16 -> Color(0xFFFF0E62)
            32 -> Color(0xFFFF3378)
            64 -> Color(0xFFFF5B93)
            128 -> Color(0xFFFF74A4)
            256 -> Color(0xFFFF8BAF)
            512 -> Color(0xFFFF9CBE)
            1024 -> Color(0xFFFFADC7)
            2048 -> Color(0xFFE8B7C7)
            else -> Color(0xFFFFFFFF)
        }
    }

    val textColor = remember(value) {
        when (value) {
            0 -> Color(0xFFFFFFFF)
            2 -> Color(0xFFFFDDE5)
            4 -> Color(0xFFFFC4D9)
            8 -> Color(0xFFFFC2D5)
            16 -> Color(0xFFFFA0C2)
            32 -> Color(0xFFFFD3E3)
            64 -> Color(0xFFFFD3E3)
            128 -> Color(0xFFFFD3E3)
            256 -> Color(0xFFED0053)
            512 -> Color(0xFFED0053)
            1024 -> Color(0xFFED0053)
            2048 -> Color(0xFFED0053)
            else -> Color(0xFFED0053)
        }
    }

    val textSize = remember(value) {
        when {
            value >= 10000 -> 14.sp
            value >= 1000 -> 20.sp
            value >= 100 -> 24.sp
            else -> 28.sp
        }
    }

    // --- Cell Glow Calculation ---
    // Apply glow only to cells with values 2, 4, 8, 16, 32
    val cellShadowModifier = if (value in 2..32) {
        Modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(6.dp),
            clip = false,
            ambientColor = Color.White.copy(alpha = 0.25f),
            spotColor = Color.White.copy(alpha = 0.25f)
        )
    } else {
        Modifier // No glow for other cells
    }

    // --- Text Glow Calculation (remains the same) ---
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

    // --- Cell Stroke Calculation ---
    // Apply border only to cells with values 2, 4, 8, 16, 32
    val borderModifier = if (value in 2..32) {
        Modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.3f),
            shape = RoundedCornerShape(6.dp)
        )
    } else {
        Modifier // No border for other cells
    }

    // --- Animation State (remains the same) ---
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

    // --- Cell Box ---
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
                            this.strokeWidth = 1f
                            this.color = Color.White.hashCode()
                            this.textSize = textSize.toPx()
                        }

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            value.toString(),
                            0.2f,
                            textSize.toPx() - 4,
                            paint
                        )
                    }
                }
            )
        }
    }
}
// ==================================================
// Dialog Components
// ==================================================
@Composable
fun GridSizeDialog(onSizeSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = PurpleDarkBackground.copy(alpha = 0.95f), // Slightly transparent dialog
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Select Grid Size", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                GridSizeButtons(onSizeSelected)
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = HighLighter) // Use theme accent color
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(score: Int, onNewGame: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = {}) { // Prevent accidental dismiss
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = PurpleDarkBackground.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Game Over!", style = MaterialTheme.typography.headlineMedium, color = HighLighter)
                Text("Final Score: $score", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NeonRoundedButton(onClick = onNewGame, text = "New Game") // Use your custom button
                    NeonRoundedButton(onClick = onExit, text = "Exit") // Use your custom button
                }
            }
        }
    }
}

@Composable
fun GridSizeButtons(onSizeSelected: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            NeonCutCornerButton(text = "3x3", onClick = { onSizeSelected(3) }, modifier = Modifier.weight(1f))
            NeonCutCornerButton(text = "4x4", onClick = { onSizeSelected(4) }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            NeonCutCornerButton(text = "5x5", onClick = { onSizeSelected(5) }, modifier = Modifier.weight(1f))
            NeonCutCornerButton(text = "6x6", onClick = { onSizeSelected(6) }, modifier = Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
fun GameCellPreview() {
    val values = listOf(0, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048)

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
