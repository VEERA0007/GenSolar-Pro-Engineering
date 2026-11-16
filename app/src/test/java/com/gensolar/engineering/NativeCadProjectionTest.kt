package com.gensolar.engineering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class NativeCadProjectionTest {
    @Test
    fun isometricProjectionSeparatesPlanAxesAndElevation() {
        val origin = nativeCadProjectPoint(NativeCad3DPoint(0.0, 0.0, 0.0))
        val xAxis = nativeCadProjectPoint(NativeCad3DPoint(1.0, 0.0, 0.0))
        val yAxis = nativeCadProjectPoint(NativeCad3DPoint(0.0, 1.0, 0.0))
        val elevated = nativeCadProjectPoint(NativeCad3DPoint(0.0, 0.0, 1.0))

        assertEquals(0f, origin.x, 0.0001f)
        assertEquals(0f, origin.y, 0.0001f)
        assertEquals(0.8660254f, xAxis.x, 0.0001f)
        assertEquals(0.5f, xAxis.y, 0.0001f)
        assertEquals(-0.8660254f, yAxis.x, 0.0001f)
        assertEquals(0.5f, yAxis.y, 0.0001f)
        assertEquals(0f, elevated.x, 0.0001f)
        assertEquals(-0.9f, elevated.y, 0.0001f)
    }

    @Test
    fun viewportFitsAllRoofAndWallVerticesInsidePadding() {
        val points = listOf(
            NativeCad3DPoint(0.0, 0.0, 0.0),
            NativeCad3DPoint(30.0, 0.0, 0.0),
            NativeCad3DPoint(30.0, 20.0, 0.0),
            NativeCad3DPoint(0.0, 20.0, 0.0),
            NativeCad3DPoint(0.0, 0.0, -6.0),
            NativeCad3DPoint(30.0, 20.0, -6.0)
        )
        val padding = 20f
        val viewport = nativeCadViewport3D(points, 360f, 320f, padding)

        assertTrue(viewport.scale > 0f && viewport.scale.isFinite())
        points.forEach { point ->
            val projected = nativeCadProjectPoint(point)
            val x = projected.x * viewport.scale + viewport.offsetX
            val y = projected.y * viewport.scale + viewport.offsetY
            assertTrue("Projected X must stay inside the drawing viewport", x >= padding - 0.01f && x <= 340f + 0.01f)
            assertTrue("Projected Y must stay inside the drawing viewport", y >= padding - 0.01f && y <= 300f + 0.01f)
        }
    }

    @Test
    fun wallGeometryUsesOnlyAValidatedBuildingHeight() {
        assertEquals(-1.0, nativeCadWallBaseElevation(5.0, 6.0)!!, 0.0001)
        assertNull(nativeCadWallBaseElevation(5.0, 0.0))
        assertNull(nativeCadWallBaseElevation(5.0, Double.NaN))
        assertNull(nativeCadWallBaseElevation(Double.NaN, 6.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteModelCoordinates() {
        nativeCadProjectPoint(NativeCad3DPoint(Double.NaN, 0.0, 0.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAViewportSmallerThanItsPadding() {
        nativeCadViewport3D(listOf(NativeCad3DPoint(0.0, 0.0, 0.0)), 30f, 30f, 20f)
    }
}
