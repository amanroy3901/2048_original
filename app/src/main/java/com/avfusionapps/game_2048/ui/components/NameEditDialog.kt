package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import com.avfusionapps.game_2048.R
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
    val cancelEditDescription = stringResource(R.string.cancel_edit_name)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = theme.surfaceColor.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_player_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = theme.textColor
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
                    shape = RoundedCornerShape(8.dp),
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
                        unfocusedBorderColor = theme.primaryColor.copy(alpha = 0.6f),
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
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("MainScreen_Button_CancelNameEdit")
                    ) {
                        Text(stringResource(R.string.cancel), color = theme.textColor.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!isError && nameInput.isNotBlank()) {
                                onNameChange(nameInput.trim())
                                onDismiss()
                            }
                        },
                        enabled = !isError && nameInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryColor,
                            disabledContainerColor = theme.primaryColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("MainScreen_Button_SaveName")
                    ) {
                        Text(stringResource(R.string.save), color = Color.White)
                    }
                }
            }
        }
    }
}
