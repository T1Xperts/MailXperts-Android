package au.com.t1xperts.mailxperts;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EditorAssetContractTest {
    @Test public void editorSupportsBoundedNativeAndLinkedImages() throws Exception {
        Path path = Paths.get("src/main/assets/editor.html");
        if (!Files.exists(path)) path = Paths.get("app/src/main/assets/editor.html");
        String editor = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertTrue(editor.contains("MailXpertsImages.chooseImage()"));
        assertTrue(editor.contains("Image URL"));
        assertTrue(editor.contains("insertImage:insertImage"));
        assertTrue(editor.contains("safeMarkup"));
    }
}
