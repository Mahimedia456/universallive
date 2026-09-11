package com.universallive.app.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.theme.*
import org.jetbrains.compose.resources.painterResource
import universallive.composeapp.generated.resources.Res
import universallive.composeapp.generated.resources.universallive_phase02_welcome_hero

@Composable
fun AuthScreenShell(
    onBack: (() -> Unit)? = null,
    centerBrand: Boolean = false,
    heroBackdrop: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF02070B),
                        Color(0xFF031018),
                        AppBackground,
                    )
                )
            )
    ) {
        if (heroBackdrop) {
            Image(
                painter = painterResource(Res.drawable.universallive_phase02_welcome_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .align(Alignment.TopCenter),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x76000509),
                                Color(0xB8020609),
                                AppBackground,
                            )
                        )
                    )
            )
        }
        Box(
            Modifier
                .size(280.dp)
                .offset(x = 190.dp, y = (-110).dp)
                .clip(CircleShape)
                .background(AppSignalBlue.copy(alpha = 0.055f))
        )
        Box(
            Modifier
                .size(250.dp)
                .offset(x = (-125).dp, y = 520.dp)
                .clip(CircleShape)
                .background(AppPrimary.copy(alpha = 0.035f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = if (centerBrand) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    AuthBackButton(onBack)
                    Spacer(Modifier.width(12.dp))
                }
                if (!centerBrand) {
                    UniversalLiveBrand(compact = true)
                }
            }
            if (centerBrand) {
                Spacer(Modifier.height(16.dp))
                UniversalLiveBrand(compact = false, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(if (centerBrand) 26.dp else 30.dp))
            content()
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun AuthBackButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = AppSurface.copy(alpha = .92f),
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("‹", color = AppText, fontSize = 29.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
fun AuthHeading(
    eyebrow: String? = null,
    title: String,
    body: String,
    centered: Boolean = false,
) {
    val alignment = if (centered) TextAlign.Center else TextAlign.Start
    val modifier = Modifier.fillMaxWidth()
    if (!eyebrow.isNullOrBlank()) {
        Text(
            eyebrow.uppercase(),
            color = AppPrimary,
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            textAlign = alignment,
            modifier = modifier,
        )
        Spacer(Modifier.height(9.dp))
    }
    Text(
        title,
        color = Color.White,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        textAlign = alignment,
        modifier = modifier,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        body,
        color = AppTextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = alignment,
        modifier = modifier,
    )
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable (() -> Unit))? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder, color = AppTextMuted) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        isError = isError,
        singleLine = true,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = AppPrimary,
            unfocusedBorderColor = Color(0xFF31505D),
            focusedLabelColor = AppPrimary,
            unfocusedLabelColor = AppTextSecondary,
            cursorColor = AppPrimary,
            focusedContainerColor = Color(0xFF07131A).copy(alpha = .94f),
            unfocusedContainerColor = Color(0xFF07131A).copy(alpha = .86f),
            errorBorderColor = AppLive,
        ),
    )
}

@Composable
fun PasswordVisibilityAction(visible: Boolean, onToggle: () -> Unit) {
    Text(
        text = if (visible) "HIDE" else "SHOW",
        color = AppPrimary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 7.dp, vertical = 6.dp),
    )
}

@Composable
fun AuthDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f), color = AppBorder)
        Text(
            label.uppercase(),
            color = AppPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(Modifier.weight(1f), color = AppBorder)
    }
}

@Composable
fun AuthApiError(message: String?, onDismiss: () -> Unit) {
    if (message.isNullOrBlank()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppLive.copy(alpha = .10f),
        border = BorderStroke(1.dp, AppLive.copy(alpha = .38f)),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(message, color = AppText, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                "×",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
fun OtpCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
) {
    BasicTextField(
        value = code,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(6)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        cursorBrush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(6) { index ->
                        val digit = code.getOrNull(index)?.toString().orEmpty()
                        val active = index == code.length.coerceAtMost(5)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(.88f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF07131A))
                                .then(
                                    if (active && code.length < 6) Modifier.background(AppPrimary.copy(alpha = .04f))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (active && code.length < 6) AppPrimary else Color(0xFF31505D),
                                ),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        digit,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
                Box(Modifier.matchParentSize()) { innerTextField() }
            }
        },
    )
}

@Composable
fun AuthStatusIcon(symbol: String, success: Boolean = false) {
    Box(
        modifier = Modifier
            .size(94.dp)
            .clip(CircleShape)
            .background(AppPrimary.copy(alpha = .07f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(AppPrimary.copy(alpha = .10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                symbol,
                color = if (success) AppSuccess else AppPrimary,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun PasswordRule(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (ok) "✓" else "○",
            color = if (ok) AppPrimary else AppTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = if (ok) AppTextSecondary else AppTextMuted, fontSize = 12.sp)
    }
}
