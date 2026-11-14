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
for marker in ("class MainActivity : ComponentActivity()", "setContent {", "NativeDashboard(", "NativeSurveyScreen(", "NativePvDesignScreen(", "NativeElectricalScreen(", "NativeCadScreen(", "NativeShadeScreen(", "NativeLocationScreen(", "NativeStaadScreen(", "NativeBomScreen(", "SurveyInput(", "NativeActionCard(", "NativeMetric(", "AndroidView(", "NavigationBar(", "Intent.ACTION_CREATE_DOCUMENT", "exportTextFile", "onExportFile", "onShowFileChooser", "onGeolocationPermissionsShowPrompt", "GenSolarNative", "MIXED_CONTENT_NEVER_ALLOW", "allowUniversalAccessFromFileURLs = false"):
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
require(re.search(r"@Composable\\s+private fun NativeBomScreen\\(", kotlin) is not None and re.search(r"@Composable\\s+private fun SurveyInput\\(", kotlin) is not None, "Compose screens must have exactly the required composable annotations")
require(re.search(r"@Composable\\s+@Composable\\s+private fun NativeBomScreen\\(", kotlin) is None, "duplicate @Composable annotation before NativeBomScreen")
require("window.GenSolarEpcBridge = Object.freeze" in html and all(x in html for x in ("getProjectSummary", "getCadSnapshot", "saveProject", "clearLayout", "clearElectrical", "generateStaad", "buildBom", "setBom")), "Controlled native EPC JavaScript bridge is incomplete")
require("bridge.buildBom()" in kotlin and "bridge.setBom(rows)" in kotlin and 'selected == "bom" && !advancedBom' in kotlin, "Native BOM screen must build, edit and persist the shared project schedule")
require((ROOT / "tools/test_bom_generation.js").exists(), "BOM quantity and rate-preservation regression test missing")
require("Your solar EPC command centre" in kotlin and "PROJECT OVERVIEW" in kotlin and "OutlinedTextField(" in kotlin and "FilterChip(" in kotlin and "NativeWorkspaceTile(" in kotlin, "Native Compose dashboard, search, filters or workspace tiles missing")
require("FIELD SURVEY" in kotlin and "gensolar_native_survey" in kotlin and "saveSurvey" in kotlin and "JSONObject(values)" in kotlin, "Native offline-first survey persistence or project sync missing")
require("PV ARRAY DESIGN" in kotlin and "gensolar_native_pv_design" in kotlin and "generateLayout" in kotlin and "NativePvDesignScreen(" in kotlin, "Native PV design screen or optimizer integration missing")
require("ELECTRICAL ENGINEERING" in kotlin and "gensolar_native_electrical" in kotlin and "calcElectrical" in kotlin and "NativeElectricalScreen(" in kotlin, "Native electrical string validation screen or calculator integration missing")
require("CAD & 3D ENGINEERING" in kotlin and "gensolar_native_cad" in kotlin and "cadCreateBuilding" in kotlin and "NativeCadScreen(" in kotlin, "Native CAD building model screen or shared CAD engine integration missing")
require("SOLAR RESOURCE & SHADE" in kotlin and "gensolar_native_shade" in kotlin and "GenSolarShadeEngine.assess($input)" in kotlin and "GenSolarShadeEngine.dayProfile($input,30)" in kotlin and 'selected == "shade"' in kotlin, "Native shade screen, offline input persistence, solar calculation bridge or day-profile integration missing")
require("SITE GPS & SOLAR RESOURCE" in kotlin and "gensolar_native_location" in kotlin and "requestNativeGps" in kotlin and "onNativeGps(latitude: Double" in kotlin and "navigator.geolocation.getCurrentPosition" in kotlin and 'selected == "location"' in kotlin, "Native GPS/resource screen, Android geolocation bridge or project synchronization missing")
require("SAVING GPS AND RESOURCE DATA" in kotlin and "PROJECT SAVE FAILED" in kotlin, "Native GPS screen must report the confirmed project save result")
require("STRUCTURAL ENGINEERING" in kotlin and "gensolar_native_staad" in kotlin and "bridge.generateStaad()" in kotlin and "staadSpan" in kotlin and 'selected == "staad"' in kotlin, "Native STAAD geometry/load workspace or structural model-generation bridge missing")
require("bridge.clearElectrical()" in kotlin and "project.layout" in kotlin and "project.electrical" in kotlin, "Electrical bridge must require a fresh PV layout and clear stale calculation results")
require(all(marker not in kotlin for marker in ("typeof state", "state.layout", "state.electrical", "typeof save", "staadGenerate()", "typeof model")), "Compose workspaces must not call private IIFE variables directly; use GenSolarEpcBridge")
require("bridge.clearLayout()" in kotlin and "bridge.getProjectSummary()" in kotlin and "project.layout" in kotlin, "PV optimizer must clear stale results and read the shared project through the controlled bridge")
require("BackHandler(enabled = selected != \"dashboard\")" in kotlin, "Android back navigation must return from workspaces to Home")
require('browser.evaluateJavascript(' in kotlin and "window.GenSolarEpcBridge.navigate" in kotlin, "Native workspace navigation must call the controlled shared workspace router")
for destination in ("location", "bom", "procurement", "construction", "dpr"):
    require(f'"{destination}"' in kotlin, "native dashboard or More menu destination missing: " + destination)

solver = ROOT / "app/src/main/assets/structural_solver.js"
require(solver.exists() and "GenSolarStructuralSolver" in solver.read_text(encoding="utf-8"), "Inbuilt structural solver asset missing")
require("staadRunAnalysis" in html and "staadAnalysisSchedule" in html, "Inbuilt structural analysis UI is not wired")
shade = ROOT / "app/src/main/assets/shade_engine.js"
require(shade.exists() and "GenSolarShadeEngine" in shade.read_text(encoding="utf-8"), "Solar shade engine asset missing")
require("GenSolarShadeEngine.assess" in html and "GenSolarShadeEngine.dayProfile" in html, "Solar shade engine is not wired to the UI")
require("obstructionWidthDeg" in html and "shadeInterval" in html, "Directional obstruction controls or day profile interval missing")
subprocess.run(["node", str(ROOT / "tools/test_structural_solver.js")], cwd=ROOT, check=True)
subprocess.run(["node", str(ROOT / "tools/test_shade_engine.js")], cwd=ROOT, check=True)
print(f"Android modernization checks passed: Kotlin 2.0.21, Compose Material 3, Java 17, secure local WebView bridge, {len(audit.ids)} unique HTML IDs and all native navigation targets.")
