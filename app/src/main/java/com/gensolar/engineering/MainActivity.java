package com.gensolar.engineering;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.*;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_REQUEST = 4102;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF103C34);
        getWindow().setNavigationBarColor(0xFF103C34);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFFF3F6F5);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setDefaultTextEncodingName("UTF-8");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "Unable to open file picker", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface public void printPage() {
                runOnUiThread(() -> {
                    try {
                        PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                        manager.print("GenSolar-EPC-Report", webView.createPrintDocumentAdapter("GenSolar EPC"), new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build());
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Print service unavailable", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, "GenSolarNative");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) results = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}