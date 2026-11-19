package com.gensolar.engineering

data class PvModuleFootprint(
    val widthM: Double,
    val heightM: Double
)

data class PvLayoutMetrics(
    val moduleCount: Int,
    val dcCapacityKwp: Double,
    val moduleAreaM2: Double
)

/**
 * Summarises the placed module geometry returned by the active project layout.
 * This is a schedule metric only; it does not replace string, structural or yield checks.
 */
object PvLayoutMath {
    fun summarize(modules: List<PvModuleFootprint>, modulePowerWp: Double): PvLayoutMetrics? {
        if (!modulePowerWp.isFinite() || modulePowerWp !in 1.0..2000.0) return null
        if (modules.any { !it.widthM.isFinite() || !it.heightM.isFinite() || it.widthM <= 0.0 || it.heightM <= 0.0 }) {
            return null
        }
        val area = modules.sumOf { it.widthM * it.heightM }
        val capacity = modules.size.toDouble() * modulePowerWp / 1000.0
        if (!area.isFinite() || !capacity.isFinite()) return null
        return PvLayoutMetrics(
            moduleCount = modules.size,
            dcCapacityKwp = capacity,
            moduleAreaM2 = area
        )
    }
}
