package com.universallive.app.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UlSectionHeader
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.theme.*

@Composable
fun AuthPageBridge(
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
                TextButton(onClick = onBack) {
                    Text("‹", color = AppText, fontSize = 28.sp)
                }
                Spacer(Modifier.width(6.dp))
            }
            UniversalLiveBrand(compact = true)
        }
        Spacer(Modifier.height(30.dp))
        UlSectionHeader(eyebrow, title, body)
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(28.dp))
    }
}
