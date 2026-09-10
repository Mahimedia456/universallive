package com.universallive.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.universallive.app.navigation.AppDestination
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TabIcon(destination: AppDestination, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        when (destination) {
            AppDestination.Home -> {
                val cell = w * .22f
                val gap = w * .10f
                val startX = (w - (cell * 2 + gap)) / 2
                val startY = (h - (cell * 2 + gap)) / 2
                repeat(2) { row ->
                    repeat(2) { col ->
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(startX + col * (cell + gap), startY + row * (cell + gap)),
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                            style = Stroke(stroke),
                        )
                    }
                }
            }

            AppDestination.Scenes -> {
                val cx = w * .5f
                drawLine(color, Offset(w*.24f,h*.42f), Offset(cx,h*.24f), stroke, StrokeCap.Round)
                drawLine(color, Offset(cx,h*.24f), Offset(w*.76f,h*.42f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w*.24f,h*.42f), Offset(cx,h*.58f), stroke, StrokeCap.Round)
                drawLine(color, Offset(cx,h*.58f), Offset(w*.76f,h*.42f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w*.29f,h*.58f), Offset(cx,h*.74f), stroke, StrokeCap.Round)
                drawLine(color, Offset(cx,h*.74f), Offset(w*.71f,h*.58f), stroke, StrokeCap.Round)
            }

            AppDestination.GoLive -> {
                drawCircle(color, radius = w*.09f, center = Offset(w*.5f,h*.5f))
                drawArc(color, 300f, 120f, false, Offset(w*.28f,h*.28f), Size(w*.44f,h*.44f), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(color, 300f, 120f, false, Offset(w*.15f,h*.15f), Size(w*.70f,h*.70f), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(color, 120f, 120f, false, Offset(w*.28f,h*.28f), Size(w*.44f,h*.44f), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(color, 120f, 120f, false, Offset(w*.15f,h*.15f), Size(w*.70f,h*.70f), style = Stroke(stroke, cap = StrokeCap.Round))
            }

            AppDestination.Activity -> {
                val bars = listOf(.36f to .70f, .50f to .48f, .64f to .30f)
                bars.forEach { (x, top) ->
                    drawLine(color, Offset(w*x,h*top), Offset(w*x,h*.78f), stroke*2.1f, StrokeCap.Round)
                }
            }

            AppDestination.Settings -> {
                drawCircle(color, radius = w*.16f, center = Offset(w*.5f,h*.36f), style = Stroke(stroke))
                drawArc(color, 205f, 130f, false, Offset(w*.21f,h*.46f), Size(w*.58f,h*.42f), style = Stroke(stroke, cap = StrokeCap.Round))
            }

            AppDestination.Overlays -> {
                drawRoundRect(color, Offset(w*.18f,h*.18f), Size(w*.64f,h*.64f), CornerRadius(5.dp.toPx()), style = Stroke(stroke))
                drawLine(color, Offset(w*.28f,h*.66f), Offset(w*.46f,h*.48f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w*.46f,h*.48f), Offset(w*.72f,h*.40f), stroke, StrokeCap.Round)
            }

            AppDestination.Connections -> {
                drawCircle(color, radius = w*.13f, center = Offset(w*.34f,h*.5f), style = Stroke(stroke))
                drawCircle(color, radius = w*.13f, center = Offset(w*.66f,h*.5f), style = Stroke(stroke))
                drawLine(color, Offset(w*.47f,h*.5f), Offset(w*.53f,h*.5f), stroke, StrokeCap.Round)
            }
        }
    }
}
