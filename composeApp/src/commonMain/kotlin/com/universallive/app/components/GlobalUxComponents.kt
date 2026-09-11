package com.universallive.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.theme.AppBackground
import com.universallive.app.theme.AppBorder
import com.universallive.app.theme.AppLive
import com.universallive.app.theme.AppPrimary
import com.universallive.app.theme.AppSuccess
import com.universallive.app.theme.AppSurface
import com.universallive.app.theme.AppSurfaceInteractive
import com.universallive.app.theme.AppText
import com.universallive.app.theme.AppTextMuted
import com.universallive.app.theme.AppTextSecondary
import com.universallive.app.theme.AppWarning
import com.universallive.app.theme.UlRadius

enum class UlStateTone {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

private fun toneColor(tone: UlStateTone): Color = when (tone) {
    UlStateTone.INFO -> AppPrimary
    UlStateTone.SUCCESS -> AppSuccess
    UlStateTone.WARNING -> AppWarning
    UlStateTone.ERROR -> AppLive
}

@Composable
fun UlPageHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable (() -> Unit))? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = AppSurfaceInteractive,
                border = BorderStroke(1.dp, AppBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("‹", color = AppText, fontSize = 29.sp, fontWeight = FontWeight.Light)
                }
            }
            Spacer(Modifier.size(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AppText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }

        trailing?.invoke()
    }
}

@Composable
fun UlActionRow(
    marker: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    value: String? = null,
    tone: Color = AppPrimary,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else .48f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UlRadius.control))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(UlRadius.control))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tone.copy(alpha = .12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(marker, color = tone.copy(alpha = contentAlpha), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AppText.copy(alpha = contentAlpha), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = AppTextSecondary.copy(alpha = contentAlpha), fontSize = 11.sp, lineHeight = 15.sp)
        }
        if (!value.isNullOrBlank()) {
            Text(value, color = tone.copy(alpha = contentAlpha), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
        }
        Text("›", color = AppTextMuted.copy(alpha = contentAlpha), fontSize = 22.sp)
    }
}

@Composable
fun UlMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: Color = AppPrimary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(UlRadius.control))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(UlRadius.control))
            .padding(14.dp),
    ) {
        Text(value, color = tone, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(label, color = AppTextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun UlStatePanel(
    title: String,
    body: String,
    tone: UlStateTone,
    marker: String,
    primaryText: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val color = toneColor(tone)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UlRadius.card))
            .background(AppSurface)
            .border(1.dp, color.copy(alpha = .28f), RoundedCornerShape(UlRadius.card))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = .12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(marker, color = color, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(body, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
        if (primaryText != null && onPrimary != null) {
            Spacer(Modifier.height(18.dp))
            UlPrimaryButton(text = primaryText, onClick = onPrimary)
        }
        if (secondaryText != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            UlSecondaryButton(text = secondaryText, onClick = onSecondary)
        }
    }
}

@Composable
fun UlLoadingState(
    title: String = "Loading",
    body: String = "Getting the latest information ready.",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UlRadius.card))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(UlRadius.card))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = AppPrimary, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = AppText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(body, color = AppTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun UlEmptyState(
    title: String,
    body: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    UlStatePanel(
        title = title,
        body = body,
        tone = UlStateTone.INFO,
        marker = "○",
        primaryText = actionText,
        onPrimary = onAction,
    )
}

@Composable
fun UlErrorState(
    title: String,
    body: String,
    retryText: String = "Try Again",
    onRetry: () -> Unit,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    UlStatePanel(
        title = title,
        body = body,
        tone = UlStateTone.ERROR,
        marker = "!",
        primaryText = retryText,
        onPrimary = onRetry,
        secondaryText = secondaryText,
        onSecondary = onSecondary,
    )
}

@Composable
fun UlSuccessState(
    title: String,
    body: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    UlStatePanel(
        title = title,
        body = body,
        tone = UlStateTone.SUCCESS,
        marker = "✓",
        primaryText = actionText,
        onPrimary = onAction,
    )
}

@Composable
fun UlSectionDivider() {
    HorizontalDivider(color = AppBorder, modifier = Modifier.padding(vertical = 4.dp))
}
