package com.gensolar.engineering
import org.junit.Assert.assertEquals
import org.junit.Test
class MeasurementMathTest {
 @Test fun distance3d(){assertEquals(5.0,MeasurementMath.distance3d(0.0,0.0,0.0,3.0,4.0,0.0),1e-9)}
 @Test fun sameGpsPointIsZero(){assertEquals(0.0,MeasurementMath.haversineMeters(17.0,78.0,17.0,78.0),1e-6)}
 @Test fun flatRoofAreaMatchesPlan(){assertEquals(200.0,MeasurementMath.roofArea(200.0,0.0),1e-9)}
 @Test(expected=IllegalArgumentException::class) fun invalidLatitudeRejected(){MeasurementMath.haversineMeters(91.0,78.0,17.0,78.0)}
}