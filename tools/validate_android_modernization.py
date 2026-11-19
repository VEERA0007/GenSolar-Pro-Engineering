#!/usr/bin/env python3
"""CI guardrails for the Kotlin + Compose Android shell and offline EPC workspace."""
from pathlib import Path
from html.parser import HTMLParser
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ROOT_GRADLE = (ROOT / "build.gradle").read_text(encoding="utf-8")
APP_GRADLE = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
KOTLIN = ROOT / "app/src/main/java/com/gensolar/engineering/MainActivity.kt"
HTML = ROOT / "app/src/main/assets/index.html"
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

class IdAudit(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = []
        self.foot_destinations = set()
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if attrs.get("id"):
            self.ids.append(attrs["id"])
        if attrs.get("data-foot"):
            self.foot_destinations.add(attrs["data-foot"])

def require(condition, message):
    if not condition:
        raise SystemExit("ANDROID MODERNIZATION CHECK FAILED: " + message)

require("org.jetbrains.kotlin.android' version '2.0.21'" in ROOT_GRADLE, "Kotlin Android plugin must be pinned to 2.0.21")
require("org.jetbrains.kotlin.plugin.compose' version '2.0.21'" in ROOT_GRADLE, "Compose compiler plugin must match Kotlin 2.0.21")
require("id 'org.jetbrains.kotlin.android'" in APP_GRADLE, "Android module must compile Kotlin")
require("id 'org.jetbrains.kotlin.plugin.compose'" in APP_GRADLE, "Compose compiler plugin missing")
require("compose true" in APP_GRADLE and "androidx.compose.material3:material3" in APP_GRADLE, "Jetpack Compose Material 3 must be enabled")
require("jvmTarget = '17'" in APP_GRADLE, "Kotlin bytecode target must be Java 17")
require(KOTLIN.exists(), "Kotlin MainActivity.kt missing")
require(not (ROOT / "app/src/main/java/com/gensolar/engineering/MainActivity.java").exists(), "Legacy Java Activity must not coexist with Kotlin launcher")
kotlin = KOTLIN.read_text(encoding="utf-8")
for marker in ("class MainActivity : ComponentActivity()", "setContent {", "NativeDashboard(", "NativeSurveyScreen(", "NativePvDesignScreen(", "NativeElectricalScreen(", "NativeCadScreen(", "NativeShadeScreen(", "NativeLocationScreen(", "NativeStaadScreen(", "NativeBomScreen(", "NativeConstructionScreen(", "SurveyInput(", "NativeActionCard(", "NativeMetric(", "AndroidView(", "NavigationBar(", "Intent.ACTION_CREATE_DOCUMENT", "exportTextFile", "onExportFile", "onShowFileChooser", "onGeolocationPermissionsShowPrompt", "GenSolarNative", "MIXED_CONTENT_NEVER_ALLOW", "allowUniversalAccessFromFileURLs = false"):
    require(marker in kotlin, "Kotlin native shell missing: " + marker)
require('android:name=".MainActivity"' in MANIFEST, "Manifest launcher must resolve to Kotlin MainActivity")
html = HTML.read_text(encoding="utf-8")
audit = IdAudit()
audit.feed(html)
duplicates = sorted({item for item in audit.ids if audit.ids.count(item) > 1})
require(not duplicates, "duplicate HTML IDs: " + ", ".join(duplicates))
for destination in ("dashboard", "site", "location", "layout", "electrical", "dpr"):
    require(destination in audit.foot_destinations, "native bottom navigation target missing from web workspace: " + destination)
require("STAAD Structural" in html and "cadstudio" in html, "STAAD and CAD workspace navigation must remain available")
native_lines = kotlin.splitlines()
native_bom_index = next((i for i, line in enumerate(native_lines) if line.strip() == "private fun NativeBomScreen("), -1)
survey_input_index = next((i for i, line in enumerate(native_lines) if line.strip() == "private fun SurveyInput("), -1)
native_bom_annotations = [line.strip() for line in native_lines[:native_bom_index] if line.strip()][-2:]
survey_input_annotations = [line.strip() for line in native_lines[:survey_input_index] if line.strip()][-2:]
require(native_bom_annotations and native_bom_annotations[-1] == "@Composable" and (len(native_bom_annotations) == 1 or native_bom_annotations[-2] != "@Composable"), "NativeBomScreen must have exactly one @Composable annotation")
require(survey_input_annotations and survey_input_annotations[-1] == "@Composable" and (len(survey_input_annotations) == 1 or survey_input_annotations[-2] != "@Composable"), "SurveyInput must have exactly one @Composable annotation")
require("window.GenSolarEpcBridge = Object.freeze" in html and all(x in html for x in ("getProjectSummary", "getCadSnapshot", "saveProject", "clearLayout", "clearElectrical", "generateStaad", "buildBom", "setBom")), "Controlled native EPC JavaScript bridge is incomplete")
require("bridge.buildBom()" in kotlin and "bridge.setBom(rows)" in kotlin and 'selected == "bom" && !advancedBom' in kotlin, "Native BOM screen must build, edit and persist the shared project schedule")
require("NativeConstructionScreen(" in kotlin and "GenSolarEpcBridge.createActivity" in kotlin and "GenSolarEpcBridge.updateActivity" in kotlin and "onExportCsv" in kotlin, "Native construction screen must create, update and export shared offline activities")
require("NativeProcurementScreen(" in kotlin and "GenSolarEpcBridge.createPurchaseOrder" in kotlin and "GenSolarEpcBridge.createGoodsReceipt" in kotlin and "GenSolar-procurement-reconciliation.csv" in kotlin, "Native procurement screen must create orders, receive goods and export reconciliation")
require("PO STATUS LIFECYCLE" in kotlin and "GenSolarEpcBridge.updatePurchaseOrderStatus" in kotlin and "GenSolar-po-status-history.csv" in kotlin, "Native procurement screen must update permitted PO statuses and export audit history")
require("updatePurchaseOrderStatus: function(ref, nextStatus)" in html and "state.poHistory.push([key, previous, next" in html, "Native PO status bridge must validate transitions and persist an audit record")
require("historyLength = state.poHistory.length" in html and "state.poHistory.length = historyLength" in html, "PO status and receipt changes must roll back audit history when project persistence fails")
require("Use this quotation in purchase order" in kotlin and "Quotation selected for PO" in kotlin and "amount = quote.toString()" in kotlin, "Supplier quotation selection must prefill supplier and quote amount into native PO creation")
require("poLineItems" in kotlin and "Description | Quantity | Unit | Rate" in kotlin and "JSONArray(lines)" in kotlin, "Native PO screen must accept validated multi-line material schedules")
require("lines.length > 30" in html and "amount:lineQty * rate" in html and "const row = [ref,vendor,amount,date,status,qty,unit,rfqRef,lines]" in html, "PO bridge must validate, calculate and persist multi-line PO details")
require("REQUEST FOR QUOTATION (RFQ)" in kotlin and "GenSolarEpcBridge.createRfq" in kotlin and "rfqRows" in kotlin and "createRfq: function(input)" in html, "Native RFQ workspace must issue and persist validated supplier requests")
require("NativeDprScreen(" in kotlin and "GenSolarEpcBridge.saveDprDetails" in kotlin and "GenSolarEpcBridge.upsertDprDocument" in kotlin and "GenSolar-DPR-" in kotlin, "Native DPR screen must save project particulars, document revisions and export snapshots")
require("NativeCommissioningScreen(" in kotlin and "GenSolarEpcBridge.createCommissioningRecord" in kotlin and "GenSolarEpcBridge.updateCommissioningAction" in kotlin and "GenSolar-commissioning-register.csv" in kotlin, "Native commissioning screen must save QA tests, update actions and export CSV")
require("NativeHandoverScreen(" in kotlin and "GenSolarEpcBridge.saveHandover" in kotlin and "GenSolarEpcBridge.updateHandoverChecklist" in kotlin, "Native handover screen must save customer acceptance and checklist records")
require("getHandover: function()" in html and "saveHandover: function(input)" in html and "updateHandoverChecklist: function(index, checked)" in html, "Handover bridge APIs missing")
require("getCommissioning: function()" in html and "createCommissioningRecord: function(input)" in html and "updateCommissioningAction: function(index, status)" in html, "Commissioning QA bridge APIs missing")
require("getDprData: function()" in html and "saveDprDetails: function(input)" in html and "upsertDprDocument: function(input)" in html, "DPR document-control bridge APIs missing")
require("getProcurement: function()" in html and "createPurchaseOrder: function(input)" in html and "createGoodsReceipt: function(input)" in html, "Native procurement bridge must expose purchase orders and goods receipts")
require("bom: Array.isArray(state.bom) ? state.bom : []" in html and "IMPORT FROM PROJECT BOM" in kotlin and "poLineItems = if (poLineItems.isBlank()) imported" in kotlin, "Native purchase order entry must import quantities and specifications from the project BOM")
require("const poLines = Array.isArray(po[8]) ? po[8] : []" in html and "previousItem + qty > lineOrdered" in html and "put(\"item\", receiptItem.trim())" in kotlin and "SELECT MATERIAL LINE" in kotlin, "Goods receipt must reconcile quantities against each individual PO material line")
require("stockLedger:" in html and "sumCondition('Good')" in html and "MATERIAL STOCK LEDGER" in kotlin and "Good received:" in kotlin and "On hand:" in kotlin, "Procurement must expose item-wise available, damaged, quarantined and pending stock balances")
require("createStockIssue: function(input)" in html and "goodReceived - alreadyIssued" in html and "Material issue to site / work package" in html and "ISSUE MATERIAL TO SITE" in kotlin and "createStockIssue(" in kotlin, "Stock issue workflow must validate Good-condition on-hand stock and record site allocations")
require('id="receiptItem"' in html and 'id="stockLedger"' in html and "const line=lines.length?lines.find" in html and "state.goodsReceipts.push([poRef,qty,unit,date,condition,receiver,documentRef,remarks,item])" in html, "Web procurement must capture material-specific receipts and render item-wise stock ledger")
require((ROOT / "tools/test_bom_generation.js").exists(), "BOM quantity and rate-preservation regression test missing")
require("Your solar EPC command centre" in kotlin and "PROJECT OVERVIEW" in kotlin and "OutlinedTextField(" in kotlin and "FilterChip(" in kotlin and "NativeWorkspaceTile(" in kotlin, "Native Compose dashboard, search, filters or workspace tiles missing")
require("FIELD SURVEY" in kotlin and "gensolar_native_survey" in kotlin and "saveSurvey" in kotlin and "JSONObject(values)" in kotlin, "Native offline-first survey persistence or project sync missing")
require("PV ARRAY DESIGN" in kotlin and "gensolar_native_pv_design" in kotlin and "generateLayout" in kotlin and "NativePvDesignScreen(" in kotlin, "Native PV design screen or optimizer integration missing")
require("java.text.ParsePosition(0)" in kotlin and "position.index == dateTimeText.length" in kotlin, "Native shade date/time parser must reject trailing unparsed input")
require("ELECTRICAL ENGINEERING" in kotlin and "gensolar_native_electrical" in kotlin and "calcElectrical" in kotlin and "NativeElectricalScreen(" in kotlin, "Native electrical string validation screen or calculator integration missing")
require("CAD / 2D / 3D ENGINEERING" in kotlin and "gensolar_native_cad" in kotlin and "cadCreateBuilding" in kotlin and "NativeCadScreen(" in kotlin and "NativeCadPreview(" in kotlin, "Native CAD Compose editor or shared CAD engine integration missing")
require("SOLAR RESOURCE & SHADE" in kotlin and "gensolar_native_shade" in kotlin and "GenSolarShadeEngine.assessMultiple($input,$extras)" in kotlin and "GenSolarShadeEngine.dayProfileMultiple($input,$extras,30)" in kotlin and "additionalObstructions" in kotlin and 'selected == "shade"' in kotlin, "Native multi-obstruction shade screen, input persistence, solar calculation or day-profile integration missing")
require("SITE GPS & SOLAR RESOURCE" in kotlin and "gensolar_native_location" in kotlin and "requestNativeGps" in kotlin and "onNativeGps(latitude: Double" in kotlin and "navigator.geolocation.getCurrentPosition" in kotlin and 'selected == "location"' in kotlin, "Native GPS/resource screen, Android geolocation bridge or project synchronization missing")
require("SAVING GPS AND RESOURCE DATA" in kotlin and "PROJECT SAVE FAILED" in kotlin, "Native GPS screen must report the confirmed project save result")
require("STRUCTURAL ENGINEERING" in kotlin and "gensolar_native_staad" in kotlin and "bridge.generateStaad()" in kotlin and "staadSpan" in kotlin and 'selected == "staad"' in kotlin, "Native STAAD geometry/load workspace or structural model-generation bridge missing")
require("bridge.clearElectrical()" in kotlin and "project.layout" in kotlin and "project.electrical" in kotlin, "Electrical bridge must require a fresh PV layout and clear stale calculation results")
require(all(marker not in kotlin for marker in ("typeof state", "state.layout", "state.electrical", "typeof save", "staadGenerate()", "typeof model")), "Compose workspaces must not call private IIFE variables directly; use GenSolarEpcBridge")
require("bridge.clearLayout()" in kotlin and "bridge.getProjectSummary()" in kotlin and "project.layout" in kotlin, "PV optimizer must clear stale results and read the shared project through the controlled bridge")
require("BackHandler(enabled = selected != \"dashboard\")" in kotlin, "Android back navigation must return from workspaces to Home")
require('"Received: %.3f %s  ·  Remaining: %.3f %s"' in kotlin and '"Delivery progress: %.1f%%"' in kotlin and "receivedReceiptQty / orderedReceiptQty" in kotlin, "Native goods receipt screen must show live received, remaining and delivery-progress values")
require("OVERDUE by " in kotlin and "Due today" in kotlin and "Due in " in kotlin and "expectedDelivery.isBefore(today)" in kotlin, "PO reconciliation must flag overdue, due-today and upcoming expected deliveries")
require("Delivery alert" in kotlin and "deliveryAlert(row)" in kotlin, "PO reconciliation CSV must include delivery health alerts")
require('browser.evaluateJavascript(' in kotlin and "window.GenSolarEpcBridge.navigate" in kotlin, "Native workspace navigation must call the controlled shared workspace router")
for destination in ("location", "bom", "procurement", "construction", "dpr"):
    require(f'"{destination}"' in kotlin, "native dashboard or More menu destination missing: " + destination)

require("snapshot.layout.forEach" in kotlin and "snapshot.roofZones.forEach" in kotlin and "layout:model.layout||[]" in html and "roofZones:Array.isArray(state.roofZones)" in html, "Native CAD preview must render actual PV modules and surveyed roof zones")
require("NativeCadPreview(" in kotlin and "private fun NativeCadPreview(" in kotlin and "ComposeCanvas(" in kotlin and "detectTapGestures" in kotlin, "Native CAD editor must render and hit-select shared project geometry in Compose")
require("b.moveCadEntity(" in kotlin and "b.duplicateCadEntity(" in kotlin and "b.rotateCadEntityBy90(" in kotlin and "b.deleteCadEntity(" in kotlin and "b.setCadLayerVisible(" in kotlin, "Native CAD controls must invoke real shared-model edit operations")
require(all(marker in html for marker in ("selectCadEntityAt: function(", "moveCadEntity: function(", "duplicateCadEntity: function(", "rotateCadEntityBy90: function(", "deleteCadEntity: function(", "setCadLayerVisible: function(", "cadCommitMutation(", "translateCadEntity(")), "Controlled CAD bridge must expose transactional selection, editing and layer operations")
require((ROOT / "tools/test_native_cad_editor_bridge.js").exists(), "Native CAD bridge regression test missing")
require("NativeCad3DPreview(" in kotlin and "nativeCadProjectPoint(" in kotlin and "nativeCadViewport3D(" in kotlin and "buildingHeight" in kotlin and "zoneElevation" in kotlin and "nativeCadWallBaseElevation(" in kotlin, "Native CAD 3D preview must project actual building, roof-zone and PV elevation data")
require((ROOT / "app/src/test/java/com/gensolar/engineering/NativeCadProjectionTest.kt").exists(), "Native CAD 3D projection unit tests missing")
require("gradle testDebugUnitTest --stacktrace" in (ROOT / ".github/workflows/android-apk.yml").read_text(encoding="utf-8"), "Native CAD 3D unit tests must run in GitHub Actions")
workflow = (ROOT / ".github/workflows/android-apk.yml").read_text(encoding="utf-8")
require("apksigner verify --verbose --print-certs" in workflow and "dump badging" in workflow, "APK signature and application ID verification missing")
subprocess.run(["node", str(ROOT / "tools/test_native_cad_editor_bridge.js")], cwd=ROOT, check=True)
solver = ROOT / "app/src/main/assets/structural_solver.js"
require(solver.exists() and "GenSolarStructuralSolver" in solver.read_text(encoding="utf-8"), "Inbuilt structural solver asset missing")
require("staadRunAnalysis" in html and "staadAnalysisSchedule" in html, "Inbuilt structural analysis UI is not wired")
shade = ROOT / "app/src/main/assets/shade_engine.js"
require(shade.exists() and "GenSolarShadeEngine" in shade.read_text(encoding="utf-8"), "Solar shade engine asset missing")
require("GenSolarShadeEngine.assess" in html and "GenSolarShadeEngine.dayProfile" in html, "Solar shade engine is not wired to the UI")
require("setShadeAnalysis: function(input)" in html and "getShadeAnalysis: function()" in html and "shadeAnalysis: state.shadeAnalysis || null" in html, "Shade results must persist in and be retrievable from the active offline project")
require("assessMultiple" in shade.read_text(encoding="utf-8") and "dayProfileMultiple" in shade.read_text(encoding="utf-8"), "Multi-obstruction shade aggregation missing")
require("obstructionWidthDeg" in html and "shadeInterval" in html, "Directional obstruction controls or day profile interval missing")
subprocess.run(["node", str(ROOT / "tools/test_structural_solver.js")], cwd=ROOT, check=True)
subprocess.run(["node", str(ROOT / "tools/test_shade_engine.js")], cwd=ROOT, check=True)
print(f"Android modernization checks passed: Kotlin 2.0.21, Compose Material 3, Java 17, secure local WebView bridge, {len(audit.ids)} unique HTML IDs and all native navigation targets.")


measurement_screen = ROOT / "app/src/main/java/com/gensolar/engineering/MeasurementScreen.kt"
measurement_store = ROOT / "app/src/main/java/com/gensolar/engineering/MeasurementHistory.kt"
measurement_test = ROOT / "app/src/test/java/com/gensolar/engineering/MeasurementHistoryTest.kt"
require(measurement_screen.exists() and measurement_store.exists(), "Native measurement history source files missing")
measurement_ui = measurement_screen.read_text(encoding="utf-8")
measurement_data = measurement_store.read_text(encoding="utf-8")
require("MeasurementHistoryStore(context)" in measurement_ui and "historyStore.append(record)" in measurement_ui, "Measurement UI must persist saved results to app-private offline storage")
require('listOf("Distance","Object","Room","Solar roof","GPS","History")' in measurement_ui and "MeasurementHistoryTool(history)" in measurement_ui, "Native measurement workspace must expose its saved history")
require("MeasurementHistoryCodec.toCsv(records)" in measurement_ui and "GenSolar-measurement-history.csv" in measurement_ui and "onExportFile" in measurement_ui, "Measurement history must export through the Android document picker")
require("getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)" in measurement_data and ".commit()" in measurement_data and "MAX_RECORDS = 200" in measurement_data, "Measurement history must use bounded app-private persistence with confirmed writes")
require("validationError(record)" in measurement_data and "return null" in measurement_data, "Corrupt or invalid measurement history must not be overwritten by an append")
require("val first = value.trimStart().firstOrNull()" in measurement_data and "first == '=' " in measurement_data or "first == '='" in measurement_data, "CSV export must neutralize spreadsheet formula-like cells")
require(measurement_test.exists(), "Measurement history Kotlin regression tests missing")
require("gradle testDebugUnitTest --stacktrace" in (ROOT / ".github/workflows/android-apk.yml").read_text(encoding="utf-8"), "Measurement history tests must run in GitHub Actions")

require("onExportFile = onExportFile" in kotlin and "MeasurementScreen(" in kotlin, "Measurement history export callback must be wired from the Android host to the Compose screen")


battery_engine = ROOT / "app/src/main/assets/battery_engine.js"
battery_screen = ROOT / "app/src/main/java/com/gensolar/engineering/BatteryScreen.kt"
battery_test = ROOT / "tools/test_battery_engine.js"
require(battery_engine.exists() and battery_screen.exists() and battery_test.exists(), "Native battery engine, Compose screen or regression test missing")
battery_source = battery_engine.read_text(encoding="utf-8")
battery_ui = battery_screen.read_text(encoding="utf-8")
require("battery_engine.js" in html and "GenSolarBatteryEngine.calculate" in html, "Battery engine asset must be loaded and used by the EPC solver")
require("calculateBattery: function(input)" in html and "getBatterySnapshot: function()" in html and "battery: state.battery || null" in html, "Controlled EPC bridge must expose and persist the active battery result")
require(all(key in html for key in ("batteryBusV", "batteryModuleV", "batteryModuleAh", "batteryModuleMaxDischargeA", "batteryModuleMaxChargeA", "batteryChargerCurrentA")), "Battery module voltage, capacity and charge/discharge limits must be project-backed")
require("NativeBatteryScreen(" in kotlin and "onOpenBattery" in kotlin and 'selected == "battery"' in kotlin, "Native electrical navigation must expose the Compose battery workspace")
require("bridge.calculateBattery(" in battery_ui and "getBatterySnapshot" in battery_ui and "gensolar_native_battery" in battery_ui, "Native battery screen must load and save the active project calculation with offline input persistence")
require("seriesModules" in battery_source and "parallelStrings" in battery_source and "designDischargeCurrentA" in battery_source and "availableChargeCurrentA" in battery_source, "Battery solver must calculate bank topology and charge/discharge current capacity")
require("node tools/test_battery_engine.js" in workflow, "Battery solver regression suite must run in GitHub Actions")
subprocess.run(["node", str(battery_test)], cwd=ROOT, check=True)
