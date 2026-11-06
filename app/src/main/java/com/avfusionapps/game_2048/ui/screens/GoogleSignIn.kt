package com.avfusionapps.game_2048.ui.screens

import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


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

