package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GmailAppPasswordTest {
    @Test public void removesGoogleGroupingSpaces() {
        assertEquals("abcdefghijklmnop",
                GmailAppPassword.normalise("abcd efgh ijkl mnop"));
    }

    @Test public void removesOtherWhitespaceWithoutChangingCharacters() {
        assertEquals("abcdefghijklmnop",
                GmailAppPassword.normalise(" abcd\tefgh\nijkl mnop "));
    }

    @Test public void acceptsSixteenCharactersAfterNormalisation() {
        assertTrue(GmailAppPassword.isValid("abcd efgh ijkl mnop"));
    }

    @Test public void rejectsOrdinaryOrIncompletePasswords() {
        assertFalse(GmailAppPassword.isValid("ordinary-password"));
        assertFalse(GmailAppPassword.isValid("abcd efgh ijkl"));
        assertFalse(GmailAppPassword.isValid(""));
    }
}
