package com.universallive.app.features.integration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.features.auth.*
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.navigation.VerificationFlow
import com.universallive.app.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Phase 03 — Sign In. Uses the real backend session flow. */
@Composable
fun ConnectedSignInScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthScreenShell(
        onBack = { onNavigate(AppRoute.Welcome) },
        centerBrand = true,
        heroBackdrop = true,
    ) {
        AuthHeading(
            title = "Welcome Back",
            body = "Sign in to your account and continue your streaming journey.",
            centered = true,
        )
        Spacer(Modifier.height(24.dp))
        AuthApiError(state.error, state::clearError)
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            placeholder = "creator@example.com",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                PasswordVisibilityAction(passwordVisible) { passwordVisible = !passwordVisible }
            },
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "Forgot password?",
                color = AppPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigate(AppRoute.ForgotPassword) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        UlPrimaryButton(
            text = "Sign In",
            onClick = {
                scope.launch {
                    if (state.signIn(email, password)) {
                        val next = when {
                            state.profile?.onboardingCompleted == false &&
                                state.onboarding?.creatorSetupCompleted == true -> AppRoute.PermissionHub
                            state.profile?.onboardingCompleted == false -> AppRoute.CreatorSetup
                            else -> AppRoute.Main(AppDestination.Home)
                        }
                        onNavigate(next)
                    }
                }
            },
            enabled = email.contains("@") && password.isNotBlank() && !state.loading,
            loading = state.loading,
        )
        Spacer(Modifier.height(22.dp))
        AuthDivider("Secure account access")
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Don't have an account? ", color = AppTextSecondary, fontSize = 13.sp)
            Text(
                "Create one",
                color = AppPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onNavigate(AppRoute.CreateAccount) },
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "STREAM  •  CREATE  •  CONNECT",
            color = AppTextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Phase 04 — Sign Up. */
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
    var passwordVisible by remember { mutableStateOf(false) }
    var accepted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthScreenShell(
        onBack = { onNavigate(AppRoute.Welcome) },
        heroBackdrop = true,
    ) {
        AuthHeading(
            title = "Create Your Account",
            body = "Join Universal Live and build your creator identity.",
        )
        Spacer(Modifier.height(22.dp))
        AuthApiError(state.error, state::clearError)
        AuthTextField(name, { name = it }, "Full Name", placeholder = "Your name")
        Spacer(Modifier.height(10.dp))
        AuthTextField(username, { username = it }, "Username", placeholder = "@creator")
        Spacer(Modifier.height(10.dp))
        AuthTextField(
            email,
            { email = it },
            "Email",
            placeholder = "creator@example.com",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(10.dp))
        AuthTextField(
            password,
            { password = it },
            "Password",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = { PasswordVisibilityAction(passwordVisible) { passwordVisible = !passwordVisible } },
        )
        Spacer(Modifier.height(10.dp))
        AuthTextField(
            confirm,
            { confirm = it },
            "Confirm Password",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = { PasswordVisibilityAction(passwordVisible) { passwordVisible = !passwordVisible } },
            isError = confirm.isNotBlank() && confirm != password,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = AppPrimary,
                    checkmarkColor = Color(0xFF001014),
                    uncheckedColor = AppTextMuted,
                ),
            )
            Text(
                "I agree to the Terms of Service and Privacy Policy.",
                color = AppTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        UlPrimaryButton(
            text = "Create Account",
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
            enabled = name.isNotBlank() &&
                username.trim().removePrefix("@").length >= 3 &&
                email.contains("@") &&
                password.length >= 8 &&
                password.any(Char::isDigit) &&
                password.any(Char::isLetter) &&
                password == confirm &&
                accepted &&
                !state.loading,
            loading = state.loading,
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", color = AppTextSecondary, fontSize = 13.sp)
            Text(
                "Sign In",
                color = AppPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigate(AppRoute.SignIn) },
            )
        }
    }
}

/** Phase 06 — Forgot Password entry. */
@Composable
fun ConnectedForgotPasswordScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    var email by remember { mutableStateOf(state.pendingEmail) }
    val scope = rememberCoroutineScope()

    AuthScreenShell(onBack = { onNavigate(AppRoute.SignIn) }) {
        Spacer(Modifier.height(30.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuthStatusIcon("▣")
        }
        Spacer(Modifier.height(20.dp))
        AuthHeading(
            title = "Forgot Password?",
            body = "Enter your email address and we'll send you a secure code to reset your password.",
            centered = true,
        )
        Spacer(Modifier.height(26.dp))
        AuthApiError(state.error, state::clearError)
        AuthTextField(
            email,
            { email = it },
            "Email",
            placeholder = "creator@example.com",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton(
            text = "Send Reset Code",
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
        Spacer(Modifier.height(18.dp))
        Text(
            "←  Back to Sign In",
            color = AppPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(AppRoute.SignIn) }
                .padding(vertical = 10.dp),
        )
    }
}

/** Phase 05 + Phase 06 recovery verification. */
@Composable
fun ConnectedVerifyEmailScreen(
    state: MobileIntegrationState,
    flow: VerificationFlow,
    onNavigate: (AppRoute) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var resendSeconds by remember { mutableStateOf(45) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1000)
            resendSeconds -= 1
        }
    }

    val accountFlow = flow == VerificationFlow.AccountCreation
    val title = if (accountFlow) "Verify Your Email" else "Reset Your Password"
    val body = if (state.pendingEmail.isNotBlank()) {
        "Enter the 6-digit code sent to ${state.pendingEmail}."
    } else {
        "Enter the 6-digit verification code sent to your email."
    }

    AuthScreenShell(
        onBack = {
            onNavigate(if (accountFlow) AppRoute.CreateAccount else AppRoute.ForgotPassword)
        },
    ) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuthStatusIcon(if (accountFlow) "✉" else "⌁")
        }
        Spacer(Modifier.height(18.dp))
        AuthHeading(title = title, body = body, centered = true)
        Spacer(Modifier.height(28.dp))
        AuthApiError(state.error, state::clearError)
        OtpCodeField(code = code, onCodeChange = { code = it })
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Didn't receive the code? ", color = AppTextSecondary, fontSize = 12.sp)
            Text(
                if (resendSeconds > 0) "Resend (0:${resendSeconds.toString().padStart(2, '0')})" else "Resend code",
                color = if (resendSeconds > 0) AppTextMuted else AppPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = if (resendSeconds == 0) {
                    Modifier.clickable {
                        scope.launch {
                            val resent = if (accountFlow) {
                                state.resendVerification()
                            } else {
                                state.requestPasswordReset(state.pendingEmail)
                            }
                            if (resent) resendSeconds = 45
                        }
                    }
                } else Modifier,
            )
        }
        Spacer(Modifier.height(22.dp))
        UlPrimaryButton(
            text = if (accountFlow) "Verify" else "Verify Code",
            onClick = {
                scope.launch {
                    val ok = if (accountFlow) state.verifyAccount(code) else state.verifyRecovery(code)
                    if (ok) {
                        onNavigate(if (accountFlow) AppRoute.AccountCreatedSuccess else AppRoute.CreateNewPassword)
                    }
                }
            },
            enabled = code.length == 6 && !state.loading,
            loading = state.loading,
        )
        Spacer(Modifier.height(40.dp))
        Text(
            if (accountFlow) "KEEP YOUR ACCOUNT SECURE" else "SECURE PASSWORD RECOVERY",
            color = AppTextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Phase 06 — New password after a verified recovery code. */
@Composable
fun ConnectedCreateNewPasswordScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val lengthOk = password.length >= 8
    val numberOk = password.any(Char::isDigit)
    val letterOk = password.any(Char::isLetter)
    val matchOk = password.isNotBlank() && password == confirm

    AuthScreenShell(onBack = { onNavigate(AppRoute.VerifyEmail(VerificationFlow.PasswordRecovery)) }) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuthStatusIcon("⌁")
        }
        Spacer(Modifier.height(18.dp))
        AuthHeading(
            title = "New Password",
            body = "Create a new password for your Universal Live account.",
            centered = true,
        )
        Spacer(Modifier.height(24.dp))
        AuthApiError(state.error, state::clearError)
        AuthTextField(
            password,
            { password = it },
            "New Password",
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = { PasswordVisibilityAction(visible) { visible = !visible } },
        )
        Spacer(Modifier.height(10.dp))
        AuthTextField(
            confirm,
            { confirm = it },
            "Confirm New Password",
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = { PasswordVisibilityAction(visible) { visible = !visible } },
            isError = confirm.isNotBlank() && !matchOk,
        )
        Spacer(Modifier.height(14.dp))
        PasswordRule(lengthOk, "At least 8 characters")
        Spacer(Modifier.height(5.dp))
        PasswordRule(numberOk, "Include a number")
        Spacer(Modifier.height(5.dp))
        PasswordRule(letterOk, "Include a letter")
        Spacer(Modifier.height(5.dp))
        PasswordRule(matchOk, "Passwords match")
        Spacer(Modifier.height(20.dp))
        UlPrimaryButton(
            text = "Reset Password",
            onClick = {
                scope.launch {
                    if (state.updatePassword(password)) {
                        state.signOut()
                        onNavigate(AppRoute.PasswordResetSuccess)
                    }
                }
            },
            enabled = lengthOk && numberOk && letterOk && matchOk && !state.loading,
            loading = state.loading,
        )
    }
}
