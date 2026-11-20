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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val MeasureInk=Color(0xFF173A32)
private val MeasureMuted=Color(0xFF687C75)
private val MeasureGreen=Color(0xFF087F5B)

@Composable
fun MeasurementScreen(
    modifier: Modifier=Modifier,
    onRequestGps: ((Double,Double,Double)->Unit,(String)->Unit)->Unit,
    onExportFile: (String,String,String)->Unit
) {
    var mode by remember { mutableStateOf("Distance") }
    var arStatus by remember { mutableStateOf("Checking ARCore…") }
    var saveStatus by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<MeasurementRecord?>(null) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    val context=LocalContext.current
    val historyStore=remember(context) { MeasurementHistoryStore(context) }
    var history by remember(historyStore) { mutableStateOf(historyStore.load()) }
    val saveMeasurement: (String,String,String,String)->Unit = { category,title,result,details ->
        val record=MeasurementRecord(
            id=UUID.randomUUID().toString(),
            category=category,
            title=title,
            result=result,
            details=details,
            createdAtEpochMs=System.currentTimeMillis()
        )
        if(historyStore.append(record)) {
            history=historyStore.load()
            saveStatus="Measurement saved on this device."
        } else {
            saveStatus="Measurement could not be saved. Existing history was left unchanged."
        }
    }
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
                Text("Live camera point picking, depth overlay and 3D scanning are not active in this build. The calculators, GPS capture and local measurement history are available.",color=MeasureMuted,fontSize=11.sp,lineHeight=16.sp)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("Distance","Area","Object","Room","Solar roof","GPS","History").forEach { FilterChip(selected=mode==it,onClick={mode=it},label={Text(it)}) }
        }
        if(saveStatus.isNotBlank()) Text(saveStatus,color=MeasureGreen,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
        when(mode) {
            "Distance" -> DistanceTool(saveMeasurement)
            "Area" -> AreaTool(saveMeasurement)
            "Object" -> ObjectTool(saveMeasurement)
            "Room" -> RoomTool(saveMeasurement)
            "Solar roof" -> RoofTool(saveMeasurement)
            "GPS" -> GpsTool(onRequestGps,saveMeasurement)
            "History" -> MeasurementHistoryTool(
                records = history,
                onExportFile = { fileName,content,mimeType -> onExportFile(fileName,content,mimeType) },
                onRequestDelete = { pendingDelete = it },
                onRequestClear = { confirmClearHistory = true }
            )
        }
        if (pendingDelete != null) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete measurement?") },
                text = { Text("This removes only the selected record from this device.") },
                confirmButton = {
                    TextButton(onClick = {
                        val record = pendingDelete
                        pendingDelete = null
                        if (record != null && historyStore.remove(record.id)) {
                            history = historyStore.load()
                            saveStatus = "Measurement deleted."
                        } else {
                            saveStatus = "Could not delete this measurement. Existing history was left unchanged."
                        }
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
            )
        }
        if (confirmClearHistory) {
            AlertDialog(
                onDismissRequest = { confirmClearHistory = false },
                title = { Text("Clear all measurement history?") },
                text = { Text("All saved measurement records on this device will be removed. Export a CSV first if you need a copy.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmClearHistory = false
                        if (historyStore.clearAll()) {
                            history = historyStore.load()
                            saveStatus = "Measurement history cleared."
                        } else {
                            saveStatus = "Could not clear history. Existing records were left unchanged."
                        }
                    }) { Text("Clear all") }
                },
                dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") } }
            )
        }
        Text("Estimates only. Verify critical dimensions with a calibrated instrument before engineering, procurement or construction.",color=MeasureMuted,fontSize=10.sp,lineHeight=15.sp)
    }
}

@Composable private fun DistanceTool(onSave:(String,String,String,String)->Unit) {
    var x1 by remember { mutableStateOf("0") }; var y1 by remember { mutableStateOf("0") }; var z1 by remember { mutableStateOf("0") }
    var x2 by remember { mutableStateOf("3") }; var y2 by remember { mutableStateOf("4") }; var z2 by remember { mutableStateOf("0") }
    val values=listOf(x1,y1,z1,x2,y2,z2).map { it.toDoubleOrNull() }
    val valid=values.all { it!=null && it.isFinite() }
    val result=if(valid) runCatching { MeasurementMath.distance3d(values[0]!!,values[1]!!,values[2]!!,values[3]!!,values[4]!!,values[5]!!) }.getOrNull()?.takeIf { it.isFinite() } else null
    val resultText=result?.let { MeasurementMath.formatMeters(it) } ?: "Check coordinates"
    ToolCard("Point-to-point distance","Enter two 3D coordinates in metres. AR camera point selection is a subsequent scanner milestone.") {
        Text("POINT A",fontSize=10.sp,color=MeasureMuted,fontWeight=FontWeight.Bold)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { Field("X m",x1,{x1=it},Modifier.weight(1f)); Field("Y m",y1,{y1=it},Modifier.weight(1f)); Field("Z m",z1,{z1=it},Modifier.weight(1f)) }
        Text("POINT B",fontSize=10.sp,color=MeasureMuted,fontWeight=FontWeight.Bold)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { Field("X m",x2,{x2=it},Modifier.weight(1f)); Field("Y m",y2,{y2=it},Modifier.weight(1f)); Field("Z m",z2,{z2=it},Modifier.weight(1f)) }
        Result("DISTANCE",resultText)
        if(result!=null) SaveMeasurementButton("Distance","3D point-to-point distance",resultText,"Point A: ("+x1+", "+y1+", "+z1+") m; Point B: ("+x2+", "+y2+", "+z2+") m",onSave)
    }
}


@Composable private fun AreaTool(onSave:(String,String,String,String)->Unit) {
    var shape by remember { mutableStateOf("Rectangle") }
    var first by remember { mutableStateOf("5") }
    var second by remember { mutableStateOf("4") }
    val a = first.toDoubleOrNull()
    val b = second.toDoubleOrNull()
    val validFirst = a != null && a.isFinite() && a > 0.0
    val validSecond = shape == "Circle" || (b != null && b.isFinite() && b > 0.0)
    val area = if (validFirst && validSecond) runCatching {
        when (shape) {
            "Rectangle" -> MeasurementMath.rectangleArea(a!!, b!!)
            "Circle" -> MeasurementMath.circleArea(a!!)
            else -> MeasurementMath.triangleArea(a!!, b!!)
        }
    }.getOrNull()?.takeIf { it.isFinite() } else null
    val summary = area?.let { String.format(Locale.getDefault(), "%.3f m² · %.1f sq.ft", it, it * 10.7639) } ?: "Check dimensions"
    ToolCard("Geometric area calculator","Calculate plan area for site zones, rooms, circular pads and triangular roof sections. Enter dimensions in metres.") {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("Rectangle","Circle","Triangle").forEach { item ->
                FilterChip(selected=shape==item,onClick={shape=item},label={Text(item)})
            }
        }
        Field(if (shape == "Circle") "Radius (m)" else if (shape == "Rectangle") "Length (m)" else "Base (m)",first,{first=it})
        if (shape != "Circle") Field(if (shape == "Rectangle") "Width (m)" else "Height (m)",second,{second=it})
        Result("AREA",summary)
        if (area != null) {
            val details = when (shape) {
                "Rectangle" -> "Rectangle: length $first m; width $second m"
                "Circle" -> "Circle: radius $first m"
                else -> "Triangle: base $first m; height $second m"
            }
            SaveMeasurementButton("Area","$shape area calculation",summary,details,onSave)
        }
    }
}

@Composable private fun ObjectTool(onSave:(String,String,String,String)->Unit) {
    var w by remember { mutableStateOf("1") }; var h by remember { mutableStateOf("1") }; var d by remember { mutableStateOf("1") }
    val a=w.toDoubleOrNull(); val b=h.toDoubleOrNull(); val c=d.toDoubleOrNull()
    val ok=listOf(a,b,c).all { it!=null && it.isFinite() && it>0.0 }
    val volume=if(ok) a!!*b!!*c!! else null
    val surface=if(ok) 2*(a!!*b!!+a*c!!+b*c!!) else null
    val summary=if(ok) String.format(Locale.getDefault(),"%.3f m³ volume; %.3f m² surface",volume!!,surface!!) else "Check dimensions"
    ToolCard("Object dimensions","Calculate estimated volume and surface area from width, height and depth.") {
        Field("Width (m)",w,{w=it}); Field("Height (m)",h,{h=it}); Field("Depth (m)",d,{d=it})
        Result("VOLUME",volume?.let { String.format(Locale.getDefault(),"%.3f m³",it) } ?: "Check inputs")
        Result("SURFACE AREA",surface?.let { String.format(Locale.getDefault(),"%.3f m²",it) } ?: "—")
        if(ok) SaveMeasurementButton("Object","Rectangular object dimensions",summary,"Width: "+w+" m; height: "+h+" m; depth: "+d+" m",onSave)
    }
}

@Composable private fun RoomTool(onSave:(String,String,String,String)->Unit) {
    var l by remember { mutableStateOf("5") }; var w by remember { mutableStateOf("4") }; var h by remember { mutableStateOf("3") }
    val a=l.toDoubleOrNull(); val b=w.toDoubleOrNull(); val c=h.toDoubleOrNull()
    val ok=listOf(a,b,c).all { it!=null && it.isFinite() && it>0.0 }
    val floor=if(ok) a!!*b!! else null
    val perimeter=if(ok) 2*(a!!+b!!) else null
    val walls=if(ok) 2*(a!!+b!!)*c!! else null
    val summary=if(ok) String.format(Locale.getDefault(),"%.2f m² floor; %.2f m perimeter; %.2f m² gross walls",floor!!,perimeter!!,walls!!) else "Check dimensions"
    ToolCard("Room dimensions","Estimate floor area, perimeter and gross wall area.") {
        Field("Length (m)",l,{l=it}); Field("Width (m)",w,{w=it}); Field("Wall height (m)",h,{h=it})
        Result("FLOOR AREA",floor?.let { String.format(Locale.getDefault(),"%.2f m² · %.1f sq.ft",it,it*10.7639) } ?: "Check inputs")
        Result("PERIMETER",perimeter?.let { String.format(Locale.getDefault(),"%.2f m",it) } ?: "—")
        Result("GROSS WALL AREA",walls?.let { String.format(Locale.getDefault(),"%.2f m²",it) } ?: "—")
        if(ok) SaveMeasurementButton("Room","Room dimensions",summary,"Length: "+l+" m; width: "+w+" m; wall height: "+h+" m",onSave)
    }
}

@Composable private fun RoofTool(onSave:(String,String,String,String)->Unit) {
    var l by remember { mutableStateOf("20") }; var w by remember { mutableStateOf("10") }; var slope by remember { mutableStateOf("0") }
    var usable by remember { mutableStateOf("75") }; var pl by remember { mutableStateOf("2.28") }; var pw by remember { mutableStateOf("1.13") }; var gap by remember { mutableStateOf("0.02") }; var watts by remember { mutableStateOf("550") }
    val a=l.toDoubleOrNull(); val b=w.toDoubleOrNull(); val s=slope.toDoubleOrNull(); val u=usable.toDoubleOrNull(); val x=pl.toDoubleOrNull(); val y=pw.toDoubleOrNull(); val g=gap.toDoubleOrNull(); val p=watts.toIntOrNull()
    val ok=a!=null&&b!=null&&s!=null&&u!=null&&x!=null&&y!=null&&g!=null&&p!=null&&a.isFinite()&&b.isFinite()&&s.isFinite()&&u.isFinite()&&x.isFinite()&&y.isFinite()&&g.isFinite()&&a>0&&b>0&&s in 0.0..89.0&&u in 0.0..100.0&&x>0&&y>0&&g in 0.0..1.0&&p>0
    val area=if(ok) MeasurementMath.roofArea(a!!*b!!,s!!) else null
    val usableArea=(area ?: 0.0)*(u ?: 0.0)/100.0
    val panelLayout=if(ok) runCatching { MeasurementMath.rectangularPanelLayout(a!!,b!!,x!!,y!!,u!!,g!!) }.getOrNull() else null\n    val count=panelLayout?.moduleCount ?: 0
    val capacity=if(ok) count*p!!/1000.0 else null
    val summary=if(ok) String.format(Locale.getDefault(),"%.2f m² roof; %d modules; %.2f kWp",area!!,count,capacity!!) else "Check roof and module inputs"
    ToolCard("Solar rooftop estimator","Rectangular roof estimate. Usable percentage is a planning allowance for setbacks, access and obstructions.") {
        Field("Roof length (m)",l,{l=it}); Field("Roof width (m)",w,{w=it}); Field("Slope (degrees)",slope,{slope=it})
        Field("Usable area (%)",usable,{usable=it}); Field("Panel length (m)",pl,{pl=it}); Field("Panel width (m)",pw,{pw=it}); Field("Module gap (m)",gap,{gap=it}); Field("Panel rating (W)",watts,{watts=it})
        Result("ROOF SURFACE",area?.let { String.format(Locale.getDefault(),"%.2f m² · %.1f sq.ft",it,it*10.7639) } ?: "Check inputs")
        Result("USABLE AREA",if(ok) String.format(Locale.getDefault(),"%.2f m²",usableArea) else "—")
        Result("ESTIMATED MODULES",panelLayout?.let { "$count panels · ${it.columns} columns × ${it.rows} rows" } ?: "—")\n        Result("LAYOUT ORIENTATION",panelLayout?.let { if (it.rotated) "Rotated module layout" else "Standard module layout" } ?: "—")
        Result("ESTIMATED DC CAPACITY",capacity?.let { String.format(Locale.getDefault(),"%.2f kWp",it) } ?: "—")
        if(ok) SaveMeasurementButton("Solar roof","Rectangular rooftop PV estimate",summary,"Roof: "+l+" × "+w+" m; slope: "+slope+"°; usable allowance: "+usable+"%; module: "+pl+" × "+pw+" m; module gap: "+gap+" m; layout: "+panelLayout?.columns+" columns × "+panelLayout?.rows+" rows; rotated: "+panelLayout?.rotated+"; rating: "+watts+" W",onSave)
    }
}

@Composable private fun GpsTool(
    onRequestGps: ((Double,Double,Double)->Unit,(String)->Unit)->Unit,
    onSave:(String,String,String,String)->Unit
) {
    var la1 by remember { mutableStateOf("") }; var lo1 by remember { mutableStateOf("") }; var la2 by remember { mutableStateOf("") }; var lo2 by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var accuracyA by remember { mutableStateOf<Double?>(null) }; var accuracyB by remember { mutableStateOf<Double?>(null) }
    val distance=runCatching { MeasurementMath.haversineMeters(la1.toDouble(),lo1.toDouble(),la2.toDouble(),lo2.toDouble()) }.getOrNull()
    val validDistance=distance?.takeIf { it.isFinite() }
    val distanceText=validDistance?.let { MeasurementMath.formatMeters(it) } ?: "Enter valid coordinates"
    val details="A: "+la1+", "+lo1+" (accuracy ±"+(accuracyA?.let { "%.1f".format(Locale.US,it) } ?: "not recorded")+" m); B: "+la2+", "+lo2+" (accuracy ±"+(accuracyB?.let { "%.1f".format(Locale.US,it) } ?: "not recorded")+" m)"
    ToolCard("GPS point-to-point","Capture two locations. GPS error depends on sky visibility, buildings and device conditions.") {
        Text("LOCATION A",fontSize=10.sp,color=MeasureMuted,fontWeight=FontWeight.Bold)
        Field("Latitude",la1,{la1=it}); Field("Longitude",lo1,{lo1=it})
        Button(onClick={onRequestGps({lat,lon,acc->la1=lat.toString();lo1=lon.toString();accuracyA=acc;status="Location A captured; reported accuracy ±"+String.format(Locale.getDefault(),"%.1f",acc)+" m"},{status=it})},modifier=Modifier.fillMaxWidth()){Text("Capture location A")}
        Text("LOCATION B",fontSize=10.sp,color=MeasureMuted,fontWeight=FontWeight.Bold)
        Field("Latitude",la2,{la2=it}); Field("Longitude",lo2,{lo2=it})
        Button(onClick={onRequestGps({lat,lon,acc->la2=lat.toString();lo2=lon.toString();accuracyB=acc;status="Location B captured; reported accuracy ±"+String.format(Locale.getDefault(),"%.1f",acc)+" m"},{status=it})},modifier=Modifier.fillMaxWidth()){Text("Capture location B")}
        if(status.isNotBlank()) Text(status,color=MeasureMuted,fontSize=11.sp)
        Result("STRAIGHT-LINE DISTANCE",distanceText)
        if(validDistance!=null) SaveMeasurementButton("GPS","GPS point-to-point distance",distanceText,details,onSave)
    }
}

@Composable private fun MeasurementHistoryTool(
    records: List<MeasurementRecord>,
    onExportFile: (String,String,String) -> Unit,
    onRequestDelete: (MeasurementRecord) -> Unit,
    onRequestClear: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchText by remember { mutableStateOf("") }
    val categories = listOf("All","Distance","Area","Object","Room","Solar roof","GPS")
    val visibleRecords = remember(records, selectedCategory, searchText) {
        MeasurementHistoryCodec.filter(records, selectedCategory, searchText)
    }
    ToolCard("Saved measurement history","Records are stored locally on this device. Search or filter records before exporting the full history.") {
        Text(records.size.toString()+" saved record(s) · "+visibleRecords.size+" shown · newest first",color=MeasureMuted,fontSize=11.sp)
        OutlinedTextField(
            value=searchText,
            onValueChange={searchText=it.take(80)},
            modifier=Modifier.fillMaxWidth(),
            label={Text("Search measurements")},
            placeholder={Text("Title, result or notes")},
            singleLine=true,
            shape=RoundedCornerShape(11.dp)
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            categories.forEach { category ->
                FilterChip(selected=selectedCategory==category,onClick={selectedCategory=category},label={Text(category)})
            }
        }
        Button(onClick={onExportFile("GenSolar-measurement-history.csv",MeasurementHistoryCodec.toCsv(records),"text/csv")},enabled=records.isNotEmpty(),modifier=Modifier.fillMaxWidth()) { Text("Export full history CSV") }
        OutlinedButton(onClick=onRequestClear,enabled=records.isNotEmpty(),modifier=Modifier.fillMaxWidth()) { Text("Clear all history") }
        if(records.isEmpty()) {
            Text("No saved measurements yet. Calculate a distance, room, object or roof estimate and choose Save measurement.",color=MeasureMuted,fontSize=12.sp)
        } else {
            if (visibleRecords.isEmpty()) {
                Text("No measurements match this search or category.",color=MeasureMuted,fontSize=12.sp)
            }
            visibleRecords.asReversed().take(50).forEach { record ->
                Card(shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFF3F6F5))) {
                    Column(Modifier.fillMaxWidth().padding(11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(record.category.uppercase(Locale.getDefault()),color=MeasureGreen,fontSize=9.sp,fontWeight=FontWeight.Bold)
                        Text(record.title,color=MeasureInk,fontSize=13.sp,fontWeight=FontWeight.Bold)
                        Text(record.result,color=MeasureGreen,fontSize=14.sp,fontWeight=FontWeight.ExtraBold)
                        Text(record.details,color=MeasureMuted,fontSize=10.sp,lineHeight=14.sp)
                        Text(SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date(record.createdAtEpochMs)),color=MeasureMuted,fontSize=9.sp)
                        TextButton(onClick={onRequestDelete(record)},modifier=Modifier.align(Alignment.End)) { Text("Delete record") }
                    }
                }
            }
            if(visibleRecords.size>50) Text("Showing the latest 50 matching records. CSV export includes all retained records.",color=MeasureMuted,fontSize=10.sp)
        }
        Text("Up to 200 records are retained in app-private storage. Clearing app data removes this history; export a copy to keep it.",color=MeasureMuted,fontSize=10.sp,lineHeight=14.sp)
    }
}

@Composable private fun SaveMeasurementButton(category:String,title:String,result:String,details:String,onSave:(String,String,String,String)->Unit) {
    Button(onClick={onSave(category,title,result,details)},modifier=Modifier.fillMaxWidth()) { Text("Save measurement") }
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
