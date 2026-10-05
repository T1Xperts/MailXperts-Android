package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards the v1.6.0-beta.5 OAuth login regression found during device QA. */
public class OAuthLoginRegressionContractTest {
    @Test public void connectionValidationPrecedesAccountPersistence() throws Exception {
        String source = read("src/main/java/au/com/t1xperts/mailxperts/OAuthConnectActivity.java",
                "app/src/main/java/au/com/t1xperts/mailxperts/OAuthConnectActivity.java");
        int validate = source.indexOf("MailRepository.testConnections(candidate);");
        int persist = source.indexOf("store.save(candidate);");
        assertTrue("OAuth candidate must be validated before account metadata is persisted",
                validate >= 0 && persist > validate);
        assertTrue(source.contains("restoreCredential(vault, candidate.id, previousCredential);"));
    }

    @Test public void alreadyFriendlyErrorsAreNotMappedAgain() throws Exception {
        String source = read("src/main/java/au/com/t1xperts/mailxperts/OAuthConnectActivity.java",
                "app/src/main/java/au/com/t1xperts/mailxperts/OAuthConnectActivity.java");
        assertTrue(source.contains("private void showFriendlyError(String message)"));
        assertTrue(source.contains("showFriendlyError(ProviderErrorMessage.forAccount(attemptedAccount, error));"));
        assertTrue(source.contains("showFriendlyError(ProviderErrorMessage.forProvider("));
        assertFalse("Friendly display helper must not invoke ProviderErrorMessage again",
                source.contains("status.setText(ProviderErrorMessage.forProvider"));
    }

    @Test public void googleAuthorizationRequestsMailboxAndEmailIdentityScopes() throws Exception {
        String source = read("src/main/java/au/com/t1xperts/mailxperts/GoogleOAuthManager.java",
                "app/src/main/java/au/com/t1xperts/mailxperts/GoogleOAuthManager.java");
        assertTrue(source.contains("https://mail.google.com/"));
        assertTrue(source.contains("https://www.googleapis.com/auth/userinfo.email"));
        assertTrue(source.contains("new Scope(MAIL_SCOPE)"));
        assertTrue(source.contains("new Scope(EMAIL_SCOPE)"));
    }

    @Test public void googleAccountMismatchIsBlocked() throws Exception {
        String source = read("src/main/java/au/com/t1xperts/mailxperts/OAuthConnectActivity.java",
                "app/src/main/java/au/com/t1xperts/mailxperts/OAuthConnectActivity.java");
        assertTrue(source.contains("Google account mismatch."));
        assertTrue(source.contains("!configuredEmail.equalsIgnoreCase(authorized)"));
    }

    private static String read(String modulePath, String rootPath) throws Exception {
        Path path = Paths.get(modulePath);
        if (!Files.exists(path)) path = Paths.get(rootPath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
