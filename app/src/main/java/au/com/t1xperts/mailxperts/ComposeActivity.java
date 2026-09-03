package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.fonts.Font;
import android.graphics.fonts.SystemFonts;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComposeActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<AccountConfig> accounts = new ArrayList<>();
    private AccountConfig account;
    private String accountId;
    private SecureStore store;
    private Spinner fromAccount;
    private EditText to;
    private EditText cc;
    private EditText bcc;
    private EditText subject;
    private WebView editor;
    private Button send;
    private Button draft;
    private Button schedule;
    private TextView status;
    private LocalStore local;
    private LocalStore.LocalMessage existing;
    private boolean editorReady;
    private boolean bodyPopulated;
    private FontBridge fontBridge;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        store = new SecureStore(this);
        local = new LocalStore(this);
        long localId = getIntent().getLongExtra("local_id", -1L);
        if (localId > 0) existing = local.get(localId);
        accounts.addAll(MailboxScope.usable(store));
        if (accounts.isEmpty()) {
            startActivity(new Intent(this, AccountsActivity.class));
            finish();
            return;
        }
        String requested = existing != null
                ? existing.accountId : getIntent().getStringExtra("account_id");
        if (MailboxScope.isAll(requested) || requested == null || requested.isEmpty()) {
            String selected = store.getSelectedId();
            requested = MailboxScope.isAll(selected) ? "" : selected;
        }
        account = MailboxScope.find(accounts, requested);
        if (account == null) account = accounts.get(0);
        accountId = account.id;

        LinearLayout root = Ui.vertical(this);
        root.addView(buildHeader());
        LinearLayout fields = Ui.card(this);
        fields.addView(Ui.label(this, "FROM ACCOUNT"));
        fields.addView(buildFromAccountSpinner());
        to = Ui.edit(this, "To");
        cc = Ui.edit(this, "Cc (optional)");
        bcc = Ui.edit(this, "Bcc (optional)");
        subject = Ui.edit(this, "Subject");
        fields.addView(to);
        fields.addView(cc);
        fields.addView(bcc);
        fields.addView(subject);
        root.addView(fields);

        editor = new WebView(this);
        editor.setBackgroundColor(Ui.background(this));
        WebSettings settings = editor.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        fontBridge = new FontBridge();
        editor.addJavascriptInterface(fontBridge, "MailXpertsFonts");
        editor.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) { return true; }
            @Override public void onPageFinished(WebView view, String url) {
                editorReady = true;
                configureEditor();
                populate();
            }
        });
        editor.loadDataWithBaseURL("https://mailxperts.local/", readEditor(), "text/html", "UTF-8", null);
        root.addView(editor, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        status = Ui.text(this, "From: " + account.email
                + " • UTF-8 Unicode + ASCII + emoji • Rich text + HTML source • device fonts");
        status.setTextColor(Ui.muted(this));
        root.addView(status);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        draft = Ui.secondaryButton(this, "Save Draft", v -> withHtml(this::saveDraft));
        schedule = Ui.secondaryButton(this, "Schedule", v -> withHtml(this::pickSchedule));
        actions.addView(draft, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        LinearLayout.LayoutParams scheduleParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f);
        scheduleParams.setMargins(Ui.dp(this, 8), 0, 0, 0);
        actions.addView(schedule, scheduleParams);
        root.addView(actions);
        Ui.setContentView(this, root);
        populate();
    }

    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button close = Ui.compactButton(this, "‹");
        close.setContentDescription("Close composer");
        close.setOnClickListener(v -> confirmClose());
        header.addView(close, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, existing == null ? "Compose Email" : "Edit Email");
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        send = Ui.compactButton(this, "➤ Send");
        send.setOnClickListener(v -> withHtml(this::sendNow));
        header.addView(send, new LinearLayout.LayoutParams(Ui.dp(this, 104), Ui.dp(this, 48)));
        return header;
    }

    private View buildFromAccountSpinner() {
        ArrayList<String> labels = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < accounts.size(); i++) {
            AccountConfig configured = accounts.get(i);
            labels.add(configured.displayName() + "  •  " + configured.email);
            if (configured.id.equals(accountId)) selected = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, labels) {
            private TextView decorate(View view, boolean dropdown) {
                TextView text = (TextView) view;
                text.setTextColor(Ui.textColor(ComposeActivity.this));
                text.setTextSize(15);
                int vertical = dropdown ? Ui.dp(ComposeActivity.this, 14) : Ui.dp(ComposeActivity.this, 9);
                text.setPadding(Ui.dp(ComposeActivity.this, 12), vertical,
                        Ui.dp(ComposeActivity.this, 12), vertical);
                if (dropdown) text.setBackgroundColor(Ui.panel(ComposeActivity.this));
                return text;
            }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getView(position, convertView, parent), false);
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getDropDownView(position, convertView, parent), true);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromAccount = new Spinner(this);
        fromAccount.setAdapter(adapter);
        fromAccount.setSelection(selected, false);
        final boolean[] ready = {false};
        fromAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!ready[0]) return;
                account = accounts.get(position);
                accountId = account.id;
                if (status != null) {
                    status.setText("From: " + account.email
                            + " • UTF-8 Unicode + ASCII + emoji • Rich text + HTML source • device fonts");
                }
            }
        });
        fromAccount.post(() -> ready[0] = true);
        return fromAccount;
    }

    private void configureEditor() {
        editor.evaluateJavascript("window.MailXpertsEditor.setTheme(" + ThemeManager.isDark(this) + ")", null);
        editor.evaluateJavascript("window.MailXpertsEditor.setDeviceFonts(" + fontBridge.fontListJson() + ")", null);
    }

    private void populate() {
        if (to == null) return;
        if (existing != null) {
            to.setText(existing.to);
            cc.setText(existing.cc);
            bcc.setText(existing.bcc);
            subject.setText(existing.subject);
            if (editorReady && !bodyPopulated) {
                bodyPopulated = true;
                setEditorHtml(existing.html);
            }
            return;
        }
        String recipient = getIntent().getStringExtra("to");
        String requestedSubject = getIntent().getStringExtra("subject");
        if (recipient != null && to.getText().length() == 0) to.setText(recipient);
        if (requestedSubject != null && subject.getText().length() == 0) subject.setText(requestedSubject);
        if (editorReady && !bodyPopulated) {
            bodyPopulated = true;
            String initial = "<p><br></p>";
            if (account.signatureEnabled && account.signatureHtml != null && !account.signatureHtml.trim().isEmpty()) {
                initial += "<div data-mailxperts-signature=\"true\"><br>" + account.signatureHtml + "</div>";
            }
            setEditorHtml(initial);
        }
    }

    private interface HtmlAction { void run(String html); }

    private void withHtml(HtmlAction action) {
        if (!editorReady) {
            Toast.makeText(this, "Editor is still loading", Toast.LENGTH_SHORT).show();
            return;
        }
        editor.evaluateJavascript("window.MailXpertsEditor.getHtml()", value -> action.run(decode(value)));
    }

    private LocalStore.LocalMessage snapshot(String html, String type) {
        LocalStore.LocalMessage message = existing == null ? new LocalStore.LocalMessage() : existing;
        message.accountId = accountId;
        message.type = type;
        message.to = to.getText().toString().trim();
        message.cc = cc.getText().toString().trim();
        message.bcc = bcc.getText().toString().trim();
        message.subject = subject.getText().toString();
        message.html = html == null ? "" : html;
        return message;
    }

    private void saveDraft(String html) {
        LocalStore.LocalMessage message = snapshot(html, LocalStore.DRAFT);
        if (existing != null && LocalStore.SCHEDULED.equals(existing.type)) Scheduler.cancel(this, existing.id);
        message.lastError = "";
        message.scheduledAt = 0;
        local.save(message);
        existing = message;
        Toast.makeText(this, "Saved in Drafts", Toast.LENGTH_SHORT).show();
        status.setTextColor(Ui.muted(this));
        status.setText("Draft saved at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(System.currentTimeMillis()));
    }

    private void sendNow(String html) {
        if (to.getText().toString().trim().isEmpty()) { to.setError("Recipient required"); return; }
        if (existing != null && LocalStore.SCHEDULED.equals(existing.type)) Scheduler.cancel(this, existing.id);
        Ui.setEnabled(send, false, "➤ Send", "Sending…");
        status.setTextColor(Ui.muted(this));
        status.setText("Sending via secure SMTP and saving a copy to Sent…");
        LocalStore.LocalMessage snapshot = snapshot(html, existing != null ? existing.type : LocalStore.DRAFT);
        AccountConfig sendingAccount = account;
        executor.execute(() -> {
            try {
                MailRepository.SendResult result = MailRepository.sendHtml(sendingAccount,
                        snapshot.to, snapshot.cc, snapshot.bcc, snapshot.subject, snapshot.html);
                if (existing != null) {
                    Scheduler.cancel(this, existing.id);
                    local.delete(existing.id);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, result.sentCopySaved ? "Sent and saved in Sent" : "Sent; Sent-folder warning: " + result.sentCopyWarning, Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception error) {
                LocalStore.LocalMessage out = snapshot;
                out.type = LocalStore.OUTBOX;
                out.lastError = MailRepository.safe(error);
                out.scheduledAt = 0;
                local.save(out);
                existing = out;
                runOnUiThread(() -> {
                    Ui.setEnabled(send, true, "➤ Send", "");
                    status.setTextColor(Ui.error(this));
                    status.setText("Send failed. Saved in Outbox for manual retry.\n" + out.lastError);
                });
            }
        });
    }

    private void pickSchedule(String html) {
        if (to.getText().toString().trim().isEmpty()) { to.setError("Recipient required"); return; }
        Calendar now = Calendar.getInstance();
        DatePickerDialog date = new DatePickerDialog(this, (dialog, year, month, day) -> {
            TimePickerDialog time = new TimePickerDialog(this, (timeDialog, hour, minute) -> {
                Calendar when = Calendar.getInstance();
                when.set(year, month, day, hour, minute, 0);
                when.set(Calendar.MILLISECOND, 0);
                if (when.getTimeInMillis() <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Choose a future time", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] labels = {"Once", "Daily", "Weekly", "Monthly"};
                String[] values = {"ONCE", "DAILY", "WEEKLY", "MONTHLY"};
                new AlertDialog.Builder(this)
                        .setTitle("Repeat schedule")
                        .setSingleChoiceItems(labels, 0, null)
                        .setPositiveButton("Schedule", (repeatDialog, which) -> {
                            int choice = ((AlertDialog) repeatDialog).getListView().getCheckedItemPosition();
                            LocalStore.LocalMessage message = snapshot(html, LocalStore.SCHEDULED);
                            if (existing != null) Scheduler.cancel(this, existing.id);
                            message.scheduledAt = when.getTimeInMillis();
                            message.recurrence = values[Math.max(0, choice)];
                            message.lastError = "";
                            local.save(message);
                            existing = message;
                            Scheduler.schedule(this, message);
                            Toast.makeText(this, "Email scheduled", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);
            time.show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        date.show();
    }

    @Override public void onBackPressed() { confirmClose(); }

    private void confirmClose() {
        new AlertDialog.Builder(this)
                .setTitle("Close composer?")
                .setMessage("Save this unsent email as a Draft?")
                .setPositiveButton("Save Draft", (dialog, which) -> withHtml(html -> { saveDraft(html); finish(); }))
                .setNegativeButton("Close", (dialog, which) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void setEditorHtml(String html) {
        editor.evaluateJavascript("window.MailXpertsEditor.setHtml(" + JSONObject.quote(html == null ? "" : html) + ")", null);
    }

    private String readEditor() {
        try (InputStream input = getAssets().open("editor.html"); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (Exception error) {
            return "<html><body>Editor unavailable</body></html>";
        }
    }

    private String decode(String value) {
        if (value == null || "null".equals(value)) return "";
        try { return new JSONArray("[" + value + "]").getString(0); }
        catch (Exception ignored) { return ""; }
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        if (editor != null) {
            editor.removeJavascriptInterface("MailXpertsFonts");
            editor.destroy();
        }
        super.onDestroy();
    }

    private final class FontBridge {
        private final LinkedHashMap<String, File> fonts = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> names = new LinkedHashMap<>();

        FontBridge() {
            if (Build.VERSION.SDK_INT < 29) return;
            try {
                int index = 0;
                for (Font font : SystemFonts.getAvailableFonts()) {
                    File file = font.getFile();
                    if (file == null || !file.isFile() || file.length() <= 0 || file.length() > 4_500_000L) continue;
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
            try (FileInputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                return "data:font/ttf;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
            } catch (Exception ignored) { return ""; }
        }

        private String friendlyName(String filename) {
            String name = filename.replaceFirst("(?i)\\.(ttf|otf|ttc)$", "").replace('_', ' ').replace('-', ' ');
            return name.replaceAll("\\s+", " ").trim();
        }
    }
}
