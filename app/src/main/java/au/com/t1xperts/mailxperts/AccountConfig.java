package au.com.t1xperts.mailxperts;

import java.util.UUID;

final class AccountConfig {
    static final String SMTP_SSL = "SSL_TLS";
    static final String SMTP_STARTTLS = "STARTTLS";

    String id = UUID.randomUUID().toString();
    String provider = ProviderPreset.T1XPERTS;
    String label = "T1Xperts Customer Care";
    String email = "customer.care@t1xperts.com.au";
    String username = "customer.care@t1xperts.com.au";
    String imapHost = "t1xperts.com.au";
    int imapPort = 993;
    String smtpHost = "t1xperts.com.au";
    int smtpPort = 465;
    String smtpSecurity = SMTP_SSL;
    String password = "";
    boolean syncEnabled = true;
    boolean notificationsEnabled = true;
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
