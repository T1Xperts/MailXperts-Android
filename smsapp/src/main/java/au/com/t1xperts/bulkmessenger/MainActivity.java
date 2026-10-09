package au.com.t1xperts.bulkmessenger;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 4421;
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.addJavascriptInterface(new NativeBridge(), "T1Native");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/plain", "application/vnd.ms-excel"});
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException ex) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        ioExecutor.shutdownNow();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    public class NativeBridge {
        @JavascriptInterface
        public String getVersion() {
            return "0.1.0-p0";
        }

        @JavascriptInterface
        public String getDeviceId() {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            return id == null ? "unknown" : id;
        }

        @JavascriptInterface
        public String newId() {
            return UUID.randomUUID().toString();
        }

        @JavascriptInterface
        public void postJson(String requestId, String endpoint, String bearerToken, String payload) {
            ioExecutor.submit(() -> {
                int status = -1;
                String body;
                try {
                    Uri parsed = Uri.parse(endpoint);
                    if (!"https".equalsIgnoreCase(parsed.getScheme())) {
                        throw new IllegalArgumentException("Only HTTPS backend endpoints are allowed.");
                    }
                    URL url = new URL(endpoint);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(25000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setRequestProperty("Accept", "application/json");
                    if (bearerToken != null && !bearerToken.trim().isEmpty()) {
                        connection.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
                    }
                    byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
                    try (OutputStream out = connection.getOutputStream()) {
                        out.write(bytes);
                    }
                    status = connection.getResponseCode();
                    InputStream stream = status >= 200 && status < 400 ? connection.getInputStream() : connection.getErrorStream();
                    body = readStream(stream);
                    connection.disconnect();
                } catch (Exception ex) {
                    body = "{\"error\":" + JSONObject.quote(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()) + "}";
                }
                final int finalStatus = status;
                final String finalBody = body == null ? "" : body;
                runOnUiThread(() -> {
                    String js = "window.t1OnNativeApiResult(" + JSONObject.quote(requestId) + "," + finalStatus + "," + JSONObject.quote(finalBody) + ")";
                    webView.evaluateJavascript(js, null);
                });
            });
        }

        private String readStream(InputStream stream) throws Exception {
            if (stream == null) return "";
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) builder.append(line).append('\n');
            }
            return builder.toString().trim();
        }
    }
}
