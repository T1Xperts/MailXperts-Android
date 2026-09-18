package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class MessageWebViewContractTest {
    @Test public void remoteImagesEnabledWhileDangerousWebCapabilitiesRemainDisabled() throws Exception {
        String source = readProjectFile(
                "src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java",
                "app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java");
        String manifest = readProjectFile("src/main/AndroidManifest.xml",
                "app/src/main/AndroidManifest.xml");

        assertTrue(source.contains("setJavaScriptEnabled(false)"));
        assertTrue(source.contains("setAllowFileAccess(false)"));
        assertTrue(source.contains("setAllowContentAccess(false)"));
        assertTrue(source.contains("setDomStorageEnabled(false)"));
        assertTrue(source.contains("setBlockNetworkLoads(false)"));
        assertTrue(source.contains("MIXED_CONTENT_COMPATIBILITY_MODE"));
        assertTrue(source.contains("ExternalLinkPolicy.isAllowedScheme"));
        assertTrue(manifest.contains("android:usesCleartextTraffic=\"true\""));
    }

    private static String readProjectFile(String modulePath, String rootPath) throws Exception {
        Path path = Paths.get(modulePath);
        if (!Files.exists(path)) path = Paths.get(rootPath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
