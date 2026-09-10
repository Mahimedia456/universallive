package com.universallive.app.features.integration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.navigation.VerificationFlow
import com.universallive.app.theme.*
import kotlinx.coroutines.launch

@Composable
private fun ApiError(message: String?, onDismiss: () -> Unit) {
    if (message.isNullOrBlank()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppLive.copy(alpha = .10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLive.copy(alpha = .35f)),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(message, color = AppText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(
                "×",
                color = AppText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun ConnectedSignInScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    com.universallive.app.features.auth.AuthPageBridge(
        eyebrow = "Creator access",
        title = "Welcome back",
        body = "Sign in securely to your live Universal Live account.",
        onBack = { onNavigate(AppRoute.Welcome) },
    ) {
        ApiError(state.error, state::clearError)

        UlTextField(email, { email = it }, "Email", placeholder = "creator@example.com")
        Spacer(Modifier.height(12.dp))
        UlTextField(
            password,
            { password = it },
            "Password",
            visualTransformation = PasswordVisualTransformation(),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onNavigate(AppRoute.ForgotPassword) }) {
                Text("Forgot Password?", color = AppPrimary)
            }
        }

        UlPrimaryButton(
            "Sign In",
            onClick = {
                scope.launch {
                    if (state.signIn(email, password)) {
                        onNavigate(AppRoute.Main(AppDestination.Home))
                    }
                }
            },
            enabled = email.contains("@") && password.isNotBlank() && !state.loading,
            loading = state.loading,
        )

        Spacer(Modifier.height(14.dp))
        Text(
            "Connected to ${state.api.baseUrl}",
            color = AppTextMuted,
            fontSize = 10.sp,
        )

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("New to Universal Live? ", color = AppTextMuted)
            Text(
                "Create Account",
                color = AppPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigate(AppRoute.CreateAccount) },
            )
        }
    }
}

@Composable
fun ConnectedCreateAccountScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    com.universallive.app.features.auth.AuthPageBridge(
        eyebrow = "New creator",
        title = "Create your studio",
        body = "Create a real Universal Live account backed by Supabase Auth.",
        onBack = { onNavigate(AppRoute.Welcome) },
    ) {
        ApiError(state.error, state::clearError)
        UlTextField(name, { name = it }, "Full Name")
        Spacer(Modifier.height(10.dp))
        UlTextField(username, { username = it }, "Username", placeholder = "@creator")
        Spacer(Modifier.height(10.dp))
        UlTextField(email, { email = it }, "Email")
        Spacer(Modifier.height(10.dp))
        UlTextField(password, { password = it }, "Password", visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(10.dp))
        UlTextField(
            confirm,
            { confirm = it },
            "Confirm Password",
            visualTransformation = PasswordVisualTransformation(),
            isError = confirm.isNotBlank() && confirm != password,
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = AppPrimary,
                    checkmarkColor = AppText,
                ),
            )
            Text(
                "I agree to the Terms and Privacy Policy.",
                color = AppTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        UlPrimaryButton(
            "Create Account",
            onClick = {
                scope.launch {
                    if (state.signUp(name, username, email, password)) {
                        if (state.isAuthenticated) {
                            onNavigate(AppRoute.AccountCreatedSuccess)
                        } else {
                            onNavigate(AppRoute.VerifyEmail(VerificationFlow.AccountCreation))
                        }
                    }
                }
            },
            enabled =
                name.isNotBlank() &&
                username.isNotBlank() &&
                email.contains("@") &&
                password.length >= 8 &&
                password == confirm &&
                accepted &&
                !state.loading,
            loading = state.loading,
        )
    }
}

@Composable
fun ConnectedForgotPasswordScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    com.universallive.app.features.auth.AuthPageBridge(
        eyebrow = "Account recovery",
        title = "Reset your password",
        body = "We'll request a secure recovery code from the backend.",
        onBack = { onNavigate(AppRoute.SignIn) },
    ) {
        ApiError(state.error, state::clearError)
        UlTextField(email, { email = it }, "Email", placeholder = "creator@example.com")
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton(
            "Send Verification Code",
            onClick = {
                scope.launch {
                    if (state.requestPasswordReset(email)) {
                        onNavigate(AppRoute.VerifyEmail(VerificationFlow.PasswordRecovery))
                    }
                }
            },
            enabled = email.contains("@") && !state.loading,
            loading = state.loading,
        )
    }
}

@Composable
fun ConnectedVerifyEmailScreen(
    state: MobileIntegrationState,
    flow: VerificationFlow,
    onNavigate: (AppRoute) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    com.universallive.app.features.auth.AuthPageBridge(
        eyebrow = "Secure verification",
        title = if (flow == VerificationFlow.AccountCreation) "Verify your email" else "Verify recovery code",
        body = if (state.pendingEmail.isBlank()) {
            "Enter the six-digit code sent to your email."
        } else {
            "Enter the six-digit code sent to ${state.pendingEmail}."
        },
        onBack = {
            onNavigate(
                if (flow == VerificationFlow.AccountCreation) AppRoute.CreateAccount
                else AppRoute.ForgotPassword
            )
        },
    ) {
        ApiError(state.error, state::clearError)

        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter(Char::isDigit).take(6) },
            label = { Text("6-digit code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UlRadius.control),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppPrimary,
                cursorColor = AppPrimary,
                focusedTextColor = AppText,
                unfocusedTextColor = AppText,
            ),
        )
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton(
            "Verify",
            onClick = {
                scope.launch {
                    val ok = if (flow == VerificationFlow.AccountCreation) {
                        state.verifyAccount(code)
                    } else {
                        state.verifyRecovery(code)
                    }

                    if (ok) {
                        onNavigate(
                            if (flow == VerificationFlow.AccountCreation) AppRoute.AccountCreatedSuccess
                            else AppRoute.CreateNewPassword
                        )
                    }
                }
            },
            enabled = code.length == 6 && !state.loading,
            loading = state.loading,
        )

        if (flow == VerificationFlow.AccountCreation) {
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = { scope.launch { state.resendVerification() } },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Send a new code", color = AppPrimary)
            }
        }
    }
}

@Composable
fun ConnectedCreateNewPasswordScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    com.universallive.app.features.auth.AuthPageBridge(
        eyebrow = "Account recovery",
        title = "Create a new password",
        body = "Update the password for your verified account.",
        onBack = { onNavigate(AppRoute.VerifyEmail(VerificationFlow.PasswordRecovery)) },
    ) {
        ApiError(state.error, state::clearError)
        UlTextField(password, { password = it }, "New Password", visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(10.dp))
        UlTextField(confirm, { confirm = it }, "Confirm New Password", visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton(
            "Update Password",
            onClick = {
                scope.launch {
                    if (state.updatePassword(password)) {
                        state.signOut()
                        onNavigate(AppRoute.PasswordResetSuccess)
                    }
                }
            },
            enabled = password.length >= 8 && password == confirm && !state.loading,
            loading = state.loading,
        )
    }
}
