package com.universallive.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.theme.*

@Composable
fun StreamCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(AppSurface, RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, AppBorder), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(title, color = AppTextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(7.dp))
        Text(value, color = AppText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
