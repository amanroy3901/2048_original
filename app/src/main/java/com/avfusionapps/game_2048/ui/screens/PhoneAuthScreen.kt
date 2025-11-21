package com.avfusionapps.game_2048.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
fun PhoneAuthScreen(
    navController: NavController,
    firebaseAuth: FirebaseAuth,
    onAuthSuccess: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var storedVerificationId by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Phone Authentication",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        if (!isCodeSent) {
            // Phone number input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                placeholder = { Text("+12345678901") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (phoneNumber.isNotBlank() && phoneNumber.length >= 10) {
                        errorMessage = ""
                        isLoading = true
                        startPhoneNumberVerification(
                            phoneNumber = phoneNumber,
                            firebaseAuth = firebaseAuth,
                            onCodeSent = { verificationId ->
                                storedVerificationId = verificationId
                                isCodeSent = true
                                isLoading = false
                            },
                            onVerificationCompleted = {
                                onAuthSuccess()
                            },
                            onError = { error ->
                                errorMessage = error
                                isLoading = false
                            }
                        )
                    } else {
                        errorMessage = "Please enter a valid phone number"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Send Verification Code")
                }
            }
        } else {
            // Verification code input
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { verificationCode = it },
                label = { Text("Verification Code") },
                placeholder = { Text("123456") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (verificationCode.isNotBlank() && verificationCode.length >= 6) {
                        errorMessage = ""
                        isLoading = true
                        verifyVerificationCode(
                            verificationCode = verificationCode,
                            verificationId = storedVerificationId,
                            firebaseAuth = firebaseAuth,
                            onSuccess = {
                                onAuthSuccess()
                            },
                            onError = { error ->
                                errorMessage = error
                                isLoading = false
                            }
                        )
                    } else {
                        errorMessage = "Please enter a valid verification code"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verify Code")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = {
                    isCodeSent = false
                    verificationCode = ""
                    storedVerificationId = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Phone Number")
            }
        }
        
        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Instructions for Firebase setup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Authentication Required",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Enter your phone number to continue",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "You'll receive a verification code via SMS",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        // Skip button removed - proper authentication required
    }
}

private fun startPhoneNumberVerification(
    phoneNumber: String,
    firebaseAuth: FirebaseAuth,
    onCodeSent: (String) -> Unit,
    onVerificationCompleted: () -> Unit,
    onError: (String) -> Unit
) {
    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification or instant verification
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onVerificationCompleted()
                    } else {
                        onError("Authentication failed: ${task.exception?.message}")
                    }
                }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            when {
                e.message?.contains("BILLING_NOT_ENABLED") == true -> {
                    onError("Firebase billing not enabled. Add test phone numbers in Firebase Console.")
                }
                e.message?.contains("CONFIGURATION_NOT_FOUND") == true -> {
                    onError("Phone authentication not configured. Enable it in Firebase Console.")
                }
                else -> {
                    onError("Verification failed: ${e.message}")
                }
            }
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            onCodeSent(verificationId)
        }
    }

    val options = PhoneAuthOptions.newBuilder(firebaseAuth)
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setCallbacks(callbacks)
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}

private fun verifyVerificationCode(
    verificationCode: String,
    verificationId: String?,
    firebaseAuth: FirebaseAuth,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (verificationId == null) {
        onError("No verification ID available")
        return
    }

    val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)
    firebaseAuth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onError("Invalid verification code: ${task.exception?.message}")
            }
        }
}