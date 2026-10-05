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

        // Keep this mapper idempotent. Several UI paths may receive text that has already been
        // converted into a safe user-facing explanation; a second pass must not erase detail.
        if (isFriendlyMessage(lower)) return message;

        if (lower.contains("cancelled") || lower.contains("canceled")) {
            return "Authorization was cancelled. No account changes were made.";
        }
        if (lower.contains("does not use oauth")) return "This provider does not use OAuth in MailXperts.";
        if (lower.contains("not configured") && outlook) {
            return "Microsoft sign-in is not configured for this build yet. Add the registered Microsoft client ID and redirect URI, then rebuild MailXperts.";
        }
        if (gmail && (lower.contains("developer_error")
                || lower.contains("api_exception: 10")
                || lower.matches(".*(^|[^0-9])10:.*"))) {
            return "Google sign-in configuration does not match this MailXperts build. Install the official production-signed APK and verify the Android OAuth package/signing certificate registration.";
        }
        if (lower.contains("network_error") || lower.contains("api_exception: 7")) {
            return "Google sign-in could not reach Google services. Check the internet connection and Google Play services, then try again.";
        }
        if (lower.contains("consent_required")) {
            if (gmail) return "Google needs Gmail permission again. Tap Continue with Google and approve Gmail access.";
            return "The provider needs authorization again. Reconnect the account and approve access.";
        }
        if (lower.contains("did not return an oauth access token")) {
            if (gmail) return "Google sign-in completed without a usable Gmail access token. Tap Continue with Google and approve Gmail access again.";
            return "The provider did not return a usable OAuth access token. Reconnect the account and try again.";
        }
        if (lower.contains("invalid_grant") || lower.contains("token has been expired")
                || lower.contains("token expired") || lower.contains("revoked")) {
            if (gmail) return "Google authorization has expired or was revoked. Reconnect this account with Continue with Google.";
            if (outlook) return "Microsoft authorization has expired or was revoked. Reconnect this account with Continue with Microsoft.";
            return "The provider authorization expired or was revoked. Reconnect this account and try again.";
        }
        if (lower.contains("authenticationfailed") || lower.contains("authentication failed")
                || lower.contains("oauth authentication failed")
                || lower.contains("invalid credentials") || lower.contains("username and password not accepted")
                || lower.contains("web login required") || lower.contains("535") || lower.contains("534")) {
            if (gmail && oauth) {
                if (lower.contains("smtp")) {
                    return "Google authorization completed, but Gmail rejected SMTP OAuth mail sending. Reconnect with Continue with Google and approve Gmail access.";
                }
                if (lower.contains("imap")) {
                    return "Google authorization completed, but Gmail rejected IMAP OAuth mailbox access. Reconnect with Continue with Google and approve Gmail access.";
                }
                return "Google rejected the mailbox OAuth sign-in. Reconnect with Continue with Google and approve Gmail access.";
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

    private static boolean isFriendlyMessage(String lower) {
        return lower.startsWith("authorization was cancelled")
                || lower.startsWith("this provider does not use oauth")
                || lower.startsWith("microsoft sign-in is not configured")
                || lower.startsWith("google sign-in configuration does not match")
                || lower.startsWith("google sign-in could not reach")
                || lower.startsWith("google needs gmail permission again")
                || lower.startsWith("google sign-in completed without")
                || lower.startsWith("google authorization has expired")
                || lower.startsWith("microsoft authorization has expired")
                || lower.startsWith("the provider authorization expired")
                || lower.startsWith("google authorization completed")
                || lower.startsWith("google rejected the mailbox")
                || lower.startsWith("google rejected the app password")
                || lower.startsWith("microsoft rejected the mailbox")
                || lower.startsWith("the mail provider rejected")
                || lower.startsWith("could not reach the mail server")
                || lower.startsWith("mailxperts could not verify")
                || lower.startsWith("imap access is disabled")
                || lower.startsWith("use continue with google")
                || lower.startsWith("google account mismatch")
                || lower.startsWith("google sign-in completed, but mailxperts could not confirm")
                || lower.startsWith("mail account connection failed");
    }
}
