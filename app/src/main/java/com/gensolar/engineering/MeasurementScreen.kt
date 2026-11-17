package com.gensolar.engineering

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private val MeasureInk=Color(0xFF173A32)
private val MeasureMuted=Color(0xFF687C75)
private val MeasureGreen=Color(0xFF087F5B)

@Composable
fun MeasurementScreen(modifier: Modifier=Modifier,onRequestGps: ((Double,Double,Double)->Unit,(String)->Unit)->Unit) {
    var mode by remember { mutableStateOf("Distance") }
    var arStatus by remember { mutableStateOf("Checking ARCore…") }
    val context=LocalContext.current
    LaunchedEffect(context) {
        val available=withContext(Dispatchers.IO) { try { ArCoreApk.getInstance().checkAvailability(context).isSupported } catch (_:Exception) { false } }
        arStatus=if(available) "ARCore support detected on this device" else "ARCore support could not be confirmed"
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("MEASUREMENT & 3D SURVEY",color=MeasureMuted,fontSize=10.sp,fontWeight=FontWeight.Bold)
        Text("Measure your site with your phone",color=MeasureInk,fontSize=23.sp,fontWeight=FontWeight.ExtraBold)
        Text("Approximate offline-first tools for survey, buildings and solar planning.",color=MeasureMuted,fontSize=12.sp)
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFE7F2EC)),shape=RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(arStatus,color=MeasureGreen,fontWeight=FontWeight.Bold,fontSize=13.sp)
                Text("Live camera point picking, depth overlay and 3D scanning are not active in this first module build. The calculators and GPS capture below are available.",color=MeasureMuted,fontSize=11.sp,lineHeight=16.sp)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("Distance","Object","Room","Solar roof","GPS").forEach { FilterChip(selected=mode==it,onClick={mode=it},label={Text(it)}) }
        }
        when(mode) {
            "Distance" -> DistanceTool()
            "Object" -> ObjectTool()
            "Room" -> RoomTool()
            "Solar roof" -> RoofTool()
            "GPS" -> GpsTool(onRequestGps)
        }
        Text("Estimates only. Verify critical dimensions with a calibrated instrument before engineering, procurement or construction.",color=MeasureMuted,fontSize=10.sp,lineHeight=15.sp)
    }
}

@Composable private fun DistanceTool() {
    var x1 by remember { mutableStateOf("0") }; var y1 by remember { mutableStateOf("0") }; var z1 by remember { mutableStateOf("0") }
    var x2 by remember { mutableStateOf("3") }; var y2 by remember { mutableStateOf("4") }; var z2 by remember { mutableStateOf("0") }
    val result=runCatching { MeasurementMath.distance3d(x1.toDouble(),y1.toDouble(),z1.toDouble(),x2.toDouble(),y2.toDouble(),z2.toDouble()) }.getOrNull()
    ToolCard("Point-to-point distance","Enter two 3D coordinates in metres. AR camera point selection is a subsequent scanner milestone.") {
        Text("POINT A",fontSize=10.sp,color=MeasureMuted,fontWeight=FontWeight.Bold)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { Field("X m",x1,{x1=it},Modifier.weight(1f)); Field("Y m",y1,{y1=it},Modifier.weight(1f)); Field("Z m",z1,{z1=it},Modifier.weight(1f)) }
        Text("POINT B",fontSize=10.sp,color=MeasureMuted,fontWeight=FontWeight.Bold)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { Field("X m",x2,{x2=it},Modifier.weight(1f)); Field("Y m",y2,{y2=it},Modifier.weight(1f)); Field("Z m",z2,{z2=it},Modifier.weight(1f)) }
        Result("DISTANCE",result?.let{MeasurementMath.formatMeters(it)} ?: "Check coordinates")
    }
}
@Composable private fun ObjectTool() {
    var w by remember { mutableStateOf("1") }; var h by remember { mutableStateOf("1") }; var d by remember { mutableStateOf("1") }
    val a=w.toDoubleOrNull(); val b=h.toDoubleOrNull(); val c=d.toDoubleOrNull(); val ok=a!=null&&b!=null&&c!=null&&a>=0&&b>=0&&c>=0
    ToolCard("Object dimensions","Calculate estimated volume and surface area from width, height and depth.") {
        Field("Width (m)",w,{w=it}); Field("Height (m)",h,{h=it}); Field("Depth (m)",d,{d=it})
        Result("VOLUME",if(ok) String.format(Locale.getDefault(),"%.3f m³",a!!*b!!*c!!) else "Check inputs")
        Result("SURFACE AREA",if(ok) String.format(Locale.getDefault(),"%.3f m²",2*(a!!*b!!+a*c!!+b*c)) else "—")
    }
}
@Composable private fun RoomTool() {
    var l by remember { mutableStateOf("5") }; var w by remember { mutableStateOf("4") }; var h by remember { mutableStateOf("3") }
    val a=l.toDoubleOrNull(); val b=w.toDoubleOrNull(); val c=h.toDoubleOrNull(); val ok=a!=null&&b!=null&&c!=null&&a>=0&&b>=0&&c>=0
    ToolCard("Room dimensions","Estimate floor area, perimeter and gross wall area.") {
        Field("Length (m)",l,{l=it}); Field("Width (m)",w,{w=it}); Field("Wall height (m)",h,{h=it})
        Result("FLOOR AREA",if(ok) String.format(Locale.getDefault(),"%.2f m² · %.1f sq.ft",a!!*b!!,a*b*10.7639) else "Check inputs")
        Result("PERIMETER",if(ok) String.format(Locale.getDefault(),"%.2f m",2*(a!!+b!!)) else "—")
        Result("GROSS WALL AREA",if(ok) String.format(Locale.getDefault(),"%.2f m²",2*(a!!+b!!)*c!!) else "—")
    }
}
@Composable private fun RoofTool() {
    var l by remember { mutableStateOf("20") }; var w by remember { mutableStateOf("10") }; var slope by remember { mutableStateOf("0") }
    var usable by remember { mutableStateOf("75") }; var pl by remember { mutableStateOf("2.28") }; var pw by remember { mutableStateOf("1.13") }; var watts by remember { mutableStateOf("550") }
    val a=l.toDoubleOrNull(); val b=w.toDoubleOrNull(); val s=slope.toDoubleOrNull(); val u=usable.toDoubleOrNull(); val x=pl.toDoubleOrNull(); val y=pw.toDoubleOrNull(); val p=watts.toIntOrNull()
    val ok=a!=null&&b!=null&&s!=null&&u!=null&&x!=null&&y!=null&&p!=null&&a>0&&b>0&&s in 0.0..89.0&&u in 0.0..100.0&&x>0&&y>0&&p>0
    val area=if(ok) MeasurementMath.roofArea(a!!*b!!,s!!) else 0.0
    val usableArea=area*(u?:0.0)/100.0
    val count=if(ok) MeasurementMath.panelCount(usableArea,x!!,y!!) else 0
    ToolCard("Solar rooftop estimator","Rectangular roof estimate. Usable percentage is a planning allowance for setbacks, access and obstructions.") {
        Field("Roof length (m)",l,{l=it}); Field("Roof width (m)",w,{w=it}); Field("Slope (degrees)",slope,{slope=it})
        Field("Usable area (%)",usable,{usable=it}); Field("Panel length (m)",pl,{pl=it}); Field("Panel width (m)",pw,{pw=it}); Field("Panel rating (W)",watts,{watts=it})
        Result("ROOF SURFACE",if(ok) String.format(Locale.getDefault(),"%.2f m² · %.1f sq.ft",area,area*10.7639) else "Check inputs")
        Result("USABLE AREA",if(ok) String.format(Locale.getDefault(),"%.2f m²",usableArea) else "—")
        Result("ESTIMATED MODULES",if(ok) "$count panels" else "—")
        Result("ESTIMATED DC CAPACITY",if(ok) String.format(Locale.getDefault(),"%.2f kWp",count*p!!/1000.0) else "—")
    }
}
@Composable private fun GpsTool(onRequestGps: ((Double,Double,Double)->Unit,(String)->Unit)->Unit) {
    var la1 by remember { mutableStateOf("") }; var lo1 by remember { mutableStateOf("") }; var la2 by remember { mutableStateOf("") }; var lo2 by remember { mutableStateOf("") }; var status by remember { mutableStateOf("") }
    val distance=runCatching { MeasurementMath.haversineMeters(la1.toDouble(),lo1.toDouble(),la2.toDouble(),lo2.toDouble()) }.getOrNull()
    ToolCard("GPS point-to-point","Capture two locations. GPS error depends on sky visibility, buildings and device conditions.") {
        Text("LOCATION A",fontSize=10.sp,color=MeasureMuted,fontWeight=FontWeight.Bold)
        Field("Latitude",la1,{la1=it}); Field("Longitude",lo1,{lo1=it})
        Button(onClick={onRequestGps({lat,lon,acc->la1=lat.toString();lo1=lon.toString();status="Reported GPS accuracy ±"+String.format(Locale.getDefault(),"%.1f",acc)+" m"},{status=it})},modifier=Modifier.fillMaxWidth()){Text("Capture location A")}
        Text("LOCATION B",fontSize=10.sp,color=MeasureMuted,fontWeight=FontWeight.Bold)
        Field("Latitude",la2,{la2=it}); Field("Longitude",lo2,{lo2=it})
        Button(onClick={onRequestGps({lat,lon,acc->la2=lat.toString();lo2=lon.toString();status="Reported GPS accuracy ±"+String.format(Locale.getDefault(),"%.1f",acc)+" m"},{status=it})},modifier=Modifier.fillMaxWidth()){Text("Capture location B")}
        if(status.isNotBlank()) Text(status,color=MeasureMuted,fontSize=11.sp)
        Result("STRAIGHT-LINE DISTANCE",distance?.let{MeasurementMath.formatMeters(it)} ?: "Enter valid coordinates")
    }
}
@Composable private fun ToolCard(title:String,description:String,content:@Composable ColumnScope.()->Unit) {
    Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(title,color=MeasureInk,fontSize=17.sp,fontWeight=FontWeight.Bold)
            Text(description,color=MeasureMuted,fontSize=11.sp,lineHeight=16.sp)
            content()
        }
    }
}
@Composable private fun Field(label:String,value:String,onChange:(String)->Unit,modifier:Modifier=Modifier) {
    OutlinedTextField(value=value,onValueChange={v->if(v.length<=16&&v.matches(Regex("-?\\d*(\\.\\d*)?")))onChange(v)},
        modifier=modifier.fillMaxWidth(),label={Text(label,fontSize=11.sp)},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),shape=RoundedCornerShape(11.dp))
}
@Composable private fun Result(label:String,value:String) {
    Surface(color=Color(0xFFEAF4EF),shape=RoundedCornerShape(11.dp)) {
        Column(Modifier.fillMaxWidth().padding(11.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(label,color=MeasureMuted,fontSize=9.sp,fontWeight=FontWeight.Bold)
            Text(value,color=MeasureGreen,fontSize=16.sp,fontWeight=FontWeight.ExtraBold)
        }
    }
}