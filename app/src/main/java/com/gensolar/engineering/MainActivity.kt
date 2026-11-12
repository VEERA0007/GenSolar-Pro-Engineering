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
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
        val script = when (destination) {
            "staad" -> "document.querySelector('.stage[data-stage=engineering]')?.click();document.querySelectorAll('.tab').forEach(function(b){if(b.textContent.includes('STAAD Structural'))b.click()});"
            "procurement" -> "document.querySelector('.stage[data-stage=procurement]')?.click();"
            "construction" -> "document.querySelector('.stage[data-stage=construction]')?.click();"
            "cadstudio" -> "document.querySelector('[data-foot=cadstudio]')?.click();"
            else -> "document.querySelector('[data-foot=$destination]')?.click();"
        }
        browser.evaluateJavascript(script, null)
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
                        onNavigate(destination)
                    }
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
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BrandGreen)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SOLAR EPC · PROJECT WORKSPACE", color = Color(0xFFF3B51B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text("Design smarter. Deliver with confidence.", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)
                Text("One workspace for site survey, PV engineering, structural checks, CAD, procurement and handover.", color = Color(0xFFD8E8E1), fontSize = 13.sp)
                Button(onClick = { onNavigate("site") }) { Text("Start site survey  →") }
            }
        }
        Text("PROJECT OVERVIEW", color = Color(0xFF60756E), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            NativeMetric("01", "Site survey", Modifier.weight(1f))
            NativeMetric("02", "PV design", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            NativeMetric("03", "Engineering", Modifier.weight(1f))
            NativeMetric("04", "DPR / BOQ", Modifier.weight(1f))
        }
        Text("ENGINEERING WORKSPACES", color = Color(0xFF60756E), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        NativeActionCard("Site survey & GPS", "Capture project details, roof dimensions, site photos and location.", "Open survey", "site", onNavigate)
        NativeActionCard("PV design & auto layout", "Size the PV array, place modules and export layout schedules.", "Open PV design", "layout", onNavigate)
        NativeActionCard("CAD / 2D / 3D studio", "Import drawings, build conceptual 3D and export DXF outputs.", "Open CAD studio", "cadstudio", onNavigate)
        NativeActionCard("Electrical engineering", "Check strings, inverter compatibility, batteries, cables and SLD.", "Open electrical", "electrical", onNavigate)
        NativeActionCard("Structural analysis", "Build portal frames, review member actions and export STAAD models.", "Open structural", "staad", onNavigate)
        NativeActionCard("BOM, procurement & DPR", "Prepare material schedules, vendor records and project handover.", "Open project outputs", "procurement", onNavigate)
        Text("Engineering outputs are preliminary until checked against approved equipment datasheets, applicable standards, site conditions and signed calculations.", color = Color(0xFF60756E), fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
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

private data class NativeDestination(val key: String, val label: String, val symbol: String)
