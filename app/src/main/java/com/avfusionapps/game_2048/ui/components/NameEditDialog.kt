package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun NameEditDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalGameTheme.current
    var nameInput by remember(currentName) { mutableStateOf(currentName) }
    // Derived state for error checking - if name exceeds 15 chars, it's an error
    val isError = nameInput.length > 15

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surfaceColor)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            theme.primaryColor.copy(alpha = 0.8f),
                            theme.primaryColor.copy(alpha = 0.2f),
                            theme.secondaryColor.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_player_name).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                theme.primaryColor,
                                theme.secondaryColor
                            )
                        )
                    ),
                    letterSpacing = 1.5.sp
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        if (it.length <= 25) {
                            nameInput = it
                        }
                    },
                    label = { Text(stringResource(R.string.enter_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("MainScreen_TextField_NameInput"),
                    shape = RoundedCornerShape(12.dp),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                text = stringResource(R.string.name_too_long),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor.copy(0.8f),
                        cursorColor = theme.primaryColor,
                        focusedBorderColor = theme.primaryColor,
                        unfocusedBorderColor = theme.primaryColor.copy(alpha = 0.4f),
                        focusedLabelColor = theme.primaryColor,
                        unfocusedLabelColor = theme.textColor.copy(alpha = 0.6f),
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        errorCursorColor = MaterialTheme.colorScheme.error,
                        errorTextColor = theme.textColor
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("MainScreen_Button_CancelNameEdit"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = theme.textColor.copy(alpha = 0.8f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.textColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NeonRoundedButton(
                            text = stringResource(R.string.save),
                            onClick = {
                                if (!isError && nameInput.isNotBlank()) {
                                    onNameChange(nameInput.trim())
                                    onDismiss()
                                }
                            },
                            enabled = !isError && nameInput.isNotBlank(),
                            buttonColor = theme.primaryColor,
                            glowColor = theme.primaryColor.copy(alpha = 0.6f),
                            cornerRadius = 24.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("MainScreen_Button_SaveName")
                        )
                    }
                }
            }
        }
    }
}
