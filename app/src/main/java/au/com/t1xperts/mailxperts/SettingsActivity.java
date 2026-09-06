package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText label;
    private EditText email;
    private EditText username;
    private EditText password;
    private EditText imapHost;
    private EditText imapPort;
    private EditText smtpHost;
    private EditText smtpPort;
    private EditText signature;
    private Spinner provider;
    private Spinner smtpSecurity;
    private Spinner syncInterval;
    private TextView providerHelp;
    private Switch syncEnabled;
    private Switch notificationsEnabled;
    private Switch deleteFromServer;
    private Switch syncReadState;
    private Switch syncDraftsToServer;
    private Switch signatureEnabled;
    private Button save;
    private TextView status;
    private SecureStore store;
    private AccountConfig current;
    private boolean providerReady;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        store = new SecureStore(this);
        String id = getIntent().getStringExtra("account_id");
        current = id == null ? new AccountConfig() : store.load(id);
        if (current.provider == null || current.provider.isEmpty()) {
            current.provider = ProviderPreset.infer(current.email, current.imapHost);
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.vertical(this);
        scroll.addView(root);
        root.addView(buildHeader(id == null ? "Add Account" : "Account Settings"));

        LinearLayout connected = Ui.card(this);
        TextView connectedTitle = Ui.text(this, "✓  Secure mail connection");
        connectedTitle.setTextColor(Ui.teal(this));
        connectedTitle.setTextSize(17);
        connected.addView(connectedTitle);
        TextView secureNote = Ui.text(this, "IMAP uses SSL/TLS. SMTP supports SSL/TLS or required STARTTLS. Server identity verification is always enabled.");
        secureNote.setTextColor(Ui.muted(this));
        connected.addView(secureNote);
        root.addView(connected);

        LinearLayout identity = Ui.card(this);
        identity.addView(Ui.label(this, "EMAIL PROVIDER"));
        List<ProviderPreset.Definition> definitions = ProviderPreset.all();
        provider = new Spinner(this);
        ArrayAdapter<ProviderPreset.Definition> providerAdapter = new ArrayAdapter<ProviderPreset.Definition>(
                this, android.R.layout.simple_spinner_item, definitions) {
            private TextView decorate(View view) {
                TextView text = (TextView) view;
                text.setTextColor(Ui.textColor(SettingsActivity.this));
                text.setTextSize(15);
                text.setPadding(Ui.dp(SettingsActivity.this, 12), Ui.dp(SettingsActivity.this, 12),
                        Ui.dp(SettingsActivity.this, 12), Ui.dp(SettingsActivity.this, 12));
                return text;
            }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getView(position, convertView, parent));
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView text = decorate(super.getDropDownView(position, convertView, parent));
                text.setBackgroundColor(Ui.panel(SettingsActivity.this));
                return text;
            }
        };
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        provider.setAdapter(providerAdapter);
        provider.setSelection(ProviderPreset.indexOf(current.provider), false);
        identity.addView(provider);
        providerHelp = Ui.text(this, ProviderPreset.find(current.provider).help);
        providerHelp.setTextColor(Ui.muted(this));
        providerHelp.setTextSize(13);
        providerHelp.setPadding(Ui.dp(this, 4), Ui.dp(this, 5), Ui.dp(this, 4), Ui.dp(this, 10));
        identity.addView(providerHelp);

        identity.addView(Ui.label(this, "ACCOUNT IDENTITY"));
        label = Ui.edit(this, "Work / Personal / Customer Care");
        label.setText(current.label);
        identity.addView(label);
        email = Ui.edit(this, "name@example.com");
        email.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setText(current.email);
        identity.addView(email);
        username = Ui.edit(this, "Usually the full email address");
        username.setText(current.username);
        identity.addView(username);
        email.setOnFocusChangeListener((view, focused) -> {
            if (!focused && username.getText().toString().trim().isEmpty()) {
                username.setText(email.getText().toString().trim());
            }
        });
        password = Ui.password(this);
        password.setHint("Password or provider app-specific password");
        password.setText(current.password);
        identity.addView(password);
        root.addView(identity);

        LinearLayout servers = Ui.card(this);
        servers.addView(Ui.label(this, "INCOMING SERVER — IMAP SSL/TLS"));
        imapHost = Ui.edit(this, "mail.example.com");
        imapHost.setText(current.imapHost);
        servers.addView(imapHost);
        imapPort = Ui.edit(this, "993");
        imapPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        imapPort.setText(String.valueOf(current.imapPort));
        servers.addView(imapPort);
        servers.addView(Ui.label(this, "OUTGOING SERVER — SMTP"));
        smtpHost = Ui.edit(this, "smtp.example.com");
        smtpHost.setText(current.smtpHost);
        servers.addView(smtpHost);
        smtpPort = Ui.edit(this, "465 or 587");
        smtpPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        smtpPort.setText(String.valueOf(current.smtpPort));
        servers.addView(smtpPort);
        ArrayList<String> securityLabels = new ArrayList<>();
        securityLabels.add("SSL/TLS");
        securityLabels.add("STARTTLS (required)");
        smtpSecurity = new Spinner(this);
        ArrayAdapter<String> securityAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, securityLabels) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getView(position, convertView, parent);
                text.setTextColor(Ui.textColor(SettingsActivity.this));
                text.setPadding(Ui.dp(SettingsActivity.this, 12), Ui.dp(SettingsActivity.this, 12),
                        Ui.dp(SettingsActivity.this, 12), Ui.dp(SettingsActivity.this, 12));
                return text;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getDropDownView(position, convertView, parent);
                text.setTextColor(Ui.textColor(SettingsActivity.this));
                text.setBackgroundColor(Ui.panel(SettingsActivity.this));
                text.setPadding(Ui.dp(SettingsActivity.this, 12), Ui.dp(SettingsActivity.this, 12),
                        Ui.dp(SettingsActivity.this, 12), Ui.dp(SettingsActivity.this, 12));
                return text;
            }
        };
        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        smtpSecurity.setAdapter(securityAdapter);
        smtpSecurity.setSelection(AccountConfig.SMTP_STARTTLS.equals(current.smtpSecurity) ? 1 : 0, false);
        servers.addView(smtpSecurity);
        root.addView(servers);

        provider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long rowId) {
                ProviderPreset.Definition definition = definitions.get(position);
                providerHelp.setText(definition.help);
                current.provider = definition.id;
                if (providerReady) applyPreset(definition);
            }
        });
        provider.post(() -> providerReady = true);

        LinearLayout preferences = Ui.card(this);
        preferences.addView(Ui.label(this, "CACHE-FIRST SYNCHRONISATION"));
        syncEnabled = preferenceSwitch("Automatic IMAP synchronisation", current.syncEnabled);
        preferences.addView(syncEnabled);
        preferences.addView(Ui.label(this, "CHECK FOR NEW MAIL"));
        syncInterval = new Spinner(this);
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, SyncPolicy.INTERVAL_LABELS) {
            private TextView decorate(View view, boolean dropdown) {
                TextView text = (TextView) view;
                text.setTextColor(Ui.textColor(SettingsActivity.this));
                text.setTextSize(15);
                text.setPadding(Ui.dp(SettingsActivity.this, 12), Ui.dp(SettingsActivity.this, 12),
                        Ui.dp(SettingsActivity.this, 12), Ui.dp(SettingsActivity.this, 12));
                if (dropdown) text.setBackgroundColor(Ui.panel(SettingsActivity.this));
                return text;
            }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getView(position, convertView, parent), false);
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getDropDownView(position, convertView, parent), true);
            }
        };
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        syncInterval.setAdapter(intervalAdapter);
        syncInterval.setSelection(SyncPolicy.indexOf(current.syncIntervalMinutes), false);
        preferences.addView(syncInterval);
        TextView cacheNote = Ui.text(this,
                "Cached messages open first. New headers are checked in the background, and older mail fills in progressively without blocking the inbox.");
        cacheNote.setTextColor(Ui.muted(this));
        cacheNote.setTextSize(13);
        preferences.addView(cacheNote);

        notificationsEnabled = preferenceSwitch("Notify me when new mail arrives", current.notificationsEnabled);
        preferences.addView(notificationsEnabled);

        preferences.addView(Ui.label(this, "SERVER ACTIONS"));
        syncReadState = preferenceSwitch(
                "Mark opened messages as read on the server", current.syncReadState);
        deleteFromServer = preferenceSwitch(
                "Delete from server too — move to Trash", current.deleteFromServer);
        syncDraftsToServer = preferenceSwitch(
                "Keep saved Drafts in the server Drafts folder", current.syncDraftsToServer);
        preferences.addView(syncReadState);
        preferences.addView(deleteFromServer);
        preferences.addView(syncDraftsToServer);
        TextView actionNote = Ui.text(this,
                "Safe default: server deletion is off. When off, Delete only hides the message on this device. Drafts always save locally first; server Drafts sync is optional.");
        actionNote.setTextColor(Ui.muted(this));
        actionNote.setTextSize(13);
        preferences.addView(actionNote);

        preferences.addView(Ui.label(this, "SIGNATURE"));
        signatureEnabled = preferenceSwitch("Automatically add signature", current.signatureEnabled);
        preferences.addView(signatureEnabled);
        signature = Ui.multiLine(this, "Signature text", 4);
        signature.setText(current.signatureHtml == null ? "" : Html.fromHtml(current.signatureHtml, Html.FROM_HTML_MODE_LEGACY).toString());
        signature.setEnabled(signatureEnabled.isChecked());
        signatureEnabled.setOnCheckedChangeListener((button, checked) -> signature.setEnabled(checked));
        syncEnabled.setOnCheckedChangeListener((button, checked) -> updateSyncPreferenceState());
        syncInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long rowId) {
                updateSyncPreferenceState();
            }
        });
        updateSyncPreferenceState();
        preferences.addView(signature);
        root.addView(preferences);

        status = Ui.text(this, "");
        status.setTextColor(Ui.muted(this));
        root.addView(status);
        save = Ui.button(this, "Test IMAP + SMTP and Save");
        save.setOnClickListener(v -> testAndSave());
        root.addView(save);
        Button forget = Ui.secondaryButton(this, "Forget password", v -> {
            password.setText("");
            if (current.id != null) store.clearPassword(current.id);
            Toast.makeText(this, "Password removed", Toast.LENGTH_SHORT).show();
        });
        root.addView(forget);
        root.addView(Ui.secondaryButton(this, "Appearance", v -> startActivity(new Intent(this, AppearanceActivity.class))));
        Ui.setContentView(this, scroll);
    }

    private void applyPreset(ProviderPreset.Definition definition) {
        if (!definition.imapHost.isEmpty()) imapHost.setText(definition.imapHost);
        imapPort.setText(String.valueOf(definition.imapPort));
        if (!definition.smtpHost.isEmpty()) smtpHost.setText(definition.smtpHost);
        smtpPort.setText(String.valueOf(definition.smtpPort));
        smtpSecurity.setSelection(AccountConfig.SMTP_STARTTLS.equals(definition.smtpSecurity) ? 1 : 0);
        if (label.getText().toString().trim().isEmpty() || "T1Xperts Customer Care".equals(label.getText().toString())) {
            label.setText(definition.name);
        }
        String address = email.getText().toString().trim();
        if (!address.isEmpty()) username.setText(address);
    }

    private LinearLayout buildHeader(String titleText) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button back = Ui.compactButton(this, "‹");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, "MailXperts");
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView section = Ui.text(this, titleText);
        section.setTextColor(Ui.muted(this));
        header.addView(section);
        return header;
    }

    private Switch preferenceSwitch(String text, boolean checked) {
        Switch toggle = new Switch(this);
        toggle.setText(text);
        toggle.setTextColor(Ui.textColor(this));
        toggle.setTextSize(15);
        toggle.setChecked(checked);
        toggle.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10));
        return toggle;
    }

    private void updateSyncPreferenceState() {
        if (syncEnabled == null || syncInterval == null || notificationsEnabled == null) return;
        boolean automatic = syncEnabled.isChecked();
        syncInterval.setEnabled(automatic);
        int index = Math.max(0, syncInterval.getSelectedItemPosition());
        boolean periodic = automatic && SyncPolicy.INTERVAL_MINUTES[index] > 0;
        notificationsEnabled.setEnabled(periodic);
    }

    private AccountConfig read() {
        AccountConfig account = current;
        ProviderPreset.Definition definition = (ProviderPreset.Definition) provider.getSelectedItem();
        account.provider = definition == null ? ProviderPreset.CUSTOM : definition.id;
        account.label = label.getText().toString().trim();
        account.email = email.getText().toString().trim();
        account.username = username.getText().toString().trim();
        account.password = password.getText().toString();
        account.imapHost = imapHost.getText().toString().trim();
        account.smtpHost = smtpHost.getText().toString().trim();
        account.smtpSecurity = smtpSecurity.getSelectedItemPosition() == 1
                ? AccountConfig.SMTP_STARTTLS : AccountConfig.SMTP_SSL;
        try { account.imapPort = Integer.parseInt(imapPort.getText().toString().trim()); }
        catch (Exception ignored) { account.imapPort = 0; }
        try { account.smtpPort = Integer.parseInt(smtpPort.getText().toString().trim()); }
        catch (Exception ignored) { account.smtpPort = 0; }
        account.syncEnabled = syncEnabled.isChecked();
        int intervalIndex = Math.max(0, syncInterval.getSelectedItemPosition());
        account.syncIntervalMinutes = SyncPolicy.INTERVAL_MINUTES[intervalIndex];
        account.notificationsEnabled = notificationsEnabled.isChecked();
        account.deleteFromServer = deleteFromServer.isChecked();
        account.syncReadState = syncReadState.isChecked();
        account.syncDraftsToServer = syncDraftsToServer.isChecked();
        account.signatureEnabled = signatureEnabled.isChecked();
        String plainSignature = signature.getText().toString().trim();
        account.signatureHtml = TextUtils.htmlEncode(plainSignature).replace("\n", "<br>");
        return account;
    }

    private void testAndSave() {
        AccountConfig account = read();
        ProviderPreset.Definition definition = ProviderPreset.find(account.provider);
        if (definition.oauthRequired) {
            status.setTextColor(Ui.error(this));
            status.setText("Outlook.com requires OAuth2/Modern Auth. The T1Xperts Microsoft app registration and redirect URI must be configured before Outlook sign-in can be enabled.");
            return;
        }
        if (!account.isUsable()) {
            status.setTextColor(Ui.error(this));
            status.setText("Please complete all required account and server fields.");
            return;
        }
        Ui.setEnabled(save, false, "Test IMAP + SMTP and Save", "Testing secure connections…");
        status.setTextColor(Ui.muted(this));
        status.setText("Authenticating securely to incoming and outgoing servers…");
        executor.execute(() -> {
            try {
                MailRepository.testConnections(account);
                store.save(account);
                NotificationScheduler.update(this, account);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connected — IMAP and SMTP authentication successful", Toast.LENGTH_SHORT).show();
                    String destination = store.loadAll().size() > 1
                            ? MailboxScope.ALL_ACCOUNTS : account.id;
                    store.setSelectedId(destination);
                    Intent intent = new Intent(this, MailboxActivity.class);
                    intent.putExtra("account_id", destination);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    Ui.setEnabled(save, true, "Test IMAP + SMTP and Save", "");
                    status.setTextColor(Ui.error(this));
                    status.setText("Authentication failed: " + MailRepository.safe(error));
                });
            }
        });
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
