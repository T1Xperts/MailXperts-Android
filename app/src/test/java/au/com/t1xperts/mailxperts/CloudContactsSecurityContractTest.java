package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Security/privacy regression contracts for direct provider contact sync. */
public class CloudContactsSecurityContractTest {
    @Test public void cloudPushIsExplicitlySelectedRatherThanAutomatic() throws Exception {
        String manager = src("CloudContactSyncManager.java");
        String store = src("CloudContactStore.java");
        assertTrue(manager.contains("store.promoted"));
        assertTrue(manager.contains("mode.canPushSelected()"));
        assertTrue(store.contains("promote(String provider, String email)"));
        assertFalse(manager.contains("for (RecipientDirectory.Entry local : history.entries()) {\n            CloudContactRecord result"));
    }

    @Test public void cardDavSecretsUseAndroidKeystoreAndHttps() throws Exception {
        String vault = src("ContactSecretVault.java");
        assertTrue(vault.contains("AndroidKeyStore"));
        assertTrue(vault.contains("AES/GCM/NoPadding"));
        assertTrue(vault.contains("https://contacts.icloud.com/"));
        assertTrue(vault.contains("CardDAV endpoint must use HTTPS"));
    }

    @Test public void cloudActivityIsNotExported() throws Exception {
        String manifest = project("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml");
        assertTrue(manifest.contains("android:name=\".CloudContactsActivity\" android:exported=\"false\""));
    }

    private static String src(String file) throws Exception {
        return project("src/main/java/au/com/t1xperts/mailxperts/" + file,
                "app/src/main/java/au/com/t1xperts/mailxperts/" + file);
    }

    private static String project(String first, String second) throws Exception {
        Path path = Paths.get(first);
        if (!Files.exists(path)) path = Paths.get(second);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
