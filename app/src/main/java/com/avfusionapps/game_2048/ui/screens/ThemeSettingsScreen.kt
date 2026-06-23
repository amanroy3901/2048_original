package com.avfusionapps.game_2048.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.testTag
import com.avfusionapps.game_2048.ui.theme.GameTheme
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.avfusionapps.game_2048.ui.components.GameCell
import com.avfusionapps.game_2048.viewmodel.ThemeViewModel

@Composable
fun ThemeSettingsScreen(
    navController: NavController,
    viewModel: ThemeViewModel = viewModel()
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    var selectedTheme by remember(currentTheme) { mutableStateOf(currentTheme) }

    // Use selectedTheme to style the preview UI
    val bgColor = selectedTheme.backgroundColor
    val surfaceColor = selectedTheme.surfaceColor
    val primaryColor = selectedTheme.primaryColor
    val secondaryColor = selectedTheme.secondaryColor
    val textColor = selectedTheme.textColor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ThemeSettingsScreen_Root")
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .safeDrawingPadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .testTag("ThemeSettingsScreen_Button_Back")
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "THEMES",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    style = LocalTextStyle.current.copy(
                        brush = Brush.horizontalGradient(
                            listOf(primaryColor, secondaryColor)
                        )
                    ),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Choose a theme to style your game",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Current Theme Preview Card
            ThemePreviewCard(theme = selectedTheme, isCurrent = selectedTheme.name == currentTheme.name)

            Spacer(modifier = Modifier.height(24.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.height(1.dp).width(40.dp).background(textColor.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "CHOOSE YOUR THEME",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.height(1.dp).width(40.dp).background(textColor.copy(alpha = 0.2f)))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of themes (using Column and Rows to measure height dynamically and avoid nested scroll limits)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val themes = GameTheme.allThemes()
                val chunkedThemes = themes.chunked(3)
                chunkedThemes.forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowThemes.forEach { themeOpt ->
                            Box(modifier = Modifier.weight(1f)) {
                                ThemeGridItem(
                                    theme = themeOpt,
                                    isSelected = themeOpt.name == selectedTheme.name,
                                    onClick = { selectedTheme = themeOpt },
                                    modifier = Modifier.testTag("ThemeSettingsScreen_Button_ThemeItem_${themeOpt.name}")
                                )
                            }
                        }
                        // Balance row if it has fewer than 3 items
                        if (rowThemes.size < 3) {
                            repeat(3 - rowThemes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Apply Button
        Button(
            onClick = {
                viewModel.setTheme(selectedTheme)
                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("ThemeSettingsScreen_Button_ApplyTheme")
                .shadow(
                    elevation = if (selectedTheme.isDark) 16.dp else 4.dp,
                    spotColor = primaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isLight = (selectedTheme.primaryColor.red * 0.2126f + selectedTheme.primaryColor.green * 0.7152f + selectedTheme.primaryColor.blue * 0.0722f) > 0.5f
                val contentColor = if (isLight) Color(0xFF1F1F1F) else Color.White
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "APPLY THEME",
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(theme: GameTheme, isCurrent: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.surfaceColor)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(listOf(theme.primaryColor, theme.secondaryColor)),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        if (isCurrent) {
            val isLight = (theme.primaryColor.red * 0.2126f + theme.primaryColor.green * 0.7152f + theme.primaryColor.blue * 0.0722f) > 0.5f
            val contentColor = if (isLight) Color(0xFF1F1F1F) else Color.White
            Box(
                modifier = Modifier
                    .offset(y = (-8).dp, x = (-8).dp)
                    .clip(RoundedCornerShape(50))
                    .background(theme.primaryColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CURRENT",
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = if (isCurrent) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview Grid
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.backgroundColor.copy(alpha = 0.5f))
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val sampleValues = listOf(
                        listOf(2, 4, 16, 2),
                        listOf(8, 16, 32, 8),
                        listOf(64, 128, 256, 64),
                        listOf(2, 4, 0, 0)
                    )
                    sampleValues.forEach { row ->
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { value ->
                                TilePreview(theme, value, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = theme.name.uppercase(),
                        color = theme.primaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getThemeDescription(theme.name),
                        color = theme.textColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureRow(Icons.Rounded.GridView, "Bold Colors", theme.primaryColor, theme.textColor)
                    FeatureRow(Icons.Rounded.Star, "Neon Glow", theme.primaryColor, theme.textColor)
                    FeatureRow(Icons.Rounded.RemoveRedEye, "Easy on the eyes", theme.primaryColor, theme.textColor)
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = textColor.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ThemeGridItem(
    theme: GameTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surfaceColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) theme.primaryColor else theme.textColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(6.dp)
        ) {
            if (isSelected) {
                val isLight = (theme.primaryColor.red * 0.2126f + theme.primaryColor.green * 0.7152f + theme.primaryColor.blue * 0.0722f) > 0.5f
                val contentColor = if (isLight) Color(0xFF1F1F1F) else Color.White
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val sampleValues = listOf(
                    listOf(2, 4, 16, 2),
                    listOf(8, 16, 32, 8),
                    listOf(64, 128, 256, 64),
                    listOf(2, 4, 0, 0)
                )
                sampleValues.forEach { row ->
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        row.forEach { value ->
                            TilePreview(theme, value, Modifier.weight(1f), 0.3f)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = theme.name.uppercase(),
            color = if (isSelected) theme.primaryColor else theme.textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TilePreview(theme: GameTheme, value: Int, modifier: Modifier = Modifier, fontSizeMultiplier: Float = 0.45f) {
    CompositionLocalProvider(LocalGameTheme provides theme) {
        GameCell(
            value = value,
            modifier = modifier.fillMaxHeight(),
            fontSizeMultiplier = fontSizeMultiplier
        )
    }
}

private fun getThemeDescription(name: String): String {
    return when (name) {
        "Neon Pink" -> "Vibrant, energetic and made to pop!"
        "Cyber Blue" -> "Sleek, futuristic, and cool to the touch."
        "Emerald" -> "Crisp, natural, and matrix-inspired."
        "Sunset" -> "Warm, inviting, and relaxing."
        "Royal Purple" -> "Luxurious, deep, and majestic."
        "Minimal White" -> "Clean, bright, and distraction-free."
        "Amoled Black" -> "Pure black. Perfect for OLED screens."
        "Ocean Teal" -> "Deep, aquatic, and refreshing."
        "Golden Hour" -> "Rich, glowing, and elegant."
        else -> "A beautiful theme for your game."
    }
}
