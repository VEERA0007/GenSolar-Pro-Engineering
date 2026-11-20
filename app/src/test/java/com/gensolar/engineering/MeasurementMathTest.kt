package com.gensolar.engineering
import org.junit.Assert.assertEquals
import org.junit.Test
class MeasurementMathTest {
 @Test fun distance3d(){assertEquals(5.0,MeasurementMath.distance3d(0.0,0.0,0.0,3.0,4.0,0.0),1e-9)}
 @Test fun sameGpsPointIsZero(){assertEquals(0.0,MeasurementMath.haversineMeters(17.0,78.0,17.0,78.0),1e-6)}
 @Test fun flatRoofAreaMatchesPlan(){assertEquals(200.0,MeasurementMath.roofArea(200.0,0.0),1e-9)}
 @Test fun geometricAreaCalculatorsReturnExpectedValues(){
  assertEquals(20.0,MeasurementMath.rectangleArea(5.0,4.0),1e-9)
  assertEquals(Math.PI*9.0,MeasurementMath.circleArea(3.0),1e-9)
  assertEquals(12.0,MeasurementMath.triangleArea(6.0,4.0),1e-9)
 }
 @Test(expected=IllegalArgumentException::class) fun rectangleRejectsZeroDimension(){MeasurementMath.rectangleArea(0.0,4.0)}
 @Test(expected=IllegalArgumentException::class) fun circleRejectsNonFiniteRadius(){MeasurementMath.circleArea(Double.NaN)}
 @Test(expected=IllegalArgumentException::class) fun triangleRejectsNegativeHeight(){MeasurementMath.triangleArea(6.0,-1.0)}
 @Test(expected=IllegalArgumentException::class) fun invalidLatitudeRejected(){MeasurementMath.haversineMeters(91.0,78.0,17.0,78.0)}
}