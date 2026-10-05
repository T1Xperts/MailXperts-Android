package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Release contracts for MX-QA-006/011/012/013/014/015/017/019/021/022/023/026. */
public class BacklogCompletionContractTest {
    @Test public void selectedBacklogCapabilitiesArePresent() throws Exception {
        String compose = src("ComposeActivity.java");
        String signature = src("SignatureEditorActivity.java");
        String message = src("MessageActivity.java");
        String navigation = src("Navigation.java");
        String mailbox = src("MailboxActivity.java");
        String settings = src("SettingsActivity.java");
        String search = src("SearchActivity.java");
        String searchSpec = src("MailSearchSpec.java");
        String manifest = project("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml");

        // 006 HTML signature image reliability
        assertTrue(compose.contains("setBlockNetworkLoads(false)"));
        assertTrue(signature.contains("setBlockNetworkLoads(false)"));
        assertTrue(signature.contains("MIXED_CONTENT_COMPATIBILITY_MODE"));

        // 011 message details
        assertTrue(message.contains("Show message details"));
        assertTrue(message.contains("Reply-To"));
        assertTrue(message.contains("Message-ID"));

        // 012 safe sender HTML under app themes
        String htmlPolicy = src("EmailHtmlPolicy.java");
        assertTrue(htmlPolicy.contains("color-scheme:light"));
        assertTrue(htmlPolicy.contains("background:#ffffff"));

        // 013 full-height structured drawer
        assertTrue(navigation.contains("Gravity.START"));
        assertTrue(navigation.contains("ViewGroup.LayoutParams.MATCH_PARENT"));
        assertTrue(navigation.contains("PEOPLE"));
        assertTrue(navigation.contains("ACCOUNTS & APP"));

        // 014 compact Smart Priority
        assertTrue(mailbox.contains("Smart Priority"));
        assertTrue(message.contains("smartStatus.setMaxLines(2)"));

        // 015 settings hierarchy/progressive disclosure
        assertTrue(settings.contains("ACCOUNT IDENTITY"));
        assertTrue(settings.contains("CACHE-FIRST SYNCHRONISATION"));
        assertTrue(settings.contains("Show advanced server settings"));
        assertTrue(settings.contains("SIGNATURE"));

        // 017 recipient chips/autocomplete/dedup
        assertTrue(compose.contains("RecipientSuggestionAdapter"));
        assertTrue(compose.contains("RecipientSet.normalise"));
        assertTrue(src("RecipientChipAutoCompleteTextView.java").contains("ChipSpan"));

        // 019 Android share-to-compose
        assertTrue(manifest.contains("android.intent.action.SEND"));
        assertTrue(manifest.contains("android.intent.action.SEND_MULTIPLE"));
        assertTrue(compose.contains("importSharedAttachments"));

        // 021 quick account switcher
        assertTrue(mailbox.contains("accountSpinner()"));
        assertTrue(mailbox.contains("Add new mail account"));
        assertTrue(mailbox.contains("AccountIdentity.dropdown"));

        // 022 friendly provider auth errors
        assertTrue(settings.contains("ProviderErrorMessage.forAccount"));
        assertTrue(src("OAuthConnectActivity.java").contains("ProviderErrorMessage"));

        // 023 clear server-delete behaviour
        assertTrue(settings.contains("Safe default: server deletion is off"));
        assertTrue(settings.contains("Current Delete behaviour"));
        assertTrue(message.contains("Delete — move to server Trash"));
        assertTrue(message.contains("Delete on this device"));

        // 026 advanced scoped/token search
        assertTrue(search.contains("All server folders"));
        assertTrue(search.contains("All Accounts"));
        assertTrue(search.contains("All words"));
        assertTrue(search.contains("Exact phrase"));
        assertTrue(searchSpec.contains("MODE_ANY"));
        assertTrue(searchSpec.contains("SCOPE_ALL"));
    }

    private static String src(String file) throws Exception {
        return project("src/main/java/au/com/t1xperts/mailxperts/" + file,
                "app/src/main/java/au/com/t1xperts/mailxperts/" + file);
    }

    private static String project(String modulePath, String rootPath) throws Exception {
        Path path = Paths.get(modulePath);
        if (!Files.exists(path)) path = Paths.get(rootPath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
