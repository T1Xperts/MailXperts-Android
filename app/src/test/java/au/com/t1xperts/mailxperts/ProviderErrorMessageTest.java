package au.com.t1xperts.mailxperts;

import org.junit.Test;

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
}
