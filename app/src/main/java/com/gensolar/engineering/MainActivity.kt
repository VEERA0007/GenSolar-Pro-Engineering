package com.gensolar.engineering

import android.Manifest
import androidx.activity.ComponentActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val BrandGreen = Color(0xFF103C34)
private val AccentGreen = Color(0xFF087F5B)
private val Canvas = Color(0xFFF3F6F5)

class MainActivity : ComponentActivity() {
    private var webView: WebView? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null
    private var nativeGpsCallback: ((Double, Double, Double) -> Unit)? = null
    private var nativeGpsErrorCallback: ((String) -> Unit)? = null
    private lateinit var filePicker: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var exportDocument: androidx.activity.result.ActivityResultLauncher<Intent>
    private var pendingExport: Pair<String, String>? = null
    private lateinit var locationPermission: androidx.activity.result.ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 60, 52)
        window.navigationBarColor = android.graphics.Color.rgb(16, 60, 52)

        filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileCallback ?: return@registerForActivityResult
            val uris = if (result.resultCode == RESULT_OK) {
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            } else null
            callback.onReceiveValue(uris)
            fileCallback = null
        }
        exportDocument = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val export = pendingExport
            pendingExport = null
            val uri = if (result.resultCode == RESULT_OK) result.data?.data else null
            if (export == null || uri == null) return@registerForActivityResult
            try {
                val stream = contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("The selected destination could not be opened.")
                stream.use { it.write(export.second.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(this, "Saved ${export.first}", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(this, "Export failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val granted = grants.values.any { it }
            geoCallback?.invoke(geoOrigin.orEmpty(), granted, false)
            geoCallback = null
            geoOrigin = null
        }

        val browser = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(243, 246, 245))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                setGeolocationEnabled(true)
                defaultTextEncodingName = "UTF-8"
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url
                    return url.scheme != "file" && url.scheme != "https" && url.scheme != "http"
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.evaluateJavascript(
                        "(function(){var s=document.getElementById('gensolar-native-shell');if(!s){s=document.createElement('style');s.id='gensolar-native-shell';s.textContent='.top,.stagewrap,.tabswrap,.foot{display:none!important}.main{padding-top:16px;padding-bottom:24px;max-width:1180px}body{padding-bottom:0!important}';document.head.appendChild(s)}})();",
                        null
                    )
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                    val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine || coarse) callback.invoke(origin, true, false)
                    else {
                        geoOrigin = origin
                        geoCallback = callback
                        locationPermission.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                }

                override fun onShowFileChooser(
                    view: WebView,
                    callback: ValueCallback<Array<Uri>>,
                    params: FileChooserParams
                ): Boolean {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = callback
                    return try {
                        filePicker.launch(params.createIntent().apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                        })
                        true
                    } catch (_: Exception) {
                        fileCallback?.onReceiveValue(null)
                        fileCallback = null
                        Toast.makeText(this@MainActivity, "Unable to open the device file picker.", Toast.LENGTH_LONG).show()
                        false
                    }
                }
            }
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onNativeGps(latitude: Double, longitude: Double, accuracy: Double) {
                    runOnUiThread {
                        nativeGpsCallback?.invoke(latitude, longitude, accuracy)
                        nativeGpsCallback = null
                        nativeGpsErrorCallback = null
                    }
                }

                @JavascriptInterface
                fun onNativeGpsError(message: String) {
                    runOnUiThread {
                        nativeGpsErrorCallback?.invoke(message)
                        nativeGpsCallback = null
                        nativeGpsErrorCallback = null
                    }
                }

                @JavascriptInterface
                fun printPage() {
                    runOnUiThread {
                        try {
                            val manager = getSystemService(PRINT_SERVICE) as PrintManager
                            manager.print(
                                "GenSolar-EPC-Report",
                                createPrintDocumentAdapter("GenSolar EPC"),
                                PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                    .build()
                            )
                        } catch (_: Exception) {
                            Toast.makeText(this@MainActivity, "Print service unavailable.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }, "GenSolarNative")
            loadUrl("file:///android_asset/index.html")
        }
        webView = browser

        onBackPressedDispatcher.addCallback(this) {
            if (browser.canGoBack()) browser.goBack() else finish()
        }

        setContent {
            GenSolarApp(
                browser = browser,
                onNavigate = ::navigateWorkspace,
                onRequestGps = ::requestNativeGps,
                onExportFile = ::exportTextFile
            )
        }
    }

    private fun exportTextFile(fileName: String, content: String, mimeType: String) {
        pendingExport = fileName to content
        try {
            exportDocument.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
            })
        } catch (error: Exception) {
            pendingExport = null
            Toast.makeText(this, "Unable to start export: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNativeGps(
        onResult: (Double, Double, Double) -> Unit,
        onError: (String) -> Unit
    ) {
        nativeGpsCallback = onResult
        nativeGpsErrorCallback = onError
        val browser = webView
        if (browser == null) {
            nativeGpsErrorCallback?.invoke("The engineering workspace is not ready.")
            nativeGpsCallback = null
            nativeGpsErrorCallback = null
            return
        }
        browser.evaluateJavascript(
            "(function(){if(!navigator.geolocation){GenSolarNative.onNativeGpsError('Geolocation is unavailable on this device.');return 'unavailable';}navigator.geolocation.getCurrentPosition(function(p){GenSolarNative.onNativeGps(p.coords.latitude,p.coords.longitude,p.coords.accuracy||0);},function(e){GenSolarNative.onNativeGpsError(e.message||'Unable to read device location.');},{enableHighAccuracy:true,timeout:20000,maximumAge:0});return 'requested';})()",
            null
        )
    }

    private fun navigateWorkspace(destination: String) {
        val browser = webView ?: return
        val allowedDestinations = setOf(
            "dashboard", "site", "location", "layout", "shade", "draw",
            "cadstudio", "electrical", "staad", "bom", "procurement",
            "construction", "dpr"
        )
        if (destination !in allowedDestinations) return
        // Use the workspace's own router. Footer buttons do not exist for every view
        // (notably BOM and STAAD), so selector-based navigation silently failed there.
        browser.evaluateJavascript(
            "if (window.GenSolarEpcBridge) { window.GenSolarEpcBridge.navigate('$destination'); }",
            null
        )
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        webView?.apply {
            stopLoading()
            removeJavascriptInterface("GenSolarNative")
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenSolarApp(
    browser: WebView,
    onNavigate: (String) -> Unit,
    onRequestGps: ((Double, Double, Double) -> Unit, (String) -> Unit) -> Unit,
    onExportFile: (String, String, String) -> Unit
) {
    var selected by remember { mutableStateOf("dashboard") }
    var moreExpanded by remember { mutableStateOf(false) }
    var advancedSite by remember { mutableStateOf(false) }
    var advancedLayout by remember { mutableStateOf(false) }
    var advancedElectrical by remember { mutableStateOf(false) }
    var advancedCad by remember { mutableStateOf(false) }
    var advancedShade by remember { mutableStateOf(false) }
    var advancedLocation by remember { mutableStateOf(false) }
    var advancedStaad by remember { mutableStateOf(false) }
    var advancedBom by remember { mutableStateOf(false) }
    var advancedConstruction by remember { mutableStateOf(false) }
    var advancedProcurement by remember { mutableStateOf(false) }
    BackHandler(enabled = selected != "dashboard") {
        if (selected == "site" && advancedSite) {
            advancedSite = false
        } else if (selected == "layout" && advancedLayout) {
            advancedLayout = false
        } else if (selected == "electrical" && advancedElectrical) {
            advancedElectrical = false
        } else if (selected == "cadstudio" && advancedCad) {
            advancedCad = false
        } else if (selected == "shade" && advancedShade) {
            advancedShade = false
        } else if (selected == "location" && advancedLocation) {
            advancedLocation = false
        } else if (selected == "staad" && advancedStaad) {
            advancedStaad = false
            advancedBom = false
        } else if (selected == "bom" && advancedBom) {
            advancedBom = false
        } else if (selected == "construction" && advancedConstruction) {
            advancedConstruction = false
        } else if (selected == "procurement" && advancedProcurement) {
            advancedProcurement = false
        } else {
            selected = "dashboard"
            advancedSite = false
            advancedLayout = false
            advancedElectrical = false
            advancedCad = false
            advancedShade = false
            advancedLocation = false
            advancedStaad = false
            advancedBom = false
            advancedConstruction = false
            advancedProcurement = false
            onNavigate("dashboard")
        }
    }
    val context = LocalContext.current
    val destinations = listOf(
        NativeDestination("dashboard", "Home", "⌂"),
        NativeDestination("site", "Survey", "⌖"),
        NativeDestination("layout", "PV Design", "▦"),
        NativeDestination("cadstudio", "CAD / 3D", "⌗")
    )
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AccentGreen,
            onPrimary = Color.White,
            secondary = Color(0xFFF3B51B),
            background = Canvas,
            surface = Color.White,
            onSurface = Color(0xFF142B28)
        )
    ) {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GENSOLAR PRO", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.1.sp)
                            Text("EPC ENGINEERING STUDIO", fontSize = 9.sp, letterSpacing = 1.4.sp, color = Color(0xFFD0E4DC))
                        }
                    },
                    navigationIcon = {
                        Surface(
                            modifier = Modifier.padding(start = 12.dp).size(38.dp),
                            shape = CircleShape,
                            color = Color(0xFFF3B51B)
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Text("GS", color = BrandGreen, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    },
                    actions = {
                        androidx.compose.material3.TextButton(onClick = { moreExpanded = true }) {
                            Text("MORE  ⋮", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                            listOf(
                                "location" to "GPS / Map",
                                "electrical" to "Electrical design",
                                "staad" to "STAAD structural",
                                "bom" to "BOM / BOQ",
                                "procurement" to "Procurement",
                                "construction" to "Construction",
                                "dpr" to "DPR & handover"
                            ).forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        moreExpanded = false
                                        selected = key
                                        advancedSite = false
                                        advancedLayout = false
                                        advancedElectrical = false
                                        advancedCad = false
                                        advancedShade = false
                                        advancedLocation = false
                                        advancedStaad = false
                                        advancedBom = false
                                        advancedConstruction = false
                                        advancedProcurement = false
                                        onNavigate(key)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BrandGreen,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = selected == destination.key,
                            onClick = {
                                selected = destination.key
                                advancedSite = false
                                advancedLayout = false
                                advancedElectrical = false
                                advancedCad = false
                                advancedShade = false
                                advancedLocation = false
                                advancedStaad = false
                                advancedBom = false
                                advancedConstruction = false
                                advancedProcurement = false
                                onNavigate(destination.key)
                            },
                            icon = { Text(destination.symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                            label = { Text(destination.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                            alwaysShowLabel = true
                        )
                    }
                    NavigationBarItem(
                        selected = selected !in destinations.map { it.key },
                        onClick = { moreExpanded = true },
                        icon = { Text("⋯", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                        label = { Text("More", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        alwaysShowLabel = true
                    )
                }
            }
        ) { padding ->
            if (selected == "dashboard") {
                NativeDashboard(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onNavigate = { destination ->
                        selected = destination
                        advancedSite = false
                        advancedLayout = false
                        advancedElectrical = false
                        advancedCad = false
                        advancedShade = false
                        advancedLocation = false
                        advancedStaad = false
                        advancedBom = false
                        advancedConstruction = false
                        advancedProcurement = false
                        onNavigate(destination)
                    }
                )
            } else if (selected == "measurement") {
                MeasurementScreen(modifier = Modifier.fillMaxSize().padding(padding), onRequestGps = onRequestGps)\n            } else if (selected == "staad" && !advancedStaad) {
                NativeStaadScreen(browser, Modifier.fillMaxSize().padding(padding)) { advancedStaad = true }
            } else if (selected == "location" && !advancedLocation) {
                NativeLocationScreen(browser, Modifier.fillMaxSize().padding(padding), onRequestGps) { advancedLocation = true }
            } else if (selected == "shade" && !advancedShade) {
                NativeShadeScreen(browser, Modifier.fillMaxSize().padding(padding)) { advancedShade = true }
            } else if (selected == "cadstudio" && !advancedCad) {
                NativeCadScreen(browser, Modifier.fillMaxSize().padding(padding)) { advancedCad = true }
            } else if (selected == "electrical" && !advancedElectrical) {
                NativeElectricalScreen(browser, Modifier.fillMaxSize().padding(padding)) { advancedElectrical = true }
            } else if (selected == "layout" && !advancedLayout) {
                NativePvDesignScreen(
                    browser = browser,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onOpenAdvanced = { advancedLayout = true }
                )
            } else if (selected == "site" && !advancedSite) {
                NativeSurveyScreen(
                    browser = browser,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onOpenAdvanced = { advancedSite = true }
                )
            } else if (selected == "procurement" && !advancedProcurement) {
                NativeProcurementScreen(
                    browser = browser,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onOpenAdvanced = { advancedProcurement = true },
                    onExportCsv = { name, csv -> onExportFile(name, csv, "text/csv") }
                )
            } else if (selected == "handover") {
                NativeHandoverScreen(
                    browser = browser,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            } else if (selected == "commissioning") {
                NativeCommissioningScreen(
                    browser = browser,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onExportCsv = { name, csv -> onExportFile(name, csv, "text/csv") }
                )
            } else if (selected == "dpr") {
                NativeDprScreen(
                    browser = browser,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onExport = { name, content, mime -> onExportFile(name, content, mime) }
                )
            } else if (selected == "construction" && !advancedConstruction) {
                NativeConstructionScreen(
                    browser = browser,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onOpenAdvanced = { advancedConstruction = true },
                    onExportCsv = { name, csv -> onExportFile(name, csv, "text/csv") }
                )
            } else if (selected == "bom" && !advancedBom) {
                NativeBomScreen(
                    browser = browser,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onOpenAdvanced = { advancedBom = true },
                    onExportCsv = { name, csv -> onExportFile(name, csv, "text/csv") }
                )
            } else {
                AndroidView(
                    factory = { browser },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }
}


@Composable
private fun NativeDashboard(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val workspaces = listOf(
        Triple("Site survey & GPS", "Roof measurements, site photos and coordinates.", "site"),
        Triple("Measurement & 3D survey", "Distance, object, room, rooftop and GPS measurement tools.", "measurement"),
        Triple("PV design & layout", "Module placement, setbacks and array sizing.", "layout"),
        Triple("CAD / 2D / 3D studio", "Drawing tools, DXF exchange and 3D model.", "cadstudio"),
        Triple("Solar shade analysis", "Sun path, obstruction direction and shade profile.", "shade"),
        Triple("Electrical engineering", "Strings, MPPT, cables, batteries and SLD.", "electrical"),
        Triple("STAAD structural", "Portal frames, wind basis and member checks.", "staad"),
        Triple("GPS & solar resource", "Location capture, irradiance and yield inputs.", "location"),
        Triple("BOM / BOQ", "Material take-off, quantities and cost schedule.", "bom"),
        Triple("Procurement", "Vendor quotations and purchase tracking.", "procurement"),
        Triple("Construction & QA", "Site progress, inspections and commissioning.", "construction"),
        Triple("DPR & handover", "Reports, drawing register and handover pack.", "dpr"),
        Triple("Commissioning & QA", "Electrical tests, measured results and corrective actions.", "commissioning"),
        Triple("Customer handover", "Acceptance checklist, EPC handover and close-out records.", "handover")
    )
    val filtered = workspaces.filter { item ->
        val matchesSearch = search.isBlank() || (item.first + " " + item.second).contains(search, ignoreCase = true)
        val group = when (item.third) {
            "site", "location", "measurement" -> "Survey"
            "layout", "cadstudio", "shade" -> "Design"
            "electrical", "staad" -> "Engineering"
            else -> "Delivery"
        }
        matchesSearch && (category == "All" || category == group)
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = BrandGreen)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFFF3B51B), modifier = Modifier.size(34.dp)) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text("GS", color = BrandGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                    Text("GENSOLAR PRO  /  EPC STUDIO", color = Color(0xFFE2F0E9), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Spacer(Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFF24584D)) {
                        Text("OFFLINE FIRST", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFFBDE8D3), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Your solar EPC command centre", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                Text("Survey, design, engineer and deliver projects from one connected workspace.", color = Color(0xFFD7E8E1), fontSize = 13.sp, lineHeight = 19.sp)
                Button(onClick = { onNavigate("site") }) { Text("＋  Create / open project") }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("PROJECT OVERVIEW", color = Color(0xFF60756E), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                NativeMetric("01", "Survey", Modifier.weight(1f))
                NativeMetric("02", "PV design", Modifier.weight(1f))
                NativeMetric("03", "Engineering", Modifier.weight(1f))
                NativeMetric("04", "DPR / BOQ", Modifier.weight(1f))
            }
        }

        NativeActionCard(
            "Continue with site survey",
            "Start with customer details, location, roof zones and survey evidence.",
            "Open survey",
            "site",
            onNavigate
        )

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("ENGINEERING WORKSPACES", color = Color(0xFF60756E), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                placeholder = { Text("Search tools, drawings or reports…", fontSize = 13.sp) },
                leadingIcon = { Text("⌕", fontSize = 22.sp, color = AccentGreen) }
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Survey", "Design", "Engineering", "Delivery").forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item, fontSize = 12.sp) }
                    )
                }
            }
            if (filtered.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Text("No workspace matches this search.", modifier = Modifier.padding(18.dp), color = Color(0xFF60756E), fontSize = 13.sp)
                }
            } else {
                filtered.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { item ->
                            NativeWorkspaceTile(item.first, item.second, item.third, onNavigate, Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1ED))
        ) {
            Text(
                "Engineering outputs remain preliminary until checked against approved datasheets, applicable standards, verified site conditions and qualified engineer sign-off.",
                modifier = Modifier.padding(14.dp),
                color = Color(0xFF38574D),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NativeWorkspaceTile(
    title: String,
    description: String,
    destination: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = when (destination) {
        "site", "location", "measurement" -> "⌖"
        "layout" -> "▦"
        "cadstudio" -> "⌗"
        "shade" -> "☼"
        "electrical" -> "ϟ"
        "staad" -> "⌂"
        "bom" -> "▤"
        "procurement" -> "⇄"
        "construction" -> "✓"
        else -> "▧"
    }
    Card(
        modifier = modifier.height(166.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F3EE), modifier = Modifier.size(36.dp)) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text(symbol, color = AccentGreen, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(title, color = Color(0xFF142B28), fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 2)
            Text(description, color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2)
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.TextButton(
                onClick = { onNavigate(destination) },
                modifier = Modifier.height(38.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Text("Open workspace  →", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NativeMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, color = AccentGreen, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color(0xFF536A63), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NativeActionCard(
    title: String,
    description: String,
    action: String,
    destination: String,
    onNavigate: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F3EE)
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text("✦", color = AccentGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color(0xFF142B28), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, color = Color(0xFF667A75), fontSize = 11.sp, lineHeight = 15.sp)
            }
            androidx.compose.material3.TextButton(onClick = { onNavigate(destination) }) {
                Text(action, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun NativePvDesignScreen(
    browser: WebView,
    modifier: Modifier = Modifier,
    onOpenAdvanced: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gensolar_native_pv_design", android.content.Context.MODE_PRIVATE) }
    var capacity by remember { mutableStateOf(prefs.getString("capacity", "100") ?: "100") }
    var moduleWp by remember { mutableStateOf(prefs.getString("wp", "550") ?: "550") }
    var moduleLength by remember { mutableStateOf(prefs.getString("moduleLength", "2278") ?: "2278") }
    var moduleWidth by remember { mutableStateOf(prefs.getString("moduleWidth", "1134") ?: "1134") }
    var orientation by remember { mutableStateOf(prefs.getString("orientation", "auto") ?: "auto") }
    var moduleGap by remember { mutableStateOf(prefs.getString("gap", "20") ?: "20") }
    var modulesPerRow by remember { mutableStateOf(prefs.getString("perRow", "10") ?: "10") }
    var sunAltitude by remember { mutableStateOf(prefs.getString("sunAlt", "30") ?: "30") }
    var areaFactor by remember { mutableStateOf(prefs.getString("areaFactor", "1.15") ?: "1.15") }
    var obstacleClearance by remember { mutableStateOf(prefs.getString("obstacleClearance", "0.5") ?: "0.5") }
    var circles by remember { mutableStateOf(prefs.getString("obstacleCircles", "") ?: "") }
    var rectangles by remember { mutableStateOf(prefs.getString("obstacleRectangles", "") ?: "") }
    var corridorEdge by remember { mutableStateOf(prefs.getString("accessCorridorEdge", "left") ?: "left") }
    var corridorWidth by remember { mutableStateOf(prefs.getString("accessCorridorWidth", "0.6") ?: "0.6") }
    var result by remember { mutableStateOf("No PV layout generated in this session.") }
    var status by remember { mutableStateOf("Inputs are saved locally when you generate a layout.") }
    val targetModules = ((capacity.toDoubleOrNull() ?: 0.0) * 1000.0 / (moduleWp.toDoubleOrNull() ?: 1.0)).let {
        if (it.isFinite() && it > 0) kotlin.math.ceil(it).toInt() else 0
    }
    val moduleArea = ((moduleLength.toDoubleOrNull() ?: 0.0) / 1000.0) * ((moduleWidth.toDoubleOrNull() ?: 0.0) / 1000.0)

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("PV ARRAY DESIGN", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text("Size the array. Fit the roof.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Configure modules, capacity and access constraints, then run the existing roof-fit optimizer and DXF / CSV workflow.", color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("01  ARRAY CAPACITY", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Target DC capacity (kWp)", capacity, { capacity = it }, Modifier.weight(1f))
                    SurveyInput("Module rating (Wp)", moduleWp, { moduleWp = it }, Modifier.weight(1f))
                }
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE8F3EE)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TARGET MODULE COUNT", color = Color(0xFF60756E), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("$targetModules modules", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(String.format("%.2f kWp", targetModules * (moduleWp.toDoubleOrNull() ?: 0.0) / 1000.0), color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("02  MODULE & ROW GEOMETRY", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Module length (mm)", moduleLength, { moduleLength = it }, Modifier.weight(1f))
                    SurveyInput("Module width (mm)", moduleWidth, { moduleWidth = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Module gap (mm)", moduleGap, { moduleGap = it }, Modifier.weight(1f))
                    SurveyInput("Modules per row", modulesPerRow, { modulesPerRow = it }, Modifier.weight(1f))
                }
                SurveyInput("Orientation (auto / portrait / landscape)", orientation, { orientation = it })
                SurveyInput("Sun altitude for pitch (degrees)", sunAltitude, { sunAltitude = it })
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF3F6F5)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("SINGLE MODULE FOOTPRINT", color = Color(0xFF60756E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%.3f m²", moduleArea), color = BrandGreen, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("03  ROOF ACCESS & OBSTACLES", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                SurveyInput("Planning area factor", areaFactor, { areaFactor = it })
                SurveyInput("Obstacle clearance (m)", obstacleClearance, { obstacleClearance = it })
                SurveyInput("Edge corridor (left / right / top / bottom / none)", corridorEdge, { corridorEdge = it })
                SurveyInput("Corridor width (m)", corridorWidth, { corridorWidth = it })
                SurveyInput("Circular obstacles: x,y,radius (m)", circles, { circles = it }, minLines = 2)
                SurveyInput("Rectangular obstacles: x,y,width,height (m)", rectangles, { rectangles = it }, minLines = 2)
            }
        }
        Button(onClick = {
            fun number(value: String): Double? = value.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
            val capacityValue = number(capacity)
            val modulePower = number(moduleWp)
            val lengthValue = number(moduleLength)
            val widthValue = number(moduleWidth)
            val gapValue = number(moduleGap)
            val rowCount = modulesPerRow.trim().toIntOrNull()
            val altitudeValue = number(sunAltitude)
            val factorValue = number(areaFactor)
            val clearanceValue = number(obstacleClearance)
            val corridorValue = number(corridorWidth)
            val validationError = when {
                capacityValue == null || capacityValue <= 0.0 || capacityValue > 100000.0 -> "Capacity must be between 0 and 100,000 kWp."
                modulePower == null || modulePower !in 1.0..2000.0 -> "Module rating must be between 1 and 2,000 Wp."
                lengthValue == null || lengthValue !in 1.0..3000.0 -> "Module length must be between 1 and 3,000 mm."
                widthValue == null || widthValue !in 1.0..3000.0 -> "Module width must be between 1 and 3,000 mm."
                orientation.trim().lowercase() !in setOf("auto", "portrait", "landscape") -> "Orientation must be auto, portrait or landscape."
                gapValue == null || gapValue !in 0.0..200.0 -> "Module gap must be between 0 and 200 mm."
                rowCount == null || rowCount !in 1..100 -> "Modules per row must be a whole number from 1 to 100."
                altitudeValue == null || altitudeValue !in 0.0..90.0 -> "Sun altitude must be between 0 and 90 degrees."
                factorValue == null || factorValue !in 0.1..3.0 -> "Planning area factor must be between 0.1 and 3.0."
                clearanceValue == null || clearanceValue !in 0.0..20.0 -> "Obstacle clearance must be between 0 and 20 metres."
                corridorEdge.trim().lowercase() !in setOf("left", "right", "top", "bottom", "none") -> "Edge corridor must be left, right, top, bottom or none."
                corridorValue == null || corridorValue !in 0.0..20.0 -> "Corridor width must be between 0 and 20 metres."
                else -> null
            }
            if (validationError != null) {
                status = validationError
                return@Button
            }
            val values = linkedMapOf(
                "capacity" to capacity, "wp" to moduleWp, "moduleLength" to moduleLength,
                "moduleWidth" to moduleWidth, "orientation" to orientation.lowercase(),
                "gap" to moduleGap, "perRow" to modulesPerRow, "sunAlt" to sunAltitude,
                "areaFactor" to areaFactor, "obstacleClearance" to obstacleClearance,
                "obstacleCircles" to circles, "obstacleRectangles" to rectangles,
                "accessCorridorEdge" to corridorEdge.lowercase(), "accessCorridorWidth" to corridorWidth
            )
            prefs.edit().apply { values.forEach { (key, value) -> putString(key, value) } }.apply()
            val payload = JSONObject(values).toString()
            val script = """
                (function(){
                  var data = $payload;
                  var root = document.getElementById('layout');
                  if (!root) return JSON.stringify({error:'PV layout workspace is unavailable.'});
                  Object.keys(data).forEach(function(key) {
                    var field = root.querySelector('[data-k="' + key + '"]');
                    if (field) {
                      field.value = data[key];
                      field.dispatchEvent(new Event('input', {bubbles:true}));
                      field.dispatchEvent(new Event('change', {bubbles:true}));
                    }
                  });
                  var button = document.getElementById('generateLayout');
                  if (!button) return JSON.stringify({error:'Layout optimizer button is unavailable.'});
                  var bridge = window.GenSolarEpcBridge;
                  if (!bridge) return JSON.stringify({error:'Native project bridge is unavailable.'});
                  bridge.clearLayout();
                  button.click();
                  var project = bridge.getProjectSummary();
                  if (!project.layout) return JSON.stringify({error:'No layout was generated. Check the site survey, roof dimensions and obstacle positions.'});
                  return JSON.stringify({
                    count:project.layout.count, kwp:project.layout.kwp,
                    pitch:project.layout.pitch, orientation:project.layout.orientation,
                    area:(project.layout.zoneStats||[]).reduce(function(a,z){return a+z.area;},0),
                    warnings:document.getElementById('layoutWarnings')?.textContent||''
                  });
                })()
            """.trimIndent()
            browser.evaluateJavascript(script) { raw ->
                try {
                    val response = org.json.JSONTokener(raw ?: "{}").nextValue() as? String ?: "{}"
                    val json = JSONObject(response)
                    if (json.has("error")) {
                        status = json.optString("error")
                    } else {
                        val count = json.optInt("count")
                        val kwp = json.optDouble("kwp")
                        val pitch = json.optDouble("pitch")
                        val orientationResult = json.optString("orientation")
                        val area = json.optDouble("area")
                        result = "$count modules · ${String.format("%.2f", kwp)} kWp · $orientationResult · pitch ${String.format("%.3f", pitch)} m · ${String.format("%.1f", area)} m² surveyed"
                        status = json.optString("warnings", "Layout generated. Review roof access and structural assumptions.")
                    }
                } catch (_: Exception) {
                    status = "Optimizer response could not be read. Open the detailed workspace to inspect its output."
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Generate PV layout & sizing") }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("OPTIMIZER OUTPUT", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text(result, color = BrandGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(status, color = Color(0xFF60756E), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        androidx.compose.material3.OutlinedButton(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) {
            Text("Open detailed PV layout, drawings & exports")
        }
        Text("Concept arrangement only. Confirm surveyed geometry, fire access, shade, structural capacity and standards before construction.", color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(8.dp))
    }
}


@Composable
private fun NativeElectricalScreen(browser: WebView, modifier: Modifier = Modifier, onOpenAdvanced: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gensolar_native_electrical", android.content.Context.MODE_PRIVATE) }
    val defaults = listOf("acKw" to "80", "mpptMin" to "200", "mpptMax" to "1000", "maxDc" to "1100", "minTemp" to "5", "mpptCurrent" to "30", "bifacialCurrentGain" to "0", "mpptCount" to "4", "maxCellTemp" to "70", "vmpCoeff" to "-0.35")
    val fields = remember { defaults.associate { (key, value) -> key to mutableStateOf(prefs.getString(key, value) ?: value) } }
    var voc by remember { mutableStateOf(prefs.getString("voc", "49.8") ?: "49.8") }
    var vmp by remember { mutableStateOf(prefs.getString("vmp", "41.8") ?: "41.8") }
    var isc by remember { mutableStateOf(prefs.getString("isc", "14") ?: "14") }
    var imp by remember { mutableStateOf(prefs.getString("imp", "13.2") ?: "13.2") }
    var vocCoeff by remember { mutableStateOf(prefs.getString("vocCoeff", "-0.28") ?: "-0.28") }
    var output by remember { mutableStateOf("Generate a PV layout, then run electrical checks.") }
    var status by remember { mutableStateOf("Inputs are stored offline when calculations are run.") }
    val labels = listOf("acKw" to "Inverter AC capacity (kW)", "mpptMin" to "Minimum MPPT voltage (V)", "mpptMax" to "Maximum MPPT voltage (V)", "maxDc" to "Maximum DC voltage (V)", "minTemp" to "Minimum design temperature (°C)", "mpptCurrent" to "Max operating current / MPPT (A)", "bifacialCurrentGain" to "Bifacial current gain (%)", "mpptCount" to "MPPT channel count", "maxCellTemp" to "Maximum cell temperature (°C)", "vmpCoeff" to "Vmp coefficient (%/°C)")

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ELECTRICAL ENGINEERING", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Validate every PV string.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Temperature-adjusted string voltage, MPPT current capacity and DC/AC ratio using the existing engineering calculator.", color = Color(0xFFD7E8E1), fontSize = 12.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("01  MODULE DATASHEET", color = AccentGreen, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Voc (V)", voc, { voc = it }, Modifier.weight(1f))
                    SurveyInput("Vmp (V)", vmp, { vmp = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Isc (A)", isc, { isc = it }, Modifier.weight(1f))
                    SurveyInput("Imp (A)", imp, { imp = it }, Modifier.weight(1f))
                }
                SurveyInput("Voc coefficient (%/°C)", vocCoeff, { vocCoeff = it })
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("02  INVERTER & DESIGN LIMITS", color = AccentGreen, fontWeight = FontWeight.ExtraBold)
                labels.forEach { (key, label) -> SurveyInput(label, fields.getValue(key).value, { fields.getValue(key).value = it }) }
            }
        }
        Button(onClick = {
            fun number(value: String): Double? = value.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
            val vocValue = number(voc)
            val vmpValue = number(vmp)
            val iscValue = number(isc)
            val impValue = number(imp)
            val vocTemperature = number(vocCoeff)
            val numeric = fields.mapValues { (_, state) -> number(state.value) }
            val acValue = numeric["acKw"]
            val mpptMinimum = numeric["mpptMin"]
            val mpptMaximum = numeric["mpptMax"]
            val dcMaximum = numeric["maxDc"]
            val minimumTemperature = numeric["minTemp"]
            val currentLimit = numeric["mpptCurrent"]
            val bifacialGain = numeric["bifacialCurrentGain"]
            val trackerCount = fields.getValue("mpptCount").value.trim().toIntOrNull()
            val cellTemperature = numeric["maxCellTemp"]
            val vmpTemperature = numeric["vmpCoeff"]
            val validationError = when {
                vocValue == null || vocValue <= 0.0 -> "Module Voc must be greater than zero."
                vmpValue == null || vmpValue <= 0.0 || vmpValue >= vocValue -> "Module Vmp must be positive and lower than Voc."
                iscValue == null || iscValue <= 0.0 -> "Module Isc must be greater than zero."
                impValue == null || impValue <= 0.0 || impValue > iscValue -> "Module Imp must be positive and no greater than Isc."
                vocTemperature == null || vocTemperature !in -1.0..0.0 -> "Voc temperature coefficient must be between -1 and 0 %/°C."
                acValue == null || acValue <= 0.0 || acValue > 100000.0 -> "Inverter AC capacity must be between 0 and 100,000 kW."
                mpptMinimum == null || mpptMinimum <= 0.0 -> "Minimum MPPT voltage must be greater than zero."
                mpptMaximum == null || mpptMaximum <= 0.0 || mpptMaximum <= (mpptMinimum ?: 0.0) -> "Maximum MPPT voltage must exceed minimum MPPT voltage."
                dcMaximum == null || dcMaximum < (mpptMaximum ?: Double.POSITIVE_INFINITY) -> "Maximum DC voltage must be at least the maximum MPPT voltage."
                minimumTemperature == null || minimumTemperature !in -40.0..40.0 -> "Minimum design temperature must be between -40 and 40 °C."
                currentLimit == null || currentLimit <= 0.0 -> "MPPT operating current limit must be greater than zero."
                bifacialGain == null || bifacialGain !in 0.0..50.0 -> "Bifacial current gain must be between 0 and 50%."
                trackerCount == null || trackerCount !in 1..100 -> "MPPT channel count must be a whole number from 1 to 100."
                cellTemperature == null || cellTemperature !in 20.0..100.0 -> "Maximum cell temperature must be between 20 and 100 °C."
                vmpTemperature == null || vmpTemperature !in -1.0..0.0 -> "Vmp temperature coefficient must be between -1 and 0 %/°C."
                else -> null
            }
            if (validationError != null) {
                status = validationError
                return@Button
            }
            val values = linkedMapOf("voc" to voc, "vmp" to vmp, "isc" to isc, "imp" to imp, "vocCoeff" to vocCoeff)
            fields.forEach { (key, value) -> values[key] = value.value }
            prefs.edit().apply { values.forEach { (key, value) -> putString(key, value) } }.apply()
            val payload = JSONObject(values).toString()
            val script = """
                (function(){
                  var data = $payload, root = document.getElementById('electrical');
                  if (!root) return JSON.stringify({error:'Electrical workspace unavailable.'});
                  Object.keys(data).forEach(function(key) {
                    var field = root.querySelector('[data-k="' + key + '"]');
                    if (field) { field.value = data[key]; field.dispatchEvent(new Event('input',{bubbles:true})); field.dispatchEvent(new Event('change',{bubbles:true})); }
                  });
                  var button = document.getElementById('calcElectrical');
                  if (!button) return JSON.stringify({error:'Electrical calculation action unavailable.'});
                  var bridge = window.GenSolarEpcBridge;
                  if (!bridge) return JSON.stringify({error:'Native project bridge is unavailable.'});
                  if (!bridge.getProjectSummary().layout) return JSON.stringify({error:'Generate a PV layout first.'});
                  bridge.clearElectrical();
                  button.click();
                  var project = bridge.getProjectSummary();
                  if (!project.electrical) return JSON.stringify({error:'No result. Verify module and inverter inputs.'});
                  return JSON.stringify({strings:project.electrical.strings,modules:project.electrical.modules,warning:document.getElementById('electricalWarning')?.textContent||''});
                })()
            """.trimIndent()
            browser.evaluateJavascript(script) { raw ->
                try {
                    val response = org.json.JSONTokener(raw ?: "{}").nextValue() as? String ?: "{}"
                    val json = JSONObject(response)
                    if (json.has("error")) status = json.optString("error")
                    else {
                        output = "${json.optInt("strings")} strings · ${json.optInt("modules")} modules"
                        status = json.optString("warning", "Review detailed calculation output.")
                    }
                } catch (_: Exception) { status = "Could not read the calculation response. Open detailed electrical workspace." }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run electrical string checks") }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ENGINEERING RESULT", color = AccentGreen, fontWeight = FontWeight.ExtraBold)
                Text(output, color = BrandGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(status, color = Color(0xFF60756E), fontSize = 11.sp)
            }
        }
        androidx.compose.material3.OutlinedButton(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) { Text("Open MPPT allocation, battery, cable & protection tools") }
        Text("Preliminary screening only. Verify inverter datasheets, MPPT operating and short-circuit limits, protection coordination, cable sizing and applicable electrical standards.", color = Color(0xFF667A75), fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
    }
}


@Composable
private fun NativeStaadScreen(browser: WebView, modifier: Modifier = Modifier, onOpenAdvanced: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gensolar_native_staad", android.content.Context.MODE_PRIVATE) }
    val defaults = listOf(
        "span" to "20", "length" to "30", "spacing" to "5", "eave" to "6", "rise" to "2",
        "dead" to "0.25", "live" to "0.75", "uplift" to "0.90", "side" to "0.60", "fy" to "250",
        "support" to "PINNED", "windFace" to "left",
        "columnSection" to "ISMB300", "rafterSection" to "ISMB300",
        "purlinSection" to "ISMC150", "braceSection" to "ISA50X50X6"
    )
    val fields = remember { defaults.associate { (key, value) -> key to mutableStateOf(prefs.getString(key, value) ?: value) } }
    var output by remember { mutableStateOf("No structural model generated in this session.") }
    var status by remember { mutableStateOf("Sample geometry is provided for initial setup. Verify every load, section and support condition for the actual project.") }
    val labels = listOf(
        "span" to "Clear building span (m)", "length" to "Building length (m)",
        "spacing" to "Target portal spacing (m)", "eave" to "Eave height (m)", "rise" to "Roof rise (m)",
        "dead" to "Roof dead load (kN/m²)", "live" to "Roof imposed load (kN/m²)",
        "uplift" to "Roof wind uplift (kN/m²)", "side" to "Side-wall wind pressure (kN/m²)",
        "fy" to "Steel yield strength fy (MPa)"
    )
    fun persist() {
        val edit = prefs.edit()
        fields.forEach { (key, value) -> edit.putString(key, value.value) }
        edit.apply()
    }
    fun generate() {
        val numeric = listOf("span", "length", "spacing", "eave", "rise", "dead", "live", "uplift", "side", "fy")
        if (numeric.any { fields[it]?.value?.toDoubleOrNull()?.isFinite() != true }) {
            status = "CHECK INPUTS"
            output = "All geometry, loading and steel-grade inputs must be valid numbers."
            return
        }
        val payload = JSONObject()
        val mapping = mapOf(
            "span" to "staadSpan", "length" to "staadLength", "spacing" to "staadFrameSpacing",
            "eave" to "staadEaveHeight", "rise" to "staadRoofRise", "dead" to "staadRoofDead",
            "live" to "staadRoofLive", "uplift" to "staadWindUplift", "side" to "staadWindSide",
            "fy" to "staadFy", "support" to "staadSupport", "windFace" to "staadWindFace",
            "columnSection" to "staadColumnSection", "rafterSection" to "staadRafterSection",
            "purlinSection" to "staadPurlinSection", "braceSection" to "staadBraceSection"
        )
        mapping.forEach { (key, domKey) -> payload.put(domKey, fields[key]?.value.orEmpty()) }
        persist()
        status = "GENERATING STRUCTURAL MODEL"
        val expression = "JSON.stringify((function(v){Object.keys(v).forEach(function(k){var e=document.querySelector('[data-k='+k+']');if(e){e.value=v[k];e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));}});var bridge=window.GenSolarEpcBridge;if(!bridge)return {ok:false,message:'Native project bridge is unavailable'};if(!bridge.saveProject())return {ok:false,message:'Project save failed'};var m=bridge.generateStaad();return {ok:!!m,frames:m?m.frameCount:0,nodes:m?m.nodes.length:0,members:m?m.members.length:0,spacing:m?m.frameSpacing:0,purlins:m?m.purlinCount:0,braces:m?m.braceCount:0,message:document.getElementById('staadStatus')?.textContent||'Model generation failed'};})($payload))"
        browser.evaluateJavascript("(function(){try{return $expression}catch(e){return JSON.stringify({ok:false,message:e.message})}})()") { raw ->
            try {
                val jsonText = org.json.JSONTokener(raw ?: "null").nextValue() as? String ?: "{}"
                val result = JSONObject(jsonText)
                if (result.optBoolean("ok")) {
                    status = "STRUCTURAL MODEL GENERATED"
                    output = "Portal frames: ${result.optInt("frames")}\nNodes: ${result.optInt("nodes")}\nMembers: ${result.optInt("members")}\nPurlins / eave struts: ${result.optInt("purlins")}\nBracing members: ${result.optInt("braces")}\nActual frame spacing: ${"%.3f".format(java.util.Locale.US, result.optDouble("spacing"))} m"
                } else {
                    status = "MODEL GENERATION BLOCKED"
                    output = result.optString("message", "Check geometry and supported ranges.")
                }
            } catch (_: Exception) {
                status = "ENGINE NOT READY"
                output = "Open the structural workspace once, then retry model generation."
            }
        }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("STRUCTURAL ENGINEERING", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Build the portal-frame model.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Generate a parametric steel portal frame and hand the model to the integrated STAAD.Pro export and in-app 2D analysis tools.", color = Color(0xFFD7E8E1), fontSize = 12.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("01  GEOMETRY & SUPPORTS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                labels.take(5).forEach { (key, label) -> SurveyInput(label, fields.getValue(key).value, { fields.getValue(key).value = it }) }
                androidx.compose.material3.Text("Support condition", color = Color(0xFF667A75), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PINNED", "FIXED").forEach { value ->
                        FilterChip(
                            selected = fields.getValue("support").value == value,
                            onClick = { fields.getValue("support").value = value },
                            label = { Text(value) }
                        )
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("02  DESIGN LOAD INPUTS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                labels.drop(5).forEach { (key, label) -> SurveyInput(label, fields.getValue(key).value, { fields.getValue(key).value = it }) }
                androidx.compose.material3.Text("Windward side", color = Color(0xFF667A75), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("left", "right").forEach { value ->
                        FilterChip(
                            selected = fields.getValue("windFace").value == value,
                            onClick = { fields.getValue("windFace").value = value },
                            label = { Text(value.uppercase()) }
                        )
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("03  SECTION TABLE ASSIGNMENTS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                listOf("columnSection" to "Column section name", "rafterSection" to "Rafter section name", "purlinSection" to "Purlin / eave strut section", "braceSection" to "Bracing section name").forEach { (key, label) ->
                    SurveyInput(label, fields.getValue(key).value, { fields.getValue(key).value = it })
                }
                Text("Section labels are sample placeholders. Confirm exact section-table names and properties in STAAD.Pro before analysis or export.", color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(status, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(output, color = Color(0xFF173B32), fontSize = 13.sp, lineHeight = 20.sp)
                Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) { Text("Generate structural model") }
                Button(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) { Text("Open STAAD solver, take-off & export workspace") }
            }
        }
        Text("The inbuilt solver covers 2D in-plane portal frames only. Longitudinal bracing, 3D effects, connections, P-Delta analysis and certified IS 800 design require further analysis in STAAD.Pro and qualified engineering review.", color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun NativeLocationScreen(
    browser: WebView,
    modifier: Modifier = Modifier,
    onRequestGps: ((Double, Double, Double) -> Unit, (String) -> Unit) -> Unit,
    onOpenAdvanced: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gensolar_native_location", android.content.Context.MODE_PRIVATE) }
    var latitude by remember { mutableStateOf(prefs.getString("lat", "") ?: "") }
    var longitude by remember { mutableStateOf(prefs.getString("lon", "") ?: "") }
    var elevation by remember { mutableStateOf(prefs.getString("elevation", "") ?: "") }
    var accuracy by remember { mutableStateOf(prefs.getString("accuracy", "") ?: "") }
    var ghi by remember { mutableStateOf(prefs.getString("ghi", "") ?: "") }
    var dni by remember { mutableStateOf(prefs.getString("dni", "") ?: "") }
    var dhi by remember { mutableStateOf(prefs.getString("dhi", "") ?: "") }
    var gti by remember { mutableStateOf(prefs.getString("gti", "") ?: "") }
    var annualYield by remember { mutableStateOf(prefs.getString("yield", "") ?: "") }
    var resourceSource by remember { mutableStateOf(prefs.getString("resourceSource", "") ?: "") }
    var status by remember { mutableStateOf("GPS NOT CAPTURED") }
    fun persist() {
        prefs.edit().putString("lat", latitude).putString("lon", longitude).putString("elevation", elevation)
            .putString("accuracy", accuracy).putString("ghi", ghi).putString("dni", dni).putString("dhi", dhi)
            .putString("gti", gti).putString("yield", annualYield).putString("resourceSource", resourceSource).apply()
    }
    fun syncProject() {
        val lat = latitude.toDoubleOrNull()
        val lon = longitude.toDoubleOrNull()
        if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            status = "INVALID COORDINATES"
            return
        }
        persist()
        val values = JSONObject().put("lat", latitude).put("lon", longitude).put("elevation", elevation)
            .put("gpsAccuracy", accuracy).put("ghi", ghi).put("dni", dni).put("dhi", dhi).put("gti", gti)
            .put("yield", annualYield).put("resourceSource", resourceSource)
        status = "SAVING GPS AND RESOURCE DATA"
        browser.evaluateJavascript(
            "(function(v){Object.keys(v).forEach(function(k){var e=document.querySelector('[data-k='+k+']');if(e){e.value=v[k];e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));}});var bridge=window.GenSolarEpcBridge;if(!bridge||!bridge.saveProject())return 'save-failed';var b=document.getElementById('pinCoordinates');if(b)b.click();return 'synced';})($values)"
        ) { raw ->
            val result = try { org.json.JSONTokener(raw ?: "\"save-failed\"").nextValue() as? String ?: "save-failed" } catch (_: Exception) { "save-failed" }
            status = if (result == "synced") "SAVED OFFLINE & SYNCED TO PROJECT" else "PROJECT SAVE FAILED · $result"
        }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("SITE GPS & SOLAR RESOURCE", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Pin the project. Record the resource.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Capture device coordinates, record location accuracy and sync resource inputs with the shared EPC project.", color = Color(0xFFD7E8E1), fontSize = 12.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("01  GPS COORDINATES", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Latitude (°)", latitude, { latitude = it }, Modifier.weight(1f))
                    SurveyInput("Longitude (°)", longitude, { longitude = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Elevation (m)", elevation, { elevation = it }, Modifier.weight(1f))
                    SurveyInput("Accuracy (m)", accuracy, { accuracy = it }, Modifier.weight(1f))
                }
                Button(onClick = {
                    status = "REQUESTING DEVICE GPS"
                    onRequestGps(
                        { lat, lon, acc ->
                            latitude = "%.7f".format(java.util.Locale.US, lat)
                            longitude = "%.7f".format(java.util.Locale.US, lon)
                            accuracy = "%.1f".format(java.util.Locale.US, acc)
                            persist()
                            status = "GPS CAPTURED · ±$accuracy m"
                        },
                        { message -> status = "GPS ERROR · $message" }
                    )
                }, modifier = Modifier.fillMaxWidth()) { Text("Capture current device GPS") }
                Text("Location permission is requested by Android when required. Confirm the coordinates against the site survey before design.", color = Color(0xFF667A75), fontSize = 10.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("02  SOLAR RESOURCE DATA", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("GHI (kWh/m²/day)", ghi, { ghi = it }, Modifier.weight(1f))
                    SurveyInput("DNI (kWh/m²/day)", dni, { dni = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("DHI (kWh/m²/day)", dhi, { dhi = it }, Modifier.weight(1f))
                    SurveyInput("GTI / POA (kWh/m²/day)", gti, { gti = it }, Modifier.weight(1f))
                }
                SurveyInput("Annual yield (kWh/kWp/year)", annualYield, { annualYield = it })
                SurveyInput("Resource source / report date", resourceSource, { resourceSource = it })
                Text("Enter values from a cited resource dataset or validated simulation. GPS coordinates alone do not establish GHI, DNI, DHI or annual yield.", color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(status, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Button(onClick = { syncProject() }, modifier = Modifier.fillMaxWidth()) { Text("Save & sync location to EPC project") }
                Button(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) { Text("Open detailed map & resource workspace") }
            }
        }
    }
}

@Composable
private fun NativeShadeScreen(browser: WebView, modifier: Modifier = Modifier, onOpenAdvanced: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gensolar_native_shade", android.content.Context.MODE_PRIVATE) }
    var date by remember { mutableStateOf(prefs.getString("date", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())) ?: "") }
    var time by remember { mutableStateOf(prefs.getString("time", "09:00") ?: "09:00") }
    var latitude by remember { mutableStateOf(prefs.getString("lat", "") ?: "") }
    var longitude by remember { mutableStateOf(prefs.getString("lon", "") ?: "") }
    var height by remember { mutableStateOf(prefs.getString("height", "2") ?: "2") }
    var distance by remember { mutableStateOf(prefs.getString("distance", "5") ?: "5") }
    var bearing by remember { mutableStateOf(prefs.getString("bearing", "180") ?: "180") }
    var width by remember { mutableStateOf(prefs.getString("width", "45") ?: "45") }
    var output by remember { mutableStateOf("Enter the project coordinates and obstruction geometry to calculate solar position and shadow reach.") }
    var status by remember { mutableStateOf("NO SIMULATION RUN") }
    var profile by remember { mutableStateOf("") }
    fun persist() {
        prefs.edit().putString("date", date).putString("time", time).putString("lat", latitude)
            .putString("lon", longitude).putString("height", height).putString("distance", distance)
            .putString("bearing", bearing).putString("width", width).apply()
    }
    fun runSimulation(dayProfile: Boolean) {
        val lat = latitude.toDoubleOrNull()
        val lon = longitude.toDoubleOrNull()
        val h = height.toDoubleOrNull()
        val d = distance.toDoubleOrNull()
        val az = bearing.toDoubleOrNull()
        val w = width.toDoubleOrNull()
        if (lat == null || lon == null || h == null || d == null || az == null || w == null ||
            lat !in -90.0..90.0 || lon !in -180.0..180.0 || h < 0 || d < 0 || az !in 0.0..360.0 || w !in 0.0..360.0 ||
            !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || !time.matches(Regex("\\d{2}:\\d{2}"))) {
            status = "CHECK INPUTS"
            output = "Enter a valid date, local time, latitude (−90 to 90), longitude (−180 to 180), and non-negative obstruction dimensions."
            return
        }
        persist()
        val zoneOffset = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
        val input = JSONObject().put("date", date).put("time", time).put("latitude", lat).put("longitude", lon)
            .put("timezoneOffsetMinutes", zoneOffset).put("obstructionHeight", h).put("obstructionDistance", d)
            .put("obstructionAzimuth", az).put("obstructionWidthDeg", w)
        val expression = if (dayProfile) {
            "JSON.stringify((function(){var a=GenSolarShadeEngine.dayProfile($input,30),s=a.filter(function(x){return x.altitude>0}),m=s.reduce(function(v,x){return Math.max(v,x.altitude)},0),n=a.filter(function(x){return x.potentialDirectShade}).length;return {samples:a.length,daylightSamples:s.length,maxAltitude:m,shadeSamples:n}})())"
        } else {
            "JSON.stringify(GenSolarShadeEngine.assess($input))"
        }
        status = "CALCULATING"
        browser.evaluateJavascript("(function(){try{return $expression}catch(e){return JSON.stringify({error:e.message})}})()") { raw ->
            try {
                val jsonText = org.json.JSONTokener(raw ?: "null").nextValue() as? String ?: "{}"
                val result = JSONObject(jsonText)
                if (result.has("error")) {
                    status = "CALCULATION ERROR"
                    output = result.optString("error")
                } else if (dayProfile) {
                    status = "DAY PROFILE COMPLETE"
                    profile = "30-minute profile · ${result.optInt("samples")} samples · ${result.optInt("daylightSamples")} daylight samples · ${result.optInt("shadeSamples")} potential shade intervals"
                    output = "Maximum solar altitude: ${"%.1f".format(java.util.Locale.US, result.optDouble("maxAltitude"))}°"
                } else {
                    val altitude = result.optDouble("altitude")
                    val shadow = result.optDouble("shadowLengthM")
                    val shade = result.optBoolean("potentialDirectShade")
                    status = if (altitude <= 0.01) "SUN BELOW HORIZON" else if (shade) "POTENTIAL DIRECT SHADE" else "NO DIRECT SHADE REACH"
                    output = "Solar altitude: ${"%.1f".format(java.util.Locale.US, altitude)}°\nSolar azimuth: ${"%.1f".format(java.util.Locale.US, result.optDouble("azimuth"))}°\nShadow length: ${if (shadow.isFinite()) "%.2f".format(java.util.Locale.US, shadow) + " m" else "No direct sun"}\nBearing difference: ${"%.1f".format(java.util.Locale.US, result.optDouble("angularDifferenceDeg"))}°\nDirection aligned: ${result.optBoolean("directionallyAligned")}"
                    profile = ""
                }
            } catch (_: Exception) {
                status = "ENGINE NOT READY"
                output = "Open the engineering workspace once, then retry the shade calculation."
            }
        }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("SOLAR RESOURCE & SHADE", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Model the sun. Screen obstructions.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Timezone-aware solar position, obstruction bearing and geometric shadow-length screening using the integrated shade engine.", color = Color(0xFFD7E8E1), fontSize = 12.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("01  SITE & TIME", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                SurveyInput("Date (YYYY-MM-DD)", date, { date = it }, Modifier.fillMaxWidth())
                SurveyInput("Local time (HH:MM)", time, { time = it }, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Latitude (°)", latitude, { latitude = it }, Modifier.weight(1f))
                    SurveyInput("Longitude (°)", longitude, { longitude = it }, Modifier.weight(1f))
                }
                Text("Use surveyed GPS coordinates for the project. Coordinates are not guessed or automatically substituted.", color = Color(0xFF667A75), fontSize = 10.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("02  OBSTRUCTION GEOMETRY", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Height (m)", height, { height = it }, Modifier.weight(1f))
                    SurveyInput("Distance (m)", distance, { distance = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Bearing / azimuth (°)", bearing, { bearing = it }, Modifier.weight(1f))
                    SurveyInput("Angular width (°)", width, { width = it }, Modifier.weight(1f))
                }
                Button(onClick = { runSimulation(false) }, modifier = Modifier.fillMaxWidth()) { Text("Run solar & shadow simulation") }
                Button(onClick = { runSimulation(true) }, modifier = Modifier.fillMaxWidth()) { Text("Generate 30-minute day profile") }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(status, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(output, color = Color(0xFF173B32), fontSize = 14.sp, lineHeight = 21.sp)
                if (profile.isNotBlank()) Text(profile, color = Color(0xFF667A75), fontSize = 11.sp)
            }
        }
        Button(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) { Text("Open detailed shade workspace & visualization") }
        Text("Preliminary geometric screening only. It is not a bankable PVsyst-grade shade-loss simulation; validate obstruction geometry, horizon profile, diffuse shading and module mismatch separately.", color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun NativeCadScreen(browser: WebView, modifier: Modifier = Modifier, onOpenAdvanced: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gensolar_native_cad", android.content.Context.MODE_PRIVATE) }
    var roofLength by remember { mutableStateOf(prefs.getString("length", "30") ?: "30") }
    var roofWidth by remember { mutableStateOf(prefs.getString("width", "20") ?: "20") }
    var wallHeight by remember { mutableStateOf(prefs.getString("height", "6") ?: "6") }
    var roofThickness by remember { mutableStateOf(prefs.getString("thickness", "0.2") ?: "0.2") }
    var moveX by remember { mutableStateOf("1") }
    var moveY by remember { mutableStateOf("0") }
    var snapshot by remember { mutableStateOf(NativeCadSnapshot()) }
    var previewMode by remember { mutableStateOf("2D") }
    var status by remember { mutableStateOf("Loading the active project CAD model…") }
    val area = (roofLength.toDoubleOrNull() ?: 0.0) * (roofWidth.toDoubleOrNull() ?: 0.0)
    val selected = snapshot.entities.firstOrNull { it.id == snapshot.selectedId }
    val layers = (listOf("ROOF", "WALLS", "OBSTACLES", "DIMENSIONS", "PV_MODULES", "ROOF_ZONES", "GENERAL") +
        snapshot.entities.map { it.layer }).filter { it.isNotBlank() }.distinct().sorted()

    fun readSnapshot(updateStatus: Boolean = true) {
        browser.evaluateJavascript("JSON.stringify(window.GenSolarEpcBridge.getCadSnapshot())") { raw ->
            try {
                val payload = org.json.JSONTokener(raw ?: "null").nextValue() as? String
                    ?: throw IllegalStateException("CAD snapshot was not returned.")
                snapshot = parseNativeCadSnapshot(JSONObject(payload))
                if (updateStatus) {
                    status = if (snapshot.entities.isEmpty()) {
                        "No CAD entities yet. Create the building model or import a DXF in the detailed workspace."
                    } else {
                        snapshot.entities.size.toString() + " editable entities loaded from the active offline project."
                    }
                }
            } catch (error: Exception) {
                if (updateStatus) status = "CAD model unavailable: " + (error.message ?: "unknown response")
            }
        }
    }

    fun runCadAction(expression: String, successMessage: String) {
        status = "UPDATING CAD MODEL"
        val script = "(function(){try{var b=window.GenSolarEpcBridge;if(!b)return JSON.stringify({ok:false,error:'Native CAD bridge unavailable.'});var value=" +
            expression +
            ";return JSON.stringify({ok:value!==false&&value!==null,error:value===false?'CAD operation was rejected.':''});}catch(e){return JSON.stringify({ok:false,error:String(e.message||e)});}})()"
        browser.evaluateJavascript(script) { raw ->
            try {
                val payload = org.json.JSONTokener(raw ?: "null").nextValue() as? String
                    ?: throw IllegalStateException("CAD action returned no result.")
                val response = JSONObject(payload)
                status = if (response.optBoolean("ok")) successMessage else response.optString("error", "CAD operation failed.")
                readSnapshot(false)
            } catch (error: Exception) {
                status = "CAD operation failed: " + (error.message ?: "unknown response")
                readSnapshot(false)
            }
        }
    }

    fun createBuilding() {
        val length = roofLength.toDoubleOrNull()
        val width = roofWidth.toDoubleOrNull()
        val height = wallHeight.toDoubleOrNull()
        val thickness = roofThickness.toDoubleOrNull()
        if (length == null || width == null || height == null || thickness == null ||
            !length.isFinite() || !width.isFinite() || !height.isFinite() || !thickness.isFinite() ||
            length !in 1.0..1000.0 || width !in 1.0..1000.0 || height !in 1.0..100.0 || thickness !in 0.05..5.0) {
            status = "Enter valid positive building dimensions within the supported modelling range."
            return
        }
        prefs.edit().putString("length", roofLength).putString("width", roofWidth)
            .putString("height", wallHeight).putString("thickness", roofThickness).apply()
        val values = linkedMapOf(
            "cadRoofLength" to roofLength,
            "cadRoofWidth" to roofWidth,
            "cadWallHeight" to wallHeight,
            "cadRoofThickness" to roofThickness
        )
        val payload = JSONObject(values).toString()
        val script = """
            (function(){
              try {
                var data = $payload, keys = Object.keys(data);
                for (var i=0;i<keys.length;i++) {
                  var field=document.getElementById(keys[i]);
                  if(!field) return JSON.stringify({ok:false,error:'CAD parameter '+keys[i]+' is unavailable.'});
                  field.value=data[keys[i]];
                  field.dispatchEvent(new Event('input',{bubbles:true}));
                  field.dispatchEvent(new Event('change',{bubbles:true}));
                }
                var button=document.getElementById('cadCreateBuilding');
                var bridge=window.GenSolarEpcBridge;
                if(!button||!bridge) return JSON.stringify({ok:false,error:'Parametric CAD action is unavailable.'});
                button.click();
                var model=bridge.getCadSnapshot();
                return JSON.stringify({ok:!!model.building&&Array.isArray(model.entities),entities:model.entities?model.entities.length:0});
              } catch(e) { return JSON.stringify({ok:false,error:String(e.message||e)}); }
            })()
        """.trimIndent()
        status = "CREATING PARAMETRIC MODEL"
        browser.evaluateJavascript(script) { raw ->
            try {
                val responseText = org.json.JSONTokener(raw ?: "null").nextValue() as? String
                    ?: throw IllegalStateException("CAD model response was empty.")
                val response = JSONObject(responseText)
                status = if (response.optBoolean("ok")) {
                    "Parametric building geometry saved to the shared CAD model."
                } else response.optString("error", "Building model was not created.")
                readSnapshot(false)
            } catch (error: Exception) {
                status = "Building model could not be confirmed: " + (error.message ?: "unknown response")
            }
        }
    }

    LaunchedEffect(browser) { readSnapshot() }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BrandGreen)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("CAD / 2D / 3D ENGINEERING", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Edit the live project drawing.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Edit the shared project drawing in native 2D, then switch to an elevation-aware 3D model view of roof zones, walls, PV modules and obstacles. CAD edits are saved through the offline project bridge.",
                    color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp
                )
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("01  BUILDING PARAMETERS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Roof length (m)", roofLength, { roofLength = it }, Modifier.weight(1f))
                    SurveyInput("Roof width (m)", roofWidth, { roofWidth = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Wall height (m)", wallHeight, { wallHeight = it }, Modifier.weight(1f))
                    SurveyInput("Roof thickness (m)", roofThickness, { roofThickness = it }, Modifier.weight(1f))
                }
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE8F3EE)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("ROOF FOOTPRINT", color = Color(0xFF60756E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(String.format(java.util.Locale.US, "%.2f m²", area), color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(String.format(java.util.Locale.US, "%.1f sq.ft", area * 10.7639), color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = { createBuilding() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create / update parametric building")
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("02  NATIVE DRAWING VIEW", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    TextButton(onClick = { readSnapshot() }) { Text("Refresh") }
                }
                Text(snapshot.entities.size.toString() + " entities · " + snapshot.layout.size.toString() + " PV modules · " + snapshot.roofZones.size.toString() + " roof zones", color = Color(0xFF60756E), fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = previewMode == "2D", onClick = { previewMode = "2D" }, label = { Text("2D editor") })
                    FilterChip(selected = previewMode == "3D", onClick = { previewMode = "3D" }, label = { Text("3D model") })
                }
                if (previewMode == "2D") {
                    NativeCadPreview(
                        snapshot = snapshot,
                        onSelect = { x, y ->
                            val xArg = String.format(java.util.Locale.US, "%.6f", x)
                            val yArg = String.format(java.util.Locale.US, "%.6f", y)
                            runCadAction("b.selectCadEntityAt(" + xArg + "," + yArg + ")", "CAD selection updated.")
                        }
                    )
                    Text("Tap an entity to select it. Coordinates and dimensions are in metres.", color = Color(0xFF667A75), fontSize = 10.sp)
                } else {
                    NativeCad3DPreview(snapshot = snapshot)
                    Text(
                        "Elevation-aware isometric preview · roof, walls, obstacles and PV modules. This is a coordination view, not a structural certification model.",
                        color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp
                    )
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("03  DRAWING LAYERS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    layers.forEach { layer ->
                        val visible = layer !in snapshot.hiddenLayers
                        FilterChip(
                            selected = visible,
                            onClick = {
                                val encoded = JSONObject.quote(layer)
                                runCadAction("b.setCadLayerVisible(" + encoded + "," + (!visible).toString() + ")", layer + if (visible) " hidden." else " visible.")
                            },
                            label = { Text(layer, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("04  SELECTED ENTITY", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                if (selected == null) {
                    Text("No entity selected. Tap a visible drawing object above.", color = Color(0xFF60756E), fontSize = 12.sp)
                } else {
                    Text(selected.type.uppercase() + " · " + selected.layer, color = BrandGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Entity ID: " + selected.id, color = Color(0xFF60756E), fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SurveyInput("Move X (m)", moveX, { moveX = it }, Modifier.weight(1f))
                        SurveyInput("Move Y (m)", moveY, { moveY = it }, Modifier.weight(1f))
                    }
                    Button(
                        onClick = {
                            val dx = moveX.toDoubleOrNull()
                            val dy = moveY.toDoubleOrNull()
                            if (dx == null || dy == null || !dx.isFinite() || !dy.isFinite()) {
                                status = "Enter finite X and Y offsets in metres."
                            } else {
                                runCadAction(
                                    "b.moveCadEntity(" + JSONObject.quote(selected.id) + "," +
                                        String.format(java.util.Locale.US, "%.6f", dx) + "," +
                                        String.format(java.util.Locale.US, "%.6f", dy) + ")",
                                    "Selected entity moved and saved."
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Move selected entity") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { runCadAction("b.duplicateCadEntity(" + JSONObject.quote(selected.id) + ")", "Entity duplicated and saved.") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Duplicate") }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { runCadAction("b.rotateCadEntityBy90(" + JSONObject.quote(selected.id) + ")", "Entity rotated 90° and saved.") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Rotate 90°") }
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { runCadAction("b.deleteCadEntity(" + JSONObject.quote(selected.id) + ")", "Entity deleted and saved.") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete selected entity") }
                }
            }
        }
        Text(status, color = AccentGreen, fontSize = 11.sp, lineHeight = 16.sp)
        androidx.compose.material3.OutlinedButton(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) {
            Text("Open advanced CAD and DXF import / export")
        }
        Text(
            "The native 2D editor and 3D coordination view use the shared CAD project model. DXF conversion and advanced drafting remain in the detailed workspace; structural and fabrication drawings require qualified engineering review.",
            color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp
        )
    }
}

private data class NativeCadPoint(val x: Double, val y: Double)

private data class NativeCadEntity(
    val id: String,
    val type: String,
    val layer: String,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val radius: Double = 0.0,
    val a: NativeCadPoint? = null,
    val b: NativeCadPoint? = null,
    val points: List<NativeCadPoint> = emptyList(),
    val closed: Boolean = false
)

private data class NativeCadModule(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val elevation: Double = 0.0
)

private data class NativeCadRoofZone(
    val name: String,
    val elevation: Double,
    val vertices: List<NativeCadPoint>
)

private data class NativeCadSnapshot(
    val entities: List<NativeCadEntity> = emptyList(),
    val layout: List<NativeCadModule> = emptyList(),
    val roofZones: List<NativeCadRoofZone> = emptyList(),
    val buildingLength: Double = 0.0,
    val buildingWidth: Double = 0.0,
    val buildingHeight: Double = 0.0,
    val selectedId: String = "",
    val hiddenLayers: Set<String> = emptySet()
)

private data class NativeCadBounds(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) {
    val width: Double get() = (maxX - minX).coerceAtLeast(1.0)
    val height: Double get() = (maxY - minY).coerceAtLeast(1.0)
}

private data class NativeCadViewport(val scale: Float, val offsetX: Float, val offsetY: Float)

private fun parseNativeCadSnapshot(json: JSONObject): NativeCadSnapshot {
    val rows = json.optJSONArray("entities") ?: org.json.JSONArray()
    fun point(value: JSONObject?): NativeCadPoint? {
        if (value == null) return null
        val x = value.optDouble("x", Double.NaN)
        val y = value.optDouble("y", Double.NaN)
        return if (x.isFinite() && y.isFinite()) NativeCadPoint(x, y) else null
    }
    val entities = (0 until rows.length()).mapNotNull { index ->
        val row = rows.optJSONObject(index) ?: return@mapNotNull null
        val pointsArray = row.optJSONArray("points") ?: org.json.JSONArray()
        val points = (0 until pointsArray.length()).mapNotNull { point(pointsArray.optJSONObject(it)) }
        NativeCadEntity(
            id = row.optString("id"),
            type = row.optString("type"),
            layer = row.optString("layer", "GENERAL"),
            x = row.optDouble("x", 0.0),
            y = row.optDouble("y", 0.0),
            width = row.optDouble("w", 0.0),
            height = row.optDouble("h", 0.0),
            radius = row.optDouble("r", 0.0),
            a = point(row.optJSONObject("a")),
            b = point(row.optJSONObject("b")),
            points = points,
            closed = row.optBoolean("closed", false)
        )
    }
    val moduleRows = json.optJSONArray("layout") ?: org.json.JSONArray()
    val layout = (0 until moduleRows.length()).mapNotNull { index ->
        val row = moduleRows.optJSONObject(index) ?: return@mapNotNull null
        val x = row.optDouble("x", Double.NaN)
        val y = row.optDouble("y", Double.NaN)
        val width = row.optDouble("w", Double.NaN)
        val height = row.optDouble("h", Double.NaN)
        if (x.isFinite() && y.isFinite() && width.isFinite() && height.isFinite() && width > 0 && height > 0) {
            NativeCadModule(x, y, width, height, row.optDouble("zoneElevation", 0.0).takeIf { it.isFinite() } ?: 0.0)
        } else null
    }
    val zoneRows = json.optJSONArray("roofZones") ?: org.json.JSONArray()
    val roofZones = (0 until zoneRows.length()).mapNotNull { index ->
        val row = zoneRows.optJSONObject(index) ?: return@mapNotNull null
        val vertices = row.optJSONArray("vertices") ?: org.json.JSONArray()
        val points = (0 until vertices.length()).mapNotNull { point(vertices.optJSONObject(it)) }
        if (points.size >= 3) NativeCadRoofZone(row.optString("name", "Roof zone"), row.optDouble("elevation", 0.0).takeIf { it.isFinite() } ?: 0.0, points) else null
    }
    val hidden = json.optJSONArray("hiddenLayers") ?: org.json.JSONArray()
    val hiddenLayers = (0 until hidden.length()).map { hidden.optString(it) }.filter { it.isNotBlank() }.toSet()
    val building = json.optJSONObject("building") ?: JSONObject()
    val buildingLength = building.optDouble("length", 0.0).takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val buildingWidth = building.optDouble("width", 0.0).takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val buildingHeight = building.optDouble("height", 0.0).takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    return NativeCadSnapshot(
        entities = entities,
        layout = layout,
        roofZones = roofZones,
        buildingLength = buildingLength,
        buildingWidth = buildingWidth,
        buildingHeight = buildingHeight,
        selectedId = json.optString("selected"),
        hiddenLayers = hiddenLayers
    )
}

private fun nativeCadBounds(entities: List<NativeCadEntity>, layout: List<NativeCadModule>, roofZones: List<NativeCadRoofZone>): NativeCadBounds {
    val xs = mutableListOf<Double>()
    val ys = mutableListOf<Double>()
    fun add(point: NativeCadPoint) {
        if (point.x.isFinite() && point.y.isFinite()) {
            xs += point.x
            ys += point.y
        }
    }
    layout.forEach { module ->
        add(NativeCadPoint(module.x, module.y))
        add(NativeCadPoint(module.x + module.width, module.y + module.height))
    }
    roofZones.forEach { zone -> zone.vertices.forEach(::add) }
    entities.forEach { entity ->
        when (entity.type) {
            "rect" -> {
                add(NativeCadPoint(entity.x, entity.y))
                add(NativeCadPoint(entity.x + entity.width, entity.y + entity.height))
            }
            "circle" -> {
                add(NativeCadPoint(entity.x - entity.radius, entity.y - entity.radius))
                add(NativeCadPoint(entity.x + entity.radius, entity.y + entity.radius))
            }
            "line", "dim" -> {
                entity.a?.let(::add)
                entity.b?.let(::add)
            }
            "poly" -> entity.points.forEach(::add)
            else -> add(NativeCadPoint(entity.x, entity.y))
        }
    }
    if (xs.isEmpty() || ys.isEmpty()) return NativeCadBounds(0.0, 0.0, 30.0, 20.0)
    var minX = xs.minOrNull() ?: 0.0
    var minY = ys.minOrNull() ?: 0.0
    var maxX = xs.maxOrNull() ?: 30.0
    var maxY = ys.maxOrNull() ?: 20.0
    if (maxX - minX < 0.1) { minX -= 0.5; maxX += 0.5 }
    if (maxY - minY < 0.1) { minY -= 0.5; maxY += 0.5 }
    return NativeCadBounds(minX, minY, maxX, maxY)
}

private fun nativeCadViewport(bounds: NativeCadBounds, width: Float, height: Float): NativeCadViewport {
    val safeWidth = width.coerceAtLeast(80f)
    val safeHeight = height.coerceAtLeast(80f)
    val scale = minOf(
        (safeWidth - 36f) / bounds.width.toFloat(),
        (safeHeight - 36f) / bounds.height.toFloat()
    ).coerceAtLeast(0.01f)
    val offsetX = (safeWidth - bounds.width.toFloat() * scale) / 2f - bounds.minX.toFloat() * scale
    val offsetY = (safeHeight - bounds.height.toFloat() * scale) / 2f + bounds.maxY.toFloat() * scale
    return NativeCadViewport(scale, offsetX, offsetY)
}

@Composable
private fun NativeCadPreview(
    snapshot: NativeCadSnapshot,
    modifier: Modifier = Modifier,
    onSelect: (Double, Double) -> Unit
) {
    val bounds = remember(snapshot.entities, snapshot.layout, snapshot.roofZones) { nativeCadBounds(snapshot.entities, snapshot.layout, snapshot.roofZones) }
    ComposeCanvas(
        modifier = modifier.fillMaxWidth().height(300.dp).pointerInput(snapshot.entities, bounds) {
            detectTapGestures { tap ->
                val viewport = nativeCadViewport(bounds, size.width.toFloat(), size.height.toFloat())
                val worldX = (tap.x - viewport.offsetX) / viewport.scale
                val worldY = (viewport.offsetY - tap.y) / viewport.scale
                onSelect(worldX.toDouble(), worldY.toDouble())
            }
        }
    ) {
        drawRect(Color(0xFFFBFDFC))
        val viewport = nativeCadViewport(bounds, size.width, size.height)
        fun screen(point: NativeCadPoint): Offset = Offset(
            viewport.offsetX + point.x.toFloat() * viewport.scale,
            viewport.offsetY - point.y.toFloat() * viewport.scale
        )
        if ("ROOF_ZONES" !in snapshot.hiddenLayers) {
            snapshot.roofZones.forEach { zone ->
                if (zone.vertices.size >= 3) {
                    val path = Path().apply {
                        val first = screen(zone.vertices.first())
                        moveTo(first.x, first.y)
                        zone.vertices.drop(1).forEach { point ->
                            val mapped = screen(point)
                            lineTo(mapped.x, mapped.y)
                        }
                        close()
                    }
                    drawPath(path, Color(0xFF0A805C).copy(alpha = 0.10f))
                    drawPath(path, Color(0xFF0A805C), style = Stroke(1.2f))
                }
            }
        }
        if ("PV_MODULES" !in snapshot.hiddenLayers) {
            snapshot.layout.forEach { module ->
                val topLeft = screen(NativeCadPoint(module.x, module.y + module.height))
                val bottomRight = screen(NativeCadPoint(module.x + module.width, module.y))
                val moduleSize = Size((bottomRight.x - topLeft.x).coerceAtLeast(0.5f), (bottomRight.y - topLeft.y).coerceAtLeast(0.5f))
                drawRect(Color(0xFF2476B8).copy(alpha = 0.28f), topLeft, moduleSize)
                drawRect(Color(0xFF2476B8), topLeft, moduleSize, style = Stroke(1.0f))
            }
        }
        snapshot.entities.forEach { entity ->
            if (entity.layer in snapshot.hiddenLayers) return@forEach
            val color = when (entity.layer) {
                "ROOF" -> Color(0xFF087F5B)
                "OBSTACLES" -> Color(0xFFC24A3A)
                "PV_MODULES" -> Color(0xFF2476B8)
                "DIMENSIONS" -> Color(0xFF9A6A00)
                "WALLS" -> Color(0xFF60756E)
                else -> Color(0xFF334B45)
            }
            val selected = entity.id == snapshot.selectedId
            val stroke = if (selected) 3.5f else 1.6f
            when (entity.type) {
                "rect" -> {
                    val topLeft = screen(NativeCadPoint(entity.x, entity.y + entity.height))
                    val bottomRight = screen(NativeCadPoint(entity.x + entity.width, entity.y))
                    val rectSize = Size((bottomRight.x - topLeft.x).coerceAtLeast(0.5f), (bottomRight.y - topLeft.y).coerceAtLeast(0.5f))
                    if (entity.layer == "ROOF") drawRect(color.copy(alpha = 0.13f), topLeft, rectSize)
                    drawRect(color, topLeft, rectSize, style = Stroke(stroke))
                }
                "circle" -> {
                    val center = screen(NativeCadPoint(entity.x, entity.y))
                    drawCircle(color, (entity.radius.toFloat() * viewport.scale).coerceAtLeast(1f), center, style = Stroke(stroke))
                }
                "line", "dim" -> {
                    val a = entity.a
                    val b = entity.b
                    if (a != null && b != null) drawLine(color, screen(a), screen(b), strokeWidth = stroke)
                }
                "poly" -> {
                    if (entity.points.size >= 2) {
                        val path = Path().apply {
                            val first = screen(entity.points.first())
                            moveTo(first.x, first.y)
                            entity.points.drop(1).forEach { point ->
                                val mapped = screen(point)
                                lineTo(mapped.x, mapped.y)
                            }
                            if (entity.closed) close()
                        }
                        if (entity.closed && entity.layer == "ROOF") drawPath(path, color.copy(alpha = 0.12f))
                        drawPath(path, color, style = Stroke(stroke))
                    }
                }
                "text" -> drawCircle(color, 3.5f, screen(NativeCadPoint(entity.x, entity.y)))
            }
        }
    }
}


internal data class NativeCad3DPoint(val x: Double, val y: Double, val z: Double)

internal data class NativeCadProjectedPoint(val x: Float, val y: Float)

internal data class NativeCad3DViewport(val scale: Float, val offsetX: Float, val offsetY: Float)

internal fun nativeCadWallBaseElevation(roofElevation: Double, buildingHeight: Double): Double? {
    if (!roofElevation.isFinite() || !buildingHeight.isFinite() || buildingHeight !in 0.5..100.0) return null
    return roofElevation - buildingHeight
}


internal fun nativeCadProjectPoint(point: NativeCad3DPoint): NativeCadProjectedPoint {
    require(point.x.isFinite() && point.y.isFinite() && point.z.isFinite()) {
        "3D CAD coordinates must be finite."
    }
    val projectedX = (point.x - point.y) * 0.8660254037844386
    val projectedY = (point.x + point.y) * 0.5 - point.z * 0.9
    require(projectedX.isFinite() && projectedY.isFinite()) { "3D CAD projection overflowed." }
    return NativeCadProjectedPoint(projectedX.toFloat(), projectedY.toFloat())
}

internal fun nativeCadViewport3D(
    points: List<NativeCad3DPoint>,
    width: Float,
    height: Float,
    padding: Float = 20f
): NativeCad3DViewport {
    require(points.isNotEmpty()) { "At least one 3D model point is required." }
    require(width.isFinite() && height.isFinite() && padding.isFinite() && padding >= 0f &&
        width > padding * 2f && height > padding * 2f) {
        "3D viewport dimensions must exceed its padding."
    }
    val projected = points.map(::nativeCadProjectPoint)
    val minX = projected.minOf { it.x }
    val maxX = projected.maxOf { it.x }
    val minY = projected.minOf { it.y }
    val maxY = projected.maxOf { it.y }
    val spanX = (maxX - minX).coerceAtLeast(1f)
    val spanY = (maxY - minY).coerceAtLeast(1f)
    val scale = minOf((width - padding * 2f) / spanX, (height - padding * 2f) / spanY)
    require(scale.isFinite() && scale > 0f) { "3D model cannot fit inside the viewport." }
    return NativeCad3DViewport(scale, padding - minX * scale, padding - minY * scale)
}

@Composable
private fun NativeCad3DPreview(snapshot: NativeCadSnapshot, modifier: Modifier = Modifier) {
    val zones = remember(snapshot.roofZones, snapshot.entities, snapshot.buildingLength, snapshot.buildingWidth) {
        if (snapshot.roofZones.isNotEmpty()) {
            snapshot.roofZones
        } else {
            val roof = snapshot.entities.firstOrNull { it.layer == "ROOF" && it.type == "rect" }
            val roofPolygon = snapshot.entities.firstOrNull { it.layer == "ROOF" && it.type == "poly" && it.closed && it.points.size >= 3 }
            when {
                roofPolygon != null -> listOf(NativeCadRoofZone("Building roof", 0.0, roofPolygon.points))
                roof != null && roof.width > 0.0 && roof.height > 0.0 -> listOf(
                    NativeCadRoofZone(
                        "Building roof", 0.0,
                        listOf(
                            NativeCadPoint(roof.x, roof.y),
                            NativeCadPoint(roof.x + roof.width, roof.y),
                            NativeCadPoint(roof.x + roof.width, roof.y + roof.height),
                            NativeCadPoint(roof.x, roof.y + roof.height)
                        )
                    )
                )
                snapshot.buildingLength > 0.0 && snapshot.buildingWidth > 0.0 -> listOf(
                    NativeCadRoofZone(
                        "Building roof", 0.0,
                        listOf(
                            NativeCadPoint(0.0, 0.0),
                            NativeCadPoint(snapshot.buildingLength, 0.0),
                            NativeCadPoint(snapshot.buildingLength, snapshot.buildingWidth),
                            NativeCadPoint(0.0, snapshot.buildingWidth)
                        )
                    )
                )
                else -> emptyList()
            }
        }
    }
    val wallBases = zones.map { zone -> nativeCadWallBaseElevation(zone.elevation, snapshot.buildingHeight) }
    val obstacles = remember(snapshot.entities, zones) {
        val baseElevation = zones.firstOrNull()?.elevation ?: 0.0
        snapshot.entities.filter { it.layer == "OBSTACLES" }.mapNotNull { entity ->
            val points = when (entity.type) {
                "rect" -> if (entity.width > 0.0 && entity.height > 0.0) listOf(
                    NativeCadPoint(entity.x, entity.y),
                    NativeCadPoint(entity.x + entity.width, entity.y),
                    NativeCadPoint(entity.x + entity.width, entity.y + entity.height),
                    NativeCadPoint(entity.x, entity.y + entity.height)
                ) else emptyList()
                "circle" -> if (entity.radius > 0.0) (0 until 24).map { index ->
                    val angle = 2.0 * Math.PI * index / 24.0
                    NativeCadPoint(entity.x + entity.radius * kotlin.math.cos(angle), entity.y + entity.radius * kotlin.math.sin(angle))
                } else emptyList()
                "poly" -> if (entity.closed && entity.points.size >= 3) entity.points else emptyList()
                else -> emptyList()
            }
            if (points.size >= 3) points.map { NativeCad3DPoint(it.x, it.y, baseElevation + 0.04) } else null
        }
    }
    val modelPoints = remember(zones, snapshot.layout, obstacles, wallBases) {
        buildList {
            zones.forEachIndexed { zoneIndex, zone ->
                zone.vertices.forEach { point ->
                    add(NativeCad3DPoint(point.x, point.y, zone.elevation))
                    wallBases[zoneIndex]?.let { base -> add(NativeCad3DPoint(point.x, point.y, base)) }
                }
            }
            snapshot.layout.forEach { module ->
                val z = module.elevation + 0.06
                add(NativeCad3DPoint(module.x, module.y, z))
                add(NativeCad3DPoint(module.x + module.width, module.y, z))
                add(NativeCad3DPoint(module.x + module.width, module.y + module.height, z))
                add(NativeCad3DPoint(module.x, module.y + module.height, z))
            }
            obstacles.flatten().forEach(::add)
        }
    }
    if (modelPoints.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF3F6F5)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
                Text("NO 3D MODEL DATA", color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Create a parametric building or save measured roof zones and PV layout data to preview the actual project model.", color = Color(0xFF60756E), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        return
    }
    ComposeCanvas(modifier = modifier.fillMaxWidth().height(320.dp)) {
        drawRect(Color(0xFFFBFDFC))
        val viewport = nativeCadViewport3D(modelPoints, size.width, size.height)
        fun screen(point: NativeCad3DPoint): Offset {
            val projected = nativeCadProjectPoint(point)
            return Offset(
                projected.x * viewport.scale + viewport.offsetX,
                projected.y * viewport.scale + viewport.offsetY
            )
        }
        fun polygon(points: List<NativeCad3DPoint>, fill: Color, outline: Color) {
            if (points.size < 3) return
            val path = Path().apply {
                val first = screen(points.first())
                moveTo(first.x, first.y)
                points.drop(1).forEach { point ->
                    val mapped = screen(point)
                    lineTo(mapped.x, mapped.y)
                }
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = Stroke(1.1f))
        }
        zones.forEachIndexed { zoneIndex, zone ->
            val base = wallBases[zoneIndex]
            if (base != null && "WALLS" !in snapshot.hiddenLayers) {
                zone.vertices.indices.forEach { index ->
                    val next = (index + 1) % zone.vertices.size
                    val topA = zone.vertices[index]
                    val topB = zone.vertices[next]
                    polygon(
                        listOf(
                            NativeCad3DPoint(topA.x, topA.y, zone.elevation),
                            NativeCad3DPoint(topB.x, topB.y, zone.elevation),
                            NativeCad3DPoint(topB.x, topB.y, base),
                            NativeCad3DPoint(topA.x, topA.y, base)
                        ),
                        if (index % 2 == 0) Color(0xFFDCE7E2) else Color(0xFFCAD9D2),
                        Color(0xFF60756E)
                    )
                }
            }
            if ("ROOF" !in snapshot.hiddenLayers) {
                polygon(zone.vertices.map { NativeCad3DPoint(it.x, it.y, zone.elevation) }, Color(0xFF77B99C).copy(alpha = 0.72f), Color(0xFF087F5B))
            }
        }
        if ("OBSTACLES" !in snapshot.hiddenLayers) {
            obstacles.forEach { polygon(it, Color(0xFFD98A63).copy(alpha = 0.78f), Color(0xFF9D4D2B)) }
        }
        if ("PV_MODULES" !in snapshot.hiddenLayers) {
            snapshot.layout.forEach { module ->
                val z = module.elevation + 0.06
                polygon(
                    listOf(
                        NativeCad3DPoint(module.x, module.y, z),
                        NativeCad3DPoint(module.x + module.width, module.y, z),
                        NativeCad3DPoint(module.x + module.width, module.y + module.height, z),
                        NativeCad3DPoint(module.x, module.y + module.height, z)
                    ),
                    Color(0xFF2476B8).copy(alpha = 0.72f),
                    Color(0xFF155E91)
                )
            }
        }
    }
}

@Composable
private fun NativeSurveyScreen(
    browser: WebView,
    modifier: Modifier = Modifier,
    onOpenAdvanced: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gensolar_native_survey", android.content.Context.MODE_PRIVATE) }
    var customer by remember { mutableStateOf(prefs.getString("customer", "") ?: "") }
    var siteName by remember { mutableStateOf(prefs.getString("site", "") ?: "") }
    var stateName by remember { mutableStateOf(prefs.getString("state", "Telangana") ?: "Telangana") }
    var district by remember { mutableStateOf(prefs.getString("district", "") ?: "") }
    var latitude by remember { mutableStateOf(prefs.getString("lat", "") ?: "") }
    var longitude by remember { mutableStateOf(prefs.getString("lon", "") ?: "") }
    var systemType by remember { mutableStateOf(prefs.getString("system", "On-grid") ?: "On-grid") }
    var mountingType by remember { mutableStateOf(prefs.getString("mount", "RCC rooftop") ?: "RCC rooftop") }
    var length by remember { mutableStateOf(prefs.getString("length", "30") ?: "30") }
    var width by remember { mutableStateOf(prefs.getString("width", "20") ?: "20") }
    var setback by remember { mutableStateOf(prefs.getString("setback", "1") ?: "1") }
    var walkway by remember { mutableStateOf(prefs.getString("walkway", "0.6") ?: "0.6") }
    var notes by remember { mutableStateOf(prefs.getString("surveyNotes", "") ?: "") }
    var saveMessage by remember { mutableStateOf("Draft is stored on this device when you save.") }
    val grossArea = (length.toDoubleOrNull() ?: 0.0) * (width.toDoubleOrNull() ?: 0.0)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BrandGreen)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("FIELD SURVEY", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text("Capture the project correctly.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Customer, site coordinates and usable roof dimensions. Data is saved offline and synced to the engineering workspace.", color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("01  PROJECT DETAILS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                SurveyInput("Customer / project", customer, { customer = it })
                SurveyInput("Site / plant name", siteName, { siteName = it })
                SurveyInput("State", stateName, { stateName = it })
                SurveyInput("District / city", district, { district = it })
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("02  SITE LOCATION", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Latitude", latitude, { latitude = it }, Modifier.weight(1f))
                    SurveyInput("Longitude", longitude, { longitude = it }, Modifier.weight(1f))
                }
                Text("Enter decimal GPS coordinates. Example: 17.3850, 78.4867.", color = Color(0xFF667A75), fontSize = 10.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("03  SYSTEM & ROOF", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                SurveyInput("System type (On-grid / Off-grid / Hybrid)", systemType, { systemType = it })
                SurveyInput("Mounting type", mountingType, { mountingType = it })
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Length (m)", length, { length = it }, Modifier.weight(1f))
                    SurveyInput("Width (m)", width, { width = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Edge setback (m)", setback, { setback = it }, Modifier.weight(1f))
                    SurveyInput("Walkway width (m)", walkway, { walkway = it }, Modifier.weight(1f))
                }
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE8F3EE)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("GROSS ROOF AREA", color = Color(0xFF60756E), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(String.format("%.2f m²", grossArea), color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(String.format("%.1f sq.ft", grossArea * 10.7639), color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                SurveyInput("Survey notes / site observations", notes, { notes = it }, minLines = 3)
            }
        }
        Button(
            onClick = {
                val values = linkedMapOf(
                    "customer" to customer, "site" to siteName, "state" to stateName,
                    "district" to district, "lat" to latitude, "lon" to longitude,
                    "system" to systemType, "mount" to mountingType, "length" to length,
                    "width" to width, "setback" to setback, "walkway" to walkway,
                    "surveyNotes" to notes
                )
                prefs.edit().apply { values.forEach { (key, value) -> putString(key, value) } }.apply()
                val payload = JSONObject(values).toString()
                val script = """
                    (function(){
                      var data = $payload;
                      var root = document.getElementById('site');
                      if (!root) return false;
                      Object.keys(data).forEach(function(key) {
                        var field = root.querySelector('[data-k="' + key + '"]');
                        if (field) {
                          field.value = data[key];
                          field.dispatchEvent(new Event('input', {bubbles:true}));
                          field.dispatchEvent(new Event('change', {bubbles:true}));
                        }
                      });
                      var save = document.getElementById('saveSurvey');
                      if (!save) return false;
                      save.click();
                      return true;
                    })()
                """.trimIndent()
                browser.evaluateJavascript(script) { result ->
                    saveMessage = if (result == "true") "Saved offline and transferred to the engineering survey." else "Saved on this device. Open the detailed survey to retry workspace transfer."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save survey to project") }
        Text(saveMessage, color = Color(0xFF60756E), fontSize = 11.sp)
        androidx.compose.material3.OutlinedButton(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) {
            Text("Open advanced survey & structural tools")
        }
        Text("Engineering values are preliminary until verified against site measurements, approved drawings and applicable standards.", color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(8.dp))
    }
}



@Composable
private fun NativeProcurementScreen(
    browser: WebView,
    modifier: Modifier = Modifier,
    onOpenAdvanced: () -> Unit,
    onExportCsv: (String, String) -> Unit
) {
    var ref by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var quoteSupplier by remember { mutableStateOf("") }
    var quoteAmount by remember { mutableStateOf("") }
    var quoteDays by remember { mutableStateOf("7") }
    var quoteNotes by remember { mutableStateOf("") }
    var quoteRfqRef by remember { mutableStateOf("") }
    var rfqRef by remember { mutableStateOf("") }
    var rfqItem by remember { mutableStateOf("") }
    var rfqQty by remember { mutableStateOf("") }
    var rfqUnit by remember { mutableStateOf("Nos") }
    var rfqDueDate by remember { mutableStateOf(java.time.LocalDate.now().plusDays(7).toString()) }
    var rfqSuppliers by remember { mutableStateOf("") }
    var rfqRows by remember { mutableStateOf(listOf<List<String>>()) }
    var vendorRows by remember { mutableStateOf(listOf<List<String>>()) }
    var amount by remember { mutableStateOf("") }
    var poRfqRef by remember { mutableStateOf("") }
    var orderedQty by remember { mutableStateOf("") }
    var poLineItems by remember { mutableStateOf("") }
    var bomRows by remember { mutableStateOf(listOf<List<String>>()) }
    var unit by remember { mutableStateOf("Nos") }
    var deliveryDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var receiptPo by remember { mutableStateOf("") }
    var receiptQty by remember { mutableStateOf("") }
    var receiptItem by remember { mutableStateOf("") }
    var receiver by remember { mutableStateOf("") }
    var receiptDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var rows by remember { mutableStateOf(listOf<List<String>>()) }
    var receipts by remember { mutableStateOf(listOf<List<String>>()) }
    var stockLedgerRows by remember { mutableStateOf(listOf<List<String>>()) }
    var stockIssueRows by remember { mutableStateOf(listOf<List<String>>()) }
    var stockIssueRef by remember { mutableStateOf("") }
    var stockIssuePo by remember { mutableStateOf("") }
    var stockIssueItem by remember { mutableStateOf("") }
    var stockIssueQty by remember { mutableStateOf("") }
    var stockIssueDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var stockIssueTo by remember { mutableStateOf("") }
    var stockIssuePackage by remember { mutableStateOf("") }
    var stockIssueRemarks by remember { mutableStateOf("") }
    var lifecyclePo by remember { mutableStateOf("") }
    var nextPoStatus by remember { mutableStateOf("RFQ sent") }
    var poHistory by remember { mutableStateOf(listOf<List<String>>()) }
    var message by remember { mutableStateOf("Procurement records are stored in the active offline project.") }

    fun refresh() {
        browser.evaluateJavascript("(function(){var b=window.GenSolarEpcBridge;return b?JSON.stringify(b.getProcurement()):'{}'})()") { raw ->
            try {
                val encoded = org.json.JSONTokener(raw ?: "\"{}\"").nextValue() as? String ?: "{}"
                val data = JSONObject(encoded)
                val pos = data.optJSONArray("purchaseOrders") ?: org.json.JSONArray()
                val rfqArray = data.optJSONArray("rfqs") ?: org.json.JSONArray()
                rfqRows = (0 until rfqArray.length()).map { i ->
                    val row = rfqArray.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j ->
                        if (j == 5 && row.opt(j) is org.json.JSONArray) {
                            val suppliers = row.optJSONArray(j) ?: org.json.JSONArray()
                            (0 until suppliers.length()).joinToString(", ") { suppliers.optString(it, "") }
                        } else row.optString(j, "")
                    }
                }
                val bomArray = data.optJSONArray("bom") ?: org.json.JSONArray()
                bomRows = (0 until bomArray.length()).map { i ->
                    val row = bomArray.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j -> row.optString(j, "") }
                }.filter { it.size >= 5 && it[0].isNotBlank() }
                val vendorArray = data.optJSONArray("vendors") ?: org.json.JSONArray()
                vendorRows = (0 until vendorArray.length()).map { i ->
                    val row = vendorArray.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j -> row.optString(j, "") }
                }.filter { it.size >= 3 && it[0].isNotBlank() && (it.getOrNull(1)?.toDoubleOrNull() ?: 0.0) > 0.0 }
                    .sortedWith(compareBy<List<String>> { it.getOrNull(1)?.toDoubleOrNull() ?: Double.MAX_VALUE }.thenBy { it.getOrNull(2)?.toIntOrNull() ?: Int.MAX_VALUE })
                val grn = data.optJSONArray("receipts") ?: org.json.JSONArray()
                val stockLedger = data.optJSONArray("stockLedger") ?: org.json.JSONArray()
                stockLedgerRows = (0 until stockLedger.length()).map { i ->
                    val row = stockLedger.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j -> row.optString(j, "") }
                }
                val issueArray = data.optJSONArray("stockIssues") ?: org.json.JSONArray()
                stockIssueRows = (0 until issueArray.length()).map { i ->
                    val row = issueArray.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j -> row.optString(j, "") }
                }
                rows = (0 until pos.length()).map { i ->
                    val row = pos.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j -> row.optString(j, "") }
                }
                receipts = (0 until grn.length()).map { i ->
                    val row = grn.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j -> row.optString(j, "") }
                }
                val historyArray = data.optJSONArray("history") ?: org.json.JSONArray()
                poHistory = (0 until historyArray.length()).map { i ->
                    val row = historyArray.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j -> row.optString(j, "") }
                }
                if (receiptPo.isBlank() && rows.isNotEmpty()) receiptPo = rows.first().getOrElse(0) { "" }
                if (lifecyclePo.isBlank() && rows.isNotEmpty()) lifecyclePo = rows.first().getOrElse(0) { "" }
            } catch (_: Exception) { message = "Could not load procurement register." }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun isValidIsoDate(value: String): Boolean = try {
        java.time.LocalDate.parse(value.trim()).toString() == value.trim()
    } catch (_: Exception) { false }


    fun saveQuote() {
        val quoted = quoteAmount.trim().toDoubleOrNull()
        val days = quoteDays.trim().toIntOrNull()
        if (quoteSupplier.isBlank() || quoted == null || !quoted.isFinite() || quoted <= 0.0 || days == null || days < 0) {
            message = "Enter a supplier, valid quote amount and whole delivery days."
            return
        }
        val payload = JSONObject().put("name", quoteSupplier.trim()).put("amount", quoted)
            .put("deliveryDays", days).put("notes", quoteNotes.trim())
            .put("rfqRef", quoteRfqRef.trim())
        browser.evaluateJavascript("window.GenSolarEpcBridge.createVendorQuote(" + payload + ")") { result ->
            if (result == "true") {
                message = "Supplier quotation saved and comparison updated."
                quoteSupplier = ""; quoteAmount = ""; quoteDays = "7"; quoteNotes = ""; quoteRfqRef = ""
                refresh()
            } else message = "Quotation not saved. Check duplicate supplier name and values."
        }
    }

    fun saveRfq() {
        val quantity = rfqQty.trim().toDoubleOrNull()
        val invited = rfqSuppliers.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (rfqRef.isBlank() || rfqItem.isBlank() || rfqUnit.isBlank()) {
            message = "Enter the RFQ reference, item specification and unit."
            return
        }
        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) {
            message = "RFQ quantity must be a valid number greater than zero."
            return
        }
        if (!isValidIsoDate(rfqDueDate)) {
            message = "Enter a valid quotation due date in YYYY-MM-DD format."
            return
        }
        if (invited.isEmpty() || invited.map { it.lowercase() }.distinct().size != invited.size) {
            message = "Enter at least one invited supplier; supplier names must be unique."
            return
        }
        val payload = JSONObject().put("ref", rfqRef.trim()).put("item", rfqItem.trim())
            .put("qty", quantity).put("unit", rfqUnit.trim()).put("dueDate", rfqDueDate.trim())
            .put("suppliers", org.json.JSONArray(invited))
        browser.evaluateJavascript("window.GenSolarEpcBridge.createRfq(" + payload + ")") { result ->
            if (result == "true") {
                message = "RFQ issued and saved in the active offline project."
                rfqRef = ""; rfqItem = ""; rfqQty = ""; rfqSuppliers = ""
                refresh()
            } else message = "RFQ not saved. Check duplicate reference, supplier names and required values."
        }
    }

    fun savePo() {
        if (ref.isBlank() || supplier.isBlank()) {
            message = "Enter the PO reference and supplier."
            return
        }
        val lines = mutableListOf<JSONObject>()
        val rawLines = poLineItems.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        for ((index, line) in rawLines.withIndex()) {
            val parts = line.split("|").map { it.trim() }
            if (parts.size != 4 || parts[0].isBlank() || parts[2].isBlank()) {
                message = "Line ${index + 1}: use Description | Quantity | Unit | Rate."
                return
            }
            val lineQty = parts[1].toDoubleOrNull()
            val lineRate = parts[3].toDoubleOrNull()
            if (lineQty == null || !lineQty.isFinite() || lineQty <= 0.0 || lineRate == null || !lineRate.isFinite() || lineRate <= 0.0) {
                message = "Line ${index + 1}: quantity and rate must be greater than zero."
                return
            }
            lines.add(JSONObject().put("item", parts[0]).put("qty", lineQty).put("unit", parts[2]).put("rate", lineRate))
        }
        if (lines.isEmpty() || lines.size > 30) {
            message = "Add between 1 and 30 valid PO material lines."
            return
        }
        val units = lines.map { it.getString("unit").lowercase() }.distinct()
        if (units.size != 1) {
            message = "Use one common unit per PO for accurate goods-receipt reconciliation."
            return
        }
        val quantity = lines.sumOf { it.getDouble("qty") }
        val orderValue = lines.sumOf { it.getDouble("qty") * it.getDouble("rate") }
        if (!isValidIsoDate(deliveryDate)) {
            message = "Enter a valid delivery date in YYYY-MM-DD format."
            return
        }
        val payload = JSONObject()
            .put("ref", ref.trim()).put("vendor", supplier.trim())
            .put("amount", orderValue).put("qty", quantity)
            .put("unit", lines.first().getString("unit")).put("date", deliveryDate.trim()).put("status", "Ordered")
            .put("rfqRef", poRfqRef.trim()).put("lines", org.json.JSONArray(lines))
        browser.evaluateJavascript("window.GenSolarEpcBridge.createPurchaseOrder(" + payload + ")") { result ->
            if (result == "true") {
                message = "Purchase order saved."
                ref = ""; supplier = ""; amount = ""; poRfqRef = ""; orderedQty = ""; poLineItems = ""
                refresh()
            } else message = "PO not saved. Check for a duplicate reference or invalid order details."
        }
    }

    fun updatePoStatus() {
        val selected = rows.firstOrNull { it.getOrElse(0) { "" } == lifecyclePo }
        if (selected == null) {
            message = "Select a saved purchase order before changing its status."
            return
        }
        if (selected.getOrElse(4) { "Draft" } == nextPoStatus) {
            message = "The purchase order already has this status."
            return
        }
        val script = "window.GenSolarEpcBridge.updatePurchaseOrderStatus(" +
            JSONObject.quote(lifecyclePo) + "," + JSONObject.quote(nextPoStatus) + ")"
        browser.evaluateJavascript(script) { result ->
            if (result == "true") {
                message = "Purchase order status updated and audit history saved."
                refresh()
            } else {
                message = "Status change rejected or not saved. Check the allowed transition and try again."
                refresh()
            }
        }
    }

    fun saveReceipt() {
        val quantity = receiptQty.trim().toDoubleOrNull()
        val selectedPo = rows.firstOrNull { it.getOrElse(0) { "" } == receiptPo }
        if (selectedPo == null) {
            message = "Select a valid purchase order before recording a receipt."
            return
        }
        if (receiver.isBlank()) {
            message = "Enter the name of the person receiving the materials."
            return
        }
        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) {
            message = "Received quantity must be a valid number greater than zero."
            return
        }
        if (!isValidIsoDate(receiptDate)) {
            message = "Enter a valid receipt date in YYYY-MM-DD format."
            return
        }
        val ordered = selectedPo.getOrNull(5)?.toDoubleOrNull()
        val poUnit = selectedPo.getOrNull(6)?.takeIf { it.isNotBlank() } ?: unit
        val selectedLines = try { org.json.JSONArray(selectedPo.getOrElse(8) { "[]" }) } catch (_: Exception) { org.json.JSONArray() }
        val selectedLine = if (selectedLines.length() > 0) (0 until selectedLines.length()).mapNotNull { selectedLines.optJSONObject(it) }.firstOrNull { it.optString("item").equals(receiptItem, true) } else null
        if (selectedLines.length() > 0 && selectedLine == null) {
            message = "Select a material line before recording this receipt."
            return
        }
        if (selectedLine != null && quantity > selectedLine.optDouble("qty") - receipts.filter { it.getOrElse(0) { "" } == receiptPo && it.getOrElse(8) { "" }.equals(receiptItem, true) }.sumOf { it.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0 } + 1e-9) {
            message = "Receipt exceeds the remaining balance for " + receiptItem + "."
            return
        }
        if (ordered != null && ordered > 0.0) {
            val alreadyReceived = receipts.filter {
                it.getOrElse(0) { "" } == receiptPo &&
                    it.getOrElse(2) { "" }.equals(poUnit, ignoreCase = true)
            }.sumOf { it.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0 }
            val remaining = (ordered - alreadyReceived).coerceAtLeast(0.0)
            if (quantity > remaining + 1e-9) {
                message = "Receipt exceeds the remaining PO balance of " + "%.3f".format(remaining) + " " + poUnit + "."
                return
            }
        }
        val payload = JSONObject()
            .put("poRef", receiptPo).put("qty", quantity)
            .put("unit", poUnit)
            .put("date", receiptDate.trim()).put("condition", "Good")
            .put("receiver", receiver.trim()).put("documentRef", "").put("remarks", "").put("item", receiptItem.trim())
        browser.evaluateJavascript("window.GenSolarEpcBridge.createGoodsReceipt(" + payload + ")") { result ->
            if (result == "true") {
                message = "Goods receipt saved and PO balance reconciled."
                receiptQty = ""; receiver = ""
                refresh()
            } else message = "Receipt not saved. Recheck the PO balance and receipt details."
        }
    }

    fun saveStockIssue() {
        val quantity = stockIssueQty.trim().toDoubleOrNull()
        if (stockIssueRef.isBlank() || stockIssuePo.isBlank() || stockIssueItem.isBlank() || stockIssueTo.isBlank() || stockIssuePackage.isBlank()) {
            message = "Enter the issue reference, PO, material, recipient and work package."
            return
        }
        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) {
            message = "Issue quantity must be greater than zero."
            return
        }
        if (!isValidIsoDate(stockIssueDate)) {
            message = "Enter a valid stock issue date in YYYY-MM-DD format."
            return
        }
        val payload = JSONObject()
            .put("issueRef", stockIssueRef.trim()).put("poRef", stockIssuePo.trim())
            .put("item", stockIssueItem.trim()).put("qty", quantity)
            .put("date", stockIssueDate.trim()).put("issuedTo", stockIssueTo.trim())
            .put("workPackage", stockIssuePackage.trim()).put("remarks", stockIssueRemarks.trim())
        browser.evaluateJavascript("window.GenSolarEpcBridge.createStockIssue(" + payload + ")") { result ->
            if (result == "true") {
                message = "Material issue recorded. Available on-hand stock has been reduced."
                stockIssueRef = ""; stockIssueQty = ""; stockIssueTo = ""; stockIssuePackage = ""; stockIssueRemarks = ""
                refresh()
            } else {
                message = "Issue rejected. Check available Good-condition stock, duplicate reference and PO material."
                refresh()
            }
        }
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("PROCUREMENT & MATERIAL CONTROL", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Purchase orders to site receipt.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Create orders, record incoming materials and reconcile ordered, received and balance quantities.", color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("SUPPLIER QUOTATION REGISTER", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text("Record comparable vendor offers before selecting the supplier.", color = Color(0xFF60756E), fontSize = 11.sp)
                SurveyInput("Supplier name", quoteSupplier, { quoteSupplier = it })
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Quoted amount (₹)", quoteAmount, { quoteAmount = it }, Modifier.weight(1f))
                    SurveyInput("Delivery days", quoteDays, { quoteDays = it }, Modifier.weight(1f))
                }
                SurveyInput("Commercial / warranty notes", quoteNotes, { quoteNotes = it })
                SurveyInput("Linked RFQ reference (optional)", quoteRfqRef, { quoteRfqRef = it })
                Button(onClick = { saveQuote() }, modifier = Modifier.fillMaxWidth()) { Text("Save supplier quotation") }
                if (vendorRows.isEmpty()) Text("No supplier quotations recorded.", color = Color(0xFF60756E), fontSize = 12.sp)
                vendorRows.forEachIndexed { index, row ->
                    val quote = row.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                    val lowest = vendorRows.firstOrNull()?.getOrNull(1)?.toDoubleOrNull() ?: quote
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (index == 0) Color(0xFFE8F3EE) else Color(0xFFF3F6F5))) {
                        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text((if (index == 0) "LOWEST QUOTE  •  " else "RANK " + (index + 1) + "  •  ") + row.getOrElse(0) { "" }, color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("₹" + "%,.2f".format(quote) + "  |  " + row.getOrElse(2) { "0" } + " delivery days  |  +" + "%,.2f".format((quote - lowest).coerceAtLeast(0.0)), color = Color(0xFF60756E), fontSize = 11.sp)
                            if (row.getOrElse(3) { "" }.isNotBlank()) Text(row[3], color = Color(0xFF60756E), fontSize = 10.sp)
                            if (row.getOrElse(4) { "" }.isNotBlank()) Text("Linked RFQ: " + row[4], color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    supplier = row.getOrElse(0) { "" }
                                    amount = quote.toString()
                                    poRfqRef = row.getOrElse(4) { "" }
                                    message = if (poRfqRef.isNotBlank()) "RFQ-linked quotation selected. The source RFQ will be validated when the PO is saved." else "Quotation selected for PO. Complete the PO reference, quantity, unit and delivery date before saving."
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Use this quotation in purchase order") }
                        }
                    }
                }
                Text("Lowest price is not automatic award approval. Verify technical compliance, warranty, taxes and payment terms.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 14.sp)
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("REQUEST FOR QUOTATION (RFQ)", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text("Create a material request, invite suppliers and set a response deadline before comparing quotations.", color = Color(0xFF60756E), fontSize = 11.sp)
                SurveyInput("RFQ reference", rfqRef, { rfqRef = it })
                SurveyInput("Material / technical specification", rfqItem, { rfqItem = it })
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Requested quantity", rfqQty, { rfqQty = it }, Modifier.weight(1f))
                    SurveyInput("Unit", rfqUnit, { rfqUnit = it }, Modifier.weight(1f))
                }
                SurveyInput("Quotation due date (YYYY-MM-DD)", rfqDueDate, { rfqDueDate = it })
                SurveyInput("Invited suppliers (comma separated)", rfqSuppliers, { rfqSuppliers = it })
                Button(onClick = { saveRfq() }, modifier = Modifier.fillMaxWidth()) { Text("Issue request for quotation") }
                if (rfqRows.isEmpty()) Text("No RFQs issued for this project.", color = Color(0xFF60756E), fontSize = 12.sp)
                rfqRows.forEach { row ->
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6F5))) {
                        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(row.getOrElse(0) { "" } + " · " + row.getOrElse(6) { "Issued" }, color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(row.getOrElse(1) { "" } + " · " + row.getOrElse(2) { "" } + " " + row.getOrElse(3) { "" }, color = Color(0xFF60756E), fontSize = 11.sp)
                            Text("Due " + row.getOrElse(4) { "" } + " · Invited: " + row.getOrElse(5) { "" }, color = Color(0xFF60756E), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("NEW PURCHASE ORDER", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                SurveyInput("PO / RFQ reference", ref, { ref = it })
                SurveyInput("Supplier / vendor", supplier, { supplier = it })
                SurveyInput("Source RFQ reference (validated)", poRfqRef, { poRfqRef = it })
                Text("IMPORT FROM PROJECT BOM", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                if (bomRows.isEmpty()) {
                    Text("Generate or save the project BOM first to import material quantities.", color = Color(0xFF60756E), fontSize = 10.sp)
                } else {
                    bomRows.take(20).forEach { bom ->
                        val itemName = bom.getOrElse(0) { "" } + if (bom.getOrElse(1) { "" }.isNotBlank()) " • " + bom[1] else ""
                        val qty = bom.getOrElse(3) { "0" }
                        val bomUnit = bom.getOrElse(2) { "Nos" }
                        val rate = bom.getOrElse(4) { "0" }
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val imported = itemName + " | " + qty + " | " + bomUnit + " | " + rate
                                poLineItems = if (poLineItems.isBlank()) imported else poLineItems.trimEnd() + "\n" + imported
                                message = "BOM material added to the PO lines. Verify its supplier rate before saving."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("＋ " + bom[0] + "  ·  " + qty + " " + bomUnit + "  ·  ₹" + rate, fontSize = 10.sp) }
                    }
                }
                SurveyInput(
                    "Material lines — Description | Quantity | Unit | Rate (one line per item)",
                    poLineItems, { poLineItems = it }, minLines = 4
                )
                Text("Example: 550W solar module | 20 | Nos | 8500. Multiple lines are totalled automatically. Keep one common unit per PO for receipt reconciliation.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Expected date (YYYY-MM-DD)", deliveryDate, { deliveryDate = it }, Modifier.weight(1f))
                }
                Button(onClick = { savePo() }, modifier = Modifier.fillMaxWidth()) { Text("Save purchase order") }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("GOODS RECEIPT", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                if (rows.isEmpty()) {
                    Text("Create a purchase order before recording goods receipt.", color = Color(0xFF60756E), fontSize = 12.sp)
                } else {
                    Text("Purchase order: " + receiptPo, color = Color(0xFF60756E), fontSize = 12.sp)
                    val selectedReceiptOrder = rows.firstOrNull { it.getOrElse(0) { "" } == receiptPo }
                    val receiptLines = try { org.json.JSONArray(selectedReceiptOrder?.getOrElse(8) { "[]" } ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
                    if (receiptLines.length() > 0) {
                        Text("SELECT MATERIAL LINE", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        for (lineIndex in 0 until receiptLines.length()) {
                            val line = receiptLines.optJSONObject(lineIndex) ?: continue
                            val itemName = line.optString("item")
                            val itemQty = line.optDouble("qty")
                            val itemUnit = line.optString("unit")
                            val itemReceived = receipts.filter { it.getOrElse(0) { "" } == receiptPo && it.getOrElse(8) { "" }.equals(itemName, true) }.sumOf { it.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0 }
                            FilterChip(
                                selected = receiptItem.equals(itemName, true),
                                onClick = { receiptItem = itemName },
                                label = { Text(itemName + " · " + "%.2f".format(itemReceived) + "/" + "%.2f".format(itemQty) + " received · " + "%.2f".format((itemQty - itemReceived).coerceAtLeast(0.0)) + " " + itemUnit + " balance", fontSize = 10.sp) }
                            )
                        }
                    }
                    val orderedReceiptQty = selectedReceiptOrder?.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                    val receiptUnit = selectedReceiptOrder?.getOrNull(6)?.takeIf { it.isNotBlank() } ?: "Nos"
                    val receivedReceiptQty = receipts.filter {
                        it.getOrElse(0) { "" } == receiptPo &&
                            it.getOrElse(2) { "" }.equals(receiptUnit, ignoreCase = true)
                    }.sumOf { it.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0 }
                    val remainingReceiptQty = (orderedReceiptQty - receivedReceiptQty).coerceAtLeast(0.0)
                    val deliveryProgress = if (orderedReceiptQty > 0.0) {
                        ((receivedReceiptQty / orderedReceiptQty) * 100.0).coerceIn(0.0, 100.0)
                    } else 0.0
                    Text(
                        "Received: %.3f %s  ·  Remaining: %.3f %s".format(receivedReceiptQty, receiptUnit, remainingReceiptQty, receiptUnit),
                        color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Text("Delivery progress: %.1f%%".format(deliveryProgress), color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        rows.forEach { row ->
                            val key = row.getOrElse(0) { "" }
                            FilterChip(selected = receiptPo == key, onClick = { receiptPo = key }, label = { Text(key, fontSize = 10.sp) })
                        }
                    }
                    SurveyInput("Received quantity", receiptQty, { receiptQty = it })
                    SurveyInput("Receipt date (YYYY-MM-DD)", receiptDate, { receiptDate = it })
                    SurveyInput("Received by", receiver, { receiver = it })
                    Button(onClick = { saveReceipt() }, modifier = Modifier.fillMaxWidth()) { Text("Save goods receipt") }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("MATERIAL STOCK LEDGER", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text("Available stock counts only Good-condition receipts. Damaged and quarantined quantities remain separate for inspection.", color = Color(0xFF60756E), fontSize = 10.sp)
                if (stockLedgerRows.isEmpty()) {
                    Text("No item-wise PO receipts recorded yet.", color = Color(0xFF60756E), fontSize = 11.sp)
                } else {
                    stockLedgerRows.forEach { stock ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6F5))) {
                            Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stock.getOrElse(1) { "" } + " · " + stock.getOrElse(0) { "" }, color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Ordered: " + stock.getOrElse(3) { "0" } + " " + stock.getOrElse(2) { "" } + " · Received: " + stock.getOrElse(4) { "0" }, color = Color(0xFF60756E), fontSize = 10.sp)
                                Text("Good received: " + stock.getOrElse(5) { "0" } + " · Issued: " + stock.getOrElse(10) { "0" } + " · On hand: " + stock.getOrElse(11) { "0" }, color = BrandGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Short: " + stock.getOrElse(6) { "0" } + " · Damaged: " + stock.getOrElse(7) { "0" } + " · Quarantined: " + stock.getOrElse(8) { "0" }, color = Color(0xFF60756E), fontSize = 10.sp)
                                Text("Pending delivery: " + stock.getOrElse(9) { "0" } + " " + stock.getOrElse(2) { "" }, color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("ISSUE MATERIAL TO SITE", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text("Only Good-condition, unissued material can be allocated to a site work package.", color = Color(0xFF60756E), fontSize = 10.sp)
                if (stockLedgerRows.isEmpty()) {
                    Text("Record a Good-condition receipt against a multi-line PO before issuing material.", color = Color(0xFF60756E), fontSize = 11.sp)
                } else {
                    stockLedgerRows.forEach { stock ->
                        val selected = stockIssuePo == stock.getOrElse(0) { "" } && stockIssueItem == stock.getOrElse(1) { "" }
                        FilterChip(
                            selected = selected,
                            onClick = { stockIssuePo = stock.getOrElse(0) { "" }; stockIssueItem = stock.getOrElse(1) { "" } },
                            label = { Text(stock.getOrElse(1) { "" } + " · On hand " + stock.getOrElse(11) { "0" } + " " + stock.getOrElse(2) { "" }, fontSize = 10.sp) }
                        )
                    }
                    SurveyInput("Issue reference", stockIssueRef, { stockIssueRef = it })
                    SurveyInput("Issue quantity", stockIssueQty, { stockIssueQty = it })
                    SurveyInput("Issue date (YYYY-MM-DD)", stockIssueDate, { stockIssueDate = it })
                    SurveyInput("Issued to", stockIssueTo, { stockIssueTo = it })
                    SurveyInput("Work package / activity", stockIssuePackage, { stockIssuePackage = it })
                    SurveyInput("Remarks", stockIssueRemarks, { stockIssueRemarks = it })
                    Button(onClick = { saveStockIssue() }, modifier = Modifier.fillMaxWidth()) { Text("Record material issue") }
                }
                stockIssueRows.takeLast(8).forEach { issue ->
                    Text(issue.getOrElse(0) { "" } + " · " + issue.getOrElse(2) { "" } + " · " + issue.getOrElse(3) { "0" } + " " + issue.getOrElse(4) { "" } + " → " + issue.getOrElse(7) { "" }, color = Color(0xFF60756E), fontSize = 10.sp)
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("PO STATUS LIFECYCLE", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                if (rows.isEmpty()) {
                    Text("Create a purchase order before recording status changes.", color = Color(0xFF60756E), fontSize = 12.sp)
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        rows.forEach { row ->
                            val key = row.getOrElse(0) { "" }
                            FilterChip(selected = lifecyclePo == key, onClick = { lifecyclePo = key }, label = { Text(key, fontSize = 10.sp) })
                        }
                    }
                    val selectedOrder = rows.firstOrNull { it.getOrElse(0) { "" } == lifecyclePo }
                    Text("Current status: " + (selectedOrder?.getOrElse(4) { "Unknown" } ?: "Select a purchase order"), color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    selectedOrder?.getOrNull(7)?.takeIf { it.isNotBlank() }?.let { Text("Source RFQ: " + it, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Draft", "RFQ sent", "Approved", "Ordered", "On hold", "Cancelled").forEach { value ->
                            FilterChip(selected = nextPoStatus == value, onClick = { nextPoStatus = value }, label = { Text(value, fontSize = 10.sp) })
                        }
                    }
                    Button(
                        onClick = { updatePoStatus() },
                        enabled = selectedOrder != null && nextPoStatus != selectedOrder.getOrElse(4) { "Draft" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save PO status change") }
                    Text("Partially received and Received are controlled by validated goods receipts and cannot be selected manually.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 14.sp)
                }
                Text("STATUS AUDIT HISTORY", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                if (poHistory.isEmpty()) {
                    Text("No purchase status changes recorded yet.", color = Color(0xFF60756E), fontSize = 11.sp)
                } else {
                    poHistory.asReversed().take(25).forEach { entry ->
                        Text(
                            entry.getOrElse(0) { "" } + "  •  " +
                                entry.getOrElse(1) { "" } + " → " + entry.getOrElse(2) { "" } +
                                "  •  " + entry.getOrElse(3) { "" },
                            color = Color(0xFF60756E),
                            fontSize = 10.sp
                        )
                    }
                    val csvCell: (String) -> String = { value -> "\"" + value.replace("\"", "\"\"") + "\"" }
                    val csv = buildString {
                        appendLine("PO reference,Previous status,New status,Updated date")
                        poHistory.forEach { entry -> appendLine(entry.take(4).map(csvCell).joinToString(",")) }
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onExportCsv("GenSolar-po-status-history.csv", csv) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Export PO status history (CSV)") }
                }
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ORDER RECONCILIATION", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    androidx.compose.material3.TextButton(onClick = { refresh() }) { Text("Refresh") }
                }
                if (rows.isEmpty()) Text("No purchase orders saved.", color = Color(0xFF60756E), fontSize = 12.sp)
                rows.forEach { row ->
                    val key = row.getOrElse(0) { "" }
                    val qty = row.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0
                    val unitName = row.getOrElse(6) { "" }
                    val received = receipts.filter { it.getOrElse(0) { "" } == key && it.getOrElse(2) { "" }.equals(unitName, true) }.sumOf { it.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0 }
                    val poLines = try { org.json.JSONArray(row.getOrElse(8) { "[]" }) } catch (_: Exception) { org.json.JSONArray() }
                    val poStatus = row.getOrElse(4) { "" }
                    val expectedDelivery = try { java.time.LocalDate.parse(row.getOrElse(3) { "" }) } catch (_: Exception) { null }
                    val today = java.time.LocalDate.now()
                    val deliveryHealth = when {
                        poStatus == "Received" -> "Delivered"
                        poStatus == "Cancelled" -> "Cancelled"
                        poStatus == "On hold" -> "On hold"
                        expectedDelivery == null -> "Check expected date"
                        expectedDelivery.isBefore(today) -> "OVERDUE by " + java.time.temporal.ChronoUnit.DAYS.between(expectedDelivery, today) + " days"
                        expectedDelivery.isEqual(today) -> "Due today"
                        !expectedDelivery.isAfter(today.plusDays(7)) -> "Due in " + java.time.temporal.ChronoUnit.DAYS.between(today, expectedDelivery) + " days"
                        else -> "On schedule"
                    }
                    val deliveryColor = when {
                        deliveryHealth.startsWith("OVERDUE") -> Color(0xFFB42318)
                        deliveryHealth.startsWith("Due") -> Color(0xFF9A6700)
                        deliveryHealth == "On schedule" || deliveryHealth == "Delivered" -> AccentGreen
                        else -> Color(0xFF60756E)
                    }
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6F5))) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(key, color = BrandGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(row.getOrElse(1) { "" } + " • ₹" + row.getOrElse(2) { "" }, color = Color(0xFF60756E), fontSize = 11.sp)
                            if (poLines.length() > 0) {
                                Text("MATERIAL LINE BREAKDOWN", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                                for (lineIndex in 0 until poLines.length()) {
                                    val line = poLines.optJSONObject(lineIndex) ?: continue
                                    Text(
                                        "• " + line.optString("item") + " — " + line.optString("qty") + " " + line.optString("unit") +
                                            " × ₹" + line.optString("rate") + " = ₹" + line.optString("amount"),
                                        color = Color(0xFF60756E), fontSize = 10.sp
                                    )
                                }
                            }
                            Text("Ordered " + qty + " " + unitName + " | Received " + received + " | Balance " + maxOf(0.0, qty - received), color = Color(0xFF60756E), fontSize = 10.sp)
                            Text("Status: " + poStatus + " • Delivery: " + row.getOrElse(3) { "" }, color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("Delivery alert: " + deliveryHealth, color = deliveryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (rows.isNotEmpty()) {
                    val deliveryAlert: (List<String>) -> String = { order ->
                        val status = order.getOrElse(4) { "" }
                        val due = try { java.time.LocalDate.parse(order.getOrElse(3) { "" }) } catch (_: Exception) { null }
                        val today = java.time.LocalDate.now()
                        when {
                            status == "Received" -> "Delivered"
                            status == "Cancelled" -> "Cancelled"
                            status == "On hold" -> "On hold"
                            due == null -> "Check expected date"
                            due.isBefore(today) -> "OVERDUE by " + java.time.temporal.ChronoUnit.DAYS.between(due, today) + " days"
                            due.isEqual(today) -> "Due today"
                            !due.isAfter(today.plusDays(7)) -> "Due in " + java.time.temporal.ChronoUnit.DAYS.between(today, due) + " days"
                            else -> "On schedule"
                        }
                    }
                    val csvCell: (String) -> String = { value -> "\"" + value.replace("\"", "\"\"") + "\"" }
                    val csv = buildString {
                        appendLine("PO reference,Supplier,Order value,Expected date,Status,Ordered quantity,Unit,Received quantity,Balance,Delivery alert")
                        rows.forEach { row ->
                            val key = row.getOrElse(0) { "" }
                            val qty = row.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0
                            val unitName = row.getOrElse(6) { "" }
                            val received = receipts.filter { it.getOrElse(0) { "" } == key && it.getOrElse(2) { "" }.equals(unitName, true) }.sumOf { it.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0 }
                            appendLine((row.take(7) + listOf(received.toString(), maxOf(0.0, qty - received).toString(), deliveryAlert(row))).map(csvCell).joinToString(","))
                        }
                    }
                    androidx.compose.material3.OutlinedButton(onClick = { onExportCsv("GenSolar-procurement-reconciliation.csv", csv) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Export PO & receipt reconciliation (CSV)")
                    }
                }
            }
        }
        Text(message, color = Color(0xFF60756E), fontSize = 11.sp, lineHeight = 16.sp)
        androidx.compose.material3.OutlinedButton(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) { Text("Open detailed purchasing workspace") }
        Text("Supplier approval, physical inspection and commercial acceptance remain project-team controls.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 15.sp)
    }
}




@Composable
private fun NativeHandoverScreen(browser: WebView, modifier: Modifier = Modifier) {
    val checklistLabels = listOf(
        "As-built PV layout and approved drawings",
        "Final SLD and protection schedule",
        "Equipment datasheets and serial number schedule",
        "Commissioning and electrical test records",
        "Operating and maintenance manual",
        "Warranty certificates and supplier contacts",
        "DISCOM / utility approvals and meter records",
        "Safety instructions and emergency shutdown procedure",
        "Training and customer demonstration",
        "Open defects and corrective actions disclosed"
    )
    var customer by remember { mutableStateOf("") }
    var site by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var installer by remember { mutableStateOf("") }
    var customerRep by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Draft") }
    var observations by remember { mutableStateOf("") }
    var packageRef by remember { mutableStateOf("") }
    var checks by remember { mutableStateOf(List(10) { false }) }
    var message by remember { mutableStateOf("Handover and acceptance records are saved in the active project.") }

    fun refresh() {
        browser.evaluateJavascript("(function(){var b=window.GenSolarEpcBridge;return b?JSON.stringify(b.getHandover()):'{}'})()") { raw ->
            try {
                val encoded = org.json.JSONTokener(raw ?: "\"{}\"").nextValue() as? String ?: "{}"
                val h = JSONObject(encoded)
                customer = h.optString("customer", "")
                site = h.optString("site", "")
                date = h.optString("date", java.time.LocalDate.now().toString())
                installer = h.optString("installer", "")
                customerRep = h.optString("customerRep", "")
                status = h.optString("status", "Draft")
                observations = h.optString("observations", "")
                packageRef = h.optString("package", "")
                val checklist = h.optJSONObject("checklist") ?: JSONObject()
                checks = List(10) { i -> checklist.optBoolean(i.toString(), false) }
            } catch (_: Exception) { message = "Unable to load the handover register." }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    fun toggle(index: Int, checked: Boolean) {
        browser.evaluateJavascript("window.GenSolarEpcBridge.updateHandoverChecklist($index,$checked)") { raw ->
            if (raw == "true") {
                checks = checks.toMutableList().also { it[index] = checked }
                message = "Checklist item saved."
            } else message = "Checklist update could not be saved."
        }
    }
    fun save() {
        val payload = JSONObject().put("customer", customer.trim()).put("site", site.trim())
            .put("date", date.trim()).put("installer", installer.trim()).put("customerRep", customerRep.trim())
            .put("status", status).put("observations", observations.trim()).put("package", packageRef.trim())
        browser.evaluateJavascript("window.GenSolarEpcBridge.saveHandover(" + payload + ")") { raw ->
            message = if (raw == "true") "Customer handover record saved." else
                "Save rejected. Enter customer, site, valid date and installer. Accepted requires customer representative and all checklist items."
            if (raw == "true") refresh()
        }
    }

    val completed = checks.count { it }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PROJECT CLOSE-OUT", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Customer handover & acceptance", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Record the handover package, customer acknowledgement and completion evidence before closing an EPC project.", color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("HANDOVER PARTICULARS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                SurveyInput("Customer / organisation", customer, { customer = it })
                SurveyInput("Project / site", site, { site = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Handover date (YYYY-MM-DD)", date, { date = it }, Modifier.weight(1f))
                    SurveyInput("Package reference", packageRef, { packageRef = it }, Modifier.weight(1f))
                }
                SurveyInput("Installer / EPC representative", installer, { installer = it })
                SurveyInput("Customer representative", customerRep, { customerRep = it })
                Text("ACCEPTANCE STATUS", color = Color(0xFF60756E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Draft", "Pending customer review", "Accepted with observations", "Accepted", "Rejected").forEach { item ->
                        FilterChip(selected = status == item, onClick = { status = item }, label = { Text(item, fontSize = 10.sp) })
                    }
                }
                SurveyInput("Customer observations / outstanding items", observations, { observations = it }, minLines = 3)
                Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Save handover record") }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("HANDOVER DOCUMENT CHECKLIST", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .8.sp)
                    Spacer(Modifier.weight(1f))
                    Text("$completed / 10", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                checklistLabels.forEachIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checks[index], onCheckedChange = { toggle(index, it) })
                        Text(label, color = Color(0xFF24413B), fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
                Text("Accepted status is blocked until all ten checklist items are complete and a customer representative is recorded.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        Text(message, color = Color(0xFF60756E), fontSize = 11.sp, lineHeight = 16.sp)
        Text("Customer acceptance is a project record only. It does not replace utility approvals, statutory certificates, safety close-out or professional engineering sign-off.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun NativeCommissioningScreen(
    browser: WebView,
    modifier: Modifier = Modifier,
    onExportCsv: (String, String) -> Unit
) {
    var category by remember { mutableStateOf("Electrical") }
    var testName by remember { mutableStateOf("") }
    var measured by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("V") }
    var criterion by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("") }
    var instrument by remember { mutableStateOf("") }
    var tester by remember { mutableStateOf("") }
    var testDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var result by remember { mutableStateOf("Not tested") }
    var action by remember { mutableStateOf("") }
    var actionOwner by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var actionStatus by remember { mutableStateOf("Open") }
    var remarks by remember { mutableStateOf("") }
    var records by remember { mutableStateOf(listOf<JSONObject>()) }
    var message by remember { mutableStateOf("Commissioning records are stored in the active offline project.") }

    fun refresh() {
        browser.evaluateJavascript("(function(){var b=window.GenSolarEpcBridge;return b?JSON.stringify(b.getCommissioning()):'[]'})()") { raw ->
            try {
                val encoded = org.json.JSONTokener(raw ?: "\"[]\"").nextValue() as? String ?: "[]"
                val array = org.json.JSONArray(encoded)
                records = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
            } catch (_: Exception) { message = "Unable to read commissioning register." }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    fun saveRecord() {
        val payload = JSONObject()
            .put("category", category).put("test", testName.trim()).put("measured", measured.trim())
            .put("unit", unit.trim()).put("criterion", criterion.trim()).put("result", result)
            .put("method", method.trim()).put("instrument", instrument.trim()).put("tester", tester.trim())
            .put("date", testDate.trim()).put("remarks", remarks.trim()).put("action", action.trim())
            .put("actionOwner", actionOwner.trim()).put("dueDate", dueDate.trim()).put("actionStatus", actionStatus)
        browser.evaluateJavascript("window.GenSolarEpcBridge.createCommissioningRecord(" + payload + ")") { raw ->
            if (raw == "true") {
                message = "Commissioning test saved."
                testName = ""; measured = ""; criterion = ""; method = ""; instrument = ""; tester = ""; remarks = ""
                action = ""; actionOwner = ""; dueDate = ""; result = "Not tested"
                refresh()
            } else message = "Record rejected. Check required fields, numeric measurement, valid dates and corrective-action details for failed tests."
        }
    }

    fun updateAction(index: Int, next: String) {
        browser.evaluateJavascript("window.GenSolarEpcBridge.updateCommissioningAction($index," + JSONObject.quote(next) + ")") { raw ->
            message = if (raw == "true") "Corrective action status updated." else "Action update rejected."
            if (raw == "true") refresh()
        }
    }

    val csv = buildString {
        appendLine("Category,Test,Measured,Unit,Criterion,Result,Method,Instrument,Tester,Date,Remarks,Corrective action,Owner,Due date,Action status")
        records.forEach { r ->
            val fields = listOf("category","test","measured","unit","criterion","result","method","instrument","tester","date","remarks","action","actionOwner","dueDate","actionStatus")
            appendLine(fields.joinToString(",") { key -> "\"" + r.optString(key).replace("\"", "\"\"") + "\"" })
        }
    }
    val passCount = records.count { it.optString("result") == "Pass" }
    val failCount = records.count { it.optString("result") == "Fail" }
    val openCount = records.count { it.optString("actionStatus") == "Open" || it.optString("actionStatus") == "In progress" }

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("COMMISSIONING & QUALITY", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Test register and corrective actions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Capture measured results, acceptance criteria, test method, instrument and accountable follow-up.", color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Records" to records.size.toString(), "Pass" to passCount.toString(), "Fail" to failCount.toString(), "Open actions" to openCount.toString()).forEach { pair ->
                Card(Modifier.weight(1f), shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(9.dp)) {
                        Text(pair.second, color = BrandGreen, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(pair.first, color = Color(0xFF60756E), fontSize = 9.sp)
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("NEW TEST RECORD", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Electrical", "Mechanical", "Safety", "Performance", "Documentation").forEach { item ->
                        FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item, fontSize = 10.sp) })
                    }
                }
                SurveyInput("Test / inspection name", testName, { testName = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Measured value", measured, { measured = it }, Modifier.weight(1f))
                    SurveyInput("Unit", unit, { unit = it }, Modifier.weight(1f))
                }
                SurveyInput("Acceptance criterion", criterion, { criterion = it })
                SurveyInput("Test method / procedure", method, { method = it })
                SurveyInput("Instrument / serial number", instrument, { instrument = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Tester / responsible person", tester, { tester = it }, Modifier.weight(1f))
                    SurveyInput("Test date (YYYY-MM-DD)", testDate, { testDate = it }, Modifier.weight(1f))
                }
                Text("TEST RESULT", color = Color(0xFF60756E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Not tested", "Pass", "Fail", "Hold / N.A.").forEach { item ->
                        FilterChip(selected = result == item, onClick = { result = item }, label = { Text(item, fontSize = 10.sp) })
                    }
                }
                SurveyInput("Corrective action (mandatory for Fail)", action, { action = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurveyInput("Action owner", actionOwner, { actionOwner = it }, Modifier.weight(1f))
                    SurveyInput("Due date (YYYY-MM-DD)", dueDate, { dueDate = it }, Modifier.weight(1f))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Open", "In progress", "Closed").forEach { item ->
                        FilterChip(selected = actionStatus == item, onClick = { actionStatus = item }, label = { Text(item, fontSize = 10.sp) })
                    }
                }
                SurveyInput("Remarks / site conditions", remarks, { remarks = it })
                Button(onClick = { saveRecord() }, modifier = Modifier.fillMaxWidth()) { Text("Save commissioning record") }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SAVED TEST REGISTER", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { refresh() }) { Text("Refresh") }
                }
                if (records.isEmpty()) Text("No commissioning tests saved yet.", color = Color(0xFF60756E), fontSize = 12.sp)
                records.forEachIndexed { index, r ->
                    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6F5))) {
                        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(r.optString("test"), color = BrandGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(r.optString("category") + " • " + r.optString("date") + " • " + r.optString("result"), color = AccentGreen, fontSize = 10.sp)
                            Text("Measured: " + r.optString("measured", "—") + " " + r.optString("unit") + " | Criterion: " + r.optString("criterion"), color = Color(0xFF60756E), fontSize = 10.sp)
                            if (r.optString("action").isNotBlank()) {
                                Text("Action: " + r.optString("action") + " • Owner: " + r.optString("actionOwner") + " • Due: " + r.optString("dueDate"), color = Color(0xFF60756E), fontSize = 10.sp)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf("Open", "In progress", "Closed").forEach { next ->
                                        FilterChip(selected = r.optString("actionStatus") == next, onClick = { updateAction(index, next) }, label = { Text(next, fontSize = 9.sp) })
                                    }
                                }
                            }
                        }
                    }
                }
                androidx.compose.material3.OutlinedButton(onClick = { onExportCsv("GenSolar-commissioning-register.csv", csv) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export commissioning register (CSV)")
                }
            }
        }
        Text(message, color = Color(0xFF60756E), fontSize = 11.sp, lineHeight = 16.sp)
        Text("Commissioning tests must follow approved procedures and manufacturer instructions using suitable calibrated instruments. Grid synchronisation, statutory approvals and electrical sign-off require authorised personnel.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun NativeDprScreen(
    browser: WebView,
    modifier: Modifier = Modifier,
    onExport: (String, String, String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var site by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var revision by remember { mutableStateOf("R0") }
    var documentName by remember { mutableStateOf("") }
    var documentRevision by remember { mutableStateOf("R0") }
    var documentOwner by remember { mutableStateOf("") }
    var documentDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var documentStatus by remember { mutableStateOf("Draft") }
    var documents by remember { mutableStateOf(listOf<List<String>>()) }
    var bom by remember { mutableStateOf(listOf<List<String>>()) }
    var activities by remember { mutableStateOf(listOf<List<String>>()) }
    var message by remember { mutableStateOf("DPR details and document revisions are stored in the active project.") }

    fun refresh() {
        browser.evaluateJavascript("(function(){var b=window.GenSolarEpcBridge;return b?JSON.stringify(b.getDprData()):'{}'})()") { raw ->
            try {
                val encoded = org.json.JSONTokener(raw ?: "\"{}\"").nextValue() as? String ?: "{}"
                val data = JSONObject(encoded)
                val p = data.optJSONObject("project") ?: JSONObject()
                projectName = p.optString("dprProjectName", "")
                customer = p.optString("dprCustomer", "")
                site = p.optString("dprSite", "")
                capacity = p.optString("dprCapacity", "")
                owner = p.optString("dprOwner", "")
                revision = p.optString("dprRevision", "R0")
                fun readRows(key: String): List<List<String>> {
                    val array = data.optJSONArray(key) ?: org.json.JSONArray()
                    return (0 until array.length()).map { i ->
                        val row = array.optJSONArray(i) ?: org.json.JSONArray()
                        (0 until row.length()).map { j -> row.optString(j, "") }
                    }
                }
                documents = readRows("documents")
                bom = readRows("bom")
                activities = readRows("activities")
            } catch (_: Exception) {
                message = "Unable to load DPR records from the project."
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun saveDetails() {
        val payload = JSONObject()
            .put("dprProjectName", projectName.trim())
            .put("dprCustomer", customer.trim())
            .put("dprSite", site.trim())
            .put("dprCapacity", capacity.trim())
            .put("dprOwner", owner.trim())
            .put("dprRevision", revision.trim())
        browser.evaluateJavascript("window.GenSolarEpcBridge.saveDprDetails(" + payload + ")") { result ->
            message = if (result == "true") "DPR project details saved." else "Save rejected. Enter all project details and a positive capacity."
            if (result == "true") refresh()
        }
    }

    fun saveDocument() {
        val payload = JSONObject()
            .put("name", documentName.trim())
            .put("revision", documentRevision.trim())
            .put("status", documentStatus)
            .put("owner", documentOwner.trim())
            .put("date", documentDate.trim())
        browser.evaluateJavascript("window.GenSolarEpcBridge.upsertDprDocument(" + payload + ")") { result ->
            message = if (result == "true") {
                documentName = ""
                documentOwner = ""
                "Document revision saved to register."
            } else "Document rejected. Check name, revision, owner, status and valid date."
            if (result == "true") refresh()
        }
    }

    val csvCell: (String) -> String = { value -> "\"" + value.replace("\"", "\"\"") + "\"" }
    val documentCsv = buildString {
        appendLine("Document,Revision,Status,Owner,Date")
        documents.forEach { row -> appendLine(row.map(csvCell).joinToString(",")) }
    }
    val dprText = buildString {
        appendLine("GENSOLAR PRO ENGINEERING")
        appendLine("DETAILED PROJECT REPORT")
        appendLine("Project: " + projectName)
        appendLine("Customer: " + customer)
        appendLine("Site: " + site)
        appendLine("Plant capacity: " + capacity + " kW")
        appendLine("Prepared by: " + owner)
        appendLine("Revision: " + revision)
        appendLine("")
        appendLine("MATERIAL / BOQ SCHEDULE")
        appendLine("Item | Specification | Unit | Quantity | Rate")
        bom.forEach { row -> appendLine(row.joinToString(" | ")) }
        appendLine("")
        appendLine("CONSTRUCTION ACTIVITY SCHEDULE")
        activities.forEach { row -> appendLine(row.joinToString(" | ")) }
        appendLine("")
        appendLine("DRAWING & DOCUMENT REGISTER")
        documents.forEach { row -> appendLine(row.joinToString(" | ")) }
        appendLine("")
        appendLine("Engineering calculations, drawings, approvals and site measurements must be attached and reviewed before issue.")
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("DPR & DOCUMENT CONTROL", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Project report and revision register.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Maintain project particulars, track drawing revisions and export a working DPR snapshot from saved project schedules.", color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("PROJECT PARTICULARS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                SurveyInput("Project name", projectName, { projectName = it })
                SurveyInput("Customer / client", customer, { customer = it })
                SurveyInput("Site / location", site, { site = it })
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Capacity (kW)", capacity, { capacity = it }, Modifier.weight(1f))
                    SurveyInput("Revision", revision, { revision = it }, Modifier.weight(1f))
                }
                SurveyInput("Prepared by / owner", owner, { owner = it })
                Button(onClick = { saveDetails() }, modifier = Modifier.fillMaxWidth()) { Text("Save DPR particulars") }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("DRAWING & DOCUMENT REGISTER", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                SurveyInput("Document / drawing name", documentName, { documentName = it })
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Revision", documentRevision, { documentRevision = it }, Modifier.weight(1f))
                    SurveyInput("Issue date", documentDate, { documentDate = it }, Modifier.weight(1f))
                }
                SurveyInput("Responsible owner", documentOwner, { documentOwner = it })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Draft", "In review", "Approved", "Issued").forEach { item ->
                        FilterChip(selected = documentStatus == item, onClick = { documentStatus = item }, label = { Text(item, fontSize = 10.sp) })
                    }
                }
                Button(onClick = { saveDocument() }, modifier = Modifier.fillMaxWidth()) { Text("Save document revision") }
                if (documents.isEmpty()) Text("No drawings or documents registered yet.", color = Color(0xFF60756E), fontSize = 12.sp)
                documents.forEach { row ->
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6F5))) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(row.getOrElse(0) { "" }, color = BrandGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Rev " + row.getOrElse(1) { "" } + " • " + row.getOrElse(2) { "" }, color = AccentGreen, fontSize = 11.sp)
                            Text(row.getOrElse(3) { "" } + " • " + row.getOrElse(4) { "" }, color = Color(0xFF60756E), fontSize = 10.sp)
                        }
                    }
                }
                androidx.compose.material3.OutlinedButton(onClick = { onExport("GenSolar-document-register.csv", documentCsv, "text/csv") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export document register (CSV)")
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("DPR SNAPSHOT", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text("Included saved data: " + bom.size + " BOQ lines • " + activities.size + " construction activities • " + documents.size + " document revisions", color = Color(0xFF60756E), fontSize = 11.sp)
                androidx.compose.material3.OutlinedButton(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh project snapshot") }
                Button(onClick = { onExport("GenSolar-DPR-" + (revision.ifBlank { "R0" }) + ".txt", dprText, "text/plain") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export DPR snapshot (TXT)")
                }
            }
        }
        Text(message, color = Color(0xFF60756E), fontSize = 11.sp, lineHeight = 16.sp)
        Text("This is a working DPR snapshot, not a signed engineering report. Attach approved drawings, site evidence, calculations, statutory approvals and professional sign-off before customer issue.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun NativeConstructionScreen(
    browser: WebView,
    modifier: Modifier = Modifier,
    onOpenAdvanced: () -> Unit,
    onExportCsv: (String, String) -> Unit
) {
    var activityName by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("1") }
    var status by remember { mutableStateOf("Not started") }
    var progress by remember { mutableStateOf("0") }
    var activities by remember { mutableStateOf(listOf<List<String>>()) }
    var message by remember { mutableStateOf("Activities are saved in the active offline project.") }

    fun refresh() {
        browser.evaluateJavascript("(function(){var b=window.GenSolarEpcBridge;return b?JSON.stringify(b.getActivities()):'[]'})()") { raw ->
            try {
                val value = org.json.JSONTokener(raw ?: "\"[]\"").nextValue() as? String ?: "[]"
                val array = org.json.JSONArray(value)
                activities = (0 until array.length()).map { i ->
                    val row = array.optJSONArray(i) ?: org.json.JSONArray()
                    (0 until row.length()).map { j -> row.optString(j, "") }
                }
            } catch (_: Exception) {
                message = "Unable to read the saved activity register."
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun createActivity() {
        val days = duration.toIntOrNull()
        val percent = progress.toIntOrNull()
        val parsedStart = try { java.time.LocalDate.parse(startDate.trim()) } catch (_: Exception) { null }
        if (activityName.isBlank() || owner.isBlank()) {
            message = "Enter both the activity name and responsible owner."
            return
        }
        if (parsedStart == null || parsedStart.toString() != startDate.trim()) {
            message = "Enter a valid planned start date in YYYY-MM-DD format."
            return
        }
        if (days == null || days < 1) {
            message = "Duration must be a whole number of at least one day."
            return
        }
        if (percent == null || percent !in 0..100) {
            message = "Progress must be a whole number from 0 to 100."
            return
        }
        if ((status == "Completed" && percent != 100) || (status == "Not started" && percent != 0)) {
            message = "Completed activities require 100%; Not started activities require 0%."
            return
        }
        val payload = JSONObject()
            .put("name", activityName.trim())
            .put("owner", owner.trim())
            .put("start", startDate.trim())
            .put("days", days)
            .put("status", status)
            .put("progress", percent)
        browser.evaluateJavascript("window.GenSolarEpcBridge.createActivity($payload)") { result ->
            if (result == "true") {
                message = "Activity saved to the active project."
                activityName = ""
                owner = ""
                duration = "1"
                status = "Not started"
                progress = "0"
                refresh()
            } else {
                message = "Not saved. Check date, duration and status/progress rules."
            }
        }
    }

    fun updateActivity(index: Int, nextStatus: String, nextProgress: String) {
        val percent = nextProgress.toIntOrNull()
        if (percent == null || percent !in 0..100) {
            message = "Progress must be a whole number from 0 to 100."
            return
        }
        if ((nextStatus == "Completed" && percent != 100) || (nextStatus == "Not started" && percent != 0)) {
            message = "Completed activities require 100%; Not started activities require 0%."
            return
        }
        val script = "window.GenSolarEpcBridge.updateActivity(" + index + "," + JSONObject.quote(nextStatus) + "," + percent + ")"
        browser.evaluateJavascript(script) { result ->
            if (result == "true") {
                message = "Activity progress updated."
                refresh()
            } else {
                message = "Update rejected. Completed requires 100%; Not started requires 0%."
            }
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("CONSTRUCTION & SITE QA", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Track delivery on site.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Create activities, assign owners and update progress in the active offline project.", color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("NEW ACTIVITY", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                SurveyInput("Activity name", activityName, { activityName = it })
                SurveyInput("Responsible owner", owner, { owner = it })
                SurveyInput("Planned start (YYYY-MM-DD)", startDate, { startDate = it })
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SurveyInput("Duration (days)", duration, { duration = it }, Modifier.weight(1f))
                    SurveyInput("Progress (%)", progress, { progress = it }, Modifier.weight(1f))
                }
                Text("Activity status", color = Color(0xFF60756E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Not started", "In progress", "Completed", "On hold").forEach { item ->
                        FilterChip(
                            selected = status == item,
                            onClick = {
                                status = item
                                if (item == "Completed") progress = "100"
                                if (item == "Not started") progress = "0"
                            },
                            label = { Text(item, fontSize = 11.sp) }
                        )
                    }
                }
                Button(onClick = { createActivity() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save activity to project")
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ACTIVITY REGISTER", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    androidx.compose.material3.TextButton(onClick = { refresh() }) { Text("Refresh") }
                }
                if (activities.isEmpty()) {
                    Text("No activities saved in this project yet.", color = Color(0xFF60756E), fontSize = 12.sp)
                } else {
                    activities.forEachIndexed { index, row ->
                        val activity = row.getOrElse(0) { "" }
                        val activityOwner = row.getOrElse(1) { "" }
                        val plannedStart = row.getOrElse(2) { "" }
                        val plannedFinish = row.getOrElse(5) { "" }
                        val days = row.getOrElse(3) { "" }
                        var rowStatus by remember(index, row) { mutableStateOf(row.getOrElse(4) { "Not started" }) }
                        var rowProgress by remember(index, row) { mutableStateOf(row.getOrElse(6) { "0" }) }
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6F5))) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(activity, color = BrandGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("$activityOwner • $plannedStart → $plannedFinish • $days day(s)", color = Color(0xFF60756E), fontSize = 10.sp)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Not started", "In progress", "Completed", "On hold").forEach { item ->
                                        FilterChip(
                                            selected = rowStatus == item,
                                            onClick = {
                                                rowStatus = item
                                                if (item == "Completed") rowProgress = "100"
                                                if (item == "Not started") rowProgress = "0"
                                            },
                                            label = { Text(item, fontSize = 10.sp) }
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SurveyInput("Progress (%)", rowProgress, { rowProgress = it }, Modifier.weight(1f))
                                    Button(onClick = { updateActivity(index, rowStatus, rowProgress) }) { Text("Update") }
                                }
                            }
                        }
                    }
                    val csvCell: (String) -> String = { value -> "\"" + value.replace("\"", "\"\"") + "\"" }
                    val csvText = buildString {
                        appendLine(listOf("Activity", "Owner", "Planned start", "Duration (days)", "Planned finish", "Status", "Progress (%)").joinToString(","))
                        activities.forEach { row -> appendLine(row.take(7).map(csvCell).joinToString(",")) }
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onExportCsv("GenSolar-construction-activity-schedule.csv", csvText) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Export activity register (CSV)") }
                }
            }
        }
        Text(message, color = Color(0xFF60756E), fontSize = 11.sp, lineHeight = 16.sp)
        androidx.compose.material3.OutlinedButton(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) {
            Text("Open detailed construction & QA workspace")
        }
        Text("Progress records do not replace site inspection acceptance, safety controls or qualified engineering sign-off.", color = Color(0xFF60756E), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun NativeBomScreen(
    browser: WebView,
    modifier: Modifier = Modifier,
    onOpenAdvanced: () -> Unit,
    onExportCsv: (String, String) -> Unit
) {
    var items by remember { mutableStateOf(emptyList<NativeBomLine>()) }
    var projectSummary by remember { mutableStateOf("No saved BOM loaded.") }
    var status by remember { mutableStateOf("Load a saved BOM or build one from the active PV layout.") }
    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
    val total = items.sumOf { it.quantity * (it.rate.toDoubleOrNull() ?: 0.0) }

    fun applyBomResponse(raw: String?) {
        try {
            val response = org.json.JSONTokener(raw ?: "{}").nextValue() as? String ?: "{}"
            val json = JSONObject(response)
            if (json.has("error")) {
                status = json.optString("error")
                return
            }
            val rows = json.optJSONArray("items") ?: org.json.JSONArray()
            val loaded = (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONArray(index) ?: return@mapNotNull null
                if (row.length() < 5) return@mapNotNull null
                NativeBomLine(
                    name = row.optString(0),
                    specification = row.optString(1),
                    unit = row.optString(2),
                    quantity = row.optDouble(3, 0.0),
                    rate = row.optDouble(4, 0.0).toString()
                )
            }
            items = loaded
            val projectName = json.optString("projectName", "Active project")
            val modules = json.optInt("modules")
            val kwp = json.optDouble("kwp")
            val strings = json.optInt("strings")
            projectSummary = "$projectName · $modules modules · ${String.format(java.util.Locale.US, "%.2f", kwp)} kWp · $strings strings"
            status = if (loaded.isEmpty()) "No BOM line items are saved for this project." else "Loaded ${loaded.size} material groups from the active project."
        } catch (_: Exception) {
            status = "Could not read the project BOM. Open the detailed workspace to inspect the saved data."
        }
    }

    fun loadSavedBom() {
        browser.evaluateJavascript(
            "(function(){try{var bridge=window.GenSolarEpcBridge;if(!bridge)return JSON.stringify({error:'Native project bridge is unavailable.'});var project=bridge.getProjectSummary();return JSON.stringify({items:project.bom||[],projectName:project.project&&project.project.customer||'Active project',modules:project.layout&&project.layout.count||0,kwp:project.layout&&project.layout.kwp||0,strings:project.electrical&&project.electrical.strings||0});}catch(e){return JSON.stringify({error:e.message});}})()"
        ) { raw -> applyBomResponse(raw) }
    }

    fun buildFromProject() {
        status = "BUILDING BOM FROM CURRENT DESIGN"
        browser.evaluateJavascript(
            "(function(){try{var bridge=window.GenSolarEpcBridge;if(!bridge)return JSON.stringify({error:'Native project bridge is unavailable.'});var before=bridge.getProjectSummary();if(!before.layout)return JSON.stringify({error:'Generate a PV layout first.'});if(!bridge.buildBom())return JSON.stringify({error:'BOM generation or offline save failed. Check the project inputs and device storage.'});var project=bridge.getProjectSummary();return JSON.stringify({items:project.bom||[],projectName:project.project&&project.project.customer||'Active project',modules:project.layout&&project.layout.count||0,kwp:project.layout&&project.layout.kwp||0,strings:project.electrical&&project.electrical.strings||0});}catch(e){return JSON.stringify({error:e.message||'BOM generation failed'});}})()"
        ) { raw -> applyBomResponse(raw) }
    }

    fun saveRates() {
        if (items.isEmpty()) {
            status = "Build or load a BOM before saving rates."
            return
        }
        val rows = org.json.JSONArray()
        items.forEach { item ->
            val rate = item.rate.toDoubleOrNull()
            if (rate == null || !rate.isFinite() || rate < 0) {
                status = "Enter a valid non-negative rate for ${item.name}."
                return
            }
            rows.put(org.json.JSONArray().put(item.name).put(item.specification).put(item.unit).put(item.quantity).put(rate))
        }
        browser.evaluateJavascript(
            "(function(rows){try{var bridge=window.GenSolarEpcBridge;if(!bridge)return 'BRIDGE_UNAVAILABLE';return bridge.setBom(rows)?'SAVED':'SAVE_FAILED';}catch(e){return 'ERROR:'+e.message;}})($rows)"
        ) { raw ->
            val result = try { org.json.JSONTokener(raw ?: "\"ERROR\"").nextValue() as? String ?: "ERROR" } catch (_: Exception) { "ERROR" }
            status = when {
                result == "SAVED" -> "Rates saved to the active project offline."
                result == "BRIDGE_UNAVAILABLE" -> "Project bridge is unavailable. Reopen the workspace and retry."
                result.startsWith("ERROR:") -> result.removePrefix("ERROR:")
                else -> "Project save failed. Check device storage and retry."
            }
        }
    }

    fun exportCsv() {
        if (items.isEmpty()) {
            status = "Build or load a BOM before exporting."
            return
        }
        val table = listOf(listOf("Item", "Specification", "Unit", "Quantity", "Rate (INR)", "Amount (INR)")) +
            items.map { item ->
                val rate = item.rate.toDoubleOrNull() ?: 0.0
                listOf(item.name, item.specification, item.unit, item.quantity.toString(), rate.toString(), (item.quantity * rate).toString())
            }
        val csv = table.joinToString("\r\n") { row ->
            row.joinToString(",") { value -> "\"" + value.replace("\"", "\"\"") + "\"" }
        } + "\r\n"
        onExportCsv("GenSolar-BOM.csv", csv)
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("BOM / BOQ", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Turn design outputs into a cost schedule.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Material quantities come from the active PV layout and shared electrical project data. Rates remain editable and are saved to the same offline project.", color = Color(0xFFD7E8E1), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("DESIGN BASIS", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(projectSummary, color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = ::loadSavedBom, modifier = Modifier.weight(1f)) { Text("Load saved BOM") }
                    androidx.compose.material3.OutlinedButton(onClick = ::buildFromProject, modifier = Modifier.weight(1f)) { Text("Build from design") }
                }
                Text(status, color = Color(0xFF60756E), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        if (items.isEmpty()) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text("No material schedule is loaded. Generate a PV layout before building the preliminary BOM.", modifier = Modifier.padding(16.dp), color = Color(0xFF667A75), fontSize = 12.sp)
            }
        } else {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("PRELIMINARY COST SUMMARY", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text(currency.format(total), color = BrandGreen, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${items.size} material groups · rates excluding taxes, freight and installation unless entered in the rate.", color = Color(0xFF667A75), fontSize = 10.sp)
                }
            }
            items.forEachIndexed { index, item ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.name, color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(item.specification, color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else String.format(java.util.Locale.US, "%.2f", item.quantity)} ${item.unit}", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(currency.format(item.quantity * (item.rate.toDoubleOrNull() ?: 0.0)), color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedTextField(
                            value = item.rate,
                            onValueChange = { next -> items = items.toMutableList().also { it[index] = item.copy(rate = next) } },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Rate per ${item.unit} (₹)", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            Button(onClick = ::saveRates, modifier = Modifier.fillMaxWidth()) { Text("Save rates to project") }
            androidx.compose.material3.OutlinedButton(onClick = ::exportCsv, modifier = Modifier.fillMaxWidth()) { Text("Export BOM CSV") }
        }
        androidx.compose.material3.OutlinedButton(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) {
            Text("Open detailed BOM, fabrication and vendor tools")
        }
        Text("Quantities are preliminary allowances. Structural member sizes, support spans, protection ratings, cable routes, earthing and final procurement quantities require project-specific engineering review.", color = Color(0xFF667A75), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun SurveyInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 11.sp) },
        singleLine = minLines == 1,
        minLines = minLines,
        shape = RoundedCornerShape(13.dp)
    )
}

private data class NativeBomLine(val name: String, val specification: String, val unit: String, val quantity: Double, val rate: String)

private data class NativeDestination(val key: String, val label: String, val symbol: String)
