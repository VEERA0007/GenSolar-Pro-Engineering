package com.gensolar.engineering
import kotlin.math.*

data class RectangularPanelLayout(
    val moduleCount: Int,
    val columns: Int,
    val rows: Int,
    val rotated: Boolean,
    val effectiveLengthM: Double,
    val effectiveWidthM: Double
)

object MeasurementMath {
    fun distance3d(x1: Double,y1: Double,z1: Double,x2: Double,y2: Double,z2: Double) =
        sqrt((x2-x1).pow(2)+(y2-y1).pow(2)+(z2-z1).pow(2))
    fun haversineMeters(lat1: Double,lon1: Double,lat2: Double,lon2: Double): Double {
        require(listOf(lat1,lon1,lat2,lon2).all { it.isFinite() })
        require(lat1 in -90.0..90.0 && lat2 in -90.0..90.0 && lon1 in -180.0..180.0 && lon2 in -180.0..180.0)
        val r=6_371_000.0; val aLat=Math.toRadians(lat2-lat1); val aLon=Math.toRadians(lon2-lon1)
        val a=sin(aLat/2).pow(2)+cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(aLon/2).pow(2)
        return 2*r*asin(sqrt(a.coerceIn(0.0,1.0)))
    }
    fun roofArea(planArea: Double,slopeDegrees: Double): Double {
        require(planArea>=0.0 && slopeDegrees in 0.0..89.0)
        return planArea/cos(Math.toRadians(slopeDegrees))
    }
    fun panelCount(usableArea: Double,panelLength: Double,panelWidth: Double): Int {
        require(usableArea>=0.0 && panelLength>0.0 && panelWidth>0.0)
        return floor(usableArea/(panelLength*panelWidth/0.78)).toInt().coerceAtLeast(0)
    }
    fun rectangularPanelLayout(
        roofLength: Double,
        roofWidth: Double,
        panelLength: Double,
        panelWidth: Double,
        usablePercent: Double,
        gapMeters: Double
    ): RectangularPanelLayout {
        require(listOf(roofLength, roofWidth, panelLength, panelWidth, usablePercent, gapMeters).all { it.isFinite() })
        require(roofLength in 0.01..10000.0 && roofWidth in 0.01..10000.0)
        require(panelLength in 0.01..100.0 && panelWidth in 0.01..100.0)
        require(usablePercent in 0.0..100.0 && gapMeters in 0.0..1.0)
        val scale = sqrt(usablePercent / 100.0)
        val effectiveLength = roofLength * scale
        val effectiveWidth = roofWidth * scale
        fun fit(length: Double, width: Double): Triple<Int, Int, Int> {
            if (length <= 0.0 || width <= 0.0) return Triple(0, 0, 0)
            val columns = floor((length + gapMeters) / (width + gapMeters)).toInt().coerceAtLeast(0)
            val rows = floor((effectiveWidth + gapMeters) / (length + gapMeters)).toInt().coerceAtLeast(0)
            return Triple(columns * rows, columns, rows)
        }
        val normal = fit(panelLength, panelWidth)
        val rotated = fit(panelWidth, panelLength)
        val useRotated = rotated.first > normal.first
        val best = if (useRotated) rotated else normal
        return RectangularPanelLayout(best.first, best.second, best.third, useRotated, effectiveLength, effectiveWidth)
    }
    fun rectangleArea(length: Double, width: Double): Double {
        require(length.isFinite() && width.isFinite() && length > 0.0 && width > 0.0)
        return length * width
    }
    fun circleArea(radius: Double): Double {
        require(radius.isFinite() && radius > 0.0)
        return Math.PI * radius * radius
    }
    fun triangleArea(base: Double, height: Double): Double {
        require(base.isFinite() && height.isFinite() && base > 0.0 && height > 0.0)
        return base * height / 2.0
    }
    fun formatMeters(m: Double) = if (m>=1000) String.format("%.3f km",m/1000) else String.format("%.2f m · %.1f ft",m,m*3.280839895)
}