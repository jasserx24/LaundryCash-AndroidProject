package com.tslaundry.cashlog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);     // localStorage = where the records are saved
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient()); // enables confirm() dialogs
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        // Close an open edit sheet first, otherwise exit.
        web.evaluateJavascript(
            "(function(){var m=document.getElementById('modal');" +
            "if(m&&m.classList.contains('on')){closeModal();return 1;}" +
            "if(!document.getElementById('p-dash').classList.contains('on')){go('dash');return 1;}return 0;})()",
            value -> { if (!"1".equals(value)) MainActivity.super.onBackPressed(); });
    }

    class Bridge {
        @JavascriptInterface
        public void share(final String text, final String title) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, title);
                i.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(i, title));
            });
        }
    }
}
