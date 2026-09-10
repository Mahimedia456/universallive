package com.universallive.app.features.batch3

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.theme.*

@Composable
fun StudioPage(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Text("‹", color = AppPrimary, fontSize = 28.sp)
                }
            } else {
                UniversalLiveBrand(
                    compact = true,
                )
                Spacer(Modifier.weight(1f))
            }

            if (onBack != null) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = AppText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                    )
                    Text(
                        subtitle,
                        color = AppTextMuted,
                        fontSize = 11.sp,
                    )
                }
            }

            actions?.invoke(this)
        }

        if (onBack == null) {
            Text(
                title,
                color = AppText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                subtitle,
                color = AppTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun PreviewCanvas(
    label: String = "PREVIEW",
    footer: String = "16:9 • Output preview",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF07171F),
                        Color(0xFF061015),
                        Color(0xFF020609),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = AppPrimary.copy(alpha = .35f),
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = .56f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(AppSuccess),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    color = AppText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "◉",
                color = AppPrimary,
                fontSize = 36.sp,
            )
            Text(
                "Screen / Gameplay",
                color = AppText,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Your composition appears here",
                color = AppTextMuted,
                fontSize = 11.sp,
            )
        }

        Text(
            footer,
            color = AppTextMuted,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        )
    }
}

@Composable
internal fun ToolCard(
    title: String,
    subtitle: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) AppSurfaceInteractive else AppSurface)
            .border(
                1.dp,
                if (selected) AppPrimary else AppBorder,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppPrimary.copy(alpha = .11f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("◆", color = AppPrimary, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = AppText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    subtitle,
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                )
            }
            Text("›", color = AppTextMuted, fontSize = 24.sp)
        }
    }
}

@Composable
internal fun EditorSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
            .padding(15.dp),
    ) {
        Text(
            title,
            color = AppText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
internal fun LabelValue(
    label: String,
    value: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = AppTextSecondary,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
        )
        Text(
            value,
            color = AppText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
