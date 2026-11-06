package com.avfusionapps.game_2048.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.drawNeonGlow
import com.avfusionapps.game_2048.ui.components.GameHubCard
import com.avfusionapps.game_2048.ui.components.GameHubMode
import com.avfusionapps.game_2048.ui.components.GoogleSignInCard
import com.avfusionapps.game_2048.ui.components.GridSizeBottomSheet
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun MainScreen(navController: NavController, viewModel: GameViewModel = viewModel()) {

    // Use gameState.playerName for immediate updates, falling back to persistent for initialization if needed
    val gameState = viewModel.gameState
    val firebaseAuth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf(firebaseAuth.currentUser) }
    val persistentPlayerName by viewModel.persistentPlayerName.collectAsState()

    // Prefer the name from gameState if it's not default, otherwise fallback to persistent or default
    val playerName = if (gameState.playerName != GameSettingsRepository.DEFAULT_PLAYER_NAME) {
        gameState.playerName
    } else {
        persistentPlayerName
    }

    val persistentHighScore by viewModel.persistentHighScore.collectAsState()
    val hasSaved by viewModel.hasSavedGameFlow.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val unlockedLevels by viewModel.unlockedLevels.collectAsState()

    var showGridSizeDialogMain by remember { mutableStateOf(false) }
    var showAuthDialog by rememberSaveable { mutableStateOf(firebaseAuth.currentUser == null) }
    val resumePrompt by viewModel.resumePrompt.collectAsState()

    LaunchedEffect(resumePrompt) {
        if (resumePrompt) {
            viewModel.consumeResumePrompt()
        }
    }

    if (showGridSizeDialogMain) {
        GridSizeBottomSheet(
            currentSize = gameState.gridSize,
            onSizeSelected = { size ->
                viewModel.updateGridSize(size)
                showGridSizeDialogMain = false
                navController.navigate("game?resume=false&newGame=false")
            },
            onDismiss = {
                showGridSizeDialogMain = false
            }
        )
    }

    // Wait for the start-up intro and welcome before asking to sign in.
    val authPromptDone by com.avfusionapps.game_2048.data.progress.Progress.authPromptDone.collectAsState()
    if (showAuthDialog && firebaseAuth.currentUser == null && !StartupFlow.busy && authPromptDone == false) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val dialogWidth = if (screenWidth * 0.9f > 400.dp) 400.dp else screenWidth * 0.9f

        Dialog(
            onDismissRequest = {
                showAuthDialog = false
                com.avfusionapps.game_2048.data.progress.Progress.markAuthPromptDone()
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            GoogleSignInCard(
                firebaseAuth = firebaseAuth,
                modifier = Modifier
                    .width(dialogWidth)
                    .testTag("GoogleSignInCard_Dialog"),
                onAuthSuccess = {
                    showAuthDialog = false
                    com.avfusionapps.game_2048.data.progress.Progress.markAuthPromptDone()
                    viewModel.loadUserDataFromFirebase()
                }
            )
        }
    }

    val coins by viewModel.coins.collectAsState()
    val seen by viewModel.seenSprites.collectAsState()
    var showCollection by remember { mutableStateOf(false) }
    SpriteHomeScreen(
        playerName = playerName,
        coins = coins,
        best = persistentHighScore,
        hasSaved = hasSaved,
        savedScore = gameState.score,
        savedGrid = gameState.grid,
        spritesFound = seen.count { it in 2..2048 },
        actions = HomeActions(
            onContinue = {
                viewModel.resumeSavedGame()
                navController.navigate("game?resume=true&newGame=false")
            },
            onNewClassic = { showGridSizeDialogMain = true },
            onTimeAttack = { navController.navigate("timeAttack") },
            onShooter = { navController.navigate("dropMerge") },
            onProfile = { navController.navigate("profile") },
            onSettings = { navController.navigate("profile") },
            onCollection = { showCollection = true },
            onAchievements = { navController.navigate("achievements") }
        )
    )
    if (showCollection) {
        com.avfusionapps.game_2048.ui.components.SpriteCollectionSheet(seen = seen, onDismiss = { showCollection = false })
    }
}

