package com.ignitarium.game_2048.ui.screens

import android.content.res.Configuration
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ignitarium.game_2048.R
import com.ignitarium.game_2048.ui.theme.HighLighter
import com.ignitarium.game_2048.ui.theme._2048OriginalTheme
import com.ignitarium.game_2048.viewmodel.GameViewModel


@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkModePreview() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MainScreen(navController = previewNavController)
        }}
}

//If you want to have a dynamic color preview:
@Preview(showSystemUi = true, apiLevel = 31)
@Composable
fun DynamicDarkModePreview() {
    val previewNavController = rememberNavController()
    _2048OriginalTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MainScreen(navController = previewNavController)
        }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    val viewModel: GameViewModel = viewModel()
    var showNameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        // Top Row: Edit Name IconButton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(4.dp),
                        ambientColor = Color.White, // Shadow color
                        spotColor = Color.White      // Shadow color
                    )
                    .background(
                        color = Color(0xFFED0053), // Button background color (HighLighter)
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { showNameDialog = true }
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color.White)
            }
        }

        // Centered Content: Welcome Player and High Score
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Take remaining vertical space
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            PlayerWelcomeText(viewModel.gameState.playerName)

            Text(
                text = "High Score: ${viewModel.gameState.highScore}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Game " + "logo",
                tint = Color.White
            )
        }

        // Bottom Row: Cylinder Play Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 32.dp, end = 16.dp, top = 0.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            CylinderPlayButton(navController)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showNameDialog) {
        NameEditDialog(currentName = viewModel.gameState.playerName,
            onNameChange = { viewModel.updatePlayerName(it) },
            onDismiss = { showNameDialog = false })
    }
}

// ... (CylinderPlayButton and NameEditDialog remain the same)

@Composable
fun CylinderPlayButton(navController: NavController) {
    Button(
        onClick = { navController.navigate("game") },
        modifier = Modifier
            .width(200.dp)
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(HighLighter),
        shape = MaterialTheme.shapes.extraLarge, // Use an extra large shape for rounded ends
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play button icon",
                tint = Color.White
            )
            Text("Play Game", color = Color.White)
        }
    }
}

@Composable
fun NameEditDialog(
    currentName: String, onNameChange: (String) -> Unit, onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Player Name", style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            onNameChange(name)
                            onDismiss()
                        }
                    }) {
                        Text("Save")
                    }
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
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append(playerName)
            }
            withStyle(style = SpanStyle(color = Color.White)) {
                append("!")
            }
        },
        style = MaterialTheme.typography.headlineMedium,
    )
}
