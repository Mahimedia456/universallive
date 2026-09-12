package com.universallive.app.features.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.AppScaffold
import com.universallive.app.navigation.AppDestination
import com.universallive.app.theme.*

@Composable
fun PlaceholderScreen(
    title: String,
    selected: AppDestination,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    AppScaffold(title, selected, onDestinationChanged) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).background(AppSurface, RoundedCornerShape(22.dp)).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = AppText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("This area is ready for Universal Live features and account data.", color = AppTextMuted, fontSize = 13.sp)
            }
        }
    }
}
