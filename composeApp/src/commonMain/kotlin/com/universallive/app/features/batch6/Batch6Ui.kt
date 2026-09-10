package com.universallive.app.features.batch6

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.theme.*

@Composable
internal fun SettingsPage(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Text("‹", color = AppPrimary, fontSize = 28.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = AppText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = AppTextMuted, fontSize = 11.sp)
                }
            } else {
                UniversalLiveBrand(compact = true)
                Spacer(Modifier.weight(1f))
            }
        }

        if (onBack == null) {
            Text(
                title,
                color = AppText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Text(
                subtitle,
                color = AppTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun SettingCard(
    title: String,
    subtitle: String? = null,
    status: String? = null,
    statusColor: Color = AppPrimary,
    onClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppSurface)
            .border(1.dp, AppBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                subtitle?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            status?.let {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = .12f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(5.dp))
                    Text(it, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        content?.let {
            Spacer(Modifier.height(12.dp))
            it()
        }
    }
}

@Composable
internal fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontWeight = FontWeight.Medium)
            Text(subtitle, color = AppTextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppBackground,
                checkedTrackColor = AppPrimary,
            ),
        )
    }
}
