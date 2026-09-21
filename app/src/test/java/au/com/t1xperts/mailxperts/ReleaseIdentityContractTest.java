package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ReleaseIdentityContractTest {
    @Test public void productionIdentitySupportsUpgradeFromPreviousMailXpertsBuilds() throws Exception {
        String gradle = readProjectFile("build.gradle", "app/build.gradle");

        assertTrue(gradle.contains("applicationId 'au.com.t1xperts.mailxperts'"));
        assertTrue("Beta must advance beyond the v1.5.4 production versionCode",
                gradle.contains("versionCode 15"));
        assertTrue(gradle.contains("versionName '1.6.0-beta.1'"));
        assertTrue(gradle.contains("applicationIdSuffix '.dev'"));
    }

    private static String readProjectFile(String modulePath, String rootPath) throws Exception {
        Path path = Paths.get(modulePath);
        if (!Files.exists(path)) path = Paths.get(rootPath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
