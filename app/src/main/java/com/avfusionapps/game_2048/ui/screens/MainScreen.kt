package com.avfusionapps.game_2048.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.avfusionapps.game_2048.ui.components.BestScoreCard
import com.avfusionapps.game_2048.ui.components.GameModeCard
import com.avfusionapps.game_2048.ui.components.GoogleSignInCard
import com.avfusionapps.game_2048.ui.components.GridSizeBottomSheet
import com.avfusionapps.game_2048.ui.components.LastGameCard
import com.avfusionapps.game_2048.ui.components.StartJourneyCard
import com.avfusionapps.game_2048.ui.components.CylinderActionButton
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
                actions = { _ -> }
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
    var showAuthDialog by rememberSaveable { mutableStateOf(firebaseAuth.currentUser == null) }
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

    if (showAuthDialog && firebaseAuth.currentUser == null) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val dialogWidth = if (screenWidth * 0.9f > 400.dp) 400.dp else screenWidth * 0.9f

        Dialog(
            onDismissRequest = { showAuthDialog = false },
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
                    viewModel.loadUserDataFromFirebase()
                }
            )
        }
    }

    MainScreenContent(
        navController = navController,
        playerName = playerName,
        highScore = persistentHighScore,
        currentLevel = currentLevel,
        unlockedLevels = unlockedLevels,
        hasSaved = hasSaved,
        score = gameState.score,
        grid = gameState.grid,
        onResumeClick = {
            viewModel.resumeSavedGame()
            navController.navigate("game?resume=true&newGame=false")
        },
        onStartClick = {
            showGridSizeDialogMain = true
        },
        onTimeAttackClick = {
            navController.navigate("timeAttack")
        },
        onSettingsClick = {
            navController.navigate("themeSettings")
        },
        actions = { dims ->
            val theme = LocalGameTheme.current
            val textSecondary = theme.textColor.copy(alpha = 0.6f)

            // All spacing and card heights derived from screen height fractions
            val cardSpacing   = dims.screenH * 0.018f  // gap between cards
            val largeCardH    = dims.screenH * 0.220f  // LastGame / StartJourney card
            val bestScoreH    = dims.screenH * 0.105f  // Best score card
            val gameModeH     = dims.screenH * 0.110f  // each game mode card
            val dividerVPad   = dims.screenH * 0.010f  // vertical padding around divider
            val dividerFontSz = (dims.screenW * 0.032f).value.sp
            val dividerHPad   = dims.screenW * 0.043f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(cardSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hasSaved) {
                    val maxTile = gameState.grid.flatten().maxOrNull() ?: 0
                    LastGameCard(
                        score = gameState.score,
                        bestTile = maxTile,
                        grid = gameState.grid,
                        onResumeClick = {
                            viewModel.resumeSavedGame()
                            navController.navigate("game?resume=true&newGame=false")
                        },
                        modifier = Modifier
                            .testTag("MainScreen_Card_LastGame")
                            .height(largeCardH)
                    )
                } else {
                    StartJourneyCard(
                        onNewGameClick = { showGridSizeDialogMain = true },
                        modifier = Modifier
                            .testTag("MainScreen_Card_StartJourney")
                            .height(largeCardH)
                    )
                }

                BestScoreCard(
                    score = persistentHighScore,
                    modifier = Modifier
                        .testTag("MainScreen_Card_BestScore")
                        .height(bestScoreH),
                    accentColor = theme.primaryColor
                )

                // ── GAME MODES divider ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dividerVPad)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(theme.textColor.copy(alpha = 0.2f))
                    )
                    Text(
                        text = stringResource(R.string.game_modes),
                        color = textSecondary,
                        fontSize = dividerFontSz,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = dividerHPad)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(theme.textColor.copy(alpha = 0.2f))
                    )
                }

                GameModeCard(
                    title = stringResource(R.string.classic_2048),
                    subtitle = stringResource(R.string.classic_2048_subtitle),
                    tagText = stringResource(R.string.classic_2048_tag),
                    accentColor = theme.primaryColor,
                    icon = { size -> MainModeIcon(mode = MainModeIconType.Classic, tint = theme.primaryColor, size = size) },
                    onClick = { showGridSizeDialogMain = true },
                    modifier = Modifier
                        .testTag("MainScreen_Button_ClassicMode")
                        .height(gameModeH)
                )

                GameModeCard(
                    title = stringResource(R.string.time_attack),
                    subtitle = stringResource(R.string.time_attack_subtitle),
                    tagText = stringResource(R.string.time_attack_tag),
                    accentColor = theme.secondaryColor,
                    icon = { size -> MainModeIcon(mode = MainModeIconType.TimeAttack, tint = theme.secondaryColor, size = size) },
                    onClick = { navController.navigate("timeAttack") },
                    modifier = Modifier
                        .testTag("MainScreen_Button_TimeAttackMode")
                        .height(gameModeH)
                )
            }
        }
    )
}


/** Carries fractional dimension tokens derived from the real screen size. */
data class ScreenDimensions(val screenW: Dp, val screenH: Dp)

@Composable
fun MainScreenContent(
    navController: NavController,
    playerName: String,
    highScore: Int,
    currentLevel: Int,
    unlockedLevels: Set<Int>,
    hasSaved: Boolean = false,
    score: Int = 0,
    grid: List<List<Int>> = emptyList(),
    onResumeClick: () -> Unit = {},
    onStartClick: () -> Unit = {},
    onTimeAttackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    actions: @Composable (ScreenDimensions) -> Unit
) {
    val theme = LocalGameTheme.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
    ) {
        val screenW = this.maxWidth
        val screenH = this.maxHeight
        val dims    = ScreenDimensions(screenW, screenH)

        // All tokens derived from actual screen fractions
        val hPadding        = screenW * 0.053f   // ~20dp on 375dp width
        val vPadding        = screenH * 0.018f   // ~14dp on 800dp height
        val topBarBottomPad = screenH * 0.025f   // ~20dp
        val iconBtnSize     = screenW * 0.130f   // ~49dp
        val iconBtnCorner   = screenW * 0.037f   // ~14dp
        val iconSize        = screenW * 0.060f   // ~22dp
        val iconLabelSize   = screenW * 0.030f   // ~11sp
        val titleFontSize   = (screenH * 0.080f).value.sp  // scales with height so it never crowds
        val subtitleFontSize= (screenH * 0.020f).value.sp
        val spacerSmall     = screenH * 0.004f

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            val scaleFactor = (screenH.value / 400f).coerceIn(0.75f, 1.5f)
            val tips = listOf(
                "Keep your highest tile in a corner to build up larger merges.",
                "Don't swipe up unless absolutely necessary to avoid messy grids.",
                "In Time Attack, merge quickly to build a massive multiplier!",
                "Planning two moves ahead ensures you never run out of options.",
                "You can change neon themes in Settings at any time!"
            )
            val activeTip = remember { tips.random() }
            
            // Calculate screen fraction sizes
            val leftColWidth = screenW * 0.28f
            val rightColWidth = screenW * 0.68f
            
            val logoHeight = screenH * 0.14f
            val cardHeightLeft = screenH * 0.13f
            val skylineHeight = screenH * 0.15f
            
            val continueCardH = screenH * 0.42f
            val gameModeCardH = screenH * 0.20f
            val tipCardH = screenH * 0.09f
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("MainScreen_Root")
                    .padding(horizontal = screenW * 0.02f, vertical = screenH * 0.03f)
                    .safeDrawingPadding(),
                horizontalArrangement = Arrangement.spacedBy(screenW * 0.02f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Logo, separator, profile, best score, settings, skyline
                Column(
                    modifier = Modifier
                        .width(leftColWidth)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.height(logoHeight)
                    ) {
                        Text(
                            text = "2048",
                            fontSize = (screenH.value * 0.09f).sp,
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
                        Spacer(modifier = Modifier.height((2 * scaleFactor).dp))
                        Text(
                            text = "N E O N   R U S H",
                            color = theme.primaryColor,
                            fontSize = (screenH.value * 0.022f).sp,
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(1.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color.Transparent, theme.primaryColor.copy(alpha = 0.5f), Color.Transparent)
                                )
                            )
                    )

                    // Best Score Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeightLeft)
                            .clip(RoundedCornerShape(cardHeightLeft * 0.20f))
                            .background(theme.surfaceColor)
                            .border(1.dp, theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(cardHeightLeft * 0.20f))
                            .padding(horizontal = cardHeightLeft * 0.18f, vertical = cardHeightLeft * 0.12f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(cardHeightLeft * 0.65f)
                                .clip(CircleShape)
                                .background(theme.primaryColor.copy(alpha = 0.15f))
                                .border(1.dp, theme.primaryColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size(cardHeightLeft * 0.38f)
                            )
                        }
                        Spacer(modifier = Modifier.width(cardHeightLeft * 0.15f))
                        Column {
                            Text(
                                text = "BEST SCORE",
                                color = theme.textColor.copy(alpha = 0.5f),
                                fontSize = (cardHeightLeft.value * 0.15f).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = highScore.toString(),
                                color = theme.textColor,
                                fontSize = (cardHeightLeft.value * 0.24f).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Profile card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeightLeft)
                            .clip(RoundedCornerShape(cardHeightLeft * 0.20f))
                            .background(theme.surfaceColor)
                            .border(1.dp, theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(cardHeightLeft * 0.20f))
                            .clickable { navController.navigate("profile") }
                            .padding(horizontal = cardHeightLeft * 0.18f, vertical = cardHeightLeft * 0.12f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(cardHeightLeft * 0.15f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(cardHeightLeft * 0.65f)
                                    .clip(CircleShape)
                                    .background(theme.secondaryColor.copy(alpha = 0.15f))
                                    .border(1.dp, theme.secondaryColor.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = theme.secondaryColor,
                                    modifier = Modifier.size(cardHeightLeft * 0.38f)
                                )
                            }
                            Column(modifier = Modifier.widthIn(max = leftColWidth * 0.5f)) {
                                Text(
                                    text = "PROFILE",
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    fontSize = (cardHeightLeft.value * 0.15f).sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = playerName,
                                    color = theme.textColor,
                                    fontSize = (cardHeightLeft.value * 0.24f).sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = theme.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(cardHeightLeft * 0.32f)
                        )
                    }

                    // Settings Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeightLeft)
                            .clip(RoundedCornerShape(cardHeightLeft * 0.20f))
                            .background(theme.surfaceColor)
                            .border(1.dp, theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(cardHeightLeft * 0.20f))
                            .clickable { onSettingsClick() }
                            .padding(horizontal = cardHeightLeft * 0.18f, vertical = cardHeightLeft * 0.12f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(cardHeightLeft * 0.15f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(cardHeightLeft * 0.65f)
                                    .clip(CircleShape)
                                    .background(theme.textColor.copy(alpha = 0.1f))
                                    .border(1.dp, theme.textColor.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = theme.textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(cardHeightLeft * 0.38f)
                                )
                            }
                            Column {
                                Text(
                                    text = "SETTINGS",
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    fontSize = (cardHeightLeft.value * 0.15f).sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Themes & Options",
                                    color = theme.textColor.copy(alpha = 0.8f),
                                    fontSize = (cardHeightLeft.value * 0.20f).sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = theme.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(cardHeightLeft * 0.32f)
                        )
                    }

                    CitySkylineGraphic(
                        primaryColor = theme.primaryColor,
                        secondaryColor = theme.secondaryColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(skylineHeight)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    theme.primaryColor.copy(alpha = 0.6f),
                                    theme.secondaryColor.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Right Column
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .width(rightColWidth)
                        .fillMaxHeight()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: Settings Button
                    Row(
                        modifier = Modifier.fillMaxWidth().height(screenH * 0.08f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(screenH * 0.08f)
                                .clip(CircleShape)
                                .background(theme.surfaceColor)
                                .border(1.dp, theme.textColor.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = theme.textColor,
                                modifier = Modifier.size(screenH * 0.045f)
                            )
                        }
                    }

                    // Continue Last Game Card
                    val maxTile = if (grid.isNotEmpty()) grid.flatten().maxOrNull() ?: 0 else 0
                    MainContinueCard(
                        hasSaved = hasSaved,
                        score = score,
                        bestTile = maxTile,
                        grid = if (grid.isNotEmpty()) grid else List(4) { List(4) { 0 } },
                        cardHeight = continueCardH,
                        primaryColor = theme.primaryColor,
                        secondaryColor = theme.secondaryColor,
                        textColor = theme.textColor,
                        surfaceColor = theme.surfaceColor,
                        onResumeClick = onResumeClick,
                        onStartClick = onStartClick,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // GAME MODES divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenH * 0.06f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(theme.textColor.copy(alpha = 0.2f))
                        )
                        Text(
                            text = "GAME MODES",
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = (screenH.value * 0.024f).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = screenW * 0.02f)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(theme.textColor.copy(alpha = 0.2f))
                        )
                    }

                    // Game Mode Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(screenW * 0.02f)
                    ) {
                        GameModeCardLandscape(
                            title = "Classic 2048",
                            subtitle = "Slide matching tiles to reach 2048!",
                            accentColor = theme.primaryColor,
                            graphic = {
                                IsometricTile2048Graphic(
                                    primaryColor = theme.primaryColor,
                                    secondaryColor = theme.secondaryColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            onClick = onStartClick,
                            cardHeight = gameModeCardH,
                            modifier = Modifier.weight(1f)
                        )

                        GameModeCardLandscape(
                            title = "Time Attack",
                            subtitle = "Beat the ticking clock to score!",
                            accentColor = theme.secondaryColor,
                            graphic = {
                                StopwatchGraphic(
                                    primaryColor = theme.primaryColor,
                                    secondaryColor = theme.secondaryColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            onClick = onTimeAttackClick,
                            cardHeight = gameModeCardH,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Tip of the Day
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(tipCardH)
                            .clip(RoundedCornerShape(tipCardH * 0.22f))
                            .background(theme.primaryColor.copy(alpha = 0.05f))
                            .border(1.dp, theme.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(tipCardH * 0.22f))
                            .padding(horizontal = tipCardH * 0.25f, vertical = tipCardH * 0.15f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(tipCardH * 0.18f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = theme.primaryColor,
                            modifier = Modifier.size(screenH * 0.045f)
                        )
                        Text(
                            text = "TIP: $activeTip",
                            color = theme.textColor.copy(alpha = 0.7f),
                            fontSize = (screenH.value * 0.022f).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("MainScreen_Root")
                    // NO verticalScroll — everything fits within screen height
                    .padding(horizontal = hPadding, vertical = vPadding)
                    .safeDrawingPadding()
            ) {
                // ── Top Bar ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = topBarBottomPad)
                ) {
                    // Title (centered)
                    Column(
                        modifier = Modifier.align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "2048",
                            fontSize = titleFontSize,
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
                        Spacer(modifier = Modifier.height(spacerSmall))
                        Text(
                            text = "N E O N   R U S H",
                            color = theme.primaryColor,
                            fontSize = subtitleFontSize,
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    // Profile button displaying user name (top-right)
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 72.dp, max = screenW * 0.28f)
                                .clip(RoundedCornerShape(iconBtnCorner))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            theme.surfaceColor,
                                            theme.backgroundColor.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                                .drawWithContent {
                                    drawContent()
                                    drawNeonGlow(theme.primaryColor.copy(alpha = 0.3f), 6.dp)
                                }
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            theme.primaryColor.copy(alpha = 0.6f),
                                            theme.secondaryColor.copy(alpha = 0.6f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(iconBtnCorner)
                                )
                                .clickable { navController.navigate("profile") }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("MainScreen_Button_Profile"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Circular glowing icon container
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                theme.secondaryColor.copy(alpha = 0.25f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = theme.secondaryColor.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = theme.secondaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playerName,
                                fontWeight = FontWeight.Bold,
                                fontFamily = lilitaOneFontFamily,
                                fontSize = (iconLabelSize.value + 1f).sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = LocalTextStyle.current.copy(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(theme.primaryColor, theme.secondaryColor)
                                    )
                                ),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // ── Actions / Game Content — fills the remaining space ──
                Box(modifier = Modifier.fillMaxSize()) {
                    actions(dims)
                }
            }
        }
    }
}

private enum class MainModeIconType { Classic, TimeAttack }

@Composable
private fun MainTopIconButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 52.dp,
    cornerRadius: Dp = 14.dp,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val theme = LocalGameTheme.current
    val cardBorder = theme.textColor.copy(alpha = 0.1f)
    val textSecondary = theme.textColor.copy(alpha = 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(cornerRadius))
                .background(theme.surfaceColor)
                .border(1.dp, cardBorder, RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = textSecondary,
            fontSize = labelFontSize
        )
    }
}

@Composable
private fun MainModeIcon(
    mode: MainModeIconType,
    tint: Color,
    size: Dp
) {
    val imageVector = when (mode) {
        MainModeIconType.Classic -> Icons.Rounded.GridView
        MainModeIconType.TimeAttack -> Icons.Rounded.Timer
    }

    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size)
    )
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

@Composable
fun CitySkylineGraphic(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 1. Draw perspective grid lines on the ground
        val gridY = height * 0.75f
        for (i in 0..4) {
            val y = gridY + (height - gridY) * (i / 4f)
            val alpha = 0.05f + 0.15f * (i / 4f)
            drawLine(
                color = secondaryColor.copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        val horizonX = width / 2f
        for (i in -3..3) {
            val targetX = horizonX + (width / 2f) * (i / 3f) * 1.5f
            drawLine(
                color = secondaryColor.copy(alpha = 0.15f),
                start = Offset(horizonX, gridY),
                end = Offset(targetX, height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Draw neon building outlines with randomized glowing dots (windows)
        val buildings = listOf(
            Triple(0.15f, 0.12f, 0.50f),
            Triple(0.30f, 0.14f, 0.65f),
            Triple(0.48f, 0.11f, 0.40f),
            Triple(0.62f, 0.15f, 0.70f),
            Triple(0.78f, 0.12f, 0.55f),
            Triple(0.90f, 0.13f, 0.45f)
        )

        buildings.forEach { (xFrac, wFrac, hFrac) ->
            val bWidth = width * wFrac
            val bHeight = height * hFrac
            val bLeft = width * xFrac - bWidth / 2f
            val bTop = gridY - bHeight
            val buildingColor = if (xFrac < 0.5f) primaryColor else secondaryColor

            // Light fill
            drawRect(
                color = buildingColor.copy(alpha = 0.08f),
                topLeft = Offset(bLeft, bTop),
                size = Size(bWidth, bHeight)
            )
            // Stroke border outline
            drawRect(
                color = buildingColor.copy(alpha = 0.6f),
                topLeft = Offset(bLeft, bTop),
                size = Size(bWidth, bHeight),
                style = Stroke(width = 1.dp.toPx())
            )

            // Random windows
            val cols = 2
            val rows = 4
            val colStep = bWidth / (cols + 1)
            val rowStep = bHeight / (rows + 1)
            for (c in 1..cols) {
                for (r in 1..rows) {
                    if ((c + r * 5) % 3 != 0) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = 0.8.dp.toPx(),
                            center = Offset(bLeft + c * colStep, bTop + r * rowStep)
                        )
                    }
                }
            }
        }
    }
}

private fun project(x: Float, y: Float, z: Float, cx: Float, cy: Float, scale: Float): Offset {
    val px = cx + (x - y) * 0.866f * scale
    val py = cy + (x + y) * 0.5f * scale - z * scale
    return Offset(px, py)
}

private fun drawIsoCube(
    drawScope: DrawScope,
    x: Float, y: Float, z: Float,
    size3D: Float,
    cx: Float, cy: Float,
    scale: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val v000 = project(x, y, z, cx, cy, scale)
    val v100 = project(x + size3D, y, z, cx, cy, scale)
    val v010 = project(x, y + size3D, z, cx, cy, scale)
    val v110 = project(x + size3D, y + size3D, z, cx, cy, scale)
    
    val v001 = project(x, y, z + size3D, cx, cy, scale)
    val v101 = project(x + size3D, y, z + size3D, cx, cy, scale)
    val v011 = project(x, y + size3D, z + size3D, cx, cy, scale)
    val v111 = project(x + size3D, y + size3D, z + size3D, cx, cy, scale)

    val pathTop = Path().apply {
        moveTo(v001.x, v001.y)
        lineTo(v101.x, v101.y)
        lineTo(v111.x, v111.y)
        lineTo(v011.x, v011.y)
        close()
    }
    drawScope.drawPath(pathTop, color = secondaryColor.copy(alpha = 0.25f))

    val pathLeft = Path().apply {
        moveTo(v000.x, v000.y)
        lineTo(v010.x, v010.y)
        lineTo(v011.x, v011.y)
        lineTo(v001.x, v001.y)
        close()
    }
    drawScope.drawPath(pathLeft, color = primaryColor.copy(alpha = 0.15f))

    val pathRight = Path().apply {
        moveTo(v010.x, v010.y)
        lineTo(v110.x, v110.y)
        lineTo(v111.x, v111.y)
        lineTo(v011.x, v011.y)
        close()
    }
    drawScope.drawPath(pathRight, color = primaryColor.copy(alpha = 0.2f))

    val edges = listOf(
        Pair(v000, v100), Pair(v000, v010), Pair(v100, v110), Pair(v010, v110),
        Pair(v001, v101), Pair(v001, v011), Pair(v101, v111), Pair(v011, v111),
        Pair(v000, v001), Pair(v100, v101), Pair(v010, v011), Pair(v110, v111)
    )
    val strokeWidthPx = with(drawScope) { 1.dp.toPx() }
    edges.forEach { (from, to) ->
        drawScope.drawLine(
            color = secondaryColor.copy(alpha = 0.8f),
            start = from,
            end = to,
            strokeWidth = strokeWidthPx
        )
    }
}

@Composable
fun StackedCubesGraphic(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height * 0.65f
        val scale = width * 0.35f

        // Draw isometric glowing base grid
        val basePoints = listOf(
            project(-0.3f, -0.3f, 0f, cx, cy, scale),
            project(1.3f, -0.3f, 0f, cx, cy, scale),
            project(1.3f, 1.3f, 0f, cx, cy, scale),
            project(-0.3f, 1.3f, 0f, cx, cy, scale)
        )
        val basePath = Path().apply {
            moveTo(basePoints[0].x, basePoints[0].y)
            lineTo(basePoints[1].x, basePoints[1].y)
            lineTo(basePoints[2].x, basePoints[2].y)
            lineTo(basePoints[3].x, basePoints[3].y)
            close()
        }
        drawPath(basePath, color = primaryColor.copy(alpha = 0.08f))
        drawPath(basePath, color = primaryColor.copy(alpha = 0.4f), style = Stroke(width = 1.dp.toPx()))

        // Grid lines on base
        for (i in 0..4) {
            val t = i / 4f
            val p0 = project(-0.3f, t * 1.6f - 0.3f, 0f, cx, cy, scale)
            val p1 = project(1.3f, t * 1.6f - 0.3f, 0f, cx, cy, scale)
            drawLine(primaryColor.copy(alpha = 0.15f), p0, p1, strokeWidth = 0.8.dp.toPx())

            val p2 = project(t * 1.6f - 0.3f, -0.3f, 0f, cx, cy, scale)
            val p3 = project(t * 1.6f - 0.3f, 1.3f, 0f, cx, cy, scale)
            drawLine(primaryColor.copy(alpha = 0.15f), p2, p3, strokeWidth = 0.8.dp.toPx())
        }

        // Draw cubes back-to-front
        val size3D = 0.5f
        drawIsoCube(this, 0f, 0.5f, 0f, size3D, cx, cy, scale, primaryColor, secondaryColor)
        drawIsoCube(this, 0.5f, 0f, 0f, size3D, cx, cy, scale, primaryColor, secondaryColor)
        drawIsoCube(this, 0.25f, 0.25f, 0.5f, size3D, cx, cy, scale, primaryColor, secondaryColor)
    }
}

@Composable
fun IsometricTile2048Graphic(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height * 0.65f
        val scale = width * 0.45f

        val tSize = 0.8f
        val x = 0.1f
        val y = 0.1f
        val z = 0f
        val thick = 0.15f

        val v000 = project(x, y, z, cx, cy, scale)
        val v100 = project(x + tSize, y, z, cx, cy, scale)
        val v010 = project(x, y + tSize, z, cx, cy, scale)
        val v110 = project(x + tSize, y + tSize, z, cx, cy, scale)

        val v001 = project(x, y, z + thick, cx, cy, scale)
        val v101 = project(x + tSize, y, z + thick, cx, cy, scale)
        val v011 = project(x, y + tSize, z + thick, cx, cy, scale)
        val v111 = project(x + tSize, y + tSize, z + thick, cx, cy, scale)

        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = width * 0.45f,
            center = v110
        )

        val pathTop = Path().apply {
            moveTo(v001.x, v001.y)
            lineTo(v101.x, v101.y)
            lineTo(v111.x, v111.y)
            lineTo(v011.x, v011.y)
            close()
        }
        drawPath(pathTop, color = primaryColor.copy(alpha = 0.35f))

        val pathLeft = Path().apply {
            moveTo(v000.x, v000.y)
            lineTo(v001.x, v001.y)
            lineTo(v011.x, v011.y)
            lineTo(v010.x, v010.y)
            close()
        }
        drawPath(pathLeft, color = secondaryColor.copy(alpha = 0.15f))

        val pathRight = Path().apply {
            moveTo(v010.x, v010.y)
            lineTo(v011.x, v011.y)
            lineTo(v111.x, v111.y)
            lineTo(v110.x, v110.y)
            close()
        }
        drawPath(pathRight, color = secondaryColor.copy(alpha = 0.2f))

        val edges = listOf(
            Pair(v000, v100), Pair(v000, v010), Pair(v100, v110), Pair(v010, v110),
            Pair(v001, v101), Pair(v001, v011), Pair(v101, v111), Pair(v011, v111),
            Pair(v000, v001), Pair(v100, v101), Pair(v010, v011), Pair(v110, v111)
        )
        edges.forEach { (from, to) ->
            drawLine(
                color = primaryColor,
                start = from,
                end = to,
                strokeWidth = 1.2.dp.toPx()
            )
        }

        val tcX = (v001.x + v111.x) / 2f
        val tcY = (v001.y + v111.y) / 2f

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 9.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText("2048", tcX, tcY + 3.dp.toPx(), paint)
        }
    }
}

@Composable
fun StopwatchGraphic(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val radius = width * 0.3f

        drawCircle(
            color = secondaryColor.copy(alpha = 0.1f),
            radius = radius * 1.2f,
            center = Offset(cx, cy)
        )

        drawCircle(
            color = secondaryColor,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )

        val btnW = radius * 0.3f
        val btnH = radius * 0.15f
        drawRect(
            color = secondaryColor,
            topLeft = Offset(cx - btnW / 2f, cy - radius - btnH),
            size = Size(btnW, btnH)
        )

        val loopRadius = radius * 0.2f
        drawCircle(
            color = secondaryColor,
            radius = loopRadius,
            center = Offset(cx, cy - radius - btnH - loopRadius),
            style = Stroke(width = 1.dp.toPx())
        )

        drawCircle(
            color = Color.White,
            radius = 1.5.dp.toPx(),
            center = Offset(cx, cy)
        )

        for (angle in 0 until 360 step 60) {
            val rad = Math.toRadians(angle.toDouble())
            val start = Offset(
                (cx + (radius - 3.dp.toPx()) * Math.cos(rad)).toFloat(),
                (cy + (radius - 3.dp.toPx()) * Math.sin(rad)).toFloat()
            )
            val end = Offset(
                (cx + radius * Math.cos(rad)).toFloat(),
                (cy + radius * Math.sin(rad)).toFloat()
            )
            drawLine(
                color = secondaryColor.copy(alpha = 0.6f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx()
            )
        }

        drawLine(
            color = Color.White,
            start = Offset(cx, cy),
            end = Offset(
                (cx + radius * 0.7f * Math.cos(Math.toRadians(-60.0))).toFloat(),
                (cy + radius * 0.7f * Math.sin(Math.toRadians(-60.0))).toFloat()
            ),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
fun MainContinueCard(
    hasSaved: Boolean,
    score: Int,
    bestTile: Int,
    grid: List<List<Int>>,
    cardHeight: Dp,
    primaryColor: Color,
    secondaryColor: Color,
    textColor: Color,
    surfaceColor: Color,
    onResumeClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardH = cardHeight
    com.avfusionapps.game_2048.ui.components.NeonCard(
        accentColor = primaryColor,
        isSelected = true,
        onClick = null,
        cornerRadius = cardH * 0.12f,
        borderWidth = 1.2.dp,
        modifier = modifier.height(cardH)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(cardH * 0.08f)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(cardH * 0.06f)
            ) {
                Box(
                    modifier = Modifier
                        .size(cardH * 0.20f)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasSaved) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.Star,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(cardH * 0.10f)
                    )
                }
                Column {
                    Text(
                        text = if (hasSaved) "CONTINUE LAST GAME" else "START NEW GAME",
                        color = primaryColor,
                        fontSize = (cardH.value * 0.075f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (hasSaved) "Pick up where you left off" else "Begin your 2048 puzzle quest",
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = (cardH.value * 0.06f).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(cardH * 0.06f))

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // MiniGrid Preview
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                ) {
                    com.avfusionapps.game_2048.ui.components.MiniGrid(
                        grid = grid,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(cardH * 0.06f))

                // Stats Column
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(cardH * 0.04f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(cardH * 0.09f)
                        )
                        Column {
                            Text("Score", color = textColor.copy(alpha = 0.5f), fontSize = (cardH.value * 0.05f).sp)
                            Text(score.toString(), color = textColor, fontSize = (cardH.value * 0.08f).sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(cardH * 0.04f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(cardH * 0.09f)
                        )
                        Column {
                            Text("Best Tile", color = textColor.copy(alpha = 0.5f), fontSize = (cardH.value * 0.05f).sp)
                            Text(bestTile.toString(), color = primaryColor, fontSize = (cardH.value * 0.08f).sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(cardH * 0.04f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(cardH * 0.09f)
                        )
                        Column {
                            Text("Session Status", color = textColor.copy(alpha = 0.5f), fontSize = (cardH.value * 0.05f).sp)
                            Text(if (hasSaved) "Active Save" else "No Saved Game", color = textColor.copy(alpha = 0.7f), fontSize = (cardH.value * 0.07f).sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(cardH * 0.06f))

                // Graphic & Button Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight().width(cardH * 1.1f)
                ) {
                    StackedCubesGraphic(
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        modifier = Modifier.weight(1f).aspectRatio(1.2f)
                    )

                    Spacer(modifier = Modifier.height(cardH * 0.04f))

                    Button(
                        onClick = if (hasSaved) onResumeClick else onStartClick,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(cardH * 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardH * 0.22f)
                            .shadow(
                                elevation = cardH * 0.05f,
                                spotColor = primaryColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(cardH * 0.15f)
                            ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(cardH * 0.10f)
                            )
                            Spacer(modifier = Modifier.width(cardH * 0.02f))
                            Text(
                                text = if (hasSaved) "CONTINUE PLAYING" else "START PLAYING",
                                color = Color.White,
                                fontSize = (cardH.value * 0.065f).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameModeCardLandscape(
    title: String,
    subtitle: String,
    accentColor: Color,
    graphic: @Composable () -> Unit,
    onClick: () -> Unit,
    cardHeight: Dp,
    modifier: Modifier = Modifier
) {
    val theme = LocalGameTheme.current
    val cardH = cardHeight
    com.avfusionapps.game_2048.ui.components.NeonCard(
        accentColor = accentColor,
        isSelected = true,
        onClick = onClick,
        cornerRadius = cardH * 0.20f,
        borderWidth = 1.dp,
        modifier = modifier.height(cardH)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(cardH * 0.14f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(cardH * 0.75f),
                contentAlignment = Alignment.Center
            ) {
                graphic()
            }

            Spacer(modifier = Modifier.width(cardH * 0.14f))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = theme.textColor,
                    fontSize = (cardH.value * 0.16f).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(cardH * 0.03f))
                Text(
                    text = subtitle,
                    color = theme.textColor.copy(alpha = 0.6f),
                    fontSize = (cardH.value * 0.10f).sp,
                    lineHeight = (cardH.value * 0.13f).sp
                )
            }

            Spacer(modifier = Modifier.width(cardH * 0.08f))

            Box(
                modifier = Modifier
                    .size(cardH * 0.40f)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(cardH * 0.22f)
                )
            }
        }
    }
}

