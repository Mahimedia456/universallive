package com.universallive.app.features.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.AppScaffold
import com.universallive.app.components.UlCard
import com.universallive.app.navigation.AppDestination
import com.universallive.app.theme.*

@Composable
fun ActivityScreen(onDestinationChanged: (AppDestination) -> Unit) {
    AppScaffold(
        title = "Activity",
        selected = AppDestination.Activity,
        onDestinationChanged = onDestinationChanged,
    ) {
        Spacer(Modifier.height(10.dp))
        UlCard {
            Text("Broadcast activity", color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Stream history and performance details arrive in Mobile UI Phase 14. This route is active now so the final navigation architecture stays stable.",
                color = AppTextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}
