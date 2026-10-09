package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Explicit provider configuration and synchronisation UI for MX-QA-027. */
public class CloudContactsActivity extends Activity {
    private static final int GOOGLE_CONTACTS_REQUEST = 6411;

    private CloudContactStore store;
    private TextView googleStatus;
    private TextView iCloudStatus;
    private CloudContactSyncMode pendingGoogleMode = CloudContactSyncMode.DISCONNECTED;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        store = new CloudContactStore(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.vertical(this);
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        android.widget.Button back = Ui.compactButton(this, "‹");
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, "Cloud Contacts");
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        TextView privacy = Ui.text(this,
                "Cloud contact access is optional. MailXperts pulls contacts only after you connect a provider. "
                        + "It never uploads learned contacts automatically: only contacts you explicitly select for cloud sync can be pushed.");
        privacy.setTextColor(Ui.muted(this));
        root.addView(privacy);

        LinearLayout google = Ui.card(this);
        google.addView(Ui.label(this, "GOOGLE CONTACTS"));
        googleStatus = Ui.text(this, "");
        google.addView(googleStatus);
        google.addView(Ui.secondaryButton(this, "Connect / change Google Contacts mode", v -> chooseGoogleMode()));
        google.addView(Ui.secondaryButton(this, "Sync Google Contacts now", v -> runGoogleSync()));
        google.addView(Ui.secondaryButton(this, "Disconnect Google Contacts", v -> disconnectGoogle()));
        root.addView(google);

        LinearLayout icloud = Ui.card(this);
        icloud.addView(Ui.label(this, "APPLE iCLOUD / CARDDAV"));
        iCloudStatus = Ui.text(this, "");
        icloud.addView(iCloudStatus);
        icloud.addView(Ui.secondaryButton(this, "Connect / change iCloud CardDAV", v -> chooseICloudMode()));
        icloud.addView(Ui.secondaryButton(this, "Sync iCloud / CardDAV now", v -> runICloudSync()));
        icloud.addView(Ui.secondaryButton(this, "Disconnect iCloud / CardDAV", v -> disconnectICloud()));
        root.addView(icloud);

        TextView note = Ui.text(this,
                "Read only imports provider contacts into the local Smart Contacts index. "
                        + "Selected push and two-way modes can write only contacts you mark for that provider from Smart Contacts. "
                        + "iCloud uses an app-specific password stored with Android Keystore encryption.");
        note.setTextColor(Ui.muted(this));
        root.addView(note);

        Ui.setContentView(this, scroll);
        refreshStatus();
    }

    private void chooseGoogleMode() {
        String[] labels = {"Read only", "Selected contacts → Google", "Two-way for selected contacts"};
        CloudContactSyncMode[] modes = {
                CloudContactSyncMode.READ_ONLY,
                CloudContactSyncMode.SELECTED_PUSH,
                CloudContactSyncMode.TWO_WAY
        };
        new AlertDialog.Builder(this)
                .setTitle("Google Contacts sync mode")
                .setItems(labels, (dialog, which) -> {
                    pendingGoogleMode = modes[which];
                    boolean write = pendingGoogleMode.canPushSelected();
                    GoogleContactsOAuthManager.begin(this, GOOGLE_CONTACTS_REQUEST, write,
                            new GoogleContactsOAuthManager.Callback() {
                                @Override public void onAuthorized(String bearer) {
                                    store.setMode(CloudContactStore.GOOGLE, pendingGoogleMode);
                                    refreshStatus();
                                    runGoogleSync();
                                }

                                @Override public void onError(String message) {
                                    toast(message);
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != GOOGLE_CONTACTS_REQUEST) return;
        if (resultCode != RESULT_OK || data == null) {
            toast("Google Contacts connection was cancelled.");
            return;
        }
        GoogleContactsOAuthManager.finish(this, data, pendingGoogleMode.canPushSelected(),
                new GoogleContactsOAuthManager.Callback() {
                    @Override public void onAuthorized(String bearer) {
                        store.setMode(CloudContactStore.GOOGLE, pendingGoogleMode);
                        refreshStatus();
                        runGoogleSync();
                    }

                    @Override public void onError(String message) {
                        toast(message);
                    }
                });
    }

    private void chooseICloudMode() {
        String[] labels = {"Read only", "Selected contacts → iCloud", "Two-way for selected contacts"};
        CloudContactSyncMode[] modes = {
                CloudContactSyncMode.READ_ONLY,
                CloudContactSyncMode.SELECTED_PUSH,
                CloudContactSyncMode.TWO_WAY
        };
        new AlertDialog.Builder(this)
                .setTitle("iCloud / CardDAV sync mode")
                .setItems(labels, (dialog, which) -> showICloudCredentials(modes[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showICloudCredentials(CloudContactSyncMode mode) {
        ContactSecretVault.CardDavCredential existing = new ContactSecretVault(this).loadICloud();
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = Ui.dp(this, 20);
        form.setPadding(padding, padding, padding, 0);

        EditText username = Ui.edit(this, "Apple ID / CardDAV username");
        username.setText(existing.username);
        username.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        form.addView(username);

        EditText password = Ui.edit(this, "App-specific password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(password);

        EditText endpoint = Ui.edit(this, "CardDAV endpoint");
        endpoint.setText(existing.endpoint.isEmpty() ? "https://contacts.icloud.com/" : existing.endpoint);
        form.addView(endpoint);

        new AlertDialog.Builder(this)
                .setTitle("Connect iCloud / CardDAV")
                .setMessage("For iCloud, use an Apple app-specific password, not your normal account password.")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Connect", (dialog, which) -> {
                    try {
                        new ContactSecretVault(this).saveICloud(
                                username.getText().toString(),
                                password.getText().toString(),
                                endpoint.getText().toString());
                        store.setMode(CloudContactStore.ICLOUD, mode);
                        store.setCollectionUrl(CloudContactStore.ICLOUD, "");
                        refreshStatus();
                        runICloudSync();
                    } catch (Exception error) {
                        toast("Could not save CardDAV settings: " + safe(error));
                    }
                })
                .show();
    }

    private void runGoogleSync() {
        if (!store.mode(CloudContactStore.GOOGLE).canRead()) {
            toast("Connect Google Contacts first.");
            return;
        }
        googleStatus.setText("Google Contacts: synchronising…");
        new Thread(() -> {
            try {
                CloudContactSyncManager.Result result = new CloudContactSyncManager(this).syncGoogle();
                runOnUiThread(() -> {
                    refreshStatus();
                    toast(result.pulled + " pulled • " + result.pushed + " pushed");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    refreshStatus();
                    toast("Google Contacts sync needs attention: " + safe(error));
                });
            }
        }, "mx-google-contacts-sync").start();
    }

    private void runICloudSync() {
        if (!store.mode(CloudContactStore.ICLOUD).canRead()) {
            toast("Connect iCloud / CardDAV first.");
            return;
        }
        iCloudStatus.setText("iCloud / CardDAV: synchronising…");
        new Thread(() -> {
            try {
                CloudContactSyncManager.Result result = new CloudContactSyncManager(this).syncICloud();
                runOnUiThread(() -> {
                    refreshStatus();
                    toast(result.pulled + " pulled • " + result.pushed + " pushed");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    refreshStatus();
                    toast("iCloud / CardDAV sync needs attention: " + safe(error));
                });
            }
        }, "mx-carddav-sync").start();
    }

    private void disconnectGoogle() {
        store.clearProvider(CloudContactStore.GOOGLE);
        refreshStatus();
        toast("Google Contacts disconnected. Local Smart Contacts were kept.");
    }

    private void disconnectICloud() {
        store.clearProvider(CloudContactStore.ICLOUD);
        new ContactSecretVault(this).clearICloud();
        refreshStatus();
        toast("iCloud / CardDAV disconnected. Local Smart Contacts were kept.");
    }

    private void refreshStatus() {
        CloudContactSyncMode googleMode = store.mode(CloudContactStore.GOOGLE);
        CloudContactSyncMode iCloudMode = store.mode(CloudContactStore.ICLOUD);
        googleStatus.setText("Mode: " + friendly(googleMode)
                + " • " + store.mappings(CloudContactStore.GOOGLE).size() + " mapped contacts"
                + " • " + store.promoted(CloudContactStore.GOOGLE).size() + " selected for push");
        iCloudStatus.setText("Mode: " + friendly(iCloudMode)
                + " • " + store.mappings(CloudContactStore.ICLOUD).size() + " mapped contacts"
                + " • " + store.promoted(CloudContactStore.ICLOUD).size() + " selected for push");
    }

    private static String friendly(CloudContactSyncMode mode) {
        if (mode == CloudContactSyncMode.READ_ONLY) return "Read only";
        if (mode == CloudContactSyncMode.SELECTED_PUSH) return "Selected push";
        if (mode == CloudContactSyncMode.TWO_WAY) return "Two-way selected";
        return "Disconnected";
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static String safe(Throwable error) {
        if (error == null) return "Unknown error";
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }
}
