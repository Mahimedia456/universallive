package com.universallive.app.features.auth

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppRoute
import com.universallive.app.navigation.VerificationFlow
import com.universallive.app.theme.*
import kotlinx.coroutines.delay

@Composable
private fun AuthPage(
    eyebrow: String,
    title: String,
    body: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = AppSurface,
                    border = BorderStroke(1.dp, AppBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("‹", color = AppText, fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            UniversalLiveBrand(compact = true)
        }
        Spacer(Modifier.height(34.dp))
        UlSectionHeader(eyebrow, title, body)
        Spacer(Modifier.height(26.dp))
        content()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1150)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            UniversalLiveBrand()
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .size(if (index == 1) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (index == 1) AppPrimary else AppDeepCyan)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onNavigate: (AppRoute) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        UniversalLiveBrand()
        Spacer(Modifier.height(34.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(AppSurface)
                .border(1.dp, AppPrimary.copy(alpha = .22f), RoundedCornerShape(24.dp)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text("GLOBAL SIGNAL", color = AppPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                Text("Your world. Live.", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Stream, create and connect from one powerful mobile studio.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        UlPrimaryButton("Create Account", onClick = { onNavigate(AppRoute.CreateAccount) })
        Spacer(Modifier.height(10.dp))
        UlSecondaryButton("Sign In", onClick = { onNavigate(AppRoute.SignIn) })
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Terms", color = AppTextMuted, fontSize = 12.sp)
            Text("  •  ", color = AppTextMuted)
            Text("Privacy", color = AppTextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
fun SignInScreen(onNavigate: (AppRoute) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    AuthPage(
        eyebrow = "Creator access",
        title = "Welcome back",
        body = "Sign in to your Universal Live studio.",
        onBack = { onNavigate(AppRoute.Welcome) },
    ) {
        UlTextField(email, { email = it }, "Email", placeholder = "creator@example.com")
        Spacer(Modifier.height(12.dp))
        UlTextField(password, { password = it }, "Password", visualTransformation = PasswordVisualTransformation())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onNavigate(AppRoute.ForgotPassword) }) {
                Text("Forgot Password?", color = AppPrimary)
            }
        }
        UlPrimaryButton(
            "Sign In",
            onClick = {
                loading = true
                onNavigate(AppRoute.Main(com.universallive.app.navigation.AppDestination.Home))
            },
            enabled = email.isNotBlank() && password.isNotBlank(),
            loading = loading,
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = AppBorder)
            Text("  or  ", color = AppTextMuted, fontSize = 12.sp)
            HorizontalDivider(Modifier.weight(1f), color = AppBorder)
        }
        Spacer(Modifier.height(14.dp))
        UlSecondaryButton("Continue with Google", onClick = {})
        Spacer(Modifier.height(10.dp))
        UlSecondaryButton("Continue with Apple", onClick = {})
        Spacer(Modifier.height(12.dp))
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
fun CreateAccountScreen(onNavigate: (AppRoute) -> Unit) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(false) }

    AuthPage(
        eyebrow = "New creator",
        title = "Create your studio",
        body = "Build your Universal Live identity and prepare your first broadcast.",
        onBack = { onNavigate(AppRoute.Welcome) },
    ) {
        UlTextField(name, { name = it }, "Full Name")
        Spacer(Modifier.height(10.dp))
        UlTextField(username, { username = it }, "Username", placeholder = "@creator")
        Spacer(Modifier.height(10.dp))
        UlTextField(email, { email = it }, "Email")
        Spacer(Modifier.height(10.dp))
        UlTextField(password, { password = it }, "Password", visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(7.dp))
        Text("Use 8+ characters with a number and symbol.", color = AppTextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        UlTextField(confirm, { confirm = it }, "Confirm Password", visualTransformation = PasswordVisualTransformation(), isError = confirm.isNotBlank() && confirm != password)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors = CheckboxDefaults.colors(checkedColor = AppPrimary, checkmarkColor = AppBackground),
            )
            Text("I agree to the Terms and Privacy Policy.", color = AppTextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        UlPrimaryButton(
            "Create Account",
            onClick = { onNavigate(AppRoute.VerifyEmail(VerificationFlow.AccountCreation)) },
            enabled = name.isNotBlank() && username.isNotBlank() && email.isNotBlank() && password.length >= 8 && password == confirm && accepted,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { onNavigate(AppRoute.SignIn) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Already have an account? Sign In", color = AppPrimary)
        }
    }
}

@Composable
fun ForgotPasswordScreen(onNavigate: (AppRoute) -> Unit) {
    var email by remember { mutableStateOf("") }
    AuthPage(
        eyebrow = "Account recovery",
        title = "Reset your password",
        body = "Enter your account email and we'll prepare a verification code.",
        onBack = { onNavigate(AppRoute.SignIn) },
    ) {
        UlTextField(email, { email = it }, "Email", placeholder = "creator@example.com")
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Send Verification Code", onClick = { onNavigate(AppRoute.VerifyEmail(VerificationFlow.PasswordRecovery)) }, enabled = email.contains("@"))
        Spacer(Modifier.height(10.dp))
        UlSecondaryButton("Back to Sign In", onClick = { onNavigate(AppRoute.SignIn) })
    }
}

@Composable
fun VerifyEmailScreen(flow: VerificationFlow, onNavigate: (AppRoute) -> Unit) {
    var code by remember { mutableStateOf("") }
    AuthPage(
        eyebrow = "Secure verification",
        title = "Verify your email",
        body = "Enter the six-digit code sent to a••••@example.com.",
        onBack = { onNavigate(if (flow == VerificationFlow.AccountCreation) AppRoute.CreateAccount else AppRoute.ForgotPassword) },
    ) {
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
            label = { Text("6-digit code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UlRadius.control),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPrimary, cursorColor = AppPrimary),
        )
        Spacer(Modifier.height(10.dp))
        Text("Resend available in 00:42", color = AppTextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton(
            "Verify",
            onClick = {
                onNavigate(
                    if (flow == VerificationFlow.AccountCreation) AppRoute.AccountCreatedSuccess
                    else AppRoute.CreateNewPassword
                )
            },
            enabled = code.length == 6,
        )
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { onNavigate(AppRoute.CodeExpired(flow)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Code expired? Send a new one", color = AppPrimary)
        }
    }
}

@Composable
fun CodeExpiredScreen(flow: VerificationFlow, onNavigate: (AppRoute) -> Unit) {
    AuthPage(
        eyebrow = "Verification",
        title = "Verification code expired",
        body = "For your security, verification codes are temporary. Request a new code to continue.",
        onBack = { onNavigate(AppRoute.VerifyEmail(flow)) },
    ) {
        UlCard {
            UlStatusBadge("CODE EXPIRED", AppWarning)
            Spacer(Modifier.height(12.dp))
            Text("No account changes were made. Your setup is still saved on this device.", color = AppTextSecondary)
        }
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Send New Code", onClick = { onNavigate(AppRoute.VerifyEmail(flow)) })
        Spacer(Modifier.height(10.dp))
        UlSecondaryButton("Change Email", onClick = { onNavigate(if (flow == VerificationFlow.AccountCreation) AppRoute.CreateAccount else AppRoute.ForgotPassword) })
    }
}

@Composable
fun CreateNewPasswordScreen(onNavigate: (AppRoute) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AuthPage(
        eyebrow = "Account recovery",
        title = "Create a new password",
        body = "Choose a strong password you haven't used for this account before.",
        onBack = { onNavigate(AppRoute.VerifyEmail(VerificationFlow.PasswordRecovery)) },
    ) {
        UlTextField(password, { password = it }, "New Password", visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(10.dp))
        UlTextField(confirm, { confirm = it }, "Confirm New Password", visualTransformation = PasswordVisualTransformation(), isError = confirm.isNotBlank() && confirm != password)
        Spacer(Modifier.height(8.dp))
        Text("8+ characters • number • symbol", color = AppTextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Update Password", onClick = { onNavigate(AppRoute.PasswordResetSuccess) }, enabled = password.length >= 8 && password == confirm)
    }
}

@Composable
fun PasswordResetSuccessScreen(onNavigate: (AppRoute) -> Unit) {
    SuccessScreen(
        eyebrow = "Security updated",
        title = "Password updated",
        body = "You can now sign in with your new password.",
        button = "Continue to Sign In",
        onClick = { onNavigate(AppRoute.SignIn) },
    )
}

@Composable
fun AccountCreatedSuccessScreen(onNavigate: (AppRoute) -> Unit) {
    SuccessScreen(
        eyebrow = "Welcome",
        title = "Welcome to Universal Live",
        body = "Let's prepare your studio for your first broadcast.",
        button = "Set Up My Studio",
        onClick = { onNavigate(AppRoute.CreatorSetup) },
    )
}

@Composable
private fun SuccessScreen(
    eyebrow: String,
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(AppBackground).systemBarsPadding().padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        UlCard {
            UlStatusBadge("SUCCESS")
            Spacer(Modifier.height(18.dp))
            UlSectionHeader(eyebrow, title, body)
            Spacer(Modifier.height(24.dp))
            UlPrimaryButton(button, onClick)
        }
    }
}
