package com.universallive.app.features.golive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.streaming.facecam.*
import com.universallive.app.theme.*

@Composable
fun FacecamControls(state: FacecamState) {
    val c = state.config
    Column(
        modifier = Modifier.fillMaxWidth().background(AppSurface, RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, AppBorder), RoundedCornerShape(18.dp)).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("FACECAM", color = AppText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Camera overlay for the outgoing stream", color = AppTextMuted, fontSize = 11.sp)
            }
            Switch(checked = c.enabled, onCheckedChange = state::setEnabled)
        }
        if (c.enabled) {
            Text("Lens", color = AppTextMuted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FacecamLens.entries.forEach { lens ->
                    FilterChip(selected = c.lens == lens, onClick = { state.setLens(lens) }, label = { Text(lens.label) })
                }
            }
            Text("Frame", color = AppTextMuted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FacecamShape.entries.forEach { shape ->
                    FilterChip(selected = c.shape == shape, onClick = { state.setShape(shape) }, label = { Text(shape.label) })
                }
            }
            Text("Size ${c.sizeLabel}", color = AppText, fontSize = 12.sp)
            Slider(value = c.size, onValueChange = state::setSize, valueRange = 0.14f..0.42f)
            Text("Horizontal ${(c.x * 100).toInt()}%", color = AppText, fontSize = 12.sp)
            Slider(value = c.x, onValueChange = { state.setPosition(it, c.y) }, valueRange = 0f..(1f-c.size).coerceAtLeast(0f))
            Text("Vertical ${(c.y * 100).toInt()}%", color = AppText, fontSize = 12.sp)
            Slider(value = c.y, onValueChange = { state.setPosition(c.x, it) }, valueRange = 0f..(1f-c.size).coerceAtLeast(0f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mirror camera", color = AppText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Switch(checked = c.mirrored, onCheckedChange = state::setMirrored)
            }
            Text("Placement ${c.positionLabel} • ${c.shape.label}", color = AppPrimarySoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
