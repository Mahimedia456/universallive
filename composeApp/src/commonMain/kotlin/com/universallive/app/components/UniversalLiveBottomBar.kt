package com.universallive.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.navigation.AppDestination
import com.universallive.app.theme.*

@Composable
fun UniversalLiveBottomBar(
    selected: AppDestination,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    val tabs = listOf(
        AppDestination.Home,
        AppDestination.Scenes,
        AppDestination.GoLive,
        AppDestination.Activity,
        AppDestination.Settings,
    )

    Surface(
        color = AppBackgroundSecondary,
        shadowElevation = 18.dp,
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(78.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selectedTab = tab == selected

                if (tab == AppDestination.GoLive) {
                    Column(
                        modifier = Modifier.width(68.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            onClick = { onDestinationChanged(tab) },
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = AppSurface,
                            border = BorderStroke(
                                if (selectedTab) 2.dp else 1.dp,
                                if (selectedTab) AppPrimary else AppPrimary.copy(alpha = .42f),
                            ),
                            shadowElevation = if (selectedTab) 10.dp else 2.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TabIcon(tab, AppPrimary, Modifier.size(27.dp))
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "LIVE",
                            color = AppPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = .8.sp,
                        )
                    }
                } else {
                    Surface(
                        onClick = { onDestinationChanged(tab) },
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.width(58.dp).height(60.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            TabIcon(
                                destination = tab,
                                color = if (selectedTab) AppPrimary else AppTextMuted,
                                modifier = Modifier.size(23.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                tab.label,
                                color = if (selectedTab) AppPrimary else AppTextMuted,
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab) FontWeight.Bold else FontWeight.Medium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .width(if (selectedTab) 20.dp else 0.dp)
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedTab) AppPrimary else androidx.compose.ui.graphics.Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}
