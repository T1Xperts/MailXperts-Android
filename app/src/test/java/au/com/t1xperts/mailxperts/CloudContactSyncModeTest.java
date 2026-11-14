package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloudContactSyncModeTest {
    @Test public void disconnectedAndReadOnlyNeverPush() {
        assertFalse(CloudContactSyncMode.DISCONNECTED.canRead());
        assertFalse(CloudContactSyncMode.DISCONNECTED.canPushSelected());
        assertTrue(CloudContactSyncMode.READ_ONLY.canRead());
        assertFalse(CloudContactSyncMode.READ_ONLY.canPushSelected());
    }

    @Test public void onlyExplicitWriteModesCanPush() {
        assertTrue(CloudContactSyncMode.SELECTED_PUSH.canPushSelected());
        assertTrue(CloudContactSyncMode.TWO_WAY.canPushSelected());
    }

    @Test public void googleScopeValidationSeparatesReadAndWrite() {
        assertTrue(GoogleContactsOAuthManager.containsRequiredScope(
                Collections.singletonList(GoogleContactsScopes.READ), false));
        assertFalse(GoogleContactsOAuthManager.containsRequiredScope(
                Collections.singletonList(GoogleContactsScopes.READ), true));
        assertTrue(GoogleContactsOAuthManager.containsRequiredScope(
                Collections.singletonList(GoogleContactsScopes.WRITE), true));
        assertTrue(GoogleContactsOAuthManager.containsRequiredScope(
                Arrays.asList("other", GoogleContactsScopes.WRITE), false));
    }
}
