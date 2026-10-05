package au.com.t1xperts.mailxperts;

import java.util.Locale;

/** Converts provider/server failures into safe, actionable user-facing explanations. */
final class ProviderErrorMessage {
    private ProviderErrorMessage() {}

    static String forAccount(AccountConfig account, Throwable error) {
        String provider = account == null ? "" : account.provider;
        boolean oauth = account != null && AuthType.isOAuth(account.authType);
        String raw = error == null ? "" : error.getMessage();
        return describe(provider, oauth, raw);
    }

    static String forProvider(String provider, String raw) {
        return describe(provider, true, raw);
    }

    private static String describe(String provider, boolean oauth, String raw) {
        String message = raw == null ? "" : raw.trim();
        String lower = message.toLowerCase(Locale.ROOT);
        boolean gmail = ProviderPreset.GMAIL.equals(provider);
        boolean outlook = ProviderPreset.OUTLOOK.equals(provider);

        if (lower.contains("cancelled") || lower.contains("canceled")) {
            return "Authorization was cancelled. No account changes were made.";
        }
        if (lower.contains("does not use oauth")) return "This provider does not use OAuth in MailXperts.";
        if (lower.contains("not configured") && outlook) {
            return "Microsoft sign-in is not configured for this build yet. Add the registered Microsoft client ID and redirect URI, then rebuild MailXperts.";
        }
        if (lower.contains("invalid_grant") || lower.contains("token has been expired")
                || lower.contains("token expired") || lower.contains("revoked")) {
            if (gmail) return "Google authorization has expired or was revoked. Reconnect this account with Continue with Google.";
            if (outlook) return "Microsoft authorization has expired or was revoked. Reconnect this account with Continue with Microsoft.";
            return "The provider authorization expired or was revoked. Reconnect this account and try again.";
        }
        if (lower.contains("authenticationfailed") || lower.contains("authentication failed")
                || lower.contains("invalid credentials") || lower.contains("username and password not accepted")
                || lower.contains("web login required") || lower.contains("535") || lower.contains("534")) {
            if (gmail && oauth) {
                return "Google rejected the mailbox sign-in. Reconnect with Continue with Google and approve Gmail access.";
            }
            if (gmail) {
                return "Google rejected the App Password. Use Continue with Google (recommended), or create a fresh 16-character App Password after enabling 2-Step Verification.";
            }
            if (outlook) {
                return "Microsoft rejected the mailbox sign-in. Reconnect with Continue with Microsoft.";
            }
            return "The mail provider rejected the sign-in. Check the account credentials or reconnect the provider.";
        }
        if (lower.contains("unknownhost") || lower.contains("unknown host")
                || lower.contains("connectexception") || lower.contains("connection refused")
                || lower.contains("timed out") || lower.contains("timeout")
                || lower.contains("no route to host")) {
            return "Could not reach the mail server. Check your internet connection and the IMAP/SMTP server address, then try again.";
        }
        if (lower.contains("sslhandshake") || lower.contains("certificate")
                || lower.contains("pkix") || lower.contains("handshake_failure")) {
            return "MailXperts could not verify the server's secure TLS connection. Check the server hostname and your device date/time.";
        }
        if (lower.contains("imap") && lower.contains("disabled")) {
            return "IMAP access is disabled for this mailbox or blocked by the provider policy. Enable IMAP access or ask the mailbox administrator.";
        }
        if (gmail && lower.contains("app password")) {
            return "Use Continue with Google (recommended). If you use the fallback App Password method, create a fresh 16-character Google App Password after enabling 2-Step Verification.";
        }
        if (message.isEmpty()) return "Mail account connection failed. Check the account settings and try again.";
        return "Mail account connection failed. Check the account settings or reconnect the provider, then try again.";
    }
}
