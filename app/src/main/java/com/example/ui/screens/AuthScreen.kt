package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.repository.LudoFirebaseRepository
import com.example.repository.UserPreferences
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.auth.auth
import androidx.compose.ui.window.Dialog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun resolveServerClientId(context: Context): String? {
    val prefs = UserPreferences(context)
    if (prefs.customWebClientId.isNotBlank()) {
        return prefs.customWebClientId.trim()
    }
    return try {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resourceId != 0) {
            val id = context.getString(resourceId).trim()
            if (id.isNotBlank()) id else null
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveServerClientId(context) ?: run {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = resolveServerClientId(context)
    if (clientId == null) {
        onAuthError("Google Sign-In client ID is not configured yet. Please configure it via ⚙️ or use the 'Email / Gamer ID' tab to register & play immediately!")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            val msg = e.localizedMessage ?: "Sign in failed"
            if (msg.contains("16:") || msg.contains("10:") || msg.contains("Developer")) {
                onAuthError("Google Sign-In: Web Client ID mismatch in Firebase. Please use 'Email / Gamer ID' to register and play, or tap ⚙️ to set your Web Client ID.")
            } else {
                onAuthError(msg)
            }
        }
    }
}

fun signOutGoogle(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

enum class AuthTab(val title: String, val icon: String) {
    GOOGLE("Google Cloud", "👑"),
    GAMER_ACCOUNT("Email / Pass", "✉️"),
    PHONE("Phone SMS", "📱"),
    GUEST("Guest Play", "⚡")
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onGuestOrCustomLogin: (username: String, avatarId: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val userPrefs = remember { UserPreferences(context) }

    var selectedTab by remember { mutableStateOf(AuthTab.GOOGLE) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Gamer ID / Email state
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailOrUsername by remember { mutableStateOf(userPrefs.userEmail.ifEmpty { userPrefs.username }) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var customNickname by remember { mutableStateOf(userPrefs.username) }
    var selectedAvatar by remember { mutableStateOf(userPrefs.avatarId) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var isSendingResetEmail by remember { mutableStateOf(false) }
    var showEmailSentDialog by remember { mutableStateOf(false) }
    var sentVerificationEmailAddress by remember { mutableStateOf("") }
    var showClientIdConfigDialog by remember { mutableStateOf(false) }
    var inputCustomClientId by remember { mutableStateOf(userPrefs.customWebClientId) }
    var showResendVerificationButton by remember { mutableStateOf(false) }
    var isResendingVerification by remember { mutableStateOf(false) }

    // Phone SMS Auth state
    var phoneNumber by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var smsOtpCode by remember { mutableStateOf("") }
    var isSmsOtpSent by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }

    // Guest Play state
    var guestName by remember { mutableStateOf("Warrior #${(100..999).random()}") }
    var guestAvatar by remember { mutableStateOf("avatar_crown") }

    val avatarOptions = listOf(
        Pair("avatar_crown", "👑"),
        Pair("avatar_lion", "🦁"),
        Pair("avatar_car", "🏎️"),
        Pair("avatar_dragon", "🐉"),
        Pair("avatar_unicorn", "🦄"),
        Pair("avatar_alien", "👽")
    )

    // Attempt auto sign-in once when screen loads
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            border = BorderStroke(1.5.dp, GoldPrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Royal Crown Logo & Title
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.15f))
                        .border(2.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 34.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ROYAL LUDO EMPIRE",
                    color = GoldPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Imperial Authentication Portal",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Navigation Tabs between Auth Methods
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassCardBorder)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AuthTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) GoldPrimary else Color.Transparent)
                                .clickable {
                                    selectedTab = tab
                                    errorMessage = null
                                    successMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.icon, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) DeepDarkBg else TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Error / Success feedback banners
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LudoRed.copy(alpha = 0.2f))
                            .border(1.dp, LudoRed, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = LudoRed,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (successMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LudoGreen.copy(alpha = 0.2f))
                            .border(1.dp, LudoGreen, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = successMessage ?: "",
                            color = LudoGreen,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // TAB CONTENT
                when (selectedTab) {
                    AuthTab.GOOGLE -> {
                        // 1. Official Google Sign-In
                        Text(
                            text = "Connect with your Google account to automatically back up your wallet, participate in live leaderboards, and sync rewards across devices.",
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { errorMsg ->
                                        isLoading = false
                                        errorMessage = errorMsg
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = DeepDarkBg
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = DeepDarkBg,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("G ", fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Sign in with Google",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⚡ Instant access?", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Try Guest Play",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { selectedTab = AuthTab.GUEST }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚙️ Configure Web Client ID (Optional)",
                                color = TextGray.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                modifier = Modifier.clickable { showClientIdConfigDialog = true }
                            )
                        }
                    }

                    AuthTab.GAMER_ACCOUNT -> {
                        // 2. Email or Custom Gamer Account (Signup & Login)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { isSignUpMode = false; errorMessage = null },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (!isSignUpMode) GoldPrimary else TextGray
                                )
                            ) {
                                Text(
                                    "Log In",
                                    fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                            Text(" | ", color = GlassCardBorder, modifier = Modifier.align(Alignment.CenterVertically))
                            TextButton(
                                onClick = { isSignUpMode = true; errorMessage = null },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (isSignUpMode) GoldPrimary else TextGray
                                )
                            ) {
                                Text(
                                    "Create Account",
                                    fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Avatar Selector for Registration
                        if (isSignUpMode) {
                            Text(
                                text = "Select Royal Avatar:",
                                color = TextWhite,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                avatarOptions.forEach { (id, emoji) ->
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (selectedAvatar == id) GoldPrimary else GlassCardBorder)
                                            .clickable { selectedAvatar = id }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Email Input
                        OutlinedTextField(
                            value = emailOrUsername,
                            onValueChange = { emailOrUsername = it },
                            label = { Text(if (isSignUpMode) "Email Address (for verification)" else "Email Address", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Nickname input during signup
                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = customNickname,
                                onValueChange = { if (it.length <= 16) customNickname = it },
                                label = { Text("Warrior Nickname", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedLabelColor = NeonCyan,
                                    unfocusedLabelColor = TextGray
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Password Input
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(if (isSignUpMode) "Create Password (min 6 chars)" else "Password", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = TextGray
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        // Confirm Password (Only in Signup Mode)
                        if (isSignUpMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password", fontSize = 12.sp) },
                                singleLine = true,
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle confirm password",
                                            tint = TextGray
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedLabelColor = NeonCyan,
                                    unfocusedLabelColor = TextGray
                                )
                            )
                        }

                        // Forgot Password Link (for Login mode)
                        if (!isSignUpMode) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Forgot Password? ✉️",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        forgotPasswordEmail = emailOrUsername.trim()
                                        showForgotPasswordDialog = true
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val email = emailOrUsername.trim()
                                if (email.isBlank()) {
                                    errorMessage = "Please enter your email address"
                                    return@Button
                                }
                                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    errorMessage = "Please enter a valid email address (e.g. name@domain.com)"
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters"
                                    return@Button
                                }
                                if (isSignUpMode && password != confirmPassword) {
                                    errorMessage = "Passwords do not match! Please check your confirm password."
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                successMessage = null

                                coroutineScope.launch {
                                    if (isSignUpMode) {
                                        // 1. REAL FIREBASE EMAIL SIGN UP + VERIFICATION LINK
                                        try {
                                            val authResult = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
                                            val user = authResult.user
                                            try {
                                                user?.sendEmailVerification()?.await()
                                            } catch (e: Exception) {
                                                Log.w("Auth", "Verification email send warning", e)
                                            }

                                            val finalName = if (customNickname.isNotBlank()) customNickname.trim() else email.substringBefore("@")
                                            userPrefs.registerGamerAccount(email, finalName, selectedAvatar)

                                            // Sign out immediately until email is verified
                                            Firebase.auth.signOut()

                                            sentVerificationEmailAddress = email
                                            showEmailSentDialog = true
                                            isSignUpMode = false // Switch to log in tab
                                            password = ""
                                            confirmPassword = ""
                                            successMessage = "✉️ Verification email sent to $email! Please verify and then log in."
                                        } catch (e: FirebaseAuthUserCollisionException) {
                                            errorMessage = "⚠️ Account already exists with this email! Please Log In with your password."
                                            isSignUpMode = false // Switch to Log In mode
                                        } catch (e: FirebaseAuthWeakPasswordException) {
                                            errorMessage = "⚠️ Password too weak! Please use at least 6 characters."
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Registration failed. Check internet."
                                        } finally {
                                            isLoading = false
                                        }
                                    } else {
                                        // 2. REAL FIREBASE EMAIL SIGN IN
                                        try {
                                            val authResult = Firebase.auth.signInWithEmailAndPassword(email, password).await()
                                            val user = authResult.user
                                            if (user != null) {
                                                try {
                                                    user.reload().await()
                                                } catch (e: Exception) { }

                                                if (!user.isEmailVerified) {
                                                    errorMessage = "⚠️ Email not verified yet! Please check your email inbox (and Spam folder) to verify $email before logging in."
                                                    showResendVerificationButton = true
                                                    Firebase.auth.signOut()
                                                    return@launch
                                                }

                                                val finalName = userPrefs.username.ifBlank { email.substringBefore("@") }
                                                val repo = LudoFirebaseRepository(context)
                                                try {
                                                    repo.saveOrInitUserProfile(username = finalName, avatarId = selectedAvatar)
                                                } catch (e: Exception) { }

                                                successMessage = "Welcome back, $finalName!"
                                                onAuthSuccess()
                                            }
                                        } catch (e: FirebaseAuthInvalidUserException) {
                                            errorMessage = "⚠️ No account found with this email. Click 'Create Account' to register."
                                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                                            errorMessage = "⚠️ Incorrect password! Please enter the password you created, or tap 'Forgot Password?'."
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Login failed. Check your password."
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSignUpMode) NeonCyan else GoldPrimary,
                                contentColor = DeepDarkBg
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DeepDarkBg, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isSignUpMode) "CREATE ACCOUNT & SEND VERIFICATION" else "LOG IN TO EMPIRE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Resend Verification Email Button
                        if (showResendVerificationButton && !isSignUpMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val email = emailOrUsername.trim()
                                    if (email.isBlank() || password.length < 6) {
                                        errorMessage = "Please enter your email and password above to resend verification email."
                                        return@OutlinedButton
                                    }
                                    isResendingVerification = true
                                    coroutineScope.launch {
                                        try {
                                            val res = Firebase.auth.signInWithEmailAndPassword(email, password).await()
                                            res.user?.sendEmailVerification()?.await()
                                            Firebase.auth.signOut()
                                            successMessage = "✉️ Verification email resent to $email! Check inbox & spam folder."
                                            errorMessage = null
                                            showResendVerificationButton = false
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to resend: ${e.localizedMessage}"
                                        } finally {
                                            isResendingVerification = false
                                        }
                                    }
                                },
                                enabled = !isResendingVerification,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                if (isResendingVerification) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                                } else {
                                    Text("✉️ Resend Verification Email Link", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    AuthTab.PHONE -> {
                        Text(
                            text = "Log in instantly using your mobile number and one-time SMS verification code.",
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isSmsOtpSent) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number (+91...)", fontSize = 12.sp) },
                                placeholder = { Text("+91 9876543210", color = TextGray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedLabelColor = NeonCyan,
                                    unfocusedLabelColor = TextGray
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val phone = phoneNumber.trim()
                                    if (phone.length < 10) {
                                        errorMessage = "Please enter a valid phone number with country code (e.g. +919876543210)"
                                        return@Button
                                    }
                                    isSendingOtp = true
                                    errorMessage = null
                                    successMessage = null

                                    try {
                                        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                                coroutineScope.launch {
                                                    try {
                                                        val authResult = Firebase.auth.signInWithCredential(credential).await()
                                                        val user = authResult.user
                                                        if (user != null) {
                                                            val repo = LudoFirebaseRepository(context)
                                                            repo.saveOrInitUserProfile(
                                                                username = user.phoneNumber ?: "Warrior",
                                                                avatarId = selectedAvatar
                                                            )
                                                            successMessage = "Phone verified successfully!"
                                                            onAuthSuccess()
                                                        }
                                                    } catch (e: Exception) {
                                                        errorMessage = e.localizedMessage ?: "Auto verification failed"
                                                    } finally {
                                                        isSendingOtp = false
                                                    }
                                                }
                                            }

                                            override fun onVerificationFailed(e: FirebaseException) {
                                                isSendingOtp = false
                                                errorMessage = e.localizedMessage ?: "Phone verification failed. Check phone format."
                                            }

                                            override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                                isSendingOtp = false
                                                verificationId = verId
                                                isSmsOtpSent = true
                                                successMessage = "6-digit OTP code sent via SMS! Please enter below."
                                            }
                                        }

                                        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
                                            .setPhoneNumber(phone)
                                            .setTimeout(60L, TimeUnit.SECONDS)
                                            .setActivity(context as Activity)
                                            .setCallbacks(callbacks)
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    } catch (e: Exception) {
                                        isSendingOtp = false
                                        errorMessage = e.localizedMessage ?: "Failed to send SMS code"
                                    }
                                },
                                enabled = !isSendingOtp && phoneNumber.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                if (isSendingOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepDarkBg, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sending SMS OTP...", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    Text("SEND 6-DIGIT OTP VIA SMS 📲", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "Enter 6-digit code sent to $phoneNumber:",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = smsOtpCode,
                                onValueChange = { if (it.length <= 6) smsOtpCode = it },
                                label = { Text("6-Digit OTP Code", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedLabelColor = NeonCyan,
                                    unfocusedLabelColor = TextGray
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val code = smsOtpCode.trim()
                                    val verId = verificationId
                                    if (code.length != 6 || verId == null) {
                                        errorMessage = "Please enter the 6-digit OTP code."
                                        return@Button
                                    }
                                    isVerifyingOtp = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            val credential = PhoneAuthProvider.getCredential(verId, code)
                                            val authResult = Firebase.auth.signInWithCredential(credential).await()
                                            val user = authResult.user
                                            if (user != null) {
                                                val repo = LudoFirebaseRepository(context)
                                                repo.saveOrInitUserProfile(
                                                    username = user.phoneNumber ?: "Warrior",
                                                    avatarId = selectedAvatar
                                                )
                                                successMessage = "Phone verified successfully! Welcome."
                                                onAuthSuccess()
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Invalid OTP code. Please try again."
                                        } finally {
                                            isVerifyingOtp = false
                                        }
                                    }
                                },
                                enabled = !isVerifyingOtp && smsOtpCode.length == 6,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                if (isVerifyingOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepDarkBg, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying OTP...", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    Text("VERIFY OTP & LOG IN 🚀", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    isSmsOtpSent = false
                                    smsOtpCode = ""
                                }
                            ) {
                                Text("Change Phone Number", color = TextGray, fontSize = 11.sp)
                            }
                        }
                    }

                    AuthTab.GUEST -> {
                        // 3. Fast Guest Play
                        Text(
                            text = "Jump directly into battle without signing up! Perfect for pass-and-play, offline tournaments, and AI skirmishes.",
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Choose Avatar
                        Text(
                            text = "Choose Guest Avatar:",
                            color = TextWhite,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            avatarOptions.forEach { (id, emoji) ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (guestAvatar == id) GoldPrimary else GlassCardBorder)
                                        .clickable { guestAvatar = id }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = guestName,
                            onValueChange = { if (it.length <= 15) guestName = it },
                            label = { Text("Guest Warrior Name", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val finalName = guestName.ifBlank { "Guest Warrior" }
                                userPrefs.username = finalName
                                userPrefs.avatarId = guestAvatar
                                userPrefs.isGuestAccount = true
                                onGuestOrCustomLogin(finalName, guestAvatar)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LudoGreen,
                                contentColor = DeepDarkBg
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PLAY AS GUEST NOW",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ✉️ FORGOT PASSWORD DIALOG
        if (showForgotPasswordDialog) {
            Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.5.dp, GoldPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f))
                                .border(1.5.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✉️", fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Reset Password via Email",
                            color = GoldPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter your registered email address. Firebase will send an official password reset link directly to your inbox.",
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = { forgotPasswordEmail = it },
                            label = { Text("Registered Email Address", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showForgotPasswordDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GlassCardBorder)
                            ) {
                                Text("CANCEL", color = TextGray, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val targetEmail = forgotPasswordEmail.trim()
                                    if (targetEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(targetEmail).matches()) {
                                        errorMessage = "Please enter a valid email address for password reset"
                                        return@Button
                                    }

                                    isSendingResetEmail = true
                                    coroutineScope.launch {
                                        try {
                                            Firebase.auth.sendPasswordResetEmail(targetEmail).await()
                                            successMessage = "✉️ Password reset link sent to $targetEmail! Check your inbox or spam folder."
                                            showForgotPasswordDialog = false
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Failed to send reset email. Verify your address."
                                        } finally {
                                            isSendingResetEmail = false
                                        }
                                    }
                                },
                                enabled = !isSendingResetEmail,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSendingResetEmail) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepDarkBg, strokeWidth = 2.dp)
                                } else {
                                    Text("SEND LINK", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 📧 VERIFICATION EMAIL SENT POPUP DIALOG
        if (showEmailSentDialog) {
            Dialog(onDismissRequest = { showEmailSentDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(2.dp, GoldPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .border(2.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✉️", fontSize = 28.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Verification Email Sent!",
                            color = GoldPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "An official activation link has been sent to:\n$sentVerificationEmailAddress",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "1. Open your Gmail / Email app.\n2. Check Inbox and Spam / Junk folder.\n3. Click the link to verify your account.\n4. Return here and Log In with your password to play!",
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Start,
                            lineHeight = 17.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                showEmailSentDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "OK, GO TO LOG IN",
                                color = DeepDarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ⚙️ GOOGLE WEB CLIENT ID CONFIGURATION DIALOG
        if (showClientIdConfigDialog) {
            Dialog(onDismissRequest = { showClientIdConfigDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    border = BorderStroke(1.5.dp, GoldPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚙️ Google Web Client ID",
                            color = GoldPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "If your Firebase project uses a custom OAuth Web Client ID, paste it below (e.g. 505688495811-xxx.apps.googleusercontent.com):",
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputCustomClientId,
                            onValueChange = { inputCustomClientId = it },
                            label = { Text("Web Client ID", fontSize = 11.sp) },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showClientIdConfigDialog = false },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, GlassCardBorder)
                            ) {
                                Text("CANCEL", color = TextGray, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    userPrefs.customWebClientId = inputCustomClientId.trim()
                                    showClientIdConfigDialog = false
                                    successMessage = "Web Client ID saved!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SAVE", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
