package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.fonts.Font;
import android.graphics.fonts.SystemFonts;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared, bounded bridges for the trusted in-app rich-text editor asset. */
final class EditorSupport {
    static final int COMPOSE_IMAGE_LIMIT_BYTES = 2_000_000;
    static final int SIGNATURE_IMAGE_LIMIT_BYTES = 180_000;
    private static final ExecutorService IMAGE_EXECUTOR =
            Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "mailxperts-editor-image");
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            });

    private EditorSupport() {}

    static String editorAsset(Context context) {
        try (InputStream input = context.getAssets().open("editor.html");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (Exception error) {
            return "<html><body>Editor unavailable</body></html>";
        }
    }

    static String decodeJavascriptString(String value) {
        if (value == null || "null".equals(value)) return "";
        try { return new JSONArray("[" + value + "]").getString(0); }
        catch (Exception ignored) { return ""; }
    }

    static void setHtml(WebView editor, String html) {
        editor.evaluateJavascript("window.MailXpertsEditor.setHtml("
                + JSONObject.quote(html == null ? "" : html) + ")", null);
    }

    static void insertPickedImage(Activity activity, WebView editor, Uri uri, int byteLimit) {
        if (uri == null) return;
        IMAGE_EXECUTOR.execute(() -> {
            try {
                String dataUri = imageDataUri(activity, uri, byteLimit);
                activity.runOnUiThread(() -> editor.evaluateJavascript(
                        "window.MailXpertsEditor.insertImage("
                                + JSONObject.quote(dataUri) + ","
                                + JSONObject.quote("Inserted image") + ")", null));
            } catch (Exception error) {
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        error.getMessage() == null ? "That image could not be inserted."
                                : error.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private static String imageDataUri(Context context, Uri uri, int byteLimit) throws Exception {
        int safeLimit = Math.max(32_000, byteLimit);
        String mime = context.getContentResolver().getType(uri);
        if (mime == null) mime = "image/png";
        mime = mime.toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(mime)) mime = "image/jpeg";
        if (!("image/png".equals(mime) || "image/jpeg".equals(mime)
                || "image/gif".equals(mime) || "image/webp".equals(mime))) {
            throw new IllegalArgumentException("Choose a PNG, JPEG, GIF or WebP image.");
        }
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalArgumentException("The selected image is unavailable.");
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > safeLimit) {
                    throw new IllegalArgumentException("Image is too large. Choose one under "
                            + (safeLimit / 1000) + " KB.");
                }
                output.write(buffer, 0, count);
            }
            return "data:" + mime + ";base64,"
                    + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        }
    }

    static final class ImageBridge {
        private final Activity activity;
        private final int requestCode;

        ImageBridge(Activity activity, int requestCode) {
            this.activity = activity;
            this.requestCode = requestCode;
        }

        @JavascriptInterface public void chooseImage() {
            activity.runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                activity.startActivityForResult(intent, requestCode);
            });
        }
    }

    static final class FontBridge {
        private final LinkedHashMap<String, File> fonts = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> names = new LinkedHashMap<>();

        FontBridge() {
            if (Build.VERSION.SDK_INT < 29) return;
            try {
                int index = 0;
                for (Font font : SystemFonts.getAvailableFonts()) {
                    File file = font.getFile();
                    if (file == null || !file.isFile() || file.length() <= 0
                            || file.length() > 4_500_000L) continue;
                    String key = "font_" + index++;
                    fonts.put(key, file);
                    names.put(key, friendlyName(file.getName()));
                }
            } catch (Exception ignored) {}
        }

        String fontListJson() {
            JSONArray array = new JSONArray();
            for (Map.Entry<String, String> entry : names.entrySet()) {
                JSONObject item = new JSONObject();
                try {
                    item.put("key", entry.getKey());
                    item.put("name", entry.getValue());
                    array.put(item);
                } catch (Exception ignored) {}
            }
            return array.toString();
        }

        @JavascriptInterface public String fontData(String key) {
            File file = fonts.get(key);
            if (file == null) return "";
            try (FileInputStream input = new FileInputStream(file);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                return "data:font/ttf;base64,"
                        + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
            } catch (Exception ignored) { return ""; }
        }

        private String friendlyName(String filename) {
            String name = filename.replaceFirst("(?i)\\.(ttf|otf|ttc)$", "")
                    .replace('_', ' ').replace('-', ' ');
            return name.replaceAll("\\s+", " ").trim();
        }
    }
}
