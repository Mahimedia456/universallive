package com.universallive.app.features.finalpolish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.theme.*

@Composable
fun ProfileActionRow(
    icon: String,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = AppPrimary,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(18.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).background(AppPrimary.copy(alpha = .10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = AppPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AppTextSecondary, fontSize = 11.sp)
        }

        badge?.let {
            Text(
                it,
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(badgeColor.copy(alpha = .10f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
            Spacer(Modifier.width(8.dp))
        }

        Text("›", color = AppTextMuted, fontSize = 24.sp)
    }
}
