package com.universallive.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.universallive.app.theme.AppLive
import com.universallive.app.theme.AppPrimary
import com.universallive.app.theme.AppText
import com.universallive.app.theme.AppTextMuted
import org.jetbrains.compose.resources.painterResource
import universallive.composeapp.generated.resources.Res
import universallive.composeapp.generated.resources.universallive_app_icon

/**
 * Canonical Universal Live brand header.
 *
 * Source of truth:
 * brand-assets/app-icon-512.png
 *
 * The logo is not generated, redrawn or approximated.
 * Header is deliberately start-aligned so it never floats toward the center.
 */
@Composable
fun UniversalLiveBrand(
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    showTagline: Boolean = true,
) {
    val iconSize = if (compact) 38.dp else 58.dp
    val titleSize = if (compact) 20.sp else 28.sp
    val taglineSize = if (compact) 7.sp else 9.sp

    Row(
        modifier = modifier.wrapContentWidth(Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Image(
            painter = painterResource(Res.drawable.universallive_app_icon),
            contentDescription = "Universal Live logo",
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.width(if (compact) 9.dp else 12.dp))

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = AppText,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ) {
                            append("Universal")
                        }
                        withStyle(
                            SpanStyle(
                                color = AppPrimary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ) {
                            append("Live")
                        }
                    },
                    fontSize = titleSize,
                    maxLines = 1,
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    text = "•",
                    color = AppLive,
                    fontSize = if (compact) 16.sp else 19.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (showTagline) {
                Text(
                    text = "L I V E   B R I N G S   U S   C L O S E R",
                    color = AppTextMuted,
                    fontSize = taglineSize,
                    letterSpacing = if (compact) 0.7.sp else 1.1.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
