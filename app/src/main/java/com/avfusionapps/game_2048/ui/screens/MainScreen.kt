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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.* // Import everything needed from runtime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.avfusionapps.game_2048.data.GameSettingsRepository // *** Import Repository ***
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.theme.HighLighter
// import com.avfusionapps.game_2048.ui.theme.Purple80 // Keep if used
import com.avfusionapps.game_2048.ui.theme.PurpleDarkBackground
import com.avfusionapps.game_2048.ui.theme._2048OriginalTheme
import com.avfusionapps.game_2048.viewmodel.GameViewModel


@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkModePreview() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MainScreenContent(
                navController = previewNavController,
                playerName = "Preview Player",
                highScore = 2048,
                onEditNameClick = {},
                showNameDialog = false, // Preview doesn't show dialog initially
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

    var showNameDialog by remember { mutableStateOf(false) }
    // *** State to track if the initial name check has been done ***
    var initialNameCheckDone by remember { mutableStateOf(false) }

    // *** LaunchedEffect to check the name when it loads or changes ***
    LaunchedEffect(persistentPlayerName) {
        viewModel.enableNotification()
        // Check if the check hasn't been done AND the name is the default
        if (!initialNameCheckDone && persistentPlayerName == GameSettingsRepository.DEFAULT_PLAYER_NAME) {
            showNameDialog = true       // Show the dialog
            initialNameCheckDone = true // Mark the check as done for this session/load
        }
        // If the name is *not* default, ensure the flag is considered 'done' anyway
        // This handles cases where the name might be set before this screen is shown.
        else if (persistentPlayerName != GameSettingsRepository.DEFAULT_PLAYER_NAME) {
            initialNameCheckDone = true
        }
    }

    MainScreenContent(
        navController = navController,
        playerName = persistentPlayerName,
        highScore = persistentHighScore,
        onEditNameClick = { showNameDialog = true }, // Allow manual opening too
        showNameDialog = showNameDialog,
        onDismissDialog = { showNameDialog = false },
        onNameChange = { newName ->
            viewModel.updatePlayerName(newName)
            // Optional: If they save a valid name, ensure the check flag is set,
            // though it likely already is by the LaunchedEffect.
            if (newName.isNotBlank() && newName != GameSettingsRepository.DEFAULT_PLAYER_NAME) {
                initialNameCheckDone = true
            }
        }
    )
}

// MainScreenContent remains the same as before
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
            .background(PurpleDarkBackground) // Use theme background color
            .padding(16.dp), // Consistent padding
    ) {
        // Top Row: Edit Name IconButton
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 16.dp), // Add padding below the button row
            horizontalArrangement = Arrangement.End // Align button to the right
        ) {
            // Edit button with shadow and background
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .shadow( // Apply shadow for depth
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp), // Slightly more rounded
                        ambientColor = HighLighter.copy(alpha = 0.5f),
                        spotColor = HighLighter.copy(alpha = 0.5f)
                    )
                    .background(
                        color = HighLighter, // Use theme accent color for button background
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onEditNameClick) // Trigger action on click
                    .padding(10.dp) // Padding inside the button
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Player Name",
                    tint = Color.White // Icon color
                )
            }
        }

        // Centered Content: Welcome Player, High Score, and Logo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f), // Takes up remaining vertical space
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Center content vertically
        ) {
            PlayerWelcomeText(playerName) // Pass the potentially updated name

            Spacer(modifier = Modifier.height(16.dp)) // Space between welcome and high score

            Text(
                text = "High Score: $highScore",
                style = MaterialTheme.typography.titleLarge, // Style for high score
                color = Color.White // Text color
            )

            Spacer(modifier = Modifier.height(32.dp)) // Space before logo

            Image(
                painter = painterResource(id = R.drawable.ic_logo), // Ensure logo exists
                contentDescription = "Game Logo",
                modifier = Modifier
                    .height(140.dp) // Adjust logo size as needed
                    .width(400.dp) // Adjust logo size as needed
                    .scale(1.2f)
            )
        } // End Centered Content Column

        // Bottom Row: Cylinder Play Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp), // Padding at the bottom
            horizontalArrangement = Arrangement.Center // Center the play button
        ) {
            CylinderPlayButton(navController) // Navigate to game screen
        }
        Spacer(modifier = Modifier.weight(1f)) // Space before logo

    }

    // Show the Name Edit Dialog when state boolean is true
    if (showNameDialog) {
        NameEditDialog(
            currentName = playerName, // Initialize dialog with current persistent name
            onNameChange = onNameChange, // Pass lambda to handle save action
            onDismiss = onDismissDialog // Pass lambda to handle dismiss action
        )
    }
}


// Helper Composables (CylinderPlayButton, NameEditDialog, PlayerWelcomeText) remain the same
// ... (Keep the existing implementations of CylinderPlayButton, NameEditDialog, PlayerWelcomeText) ...
@Composable
fun CylinderPlayButton(navController: NavController) {
    Button(
        onClick = { navController.navigate("game") }, // Navigate to the game route
        modifier = Modifier
            .width(220.dp) // Slightly wider button
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HighLighter), // Use containerColor for M3
        shape = RoundedCornerShape(percent = 50), // Fully rounded ends (cylinder shape)
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp) // Adjust padding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased space
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
    // Local state for the text field within the dialog
    var nameInput by remember(currentName) { mutableStateOf(currentName) } // Reset if currentName changes

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp), // More rounded dialog
            color = PurpleDarkBackground.copy(alpha = 0.95f), // Slightly transparent background
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp), // Inner padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Player Name",
                    style = MaterialTheme.typography.headlineSmall, // Style title
                    color = Color.White
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it }, // Update local state
                    label = { Text("Enter name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), // Rounded text field
                    colors = OutlinedTextFieldDefaults.colors( // Customize colors for theme
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(0.8f),
                        cursorColor = HighLighter,
                        focusedBorderColor = HighLighter,
                        unfocusedBorderColor = HighLighter.copy(alpha = 0.6f),
                        focusedLabelColor = HighLighter,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.Transparent, // Optional: background
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                // Action Buttons (Cancel, Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End, // Align buttons to the right
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Use your custom button for Save
                    NeonRoundedButton(
                        onClick = {
                            // Only call onNameChange if the input is not blank
                            if (nameInput.isNotBlank()) {
                                onNameChange(nameInput.trim()) // Trim whitespace before saving
                            }
                            onDismiss() // Dismiss dialog regardless of save success
                        },
                        text = "Save"
                    )
                }
            }
        }
    }
}

// Font definition (ensure the font file is in res/font)
val lilitaOneFontFamily = FontFamily(Font(R.font.lilitaone_regular))

@Composable
fun PlayerWelcomeText(playerName: String) {
    Text(
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.White)) {
                append("Welcome ")
            }
            // Apply special style to the player name
            withStyle(
                style = MaterialTheme.typography.headlineLarge.toSpanStyle().copy(
                    color = HighLighter, // Use accent color for name
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = lilitaOneFontFamily // Apply custom font if desired
                )
            ) {
                append(playerName) // Append the dynamic player name
            }
            withStyle(style = SpanStyle(color = Color.White)) {
                append("!") // Exclamation mark
            }
        },
        style = MaterialTheme.typography.headlineMedium, // Base style for the text
        textAlign = TextAlign.Center // Center align the welcome text
    )
}