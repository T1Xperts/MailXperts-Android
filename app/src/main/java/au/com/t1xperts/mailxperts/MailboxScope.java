package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.List;

/** Identifies whether a screen is showing one mailbox or the unified mailbox. */
final class MailboxScope {
    static final String ALL_ACCOUNTS = "__ALL_ACCOUNTS__";

    private MailboxScope() {}

    static boolean isAll(String accountId) {
        return ALL_ACCOUNTS.equals(accountId);
    }

    static List<AccountConfig> usable(SecureStore store) {
        ArrayList<AccountConfig> accounts = new ArrayList<>();
        for (AccountConfig account : store.loadAll()) {
            if (account != null && account.isUsable()) accounts.add(account);
        }
        return accounts;
    }

    static AccountConfig find(List<AccountConfig> accounts, String accountId) {
        if (accounts == null || accountId == null) return null;
        for (AccountConfig account : accounts) {
            if (accountId.equals(account.id)) return account;
        }
        return null;
    }
}
