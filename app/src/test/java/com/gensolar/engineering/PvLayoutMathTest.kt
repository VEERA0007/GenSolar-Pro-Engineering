package com.gensolar.engineering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PvLayoutMathTest {
    @Test
    fun summarizesPlacedModuleCountCapacityAndActualFootprint() {
        val result = PvLayoutMath.summarize(
            listOf(
                PvModuleFootprint(widthM = 2.278, heightM = 1.134),
                PvModuleFootprint(widthM = 2.0, heightM = 1.0)
            ),
            modulePowerWp = 550.0
        )

        assertNotNull(result)
        assertEquals(2, result!!.moduleCount)
        assertEquals(1.1, result.dcCapacityKwp, 1e-9)
        assertEquals(4.583252, result.moduleAreaM2, 1e-9)
    }

    @Test
    fun emptyProjectLayoutReportsZeroModulesWithoutInventingCapacity() {
        val result = PvLayoutMath.summarize(emptyList(), modulePowerWp = 550.0)

        assertNotNull(result)
        assertEquals(0, result!!.moduleCount)
        assertEquals(0.0, result.dcCapacityKwp, 0.0)
        assertEquals(0.0, result.moduleAreaM2, 0.0)
    }

    @Test
    fun rejectsInvalidModulePowerAndGeometry() {
        val valid = listOf(PvModuleFootprint(2.0, 1.0))
        assertNull(PvLayoutMath.summarize(valid, 0.0))
        assertNull(PvLayoutMath.summarize(valid, 2000.1))
        assertNull(PvLayoutMath.summarize(valid, Double.NaN))
        assertNull(PvLayoutMath.summarize(listOf(PvModuleFootprint(0.0, 1.0)), 550.0))
        assertNull(PvLayoutMath.summarize(listOf(PvModuleFootprint(2.0, Double.POSITIVE_INFINITY)), 550.0))
        assertNull(PvLayoutMath.summarize(listOf(PvModuleFootprint(1e308, 1e308)), 550.0))
    }
}
