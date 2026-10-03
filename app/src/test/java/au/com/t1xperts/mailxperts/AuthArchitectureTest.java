package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthArchitectureTest {
    @Test public void oauthAccountDoesNotRequirePassword() {
        AccountConfig account = new AccountConfig();
        account.authType = AuthType.OAUTH2;
        account.email = "user@gmail.com";
        account.username = "user@gmail.com";
        account.imapHost = "imap.gmail.com";
        account.imapPort = 993;
        account.smtpHost = "smtp.gmail.com";
        account.smtpPort = 465;
        account.password = "";

        assertTrue(account.isUsable());
    }

    @Test public void passwordAccountStillRequiresSecret() {
        AccountConfig account = new AccountConfig();
        account.authType = AuthType.PASSWORD;
        account.email = "user@example.com";
        account.username = "user@example.com";
        account.password = "";

        assertFalse(account.isUsable());
        account.password = "secret";
        assertTrue(account.isUsable());
    }

    @Test public void gmailPrefersOauthButKeepsAppPasswordFallback() {
        ProviderPreset.Definition gmail = ProviderPreset.find(ProviderPreset.GMAIL);
        assertEquals(AuthType.OAUTH2, gmail.preferredAuthType);
        assertTrue(gmail.supportsOAuth2);
        assertTrue(gmail.supportsAppPassword);
        assertFalse(gmail.supportsPassword);
    }

    @Test public void outlookRequiresModernOauth() {
        ProviderPreset.Definition outlook = ProviderPreset.find(ProviderPreset.OUTLOOK);
        assertEquals(AuthType.OAUTH2, outlook.preferredAuthType);
        assertTrue(outlook.supportsOAuth2);
        assertFalse(outlook.supportsAppPassword);
        assertFalse(outlook.supportsPassword);
    }

    @Test public void hostedAndCustomMailKeepStandardsBasedFallbacks() {
        ProviderPreset.Definition hosted = ProviderPreset.find(ProviderPreset.T1XPERTS);
        ProviderPreset.Definition custom = ProviderPreset.find(ProviderPreset.CUSTOM);

        assertEquals(AuthType.PASSWORD, hosted.preferredAuthType);
        assertTrue(hosted.supportsPassword);
        assertTrue(custom.supportsPassword);
        assertTrue(custom.supportsAppPassword);
    }

    @Test public void oauthCredentialHonoursExpiryMargin() {
        OAuthCredential live = new OAuthCredential(
                "access", "refresh", System.currentTimeMillis() + 5L * 60L * 1000L);
        OAuthCredential expiring = new OAuthCredential(
                "access", "refresh", System.currentTimeMillis() + 10_000L);

        assertTrue(live.isUsableNow());
        assertFalse(expiring.isUsableNow());
    }
}
