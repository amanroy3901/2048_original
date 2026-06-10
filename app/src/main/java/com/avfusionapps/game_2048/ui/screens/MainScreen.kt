package com.avfusionapps.game_2048.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.components.BestScoreCard
import com.avfusionapps.game_2048.ui.components.GameModeCard
import com.avfusionapps.game_2048.ui.components.GoogleSignInCard
import com.avfusionapps.game_2048.ui.components.GridSizeBottomSheet
import com.avfusionapps.game_2048.ui.components.LastGameCard
import com.avfusionapps.game_2048.ui.components.StartJourneyCard
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.PurpleDarkBackground
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkModePreview() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            modifier = Modifier.fillMaxSize()
        ) { _ ->
            MainScreenContent(
                navController = previewNavController,
                playerName = "Preview Player",
                highScore = 2048,
                currentLevel = 1,
                unlockedLevels = setOf(1),
                actions = {}
            )
        }
    }
}

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
    val resumePrompt by viewModel.resumePrompt.collectAsState()

    LaunchedEffect(resumePrompt) {
        if (resumePrompt) {
            viewModel.consumeResumePrompt()
        }
    }

    LaunchedEffect(persistentPlayerName) {
        viewModel.enableNotification()
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

    MainScreenContent(
        navController = navController,
        playerName = playerName,
        highScore = persistentHighScore,
        currentLevel = currentLevel,
        unlockedLevels = unlockedLevels,
        actions = {
            val theme = LocalGameTheme.current
            val textSecondary = theme.textColor.copy(alpha = 0.6f)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hasSaved) {
                    // Extract best tile from current game state, or use a default
                    val maxTile = gameState.grid.flatten().maxOrNull() ?: 0
                    LastGameCard(
                        score = gameState.score,
                        bestTile = maxTile,
                        grid = gameState.grid,
                        onResumeClick = {
                            viewModel.resumeSavedGame()
                            navController.navigate("game?resume=true&newGame=false")
                        }
                    )
                } else {
                    StartJourneyCard(
                        onNewGameClick = {
                            showGridSizeDialogMain = true
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(theme.textColor.copy(alpha = 0.2f)))
                    Text(
                        text = stringResource(R.string.game_modes),
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(theme.textColor.copy(alpha = 0.2f)))
                }

                GameModeCard(
                    title = stringResource(R.string.classic_2048),
                    subtitle = stringResource(R.string.classic_2048_subtitle),
                    tagText = stringResource(R.string.classic_2048_tag),
                    accentColor = theme.primaryColor,
                    icon = { MainModeIcon(mode = MainModeIconType.Classic, tint = theme.primaryColor) },
                    onClick = {
                        if (hasSaved) {
                            viewModel.declineSavedGame()
                        }
                        showGridSizeDialogMain = true
                    }
                )

                GameModeCard(
                    title = stringResource(R.string.time_attack),
                    subtitle = stringResource(R.string.time_attack_subtitle),
                    tagText = stringResource(R.string.time_attack_tag),
                    accentColor = theme.secondaryColor,
                    icon = { MainModeIcon(mode = MainModeIconType.TimeAttack, tint = theme.secondaryColor) },
                    onClick = {
                        navController.navigate("timeAttack")
                    }
                )
            }
        }
    )
}


@Composable
fun MainScreenContent(
    navController: NavController,
    playerName: String,
    highScore: Int,
    currentLevel: Int,
    unlockedLevels: Set<Int>,
    actions: @Composable () -> Unit
) {
    val theme = LocalGameTheme.current
    val textSecondary = theme.textColor.copy(alpha = 0.6f)
    val cardBorder = theme.textColor.copy(alpha = 0.1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("MainScreen_Root")
            .background(theme.backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .safeDrawingPadding(),
    ) {
        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Title Section (Centered)
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "2048",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = lilitaOneFontFamily,
                    fontStyle = FontStyle.Italic,
                    color = Color.Transparent,
                    style = LocalTextStyle.current.copy(
                        brush = Brush.horizontalGradient(
                            listOf(theme.primaryColor, theme.secondaryColor)
                        )
                    ),
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "N E O N   R U S H",
                    color = theme.primaryColor,
                    fontSize = 16.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic
                )
            }

            // Profile Button (Top Right)
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                MainTopIconButton(
                    label = "PROFILE",
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = theme.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = { navController.navigate("profile") }
                )
            }
        }

        // Actions / Game Content
        actions()

        Spacer(modifier = Modifier.weight(1f))
    }
}

private enum class MainModeIconType { Classic, TimeAttack }

@Composable
private fun MainTopIconButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val theme = LocalGameTheme.current
    val cardBorder = theme.textColor.copy(alpha = 0.1f)
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(theme.surfaceColor)
                .border(1.dp, cardBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = textSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MainModeIcon(
    mode: MainModeIconType,
    tint: Color,
) {
    val imageVector = when (mode) {
        MainModeIconType.Classic -> Icons.Rounded.GridView
        MainModeIconType.TimeAttack -> Icons.Rounded.Timer
    }

    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(30.dp)
    )
}


@Composable
fun CylinderActionButton(
    modifier: Modifier = Modifier
        .width(220.dp)
        .height(60.dp),
    text: String,
    onClick: () -> Unit,
    leadingIcon: Int? = null,
) {
    val theme = LocalGameTheme.current

    Button(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = text },
        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
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
            if (leadingIcon != null) {
                Icon(painter = painterResource(leadingIcon), contentDescription = null, tint = Color.White)
            }
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
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
