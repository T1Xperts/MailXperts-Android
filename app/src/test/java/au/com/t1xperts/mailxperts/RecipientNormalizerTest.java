package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecipientNormalizerTest {
    @Test public void acceptsSemicolonSeparatedRecipients() {
        String value = RecipientNormalizer.normalise("alpha@example.com; beta@example.com");
        assertEquals("alpha@example.com, beta@example.com", value);
    }

    @Test public void acceptsCommaSeparatedRecipients() {
        String value = RecipientNormalizer.normalise("alpha@example.com, beta@example.com");
        assertTrue(value.contains("alpha@example.com"));
        assertTrue(value.contains("beta@example.com"));
    }

    @Test public void preservesValidRfcGroupSyntax() {
        String raw = "Team: alpha@example.com, beta@example.com;";
        assertEquals(raw, RecipientNormalizer.normalise(raw));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidRecipient() {
        RecipientNormalizer.normalise("not an email; beta@example.com");
    }
}
