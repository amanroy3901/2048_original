package com.avfusionapps.game_2048.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.PurpleDarkBackground
import com.avfusionapps.game_2048.viewmodel.ThemeViewModel

@Composable
fun ThemeSettingsScreen(
    navController: NavController,
    viewModel: ThemeViewModel = viewModel()
) {
    val currentTheme by viewModel.currentTheme.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Choose Theme",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Neon Rush Theme Card
            ThemeCard(
                theme = GameTheme.NeonRush,
                isSelected = currentTheme is GameTheme.NeonRush,
                onClick = { viewModel.setTheme(GameTheme.NeonRush) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Classic 2048 Theme Card
            ThemeCard(
                theme = GameTheme.Classic2048,
                isSelected = currentTheme is GameTheme.Classic2048,
                onClick = { viewModel.setTheme(GameTheme.Classic2048) }
            )
        }
    }
}

@Composable
private fun ThemeCard(
    theme: GameTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) theme.accentColor else theme.surfaceColor
        ),
        border = if (isSelected) {
            BorderStroke(3.dp, HighLighter)
        } else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme preview with sample tiles
            ThemePreview(theme)

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.textColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isSelected) {
                    Text(
                        text = "Active Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (theme.isDark) HighLighter else theme.accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreview(theme: GameTheme) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surfaceColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TilePreview(theme, 2)
                TilePreview(theme, 4)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TilePreview(theme, 8)
                TilePreview(theme, 16)
            }
        }
    }
}

@Composable
private fun TilePreview(theme: GameTheme, value: Int) {
    val color = theme.tileColors[value] ?: theme.tileColors[0]!!
    val textColor = if (value <= 4) theme.textColor else Color.White

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
