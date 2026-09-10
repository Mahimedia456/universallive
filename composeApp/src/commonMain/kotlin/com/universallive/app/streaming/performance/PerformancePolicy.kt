package com.universallive.app.streaming.performance

enum class ThermalState { NORMAL, WARM, HOT, CRITICAL }

data class RecommendedProfile(
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrateKbps: Int,
    val reason: String,
)

object PerformancePolicy {
    fun recommend(thermal: ThermalState, uploadKbps: Int? = null): RecommendedProfile {
        val base = when (thermal) {
            ThermalState.NORMAL -> RecommendedProfile(1920, 1080, 30, 6800, "Quality profile")
            ThermalState.WARM -> RecommendedProfile(1280, 720, 60, 6000, "Balanced thermal profile")
            ThermalState.HOT -> RecommendedProfile(1280, 720, 30, 4000, "Reduced thermal load")
            ThermalState.CRITICAL -> RecommendedProfile(854, 480, 30, 2500, "Critical thermal protection")
        }
        val available = uploadKbps ?: return base
        val safe = (available * 0.70f).toInt().coerceAtLeast(1000)
        return if (base.bitrateKbps <= safe) base else base.copy(
            bitrateKbps = safe,
            reason = "Limited by measured upload headroom",
        )
    }
}
