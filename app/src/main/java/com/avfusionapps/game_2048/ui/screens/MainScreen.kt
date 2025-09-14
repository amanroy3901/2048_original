package com.avfusionapps.game_2048.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.PurpleDarkBackground
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel

@Composable
fun ResumeGameDialog(
    onResumeGame: () -> Unit,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = HighLighter.copy(alpha = 0.5f),
                    spotColor = HighLighter.copy(alpha = 0.5f)
                ),
            shape = RoundedCornerShape(16.dp),
            color = PurpleDarkBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Game Logo",
                    modifier = Modifier
                        .height(80.dp)
                        .scale(1.1f),
                    colorFilter = ColorFilter.tint(
                        color = HighLighter.copy(alpha = 0.7f),
                        blendMode = BlendMode.SrcAtop
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Resume Previous Game?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You have an unfinished game. Would you like to resume where you left off or start a new game?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NeonRoundedButton(
                        onClick = onNewGame,
                        text = "New Game",
                        modifier = Modifier.weight(1f)
                    )

                    NeonRoundedButton(
                        onClick = onResumeGame,
                        text = "Resume",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkModePreview() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            MainScreenContent(
                navController = previewNavController,
                playerName = "Preview Player",
                highScore = 2048,
                onEditNameClick = {},
                showNameDialog = false,
                onDismissDialog = {},
                onNameChange = {}
            )
        }
    }
}

@Composable
fun MainScreen(navController: NavController, viewModel: GameViewModel = viewModel()) {

    val persistentPlayerName by viewModel.persistentPlayerName.collectAsState()
    val persistentHighScore by viewModel.persistentHighScore.collectAsState()
    val gameState by viewModel.gameStateFlow.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showResumeGameDialog by remember { mutableStateOf(false) }
    var initialNameCheckDone by remember { mutableStateOf(false) }

    LaunchedEffect(gameState) {
        showResumeGameDialog = gameState.hasSavedGame
    }

    LaunchedEffect(persistentPlayerName) {
        viewModel.enableNotification()
        if (!initialNameCheckDone && persistentPlayerName == GameSettingsRepository.DEFAULT_PLAYER_NAME) {
            showNameDialog = true
            initialNameCheckDone = true
        } else if (persistentPlayerName != GameSettingsRepository.DEFAULT_PLAYER_NAME) {
            initialNameCheckDone = true
        }
    }

    if (showResumeGameDialog) {
        ResumeGameDialog(
            onResumeGame = {
                viewModel.resumeSavedGame()
                navController.navigate("game?resume=true")
                showResumeGameDialog = false
            },
            onNewGame = {
                viewModel.initializeGame()
                navController.navigate("game")
                showResumeGameDialog = false
            },
            onDismiss = {
                showResumeGameDialog = false
            }
        )
    }

    MainScreenContent(
        navController = navController,
        playerName = persistentPlayerName,
        highScore = persistentHighScore,
        onEditNameClick = { showNameDialog = true },
        showNameDialog = showNameDialog,
        onDismissDialog = { showNameDialog = false },
        onNameChange = { newName ->
            viewModel.updatePlayerName(newName)
            if (newName.isNotBlank() && newName != GameSettingsRepository.DEFAULT_PLAYER_NAME) {
                initialNameCheckDone = true
            }
        }
    )
}

@Composable
fun MainScreenContent(
    navController: NavController,
    playerName: String,
    highScore: Int,
    onEditNameClick: () -> Unit,
    showNameDialog: Boolean,
    onDismissDialog: () -> Unit,
    onNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleDarkBackground)
            .padding(16.dp), // Consistent padding
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = HighLighter.copy(alpha = 0.5f),
                        spotColor = HighLighter.copy(alpha = 0.5f)
                    )
                    .background(
                        color = HighLighter,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onEditNameClick)
                    .padding(10.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Player Name",
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            PlayerWelcomeText(playerName)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Highest Score: $highScore",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Game Logo",
                modifier = Modifier
                    .height(140.dp)
                    .width(400.dp)
                    .scale(1.2f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            CylinderPlayButton(navController)
        }
        Spacer(modifier = Modifier.weight(1f))

    }

    if (showNameDialog) {
        NameEditDialog(
            currentName = playerName,
            onNameChange = onNameChange,
            onDismiss = onDismissDialog
        )
    }
}


@Composable
fun CylinderPlayButton(navController: NavController) {
    Button(
        onClick = { navController.navigate("game") },
        modifier = Modifier
            .width(220.dp)
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HighLighter),
        shape = RoundedCornerShape(percent = 50),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play button icon",
                tint = Color.White
            )
            Text("Play Game", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun NameEditDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(currentName) { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = PurpleDarkBackground.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Player Name",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Enter name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(0.8f),
                        cursorColor = HighLighter,
                        focusedBorderColor = HighLighter,
                        unfocusedBorderColor = HighLighter.copy(alpha = 0.6f),
                        focusedLabelColor = HighLighter,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonRoundedButton(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onNameChange(nameInput.trim())
                            }
                            onDismiss()
                        },
                        text = "Save"
                    )
                }
            }
        }
    }
}

val lilitaOneFontFamily = FontFamily(Font(R.font.lilitaone_regular))

@Composable
fun PlayerWelcomeText(playerName: String) {
    Text(
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.White)) {
                append("Welcome ")
            }
            withStyle(
                style = MaterialTheme.typography.headlineLarge.toSpanStyle().copy(
                    color = HighLighter,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = lilitaOneFontFamily
                )
            ) {
                append(playerName)
            }
            withStyle(style = SpanStyle(color = Color.White)) {
                append("!") // Exclamation mark
            }
        },
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
}