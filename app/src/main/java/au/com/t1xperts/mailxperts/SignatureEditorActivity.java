package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Full-screen visual/HTML signature editor shared with the message composer. */
public class SignatureEditorActivity extends Activity {
    static final String EXTRA_SIGNATURE_HTML = "signature_html";
    private static final int PICK_SIGNATURE_IMAGE = 5101;

    private WebView editor;
    private Button save;
    private EditorSupport.FontBridge fontBridge;
    private EditorSupport.ImageBridge imageBridge;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);

        LinearLayout root = Ui.vertical(this);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button cancel = Ui.compactButton(this, "‹ Cancel");
        cancel.setOnClickListener(v -> cancel());
        header.addView(cancel, new LinearLayout.LayoutParams(
                Ui.dp(this, 100), Ui.dp(this, 48)));
        TextView title = Ui.title(this, "Signature Editor");
        title.setPadding(Ui.dp(this, 8), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        TextView note = Ui.text(this,
                "Use Rich Text for visual editing or HTML Source for exact markup. "
                        + "Image accepts local/shared PNG, JPEG, GIF or WebP files; Image URL "
                        + "keeps an http/https link in the outgoing signature. Linked images "
                        + "are blocked in this secure editor preview and load in compatible "
                        + "mail clients after sending.");
        note.setTextColor(Ui.muted(this));
        note.setTextSize(13);
        root.addView(note);

        editor = new WebView(this);
        editor.setBackgroundColor(Ui.background(this));
        WebSettings settings = editor.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        fontBridge = new EditorSupport.FontBridge();
        imageBridge = new EditorSupport.ImageBridge(this, PICK_SIGNATURE_IMAGE);
        editor.addJavascriptInterface(fontBridge, "MailXpertsFonts");
        editor.addJavascriptInterface(imageBridge, "MailXpertsImages");
        String initial = SignatureHtml.normaliseStored(
                getIntent().getStringExtra(EXTRA_SIGNATURE_HTML));
        editor.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(
                    WebView view, android.webkit.WebResourceRequest request) {
                return true;
            }

            @Override public void onPageFinished(WebView view, String url) {
                editor.evaluateJavascript("window.MailXpertsEditor.setTheme("
                        + ThemeManager.isDark(SignatureEditorActivity.this) + ")", null);
                editor.evaluateJavascript("window.MailXpertsEditor.setDeviceFonts("
                        + fontBridge.fontListJson() + ")", null);
                EditorSupport.setHtml(editor, initial);
                if (save != null) save.setEnabled(true);
            }
        });
        editor.loadDataWithBaseURL("https://mailxperts.local/",
                EditorSupport.editorAsset(this), "text/html", "UTF-8", null);
        root.addView(editor, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        save = Ui.button(this, "Save formatted signature");
        save.setEnabled(false);
        save.setOnClickListener(v -> save());
        root.addView(save);
        Ui.setContentView(this, root);
    }

    private void save() {
        save.setEnabled(false);
        editor.evaluateJavascript("window.MailXpertsEditor.getHtml()", value -> {
            String html = SignatureHtml.sanitise(
                    EditorSupport.decodeJavascriptString(value));
            if (!SignatureHtml.isWithinLimit(html)) {
                save.setEnabled(true);
                Toast.makeText(this,
                        "Signature is too large. Keep its HTML and embedded images under 300 KB.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Intent result = new Intent();
            result.putExtra(EXTRA_SIGNATURE_HTML, html);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SIGNATURE_IMAGE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        EditorSupport.insertPickedImage(this, editor, uri,
                EditorSupport.SIGNATURE_IMAGE_LIMIT_BYTES);
    }

    @Override protected void onDestroy() {
        if (editor != null) {
            editor.removeJavascriptInterface("MailXpertsFonts");
            editor.removeJavascriptInterface("MailXpertsImages");
            editor.destroy();
        }
        super.onDestroy();
    }
}
