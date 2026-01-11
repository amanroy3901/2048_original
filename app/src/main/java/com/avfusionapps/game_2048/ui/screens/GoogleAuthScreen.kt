package com.avfusionapps.game_2048.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.ui.theme.PurpleDarkBackground
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@Composable
fun GoogleAuthScreen(
    firebaseAuth: FirebaseAuth,
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleDarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Game Logo",
            modifier = Modifier
                .height(140.dp)
                .width(400.dp)
                .scale(1.2f)
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Sign in to save your progress and play across devices",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f)
        )

        CylinderActionButton(
            modifier = Modifier
                .width(220.dp)
                .height(60.dp)
                .testTag("GoogleSignInButton"),
            text = if (isLoading) "Signing in..." else "Sign in with Google",
            onClick = {
                isLoading = true
                errorMessage = ""
                
                coroutineScope.launch {
                    try {
                        val result = credentialManager.getCredential(
                            context = context,
                            request = request
                        )
                        handleSignInResult(result, firebaseAuth, onAuthSuccess) { error ->
                            isLoading = false
                            errorMessage = error
                        }
                    } catch (e: GetCredentialCancellationException) {
                        isLoading = false
                        errorMessage = ""
                        android.util.Log.d("GoogleAuth", "Sign-in cancelled by user")
                    } catch (e: GetCredentialException) {
                        isLoading = false
                        errorMessage = "Google Sign-In failed: ${e.message ?: "Please check your Firebase configuration"}"
                        android.util.Log.e("GoogleAuth", "GetCredentialException: ${e.message}", e)
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Unexpected error: ${e.message}"
                        android.util.Log.e("GoogleAuth", "Unexpected error: ${e.message}", e)
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        CylinderActionButton(
            text = "Play as Guest",
            modifier = Modifier.testTag("PlayAsGuestButton").width(220.dp).height(60.dp),
            onClick = {
                onAuthSuccess()
            },
        )

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun handleSignInResult(
    result: GetCredentialResponse,
    firebaseAuth: FirebaseAuth,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Firebase authentication failed: ${authTask.exception?.message}")
                    }
                }
        } else {
            onError("Invalid credential type")
        }
    } catch (e: Exception) {
        onError("Sign-in failed: ${e.message}")
    }
}

// Keep the old function for compatibility but mark as deprecated
@Deprecated("Use handleSignInResult with Credential Manager instead")
fun handleGoogleSignInResult(
    task: com.google.android.gms.tasks.Task<Any>, // Changed from GoogleSignInAccount to Any
    firebaseAuth: FirebaseAuth,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // Implementation would go here but this is now deprecated
    onError("Old Google Sign-In API is deprecated. Please update your implementation.")
}