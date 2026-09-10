package com.universallive.app.features.batch2

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
fun Batch2Page(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) { Text("‹ Back", color = AppPrimary) }
                Spacer(Modifier.width(4.dp))
            }
            UniversalLiveBrand(compact = true)
        }
        Text(title, color = AppText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = AppTextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
fun InfoCard(
    title: String,
    body: String,
    status: String? = null,
    statusColor: Color = AppSuccess,
    onClick: (() -> Unit)? = null,
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
            Text(title, color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            status?.let {
                Row(
                    Modifier.clip(CircleShape).background(statusColor.copy(alpha = .12f)).padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(5.dp))
                    Text(it, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(body, color = AppTextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
internal fun StepStatus(label: String, value: String, good: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (good) AppPrimary.copy(alpha=.12f) else AppWarning.copy(alpha=.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (good) "✓" else "!", color = if (good) AppPrimary else AppWarning, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = AppText, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, color = if (good) AppSuccess else AppWarning, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
