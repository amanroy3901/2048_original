package com.avfusionapps.game_2048.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
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
