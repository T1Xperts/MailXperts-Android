package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecipientSetTest {
    @Test public void duplicateRecipientsAreRemovedAcrossToCcBcc() {
        RecipientSet.Fields fields = RecipientSet.normalise(
                "Alice <alice@example.com>; bob@example.com",
                "ALICE@example.com, carol@example.com",
                "bob@example.com, dave@example.com");
        assertTrue(fields.to.toLowerCase().contains("alice@example.com"));
        assertTrue(fields.to.toLowerCase().contains("bob@example.com"));
        assertFalse(fields.cc.toLowerCase().contains("alice@example.com"));
        assertTrue(fields.cc.toLowerCase().contains("carol@example.com"));
        assertFalse(fields.bcc.toLowerCase().contains("bob@example.com"));
        assertEquals("dave@example.com", fields.bcc);
    }
}
