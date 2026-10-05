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
                @Override public void onError(String message) { showProviderError(message); }
            });
            return;
        }
        if (ProviderPreset.OUTLOOK.equals(account.provider)) {
            MicrosoftOAuthManager.begin(this, MICROSOFT_AUTHORIZE,
                    new MicrosoftOAuthManager.Callback() {
                        @Override public void onAuthorized(OAuthCredential credential) {
                            persistAndValidate(credential, "");
                        }
                        @Override public void onError(String message) { showProviderError(message); }
                    });
            return;
        }
        showFriendlyError("This provider does not use OAuth in MailXperts.");
    }

    private void handleGoogleAuthorized(String email, String accessToken, long expiresAtMillis) {
        persistAndValidate(new OAuthCredential(accessToken, "", expiresAtMillis), email);
    }

    /**
     * Validates the candidate OAuth account before committing account metadata. The token must be
     * temporarily available to MailAuth during IMAP/SMTP validation, but any previous token is
     * restored if validation fails so the account cannot be left half-migrated.
     */
    private void persistAndValidate(OAuthCredential credential, String authorizedEmail) {
        final AccountConfig candidate = copyAccount(account);
        final String authorized = trim(authorizedEmail);
        final String configuredEmail = trim(candidate.email);

        if (ProviderPreset.GMAIL.equals(candidate.provider)) {
            if (!authorized.isEmpty()) {
                if (!configuredEmail.isEmpty()
                        && !configuredEmail.equalsIgnoreCase(authorized)) {
                    showFriendlyError("Google account mismatch. You authorized " + authorized
                            + ", but this MailXperts account is configured as " + configuredEmail
                            + ". Choose the same Google account or add it as a separate mailbox.");
                    return;
                }
                candidate.email = authorized;
                candidate.username = authorized;
            } else if (configuredEmail.isEmpty()) {
                showFriendlyError("Google sign-in completed, but MailXperts could not confirm the "
                        + "authorized Gmail address. Return to account settings, enter the Gmail "
                        + "address, then use Continue with Google again.");
                return;
            } else {
                // Google Identity can occasionally omit the account email from an authorization
                // result. In that case validate the token against the explicitly configured Gmail
                // identity rather than silently switching to another account.
                candidate.email = configuredEmail;
                candidate.username = configuredEmail;
            }
        } else {
            if (!authorized.isEmpty()) {
                candidate.email = authorized;
                candidate.username = authorized;
            } else if (candidate.username == null || candidate.username.trim().isEmpty()) {
                candidate.username = candidate.email == null ? "" : candidate.email.trim();
            }
        }

        candidate.authType = AuthType.OAUTH2;
        candidate.password = "";

        final CredentialVault vault = new CredentialVault(getApplicationContext());
        final OAuthCredential previousCredential = vault.load(candidate.id);

        executor.execute(() -> {
            try {
                // MailRepository.testConnections() obtains OAuth credentials through MailAuth,
                // therefore stage the new token in the vault before testing. Account metadata is
                // deliberately not saved until both IMAP and SMTP validation succeed.
                vault.save(candidate.id, credential);
                MailRepository.testConnections(candidate);
                store.save(candidate);
                account = candidate;

                boolean backgroundReady;
                try {
                    backgroundReady = NotificationScheduler.update(
                            getApplicationContext(), candidate);
                } catch (Exception schedulingError) {
                    backgroundReady = false;
                }

                final boolean finalBackgroundReady = backgroundReady;
                runOnUiThread(() -> {
                    status.setTextColor(Ui.teal(this));
                    status.setText(finalBackgroundReady
                            ? "Connected. IMAP and SMTP OAuth authentication succeeded."
                            : "Connected. Manual mail access works; background scheduling needs attention.");
                    Intent result = new Intent();
                    result.putExtra(EXTRA_CONNECTED, true);
                    setResult(RESULT_OK, result);
                    finish();
                });
            } catch (Exception error) {
                restoreCredential(vault, candidate.id, previousCredential);
                showConnectionError(candidate, error);
            }
        });
    }

    private void restoreCredential(
            CredentialVault vault, String accountId, OAuthCredential previousCredential) {
        try {
            if (previousCredential != null
                    && (previousCredential.hasAccessToken() || previousCredential.hasRefreshToken())) {
                vault.save(accountId, previousCredential);
            } else {
                vault.clear(accountId);
            }
        } catch (Exception ignored) {
            // The primary failure remains the connection error. Do not replace it with a rollback
            // storage exception, but do leave account metadata untouched because it was not saved.
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            showFriendlyError("Authorization was cancelled or did not complete.");
            return;
        }
        if (requestCode == GOOGLE_AUTHORIZE) {
            GoogleOAuthManager.finish(this, data, new GoogleOAuthManager.Callback() {
                @Override public void onAuthorized(
                        String email, String accessToken, long expiresAtMillis) {
                    handleGoogleAuthorized(email, accessToken, expiresAtMillis);
                }
                @Override public void onError(String message) { showProviderError(message); }
            });
        } else if (requestCode == MICROSOFT_AUTHORIZE) {
            MicrosoftOAuthManager.finish(this, data, new MicrosoftOAuthManager.Callback() {
                @Override public void onAuthorized(OAuthCredential credential) {
                    persistAndValidate(credential, "");
                }
                @Override public void onError(String message) { showProviderError(message); }
            });
        }
    }

    /** Maps a raw provider/SDK message exactly once. */
    private void showProviderError(String rawMessage) {
        showFriendlyError(ProviderErrorMessage.forProvider(
                account == null ? "" : account.provider, rawMessage));
    }

    /** Maps a mail-transport exception exactly once. */
    private void showConnectionError(AccountConfig attemptedAccount, Throwable error) {
        showFriendlyError(ProviderErrorMessage.forAccount(attemptedAccount, error));
    }

    /** Displays an already safe user-facing message without remapping it. */
    private void showFriendlyError(String message) {
        runOnUiThread(() -> {
            Ui.setEnabled(connect, true, connectLabel(), "");
            status.setTextColor(Ui.error(this));
            status.setText(message == null || message.trim().isEmpty()
                    ? "Authentication failed. Please try again."
                    : message.trim());
        });
    }

    private static AccountConfig copyAccount(AccountConfig source) {
        AccountConfig copy = new AccountConfig();
        copy.id = source.id;
        copy.provider = source.provider;
        copy.authType = source.authType;
        copy.label = source.label;
        copy.email = source.email;
        copy.username = source.username;
        copy.imapHost = source.imapHost;
        copy.imapPort = source.imapPort;
        copy.smtpHost = source.smtpHost;
        copy.smtpPort = source.smtpPort;
        copy.smtpSecurity = source.smtpSecurity;
        copy.password = source.password;
        copy.syncEnabled = source.syncEnabled;
        copy.syncIntervalMinutes = source.syncIntervalMinutes;
        copy.notificationsEnabled = source.notificationsEnabled;
        copy.deleteFromServer = source.deleteFromServer;
        copy.syncReadState = source.syncReadState;
        copy.syncDraftsToServer = source.syncDraftsToServer;
        copy.signatureEnabled = source.signatureEnabled;
        copy.signatureHtml = source.signatureHtml;
        return copy;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
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
