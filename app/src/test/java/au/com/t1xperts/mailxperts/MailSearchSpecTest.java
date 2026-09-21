package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MailSearchSpecTest {
    @Test public void allWordsMatchLocalTextRegardlessOfOrderAndCase() {
        LocalStore.LocalMessage message = new LocalStore.LocalMessage();
        message.subject = "Quarterly John report";
        message.html = "<p>Prepared for DOE</p>";

        MailSearchSpec spec = new MailSearchSpec(
                "doe JOHN", MailSearchSpec.MODE_ALL,
                MailSearchSpec.SCOPE_DRAFTS, MailRepository.INBOX);

        assertTrue(spec.matchesLocal(message));
    }

    @Test public void wildcardTokensBehaveAsPracticalSubstringSearch() {
        LocalStore.LocalMessage message = new LocalStore.LocalMessage();
        message.to = "john.doe@example.com";

        MailSearchSpec spec = new MailSearchSpec(
                "*doe*", MailSearchSpec.MODE_ALL,
                MailSearchSpec.SCOPE_DRAFTS, MailRepository.INBOX);

        assertTrue(spec.matchesLocal(message));
    }

    @Test public void anyAndExactModesDiffer() {
        LocalStore.LocalMessage message = new LocalStore.LocalMessage();
        message.subject = "John quarterly Doe";

        assertTrue(new MailSearchSpec("john absent", MailSearchSpec.MODE_ANY,
                MailSearchSpec.SCOPE_DRAFTS, MailRepository.INBOX).matchesLocal(message));
        assertFalse(new MailSearchSpec("John Doe", MailSearchSpec.MODE_EXACT,
                MailSearchSpec.SCOPE_DRAFTS, MailRepository.INBOX).matchesLocal(message));
    }

    @Test public void allScopeIncludesServerAndLocalFolders() {
        MailSearchSpec spec = new MailSearchSpec("x", MailSearchSpec.MODE_ALL,
                MailSearchSpec.SCOPE_ALL, MailRepository.INBOX);
        assertEquals(3, spec.serverFolders().size());
        assertEquals(3, spec.localFolders().size());
    }
}
