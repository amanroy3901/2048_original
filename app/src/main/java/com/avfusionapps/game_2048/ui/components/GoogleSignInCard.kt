package com.avfusionapps.game_2048.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.screens.handleSignInResult
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInCard(
    firebaseAuth: FirebaseAuth,
    modifier: Modifier = Modifier,
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val theme = LocalGameTheme.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = firebaseAuth.currentUser
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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

    NeonCard(
        accentColor = theme.primaryColor,
        isSelected = true,
        onClick = null,
        cornerRadius = 24.dp,
        borderWidth = 1.5.dp,
        modifier = modifier.fillMaxWidth().testTag("GoogleSignInCard_Root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentUser == null) {
                Text(
                    text = stringResource(R.string.sign_in_sync_progress),
                    color = theme.textColor.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = ""

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
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onError = { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                } catch (_: GetCredentialCancellationException) {
                                    isLoading = false
                                } catch (e: GetCredentialException) {
                                    isLoading = false
                                    errorMessage = context.getString(
                                        R.string.google_sign_in_failed,
                                        e.message ?: context.getString(R.string.google_sign_in_config_hint)
                                    )
                                    Log.e("GoogleAuth", "GetCredentialException: ${e.message}", e)
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = context.getString(
                                        R.string.unexpected_error,
                                        e.message ?: ""
                                    )
                                    Log.e("GoogleAuth", "Unexpected error: ${e.message}", e)
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f).height(48.dp).testTag("GoogleSignInCard_Button_GoogleSignIn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F1F1F)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (isLoading) "Signing in..." else "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onAuthSuccess,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("GoogleSignInCard_Button_ContinueAsGuest"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = theme.textColor.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, theme.textColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = stringResource(R.string.continue_as_guest),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = theme.primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(
                                R.string.signed_in_as,
                                currentUser.displayName ?: currentUser.email ?: currentUser.uid
                            ),
                            color = theme.textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.sync_enabled),
                            color = theme.textColor.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
