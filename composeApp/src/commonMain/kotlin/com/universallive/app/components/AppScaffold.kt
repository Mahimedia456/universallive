package com.universallive.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.navigation.AppDestination
import com.universallive.app.theme.*

/**
 * Main Universal Live shell.
 *
 * Layout contract:
 * - header remains fixed
 * - body is the only vertically scrollable region
 * - bottom navigation remains fixed
 *
 * This prevents the "stuck/cut-off scroll" issue on smaller Android/iOS screens.
 */
@Composable
fun AppScaffold(
    title: String,
    selected: AppDestination,
    onDestinationChanged: (AppDestination) -> Unit,
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
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            UniversalLiveBrand(
                compact = true,
                modifier = Modifier.weight(1f, fill = false),
            )

            Spacer(Modifier.weight(1f))

            UlStatusBadge("READY")
        }

        Text(
            text = title,
            color = AppText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 3.dp,
                bottom = 7.dp,
            ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content,
        )

        UniversalLiveBottomBar(
            selected = selected,
            onDestinationChanged = onDestinationChanged,
        )
    }
}
