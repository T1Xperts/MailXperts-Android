package au.com.t1xperts.mailxperts;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ManifestContractTest {
    @Test public void backgroundNetworkConstraintHasRequiredPermission() throws Exception {
        String manifest = readProjectFile("src/main/AndroidManifest.xml",
                "app/src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android.permission.ACCESS_NETWORK_STATE"));
        assertTrue(manifest.contains(".MailSyncJobService"));
    }

    @Test public void richSignatureEditorIsRegisteredAndPrivate() throws Exception {
        String manifest = readProjectFile("src/main/AndroidManifest.xml",
                "app/src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\".SignatureEditorActivity\""));
        assertTrue(manifest.matches("(?s).*SignatureEditorActivity\"\\s+android:exported=\"false\".*"));
    }

    private static String readProjectFile(String modulePath, String rootPath) throws Exception {
        Path path = Paths.get(modulePath);
        if (!Files.exists(path)) path = Paths.get(rootPath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
