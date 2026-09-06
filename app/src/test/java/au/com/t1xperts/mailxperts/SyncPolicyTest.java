package au.com.t1xperts.mailxperts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SyncPolicyTest {
    @Test public void unsupportedIntervalFallsBackToFifteenMinutes() {
        assertEquals(15, SyncPolicy.normalizeInterval(7));
        assertEquals(0, SyncPolicy.normalizeInterval(0));
        assertEquals(1_440, SyncPolicy.normalizeInterval(1_440));
    }

    @Test public void manualModeNeverBecomesDue() {
        assertFalse(SyncPolicy.isDue(0L, 1_000_000L, SyncPolicy.MANUAL));
    }

    @Test public void intervalBecomesDueAtBoundary() {
        long last = 10_000L;
        assertFalse(SyncPolicy.isDue(last, last + 899_999L, 15));
        assertTrue(SyncPolicy.isDue(last, last + 900_000L, 15));
    }
}
