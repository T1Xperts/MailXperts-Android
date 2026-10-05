package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProviderErrorMessageTest {
    @Test public void gmailOAuthFailureIsActionableAndDoesNotExposeRawServerText() {
        AccountConfig account = new AccountConfig();
        account.provider = ProviderPreset.GMAIL;
        account.authType = AuthType.OAUTH2;
        String message = ProviderErrorMessage.forAccount(account,
                new Exception("AUTHENTICATIONFAILED 535 invalid credentials token=secret"));
        assertTrue(message.contains("Continue with Google"));
        assertFalse(message.contains("535"));
        assertFalse(message.contains("secret"));
    }

    @Test public void networkFailureIsExplainedWithoutExceptionName() {
        String message = ProviderErrorMessage.forProvider(
                ProviderPreset.GMAIL, "java.net.UnknownHostException: imap.gmail.com");
        assertTrue(message.contains("Could not reach the mail server"));
        assertFalse(message.contains("UnknownHostException"));
    }

    @Test public void alreadyFriendlyMessageSurvivesSecondMapping() {
        String friendly = "Google authorization completed, but Gmail rejected IMAP OAuth mailbox access. "
                + "Reconnect with Continue with Google and approve Gmail access.";
        assertEquals(friendly, ProviderErrorMessage.forProvider(ProviderPreset.GMAIL, friendly));
    }

    @Test public void gmailImapOAuthFailurePreservesStage() {
        AccountConfig account = new AccountConfig();
        account.provider = ProviderPreset.GMAIL;
        account.authType = AuthType.OAUTH2;
        String message = ProviderErrorMessage.forAccount(account,
                new Exception("IMAP OAuth authentication failed. Reconnect this Gmail account using Continue with Google."));
        assertTrue(message.contains("IMAP"));
        assertTrue(message.contains("Continue with Google"));
    }

    @Test public void gmailSmtpOAuthFailurePreservesStage() {
        AccountConfig account = new AccountConfig();
        account.provider = ProviderPreset.GMAIL;
        account.authType = AuthType.OAUTH2;
        String message = ProviderErrorMessage.forAccount(account,
                new Exception("SMTP OAuth authentication failed. Reconnect this Gmail account using Continue with Google."));
        assertTrue(message.contains("SMTP"));
        assertTrue(message.contains("Continue with Google"));
    }

    @Test public void googleDeveloperErrorExplainsProductionIdentityMismatch() {
        String message = ProviderErrorMessage.forProvider(
                ProviderPreset.GMAIL, "ApiException: 10: DEVELOPER_ERROR");
        assertTrue(message.contains("Google sign-in configuration"));
        assertTrue(message.contains("production-signed APK"));
        assertFalse(message.contains("DEVELOPER_ERROR"));
    }
}
