package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GmailOAuthTransportContractTest {
    @Test public void gmailScopeValidationRequiresMailScope() {
        assertTrue(GoogleOAuthManager.containsMailScope(
                Collections.singletonList(GoogleOAuthManager.MAIL_SCOPE)));
        assertFalse(GoogleOAuthManager.containsMailScope(
                Collections.singletonList("https://www.googleapis.com/auth/userinfo.email")));
        assertFalse(GoogleOAuthManager.containsMailScope(null));
    }

    @Test public void googleAuthorizationRequestsOnlyMailboxScope() throws Exception {
        String source = src("GoogleOAuthManager.java");
        assertTrue(source.contains("Collections.singletonList(new Scope(MAIL_SCOPE))"));
        assertTrue(source.contains("setOptOutIncludingGrantedScopes(true)"));
        assertTrue(source.contains("getGrantedScopes()"));
        assertFalse(source.contains("EMAIL_SCOPE"));
    }

    @Test public void javaMailExplicitlyEnablesXoauth2AndDisablesPasswordFallback() throws Exception {
        String source = src("MailAuth.java");
        assertTrue(source.contains("mail.imaps.auth.mechanisms\", \"XOAUTH2"));
        assertTrue(source.contains("mail.imaps.auth.xoauth2.disable\", \"false"));
        assertTrue(source.contains("mail.imaps.auth.login.disable\", \"true"));
        assertTrue(source.contains("mail.imaps.auth.plain.disable\", \"true"));
        assertTrue(source.contains("mail.smtp.auth.mechanisms\", \"XOAUTH2"));
        assertTrue(source.contains("mail.smtp.auth.xoauth2.disable\", \"false"));
    }

    private static String src(String file) throws Exception {
        Path path = Paths.get("src/main/java/au/com/t1xperts/mailxperts/" + file);
        if (!Files.exists(path)) {
            path = Paths.get("app/src/main/java/au/com/t1xperts/mailxperts/" + file);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
