package com.gensolar.engineering
import kotlin.math.*

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