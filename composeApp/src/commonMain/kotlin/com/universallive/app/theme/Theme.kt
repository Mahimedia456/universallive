package com.universallive.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkScheme = darkColorScheme(
    primary = AppPrimary,
    secondary = AppSignalBlue,
    background = AppBackground,
    surface = AppSurface,
    surfaceVariant = AppSurfaceRaised,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppText,
    onSurface = AppText,
    onSurfaceVariant = AppTextSecondary,
    error = AppLive,
    onError = AppText,
)

private val UniversalLiveTypography = Typography(
    headlineLarge = TextStyle(
        color = AppText,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = TextStyle(
        color = AppText,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        color = AppText,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        color = AppTextSecondary,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        color = AppTextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    ),
)

@Composable
fun UniversalLiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = UniversalLiveTypography,
        content = content,
    )
}
