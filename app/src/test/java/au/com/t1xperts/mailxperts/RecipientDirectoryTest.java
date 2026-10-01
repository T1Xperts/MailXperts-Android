package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecipientDirectoryTest {
    @Test public void parsesAndDeduplicatesRecipientsCaseInsensitively() {
        List<RecipientDirectory.Entry> entries = RecipientDirectory.parseAddresses(
                "Ahanaf Tahmid <ahanaf@example.com>, OTHER@example.com",
                "AHANAF@example.com");

        assertEquals(2, entries.size());
        assertTrue(entries.get(0).label().contains("Ahanaf Tahmid"));
    }

    @Test public void suggestionsMatchSubstringIgnoringCaseAndRankPrefixFirst() {
        ArrayList<RecipientDirectory.Entry> entries = new ArrayList<>();
        entries.add(new RecipientDirectory.Entry("Client Ahanaf", "other@example.com", 9, 10));
        entries.add(new RecipientDirectory.Entry("Ahanaf Tahmid", "ahanaf@example.com", 2, 20));
        entries.add(new RecipientDirectory.Entry("Someone", "sales.ahanaf@example.com", 5, 30));

        List<String> result = RecipientDirectory.suggestions(entries, "AHANAF", 10);

        assertEquals(3, result.size());
        assertTrue(result.get(0).startsWith("Ahanaf Tahmid"));
    }

    @Test public void automatedSystemAddressesAreNotLearnable() {
        assertTrue(!RecipientDirectory.isLearnableEmail("no-reply@example.com"));
        assertTrue(!RecipientDirectory.isLearnableEmail("mailer-daemon@example.com"));
        assertTrue(RecipientDirectory.isLearnableEmail("accounts@example.com"));
    }

    @Test public void mergeEntriesDeduplicatesDeviceAndLearnedContacts() {
        ArrayList<RecipientDirectory.Entry> learned = new ArrayList<>();
        learned.add(new RecipientDirectory.Entry("Alice", "alice@example.com", 5, 100,
                10, 3, 2, "MailXperts"));
        ArrayList<RecipientDirectory.Entry> device = new ArrayList<>();
        device.add(new RecipientDirectory.Entry("", "ALICE@example.com", 1, 0,
                0, 0, 0, "Android Contacts"));

        List<RecipientDirectory.Entry> merged = RecipientDirectory.mergeEntries(learned, device);

        assertEquals(1, merged.size());
        assertEquals("Alice", merged.get(0).name);
        assertTrue(merged.get(0).source.contains("Android Contacts"));
    }
}
