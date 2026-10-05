package au.com.t1xperts.mailxperts;

import java.util.Properties;

/** Resolves provider credentials and configures JavaMail SASL without exposing secrets. */
final class MailAuth {
    private MailAuth() {}

    static String secret(AccountConfig account) throws Exception {
        if (!AuthType.isOAuth(account.authType)) return account.password;

        CredentialVault vault = new CredentialVault(MailXpertsApplication.context());
        OAuthCredential cached = vault.load(account.id);
        if (cached.isUsableNow()) return cached.accessToken;

        OAuthCredential fresh;
        if (ProviderPreset.GMAIL.equals(account.provider)) {
            fresh = GoogleOAuthManager.acquireSilently(MailXpertsApplication.context());
        } else if (ProviderPreset.OUTLOOK.equals(account.provider)) {
            fresh = MicrosoftOAuthManager.refreshBlocking(
                    MailXpertsApplication.context(), cached);
        } else {
            throw new IllegalStateException(
                    "CONFIGURATION_ERROR: OAuth is not available for this provider.");
        }
        vault.save(account.id, fresh);
        return fresh.accessToken;
    }

    static void configureImap(Properties properties, AccountConfig account) {
        if (!AuthType.isOAuth(account.authType)) return;
        properties.put("mail.imaps.auth.mechanisms", "XOAUTH2");
        // JavaMail documents XOAUTH2 as disabled by default. auth.mechanisms should override that,
        // but set the switch explicitly as well so Android Mail cannot fall back to LOGIN/PLAIN.
        properties.put("mail.imaps.auth.xoauth2.disable", "false");
        properties.put("mail.imaps.auth.login.disable", "true");
        properties.put("mail.imaps.auth.plain.disable", "true");
    }

    static void configureSmtp(Properties properties, AccountConfig account) {
        if (!AuthType.isOAuth(account.authType)) return;
        properties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        properties.put("mail.smtp.auth.xoauth2.disable", "false");
        properties.put("mail.smtp.auth.login.disable", "true");
        properties.put("mail.smtp.auth.plain.disable", "true");
    }
}
