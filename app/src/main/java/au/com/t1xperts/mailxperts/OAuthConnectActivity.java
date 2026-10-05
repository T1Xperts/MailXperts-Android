package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Interactive provider authorization entry point used by account settings. */
public final class OAuthConnectActivity extends Activity {
    static final String EXTRA_ACCOUNT_ID = "account_id";
    static final String EXTRA_CONNECTED = "oauth_connected";

    private static final int GOOGLE_AUTHORIZE = 7301;
    private static final int MICROSOFT_AUTHORIZE = 7302;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SecureStore store;
    private AccountConfig account;
    private TextView status;
    private Button connect;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        store = new SecureStore(this);
        account = store.load(getIntent().getStringExtra(EXTRA_ACCOUNT_ID));

        LinearLayout root = Ui.vertical(this);
        root.addView(Ui.title(this, "Secure provider sign-in"));
        TextView provider = Ui.text(this, providerTitle());
        provider.setTextSize(18);
        root.addView(provider);

        TextView explanation = Ui.text(this, explanation());
        explanation.setTextColor(Ui.muted(this));
        root.addView(explanation);

        status = Ui.text(this, "");
        status.setTextColor(Ui.muted(this));
        root.addView(status);

        connect = Ui.button(this, connectLabel());
        connect.setOnClickListener(v -> begin());
        root.addView(connect);
        root.addView(Ui.secondaryButton(this, "Cancel", v -> finish()));
        Ui.setContentView(this, root);

        if (ProviderPreset.OUTLOOK.equals(account.provider)
                && !MicrosoftOAuthManager.isConfigured()) {
            status.setTextColor(Ui.error(this));
            status.setText("Microsoft OAuth client registration is not configured in this build yet. "
                    + "The app code is ready for MX_MICROSOFT_CLIENT_ID and MX_MICROSOFT_REDIRECT_URI.");
        }
    }

    private void begin() {
        Ui.setEnabled(connect, false, connectLabel(), "Opening secure sign-in…");
        status.setTextColor(Ui.muted(this));
        status.setText("MailXperts will never ask for your normal Google or Microsoft password.");

        if (ProviderPreset.GMAIL.equals(account.provider)) {
            GoogleOAuthManager.begin(this, GOOGLE_AUTHORIZE, new GoogleOAuthManager.Callback() {
                @Override public void onAuthorized(
                        String email, String accessToken, long expiresAtMillis) {
                    handleGoogleAuthorized(email, accessToken, expiresAtMillis);
                }
                @Override public void onError(String message) { showError(message); }
            });
            return;
        }
        if (ProviderPreset.OUTLOOK.equals(account.provider)) {
            MicrosoftOAuthManager.begin(this, MICROSOFT_AUTHORIZE,
                    new MicrosoftOAuthManager.Callback() {
                        @Override public void onAuthorized(OAuthCredential credential) {
                            persistAndValidate(credential, "");
                        }
                        @Override public void onError(String message) { showError(message); }
                    });
            return;
        }
        showError("This provider does not use OAuth in MailXperts.");
    }

    private void handleGoogleAuthorized(String email, String accessToken, long expiresAtMillis) {
        persistAndValidate(new OAuthCredential(accessToken, "", expiresAtMillis), email);
    }

    private void persistAndValidate(OAuthCredential credential, String authorizedEmail) {
        if (authorizedEmail != null && !authorizedEmail.trim().isEmpty()) {
            account.email = authorizedEmail.trim();
            account.username = authorizedEmail.trim();
        } else if (account.username == null || account.username.trim().isEmpty()) {
            account.username = account.email == null ? "" : account.email.trim();
        }
        account.authType = AuthType.OAUTH2;
        account.password = "";

        executor.execute(() -> {
            try {
                new CredentialVault(getApplicationContext()).save(account.id, credential);
                store.save(account);
                MailRepository.testConnections(account);
                boolean backgroundReady = NotificationScheduler.update(
                        getApplicationContext(), account);
                runOnUiThread(() -> {
                    status.setTextColor(Ui.teal(this));
                    status.setText(backgroundReady
                            ? "Connected. IMAP and SMTP OAuth authentication succeeded."
                            : "Connected. Manual mail access works; background scheduling needs attention.");
                    Intent result = new Intent();
                    result.putExtra(EXTRA_CONNECTED, true);
                    setResult(RESULT_OK, result);
                    finish();
                });
            } catch (Exception error) {
                showError(ProviderErrorMessage.forAccount(account, error));
            }
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            showError("Authorization was cancelled or did not complete.");
            return;
        }
        if (requestCode == GOOGLE_AUTHORIZE) {
            GoogleOAuthManager.finish(this, data, new GoogleOAuthManager.Callback() {
                @Override public void onAuthorized(
                        String email, String accessToken, long expiresAtMillis) {
                    handleGoogleAuthorized(email, accessToken, expiresAtMillis);
                }
                @Override public void onError(String message) { showError(message); }
            });
        } else if (requestCode == MICROSOFT_AUTHORIZE) {
            MicrosoftOAuthManager.finish(this, data, new MicrosoftOAuthManager.Callback() {
                @Override public void onAuthorized(OAuthCredential credential) {
                    persistAndValidate(credential, "");
                }
                @Override public void onError(String message) { showError(message); }
            });
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Ui.setEnabled(connect, true, connectLabel(), "");
            status.setTextColor(Ui.error(this));
            status.setText(ProviderErrorMessage.forProvider(
                    account == null ? "" : account.provider, message));
        });
    }

    private String providerTitle() {
        if (ProviderPreset.GMAIL.equals(account.provider)) return "Google Gmail / Workspace";
        if (ProviderPreset.OUTLOOK.equals(account.provider)) return "Microsoft Outlook / Microsoft 365";
        return account.displayName();
    }

    private String connectLabel() {
        if (ProviderPreset.GMAIL.equals(account.provider)) return "Continue with Google";
        if (ProviderPreset.OUTLOOK.equals(account.provider)) return "Continue with Microsoft";
        return "Connect";
    }

    private String explanation() {
        if (ProviderPreset.GMAIL.equals(account.provider)) {
            return "Google grants MailXperts an OAuth access token for Gmail. "
                    + "Your normal Google password is never collected or stored.";
        }
        if (ProviderPreset.OUTLOOK.equals(account.provider)) {
            return "Microsoft identity grants delegated IMAP and SMTP access. "
                    + "Your normal Microsoft password is never collected or stored.";
        }
        return "Secure provider authorization.";
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
