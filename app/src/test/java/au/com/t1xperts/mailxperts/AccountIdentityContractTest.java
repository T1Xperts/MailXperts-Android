package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class AccountIdentityContractTest {
    @Test public void mailboxUsesAccountIdentityFormatter() throws Exception {
        String source = readProjectFile(
                "src/main/java/au/com/t1xperts/mailxperts/MailboxActivity.java",
                "app/src/main/java/au/com/t1xperts/mailxperts/MailboxActivity.java");
        assertTrue(source.contains("AccountIdentity.subtitle(account)"));
        assertTrue(source.contains("AccountIdentity.compact(account)"));
        assertTrue(source.contains("AccountIdentity.dropdown(account)"));
        assertTrue(source.contains("AccountChoice.account(configured)"));
    }

    private static String readProjectFile(String modulePath, String rootPath) throws Exception {
        Path path = Paths.get(modulePath);
        if (!Files.exists(path)) path = Paths.get(rootPath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
