package com.gensolar.engineering

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

private val BatteryInk = Color(0xFF173A32)
private val BatteryMuted = Color(0xFF687C75)
private val BatteryGreen = Color(0xFF087F5B)

@Composable
fun NativeBatteryScreen(browser: WebView, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("gensolar_native_battery", android.content.Context.MODE_PRIVATE)
    }
    val defaults = listOf(
        "loadKw" to "3",
        "backupHours" to "8",
        "dod" to "80",
        "batteryEff" to "90",
        "batteryBusV" to "51.2",
        "batteryModuleV" to "12.8",
        "batteryModuleAh" to "100",
        "batteryModuleMaxDischargeA" to "100",
        "batteryModuleMaxChargeA" to "50",
        "batteryChargerCurrentA" to "0"
    )
    val fields = remember(preferences) {
        defaults.associate { (key, value) -> key to mutableStateOf(preferences.getString(key, value) ?: value) }
    }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var status by remember { mutableStateOf("Load the active project's battery data or run a new bank calculation.") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(browser) {
        val script = "JSON.stringify((function(){var b=window.GenSolarEpcBridge;if(!b)return null;var p=b.getProjectSummary();return {project:p.project||{},battery:b.getBatterySnapshot()};})())"
        browser.evaluateJavascript(script) { raw ->
            try {
                val response = JSONTokener(raw ?: "null").nextValue() as? String ?: return@evaluateJavascript
                val snapshot = JSONObject(response)
                val project = snapshot.optJSONObject("project")
                defaults.forEach { (key, fallback) ->
                    if (project?.has(key) == true) fields[key]?.value = project.optString(key, fallback)
                }
                val saved = snapshot.optJSONObject("battery")
                if (saved?.optBoolean("ok") == true) {
                    result = saved
                    status = saved.optString("status", "Saved battery result loaded from the active project.")
                }
            } catch (_: Exception) {
                status = "Project battery data is not available yet. Run the calculation after the engineering workspace loads."
            }
        }
    }

    fun calculate() {
        val numeric = fields.mapValues { (_, state) -> state.value.trim().toDoubleOrNull()?.takeIf { it.isFinite() } }
        val invalid = numeric.entries.firstOrNull { it.value == null }
        if (invalid != null) {
            status = "Enter a valid finite number for " + invalid.key + "."
            result = null
            return
        }
        preferences.edit().apply {
            fields.forEach { (key, state) -> putString(key, state.value.trim()) }
        }.apply()
        val payload = JSONObject()
        numeric.forEach { (key, value) -> payload.put(key, value!!) }
        val script = """
            (function(){
              var bridge=window.GenSolarEpcBridge;
              if(!bridge||typeof bridge.calculateBattery!=='function')
                return JSON.stringify({ok:false,error:'Battery calculation bridge is not ready. Open the electrical workspace and retry.'});
              return JSON.stringify(bridge.calculateBattery($payload));
            })()
        """.trimIndent()
        busy = true
        browser.evaluateJavascript(script) { raw ->
            busy = false
            try {
                val response = JSONTokener(raw ?: "null").nextValue() as? String ?: "{}"
                val json = JSONObject(response)
                if (json.optBoolean("ok")) {
                    result = json
                    status = json.optString("status", "Battery bank calculated and saved to the active project.")
                } else {
                    result = null
                    status = json.optString("error", "Battery calculation failed. Check the entered values.")
                }
            } catch (_: Exception) {
                result = null
                status = "Could not read the battery calculation response. Check the project bridge and retry."
            }
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BatteryInk)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("BATTERY ENGINEERING", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Size the battery bank.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Energy reserve, series / parallel module configuration and continuous current screening using the active project solver.",
                    color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 17.sp
                )
            }
        }
        BatteryCard("01  LOAD & BACKUP REQUIREMENT") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BatteryField("Connected load (kW)", fields.getValue("loadKw").value, { fields.getValue("loadKw").value = it }, Modifier.weight(1f))
                BatteryField("Backup hours", fields.getValue("backupHours").value, { fields.getValue("backupHours").value = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BatteryField("Depth of discharge (%)", fields.getValue("dod").value, { fields.getValue("dod").value = it }, Modifier.weight(1f))
                BatteryField("Discharge / inverter efficiency (%)", fields.getValue("batteryEff").value, { fields.getValue("batteryEff").value = it }, Modifier.weight(1f))
            }
        }
        BatteryCard("02  BATTERY MODULE DATASHEET") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BatteryField("DC bus voltage (V)", fields.getValue("batteryBusV").value, { fields.getValue("batteryBusV").value = it }, Modifier.weight(1f))
                BatteryField("Module voltage (V)", fields.getValue("batteryModuleV").value, { fields.getValue("batteryModuleV").value = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BatteryField("Module capacity (Ah)", fields.getValue("batteryModuleAh").value, { fields.getValue("batteryModuleAh").value = it }, Modifier.weight(1f))
                BatteryField("Module max discharge (A)", fields.getValue("batteryModuleMaxDischargeA").value, { fields.getValue("batteryModuleMaxDischargeA").value = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BatteryField("Module max charge (A)", fields.getValue("batteryModuleMaxChargeA").value, { fields.getValue("batteryModuleMaxChargeA").value = it }, Modifier.weight(1f))
                BatteryField("Charger max current (A; 0 = not entered)", fields.getValue("batteryChargerCurrentA").value, { fields.getValue("batteryChargerCurrentA").value = it }, Modifier.weight(1f))
            }
        }
        Button(onClick = { calculate() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "Calculating…" else "Calculate & save battery bank")
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PROJECT RESULT", color = BatteryGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(status, color = if (status.contains("REVIEW", true) || status.contains("failed", true)) Color(0xFFB54708) else BatteryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val current = result
                if (current == null) {
                    Text("No valid battery bank result is available for this project.", color = BatteryMuted, fontSize = 11.sp)
                } else {
                    BatteryMetric("Usable load energy", formatBattery(current.optDouble("usableEnergyKwh"), "kWh"))
                    BatteryMetric("Required nominal energy", formatBattery(current.optDouble("requiredNominalKwh"), "kWh"))
                    BatteryMetric(
                        "Bank arrangement",
                        current.optInt("seriesModules").toString() + "S × " + current.optInt("parallelStrings") + "P"
                    )
                    BatteryMetric("Total battery modules", current.optInt("totalModules").toString())
                    BatteryMetric("Installed nominal capacity", formatBattery(current.optDouble("installedNominalKwh"), "kWh"))
                    BatteryMetric("Installed bank capacity", formatBattery(current.optDouble("installedBankAh"), "Ah"))
                    BatteryMetric("Required DC discharge current", formatBattery(current.optDouble("designDischargeCurrentA"), "A"))
                    BatteryMetric("Available continuous discharge", formatBattery(current.optDouble("availableDischargeCurrentA"), "A"))
                    BatteryMetric("Available continuous charge", formatBattery(current.optDouble("availableChargeCurrentA"), "A"))
                    val warnings = current.optJSONArray("warnings")
                    if (warnings != null) {
                        for (index in 0 until warnings.length()) {
                            Text("• " + warnings.optString(index), color = Color(0xFFB54708), fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
        Text(
            "Enter manufacturer datasheet values. The series count requires the DC bus voltage to be an integer multiple of module nominal voltage. Verify BMS limits, fuses, cables, thermal conditions, inverter compatibility and manufacturer-approved topology before procurement or installation.",
            color = BatteryMuted, fontSize = 10.sp, lineHeight = 15.sp
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to electrical design") }
    }
}

@Composable private fun BatteryCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, color = BatteryGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

@Composable private fun BatteryField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { next -> if (next.length <= 16 && next.matches(Regex("-?\\d*(\\.\\d*)?"))) onChange(next) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 10.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(11.dp)
    )
}

@Composable private fun BatteryMetric(label: String, value: String) {
    Surface(color = Color(0xFFEAF4EF), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = BatteryMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = BatteryGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun formatBattery(value: Double, unit: String): String =
    String.format(Locale.US, "%.2f %s", value, unit)
