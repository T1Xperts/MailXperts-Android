package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountIdentityTest {
    @Test public void compactIdentityUsesActualEmailAddress() {
        AccountConfig account = new AccountConfig();
        account.label = "Google Gmail";
        account.email = "user@gmail.com";
        assertEquals("user@gmail.com", AccountIdentity.compact(account));
    }

    @Test public void dropdownShowsFriendlyNameAndActualEmail() {
        AccountConfig account = new AccountConfig();
        account.label = "Google Gmail";
        account.email = "user@gmail.com";
        assertEquals("Google Gmail\nuser@gmail.com", AccountIdentity.dropdown(account));
    }

    @Test public void subtitleIncludesProviderAndEmail() {
        AccountConfig account = new AccountConfig();
        account.provider = ProviderPreset.GMAIL;
        account.email = "user@gmail.com";
        String subtitle = AccountIdentity.subtitle(account);
        assertTrue(subtitle.contains("Google Gmail"));
        assertTrue(subtitle.contains("user@gmail.com"));
    }
}
