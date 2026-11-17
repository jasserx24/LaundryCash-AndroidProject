package com.tslaundry.cashlog;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQ_FILE = 1001;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);     // localStorage = where the records are saved
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(Intent.createChooser(i, "Choose the backup file"), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        // Close an open edit sheet first, then go back to Dashboard, otherwise exit.
        web.evaluateJavascript(
            "(function(){var m=document.getElementById('modal');" +
            "if(m&&m.classList.contains('on')){closeModal();return 1;}" +
            "if(!document.getElementById('p-dash').classList.contains('on')){go('dash');return 1;}return 0;})()",
            value -> { if (!"1".equals(value)) exitApp(); });
    }

    private void exitApp() {
        super.onBackPressed();
    }

    private void toast(final String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show());
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

        /** Saves a file (base64) to Downloads (Android 10+) and opens the share sheet. */
        @JavascriptInterface
        public void saveFile(final String base64, final String rawName, final String mime) {
            final String name = rawName.replaceAll("[\\\\/:*?\"<>|]", "_");
            byte[] bytes;
            try {
                bytes = Base64.decode(base64, Base64.DEFAULT);
            } catch (Exception e) {
                toast("Could not save the file");
                return;
            }

            // 1) Copy kept inside the app, used for sharing
            File dir = new File(getCacheDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, name);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(bytes);
            } catch (Exception e) {
                toast("Could not save the file");
                return;
            }

            // 2) Copy in the phone's Download folder (Android 10 and up)
            boolean savedToDownloads = false;
            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (u != null) {
                        try (OutputStream os = getContentResolver().openOutputStream(u)) {
                            if (os != null) { os.write(bytes); savedToDownloads = true; }
                        }
                    }
                } catch (Exception ignored) { }
            }
            if (savedToDownloads) toast("Saved to Download folder: " + name);

            // 3) Share sheet (Messenger, Gmail, Drive, Viber…)
            final Uri shareUri = Uri.parse("content://" + FileShareProvider.AUTHORITY + "/" + Uri.encode(name));
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType(mime);
                    i.putExtra(Intent.EXTRA_STREAM, shareUri);
                    i.putExtra(Intent.EXTRA_SUBJECT, name);
                    i.setClipData(ClipData.newRawUri(name, shareUri));
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Intent chooser = Intent.createChooser(i, "Share " + name);
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(chooser);
                } catch (Exception e) {
                    toast("No app available to share with");
                }
            });
        }
    }
}
