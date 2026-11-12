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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
            geoCallback?.invoke(geoOrigin, granted, false)
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
            AndroidView(
                factory = { browser },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

private data class NativeDestination(val key: String, val label: String, val symbol: String)
