package com.avfusionapps.game_2048.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import com.avfusionapps.game_2048.ui.components.NameEditDialog
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.avfusionapps.game_2048.ui.components.GoogleSignInCard
import com.avfusionapps.game_2048.ui.components.SquareIconButton
import com.avfusionapps.game_2048.ui.components.SectionHeader
import com.avfusionapps.game_2048.ui.components.SettingsCard
import com.avfusionapps.game_2048.ui.components.SupportItem
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.avfusionapps.game_2048.viewmodel.ThemeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.scale
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch
import android.util.Log
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.components.NeonCard
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.rounded.Undo

@Composable
fun ProfileScreen(
    navController: NavController,
    gameViewModel: GameViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val theme = LocalGameTheme.current
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val persistentHighScore by gameViewModel.persistentHighScore.collectAsState()
    val persistentPlayerName by gameViewModel.persistentPlayerName.collectAsState()
    
    val firebaseAuth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf(firebaseAuth.currentUser) }
    var showNameDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val soundEnabled by gameViewModel.soundEnabled.collectAsState()
    val vibrationEnabled by gameViewModel.vibrationEnabled.collectAsState()

    val packageInfo = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"

    val playerName = if (gameViewModel.gameState.playerName != GameSettingsRepository.DEFAULT_PLAYER_NAME) {
        gameViewModel.gameState.playerName
    } else {
        persistentPlayerName
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        ProfileScreenLandscape(
            navController = navController,
            playerName = playerName,
            persistentHighScore = persistentHighScore,
            currentTheme = currentTheme,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            versionName = versionName,
            currentUser = currentUser,
            firebaseAuth = firebaseAuth,
            onEditNameClick = { showNameDialog = true },
            onSoundToggle = { gameViewModel.updateSoundEnabled(it) },
            onVibrationToggle = { gameViewModel.updateVibrationEnabled(it) },
            onSignOutClick = {
                firebaseAuth.signOut()
                currentUser = null
            },
            onAuthSuccess = {
                currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    gameViewModel.loadUserDataFromFirebase()
                }
            },
            onRateApp = {
                val packageName = context.packageName
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=$packageName")
                    setPackage("com.android.vending")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                    context.startActivity(webIntent)
                }
            },
            onShareApp = {
                val packageName = context.packageName
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "2048 Neon Rush")
                    putExtra(Intent.EXTRA_TEXT, "Hey, check out this awesome, glowing 2048 game: https://play.google.com/store/apps/details?id=$packageName")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share via"))
            },
            onPrivacyPolicy = {
                val privacyUrl = "https://amanroy3901.github.io/privacy/privacy_neon_rush_2048.html"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ProfileScreen_Root")
                .background(theme.backgroundColor)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .safeDrawingPadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SquareIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.testTag("ProfileScreen_Button_Back")
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "2048",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        color = Color.Transparent,
                        style = LocalTextStyle.current.copy(
                            brush = Brush.horizontalGradient(
                                listOf(theme.primaryColor, theme.secondaryColor)
                            )
                        ),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "N E O N   R U S H",
                        color = theme.primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 2.sp
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.surfaceColor)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(theme.primaryColor, theme.secondaryColor)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(theme.primaryColor, theme.secondaryColor))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playerName,
                                        color = theme.textColor,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit Name",
                                        tint = theme.primaryColor,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .testTag("ProfileScreen_Button_EditName")
                                            .clickable { showNameDialog = true }
                                    )
                                }
                                Text(
                                    text = "Neon Master",
                                    color = theme.textColor.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "HIGH SCORE",
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = persistentHighScore.toString(),
                                        color = theme.primaryColor,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        tint = theme.primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        // Cube Icon Placeholder
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, theme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .background(theme.backgroundColor.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "2048",
                                color = theme.primaryColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Account Section
                SectionHeader(icon = Icons.Rounded.Person, title = "ACCOUNT")
                key(currentUser?.uid) {
                    GoogleSignInCard(
                        firebaseAuth = firebaseAuth,
                        modifier = Modifier.testTag("ProfileScreen_Card_GoogleSignIn"),
                        onAuthSuccess = {
                            currentUser = firebaseAuth.currentUser
                            if (currentUser != null) {
                                gameViewModel.loadUserDataFromFirebase()
                            }
                        }
                    )
                }

                // Appearance Section
                SectionHeader(icon = Icons.Rounded.Palette, title = "APPEARANCE")
                SettingsCard(
                    onClick = { navController.navigate("themeSettings") },
                    modifier = Modifier.testTag("ProfileScreen_Button_ThemeSettings")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta))),
                            contentAlignment = Alignment.Center
                        ) {}
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Current Theme",
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = currentTheme.name,
                                color = theme.primaryColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Mini preview
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.backgroundColor.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[2]!!, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[4]!!, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[16]!!, RoundedCornerShape(2.dp)))
                                }
                                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[8]!!, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[16]!!, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[32]!!, RoundedCornerShape(2.dp)))
                                }
                                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[64]!!, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[128]!!, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.tileColors[256]!!, RoundedCornerShape(2.dp)))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = theme.textColor.copy(alpha = 0.5f)
                        )
                    }
                }

                // Preferences Section
                SectionHeader(icon = Icons.Rounded.Settings, title = "PREFERENCES")
                SettingsCard(onClick = null) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = theme.primaryColor)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Sound", color = theme.textColor, fontWeight = FontWeight.Bold)
                                    Text("Enable or disable game sounds", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = soundEnabled, 
                                onCheckedChange = { gameViewModel.updateSoundEnabled(it) },
                                modifier = Modifier.testTag("ProfileScreen_Switch_Sound"),
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = theme.primaryColor)
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.textColor.copy(alpha = 0.1f)))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Vibration, null, tint = theme.primaryColor)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Vibration", color = theme.textColor, fontWeight = FontWeight.Bold)
                                    Text("Enable or disable vibration", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = vibrationEnabled, 
                                onCheckedChange = { gameViewModel.updateVibrationEnabled(it) },
                                modifier = Modifier.testTag("ProfileScreen_Switch_Vibration"),
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = theme.primaryColor)
                            )
                        }
                    }
                }

                // Support Section
                SectionHeader(icon = Icons.Rounded.Favorite, title = "SUPPORT")
                SettingsCard(onClick = null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SupportItem(
                            icon = Icons.Rounded.Star, 
                            label = "Rate App", 
                            sub = "Show your support",
                            modifier = Modifier.testTag("ProfileScreen_Button_RateApp"),
                            onClick = {
                                val packageName = context.packageName
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=$packageName")
                                    setPackage("com.android.vending")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                                    context.startActivity(webIntent)
                                }
                            }
                        )
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.textColor.copy(alpha = 0.1f)))
                        SupportItem(
                            icon = Icons.Rounded.Share, 
                            label = "Share App", 
                            sub = "Tell your friends",
                            modifier = Modifier.testTag("ProfileScreen_Button_ShareApp"),
                            onClick = {
                                val packageName = context.packageName
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "2048 Neon Rush")
                                    putExtra(Intent.EXTRA_TEXT, "Hey, check out this awesome, glowing 2048 game: https://play.google.com/store/apps/details?id=$packageName")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                            }
                        )
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(theme.textColor.copy(alpha = 0.1f)))
                        SupportItem(
                            icon = Icons.Rounded.PrivacyTip, 
                            label = "Privacy Policy", 
                            sub = "Read our policy",
                            modifier = Modifier.testTag("ProfileScreen_Button_PrivacyPolicy"),
                            onClick = {
                                val privacyUrl = "https://amanroy3901.github.io/privacy/privacy_neon_rush_2048.html"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                // About Section
                SectionHeader(icon = Icons.Rounded.Info, title = "ABOUT")
                SettingsCard(onClick = null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.backgroundColor)
                                .border(1.dp, theme.primaryColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "2048",
                                    color = theme.primaryColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontStyle = FontStyle.Italic
                                )
                                Text(
                                    text = "NEON RUSH",
                                    color = theme.secondaryColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("2048 Neon Rush", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Version $versionName", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Made with ❤️ by Sayne Design", color = theme.textColor.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showNameDialog) {
        NameEditDialog(
            currentName = playerName,
            onNameChange = { newName ->
                gameViewModel.updatePlayerName(newName)
            },
            onDismiss = { showNameDialog = false }
        )
    }
}

@Composable
private fun ProfileScreenLandscape(
    navController: NavController,
    playerName: String,
    persistentHighScore: Int,
    currentTheme: GameTheme,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    versionName: String,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    firebaseAuth: com.google.firebase.auth.FirebaseAuth,
    onEditNameClick: () -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit,
    onSignOutClick: () -> Unit,
    onAuthSuccess: () -> Unit,
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    onPrivacyPolicy: () -> Unit
) {
    val theme = LocalGameTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollStateRight = rememberScrollState()
    val density = LocalDensity.current

    val credentialManager = remember { CredentialManager.create(context) }
    val googleIdOption = remember {
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("305513134474-v65v4qb636fnkhvrbl7k9m9tpu07ap5v.apps.googleusercontent.com")
            .build()
    }
    val request = remember {
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .safeDrawingPadding()
    ) {
        val screenW = maxWidth
        val screenH = maxHeight

        val leftColWidth = screenW * 0.28f
        val rightColWidth = screenW * 0.68f

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = screenW * 0.02f, vertical = screenH * 0.03f),
            horizontalArrangement = Arrangement.spacedBy(screenW * 0.02f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column
            Column(
                modifier = Modifier
                    .width(leftColWidth)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(screenH * 0.02f)
            ) {
                // Header (Back button + logo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenH * 0.12f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SquareIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = { navController.popBackStack() },
                        size = screenH * 0.09f
                    )
                    Spacer(modifier = Modifier.width(screenW * 0.015f))
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "2048",
                            fontSize = with(density) { (screenH * 0.06f).toSp() },
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            color = Color.Transparent,
                            style = LocalTextStyle.current.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(theme.primaryColor, theme.secondaryColor)
                                )
                            ),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "N E O N   R U S H",
                            color = theme.primaryColor,
                            fontSize = with(density) { (screenH * 0.020f).toSp() },
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Profile Card
                NeonCard(
                    accentColor = theme.primaryColor,
                    isSelected = true,
                    onClick = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenH * 0.18f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = screenH * 0.02f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(screenH * 0.12f)
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(theme.primaryColor, theme.secondaryColor))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(screenH * 0.07f)
                            )
                        }
                        Spacer(modifier = Modifier.width(screenW * 0.015f))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = playerName,
                                    color = theme.textColor,
                                    fontSize = with(density) { (screenH * 0.04f).toSp() },
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(screenW * 0.005f))
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit Name",
                                    tint = theme.primaryColor,
                                    modifier = Modifier
                                        .size(screenH * 0.035f)
                                        .clickable { onEditNameClick() }
                                )
                            }
                            Text(
                                text = "Neon Master",
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = with(density) { (screenH * 0.025f).toSp() }
                            )
                        }
                        Spacer(modifier = Modifier.width(screenW * 0.005f))
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size(screenH * 0.05f)
                            )
                            Spacer(modifier = Modifier.height(screenH * 0.005f))
                            Text(
                                text = persistentHighScore.toString(),
                                color = theme.primaryColor,
                                fontSize = with(density) { (screenH * 0.045f).toSp() },
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Theme Preview Card
                NeonCard(
                    accentColor = theme.primaryColor,
                    isSelected = true,
                    onClick = { navController.navigate("themeSettings") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenH * 0.38f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding((screenH.value * 0.02f).dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "THEME PREVIEW",
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = (screenH.value * 0.024f).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // 4x4 Grid preview of standard neon tiles
                        val previewValues = listOf(
                            listOf(2, 4, 8, 16),
                            listOf(8, 16, 32, 64),
                            listOf(128, 256, 512, 1024),
                            listOf(2048, 0, 0, 0)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((screenH.value * 0.20f).dp),
                            verticalArrangement = Arrangement.spacedBy((screenH.value * 0.005f).dp)
                        ) {
                            for (row in previewValues) {
                                Row(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy((screenH.value * 0.005f).dp)
                                ) {
                                    for (value in row) {
                                        val tileBg = if (value > 0) theme.tileColors[value] ?: theme.primaryColor else theme.textColor.copy(alpha = 0.05f)
                                        val tileBorderColor = if (value > 0) theme.primaryColor.copy(alpha = 0.3f) else theme.textColor.copy(alpha = 0.1f)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape((screenH.value * 0.01f).dp))
                                                .background(tileBg)
                                                .border(1.dp, tileBorderColor, RoundedCornerShape((screenH.value * 0.01f).dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (value > 0) {
                                                val tileTextColor = if (theme.isDark) Color.White else theme.textColor
                                                Text(
                                                    text = value.toString(),
                                                    color = tileTextColor,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = (screenH.value * 0.026f).sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size((screenH.value * 0.04f).dp)
                            )
                            Spacer(modifier = Modifier.width((screenW.value * 0.01f).dp))
                            Text(
                                text = currentTheme.name,
                                color = theme.primaryColor,
                                fontSize = (screenH.value * 0.028f).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = theme.textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size((screenH.value * 0.035f).dp)
                            )
                        }
                    }
                }

                // Game Resources Card
                NeonCard(
                    accentColor = theme.primaryColor,
                    isSelected = true,
                    onClick = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenH * 0.20f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding((screenH.value * 0.018f).dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GAME RESOURCES",
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = (screenH.value * 0.022f).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size((screenH.value * 0.038f).dp)
                            )
                            Spacer(modifier = Modifier.width((screenW.value * 0.01f).dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Undo Remaining",
                                    color = theme.textColor,
                                    fontSize = (screenH.value * 0.026f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Use to undo your last move",
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    fontSize = (screenH.value * 0.020f).sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape((screenH.value * 0.015f).dp))
                                    .background(theme.primaryColor.copy(alpha = 0.15f))
                                    .border(1.dp, theme.primaryColor.copy(alpha = 0.4f), RoundedCornerShape((screenH.value * 0.015f).dp))
                                    .padding(horizontal = (screenH.value * 0.025f).dp, vertical = (screenH.value * 0.005f).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "3",
                                    color = theme.primaryColor,
                                    fontSize = (screenH.value * 0.028f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size((screenH.value * 0.038f).dp)
                            )
                            Spacer(modifier = Modifier.width((screenW.value * 0.01f).dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hints Remaining",
                                    color = theme.textColor,
                                    fontSize = (screenH.value * 0.026f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Use hints to find the best moves",
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    fontSize = (screenH.value * 0.020f).sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape((screenH.value * 0.015f).dp))
                                    .background(theme.primaryColor.copy(alpha = 0.15f))
                                    .border(1.dp, theme.primaryColor.copy(alpha = 0.4f), RoundedCornerShape((screenH.value * 0.015f).dp))
                                    .padding(horizontal = (screenH.value * 0.025f).dp, vertical = (screenH.value * 0.005f).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "5",
                                    color = theme.primaryColor,
                                    fontSize = (screenH.value * 0.028f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Divider Line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, theme.primaryColor.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )

            // Right Column
            Column(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
                    .verticalScroll(scrollStateRight),
                verticalArrangement = Arrangement.spacedBy((screenH.value * 0.02f).dp)
            ) {
                // ACCOUNT Group
                LandscapeSectionHeader(icon = Icons.Rounded.Person, title = "ACCOUNT", screenH = screenH)
                LandscapeSettingsCard(accentColor = theme.primaryColor) {
                    if (currentUser == null) {
                        SettingsRow(
                            iconPainter = painterResource(id = R.drawable.ic_google),
                            title = "Sign in with Google",
                            subtitle = "Sync your progress across devices",
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val result = credentialManager.getCredential(
                                            context = context,
                                            request = request
                                        )
                                        handleSignInResult(
                                            result = result,
                                            firebaseAuth = firebaseAuth,
                                            onSuccess = {
                                                onAuthSuccess()
                                            },
                                            onError = { error ->
                                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Log.e("GoogleAuth", "Google sign in failed: ${e.message}", e)
                                    }
                                }
                            },
                            screenH = screenH
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.textColor.copy(alpha = 0.08f)))
                        SettingsRow(
                            iconVector = Icons.Rounded.AccountCircle,
                            title = "Continue as Guest",
                            subtitle = "Play without an account",
                            onClick = { onAuthSuccess() },
                            screenH = screenH
                        )
                    } else {
                        SettingsRow(
                            iconVector = Icons.Rounded.AccountCircle,
                            title = currentUser.displayName ?: currentUser.email ?: "Guest User",
                            subtitle = "Signed In • Tap to sign out",
                            onClick = { onSignOutClick() },
                            screenH = screenH
                        )
                    }
                }

                // APPEARANCE Group
                LandscapeSectionHeader(icon = Icons.Rounded.Palette, title = "APPEARANCE", screenH = screenH)
                LandscapeSettingsCard(accentColor = theme.primaryColor) {
                    SettingsRow(
                        iconVector = Icons.Rounded.Palette,
                        title = "Theme",
                        subtitle = "Customize the look and feel of the game",
                        onClick = { navController.navigate("themeSettings") },
                        control = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentTheme.name,
                                    color = theme.primaryColor,
                                    fontSize = (screenH.value * 0.026f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width((screenW.value * 0.005f).dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = theme.textColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size((screenH.value * 0.035f).dp)
                                )
                            }
                        },
                        screenH = screenH
                    )
                }

                // PREFERENCES Group
                LandscapeSectionHeader(icon = Icons.Rounded.Settings, title = "PREFERENCES", screenH = screenH)
                LandscapeSettingsCard(accentColor = theme.primaryColor) {
                    SettingsRow(
                        iconVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        title = "Sound",
                        subtitle = "Enable or disable game sounds",
                        onClick = null,
                        showChevron = false,
                        control = {
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = onSoundToggle,
                                modifier = Modifier.scale(0.85f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = theme.primaryColor
                                )
                            )
                        },
                        screenH = screenH
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.textColor.copy(alpha = 0.08f)))
                    SettingsRow(
                        iconVector = Icons.Rounded.Vibration,
                        title = "Vibration",
                        subtitle = "Enable or disable vibration",
                        onClick = null,
                        showChevron = false,
                        control = {
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = onVibrationToggle,
                                modifier = Modifier.scale(0.85f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = theme.primaryColor
                                )
                            )
                        },
                        screenH = screenH
                    )
                }

                // SUPPORT Group
                LandscapeSectionHeader(icon = Icons.Rounded.Favorite, title = "SUPPORT", screenH = screenH)
                LandscapeSettingsCard(accentColor = theme.primaryColor) {
                    SettingsRow(
                        iconVector = Icons.Rounded.Star,
                        title = "Rate App",
                        subtitle = "Enjoying the game? Rate us on the Play Store",
                        onClick = onRateApp,
                        screenH = screenH
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.textColor.copy(alpha = 0.08f)))
                    SettingsRow(
                        iconVector = Icons.Rounded.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "Read our privacy policy",
                        onClick = onPrivacyPolicy,
                        screenH = screenH
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.textColor.copy(alpha = 0.08f)))
                    SettingsRow(
                        iconVector = Icons.Rounded.Info,
                        title = "About",
                        subtitle = "Version $versionName • Made with ❤️ by Sayne Design",
                        onClick = null,
                        showChevron = false,
                        screenH = screenH
                    )
                }

                // Version text footer at the bottom
                Spacer(modifier = Modifier.height((screenH.value * 0.02f).dp))
                Text(
                    text = "App Version $versionName",
                    color = theme.textColor.copy(alpha = 0.4f),
                    fontSize = (screenH.value * 0.022f).sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LandscapeSectionHeader(
    icon: ImageVector,
    title: String,
    screenH: Dp
) {
    val theme = LocalGameTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size((screenH.value * 0.05f).dp)
                .clip(CircleShape)
                .background(theme.primaryColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.primaryColor,
                modifier = Modifier.size((screenH.value * 0.03f).dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = theme.textColor.copy(alpha = 0.8f),
            fontSize = (screenH.value * 0.024f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun LandscapeSettingsCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalGameTheme.current
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(theme.surfaceColor.copy(alpha = 0.85f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), cardShape)
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    iconVector: ImageVector? = null,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
    control: (@Composable () -> Unit)? = null,
    screenH: Dp
) {
    val theme = LocalGameTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = (screenH.value * 0.015f).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((screenH.value * 0.06f).dp)
                .clip(CircleShape)
                .background(theme.primaryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size((screenH.value * 0.035f).dp)
                )
            } else if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = theme.primaryColor,
                    modifier = Modifier.size((screenH.value * 0.035f).dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = theme.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = (screenH.value * 0.026f).sp
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = theme.textColor.copy(alpha = 0.5f),
                    fontSize = (screenH.value * 0.020f).sp
                )
            }
        }
        if (control != null) {
            control()
        } else if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = theme.textColor.copy(alpha = 0.3f),
                modifier = Modifier.size((screenH.value * 0.035f).dp)
            )
        }
    }
}


