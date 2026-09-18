package au.com.t1xperts.mailxperts;

import java.util.UUID;

final class AccountConfig {
    static final String SMTP_SSL = "SSL_TLS";
    static final String SMTP_STARTTLS = "STARTTLS";

    String id = UUID.randomUUID().toString();
    String provider = ProviderPreset.T1XPERTS;
    // Account identity must never be pre-populated for a newly-created account.
    // Existing accounts are restored only from SecureStore when an explicit account ID is edited.
    String label = "";
    String email = "";
    String username = "";
    String imapHost = "t1xperts.com.au";
    int imapPort = 993;
    String smtpHost = "t1xperts.com.au";
    int smtpPort = 465;
    String smtpSecurity = SMTP_SSL;
    String password = "";
    boolean syncEnabled = true;
    int syncIntervalMinutes = SyncPolicy.DEFAULT_INTERVAL_MINUTES;
    boolean notificationsEnabled = true;
    boolean deleteFromServer = false;
    boolean syncReadState = true;
    boolean syncDraftsToServer = false;
    boolean signatureEnabled = false;
    String signatureHtml = "";

    boolean isUsable() {
        return id != null && !id.trim().isEmpty()
                && email != null && !email.trim().isEmpty()
                && username != null && !username.trim().isEmpty()
                && imapHost != null && !imapHost.trim().isEmpty()
                && imapPort > 0
                && smtpHost != null && !smtpHost.trim().isEmpty()
                && smtpPort > 0
                && password != null && !password.isEmpty();
    }

    String displayName() {
        return label == null || label.trim().isEmpty() ? email : label.trim();
    }
}
