package com.universallive.app.streaming.facecam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class FacecamLens(val label: String) { FRONT("Front"), BACK("Back") }
enum class FacecamShape(val label: String) { CIRCLE("Circle"), ROUNDED("Rounded"), SQUARE("Square") }

data class FacecamConfig(
    val enabled: Boolean = false,
    val lens: FacecamLens = FacecamLens.FRONT,
    val shape: FacecamShape = FacecamShape.CIRCLE,
    val x: Float = 0.72f,
    val y: Float = 0.08f,
    val size: Float = 0.24f,
    val mirrored: Boolean = true,
) {
    val positionLabel: String get() = "${(x * 100).toInt()}% × ${(y * 100).toInt()}%"
    val sizeLabel: String get() = "${(size * 100).toInt()}%"
}

class FacecamState {
    var config by mutableStateOf(FacecamConfig()); private set
    fun setEnabled(v: Boolean) { config = config.copy(enabled = v) }
    fun setLens(v: FacecamLens) { config = config.copy(lens = v, mirrored = v == FacecamLens.FRONT) }
    fun setShape(v: FacecamShape) { config = config.copy(shape = v) }
    fun setSize(v: Float) { config = config.copy(size = v.coerceIn(0.14f, 0.42f)) }
    fun setPosition(x: Float, y: Float) { config = config.copy(x = x.coerceIn(0f, 1f - config.size), y = y.coerceIn(0f, 1f - config.size)) }
    fun setMirrored(v: Boolean) { config = config.copy(mirrored = v) }
    fun reset() { config = FacecamConfig() }
}
