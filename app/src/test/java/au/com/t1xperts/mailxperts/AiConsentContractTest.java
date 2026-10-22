package au.com.t1xperts.mailxperts;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Static source safety contract; device consent flows require physical-device UAT. */
public class AiConsentContractTest {
    private static String source(String filename) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(
                "src/main/java/au/com/t1xperts/mailxperts/" + filename));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Test public void consentDefaultsOffAndDisconnectRevokes() throws Exception {
        String prefs = source("AiPreferences.java");
        assertTrue(prefs.contains("prefs.getBoolean(KEY_CLOUD_AI_CONSENT, false)"));
        assertTrue(prefs.contains("putBoolean(KEY_CLOUD_AI_CONSENT, false)"));
    }

    @Test public void gatewayBlocksCloudRequestsWithoutConsent() throws Exception {
        String gateway = source("AiGatewayClient.java");
        int consent = gateway.indexOf("if (!preferences.cloudAiAllowed())");
        int post = gateway.indexOf(".post(RequestBody.create");
        assertTrue("Check cloud consent before building network request",
                consent >= 0 && post > consent);
    }

    @Test public void userHasVisibleCloudDataDisclosureAndOptIn() throws Exception {
        String ui = source("AiAssistantActivity.java");
        assertTrue(ui.contains("PRIVACY NOTICE:"));
        assertTrue(ui.contains("cloudConsent.setChecked(preferences.cloudAiAllowed())"));
        assertTrue(ui.contains("preferences.setCloudAiAllowed(cloudConsent.isChecked())"));
        assertTrue(ui.contains("Cloud AI disabled"));
    }
}
