package com.gensolar.engineering
import org.junit.Assert.assertEquals
import org.junit.Test
class MeasurementMathTest {
 @Test fun distance3d(){assertEquals(5.0,MeasurementMath.distance3d(0.0,0.0,0.0,3.0,4.0,0.0),1e-9)}
 @Test fun sameGpsPointIsZero(){assertEquals(0.0,MeasurementMath.haversineMeters(17.0,78.0,17.0,78.0),1e-6)}
 @Test fun flatRoofAreaMatchesPlan(){assertEquals(200.0,MeasurementMath.roofArea(200.0,0.0),1e-9)}
 @Test fun rectangularPanelLayoutUsesBestOrientationAndAccountsForGaps(){
  val layout=MeasurementMath.rectangularPanelLayout(10.0,5.0,2.0,1.0,100.0,0.1)
  assertEquals(18,layout.moduleCount)
  assertEquals(9,layout.columns)
  assertEquals(2,layout.rows)
  assertEquals(true,layout.rotated)
 }
 @Test fun rectangularPanelLayoutAppliesUsableAreaAllowance(){
  val full=MeasurementMath.rectangularPanelLayout(10.0,5.0,2.0,1.0,100.0,0.0)
  val reduced=MeasurementMath.rectangularPanelLayout(10.0,5.0,2.0,1.0,25.0,0.0)
  assertEquals(25,full.moduleCount)
  assertEquals(5,reduced.moduleCount)
 }
 @Test(expected=IllegalArgumentException::class) fun panelLayoutRejectsNegativeGap(){
  MeasurementMath.rectangularPanelLayout(10.0,5.0,2.0,1.0,100.0,-0.1)
 }
 @Test(expected=IllegalArgumentException::class) fun panelLayoutRejectsNonFiniteRoof(){
  MeasurementMath.rectangularPanelLayout(Double.NaN,5.0,2.0,1.0,100.0,0.0)
 }
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