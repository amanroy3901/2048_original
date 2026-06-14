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


