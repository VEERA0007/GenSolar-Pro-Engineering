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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    private lateinit var filePicker: androidx.activity.result.ActivityResultLauncher<Intent>
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
                onNavigate = ::navigateWorkspace
            )
        }
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
            "if (typeof go === 'function') { go('$destination'); }",
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
private fun GenSolarApp(browser: WebView, onNavigate: (String) -> Unit) {
    var selected by remember { mutableStateOf("dashboard") }
    var moreExpanded by remember { mutableStateOf(false) }
    var advancedSite by remember { mutableStateOf(false) }
    var advancedLayout by remember { mutableStateOf(false) }
    BackHandler(enabled = selected != "dashboard") {
        if (selected == "site" && advancedSite) {
            advancedSite = false
        } else if (selected == "layout" && advancedLayout) {
            advancedLayout = false
        } else {
            selected = "dashboard"
            advancedSite = false
            advancedLayout = false
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
                        onNavigate(destination)
                    }
                )
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
        Triple("PV design & layout", "Module placement, setbacks and array sizing.", "layout"),
        Triple("CAD / 2D / 3D studio", "Drawing tools, DXF exchange and 3D model.", "cadstudio"),
        Triple("Solar shade analysis", "Sun path, obstruction direction and shade profile.", "shade"),
        Triple("Electrical engineering", "Strings, MPPT, cables, batteries and SLD.", "electrical"),
        Triple("STAAD structural", "Portal frames, wind basis and member checks.", "staad"),
        Triple("GPS & solar resource", "Location capture, irradiance and yield inputs.", "location"),
        Triple("BOM / BOQ", "Material take-off, quantities and cost schedule.", "bom"),
        Triple("Procurement", "Vendor quotations and purchase tracking.", "procurement"),
        Triple("Construction & QA", "Site progress, inspections and commissioning.", "construction"),
        Triple("DPR & handover", "Reports, drawing register and handover pack.", "dpr")
    )
    val filtered = workspaces.filter { item ->
        val matchesSearch = search.isBlank() || (item.first + " " + item.second).contains(search, ignoreCase = true)
        val group = when (item.third) {
            "site", "location" -> "Survey"
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
        "site", "location" -> "⌖"
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
                  if (typeof state !== 'undefined') delete state.layout;
                  button.click();
                  if (typeof state === 'undefined' || !state.layout) return JSON.stringify({error:'No layout was generated. Check the site survey, roof dimensions and obstacle positions.'});
                  return JSON.stringify({
                    count:state.layout.count, kwp:state.layout.kwp,
                    pitch:state.layout.pitch, orientation:state.layout.orientation,
                    area:(state.layout.zoneStats||[]).reduce(function(a,z){return a+z.area;},0),
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

private data class NativeDestination(val key: String, val label: String, val symbol: String)
