package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountConfigTest {
    @Test public void newAccountStartsWithBlankIdentityAndCredentialFields() {
        AccountConfig account = new AccountConfig();

        assertEquals("", account.label);
        assertEquals("", account.email);
        assertEquals("", account.username);
        assertEquals("", account.password);
        assertTrue("A new account must not be usable until the user enters credentials",
                !account.isUsable());
    }
}
