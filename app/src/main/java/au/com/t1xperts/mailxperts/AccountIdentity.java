package au.com.t1xperts.mailxperts;

/** Human-readable account identity for headers and account switchers. */
final class AccountIdentity {
    private AccountIdentity() {}

    static String email(AccountConfig account) {
        if (account == null || account.email == null) return "";
        return account.email.trim();
    }

    static String friendlyName(AccountConfig account) {
        if (account == null) return "";
        String label = account.displayName();
        return label == null ? "" : label.trim();
    }

    static String compact(AccountConfig account) {
        String email = email(account);
        if (!email.isEmpty()) return email;
        String friendly = friendlyName(account);
        return friendly.isEmpty() ? "Mail account" : friendly;
    }

    static String dropdown(AccountConfig account) {
        String friendly = friendlyName(account);
        String email = email(account);
        if (friendly.isEmpty()) return compact(account);
        if (email.isEmpty() || friendly.equalsIgnoreCase(email)) return friendly;
        return friendly + "\n" + email;
    }

    static String subtitle(AccountConfig account) {
        if (account == null) return "";
        String provider = ProviderPreset.find(account.provider).name;
        String email = email(account);
        if (email.isEmpty()) return provider + "  •  Secure IMAP/SMTP";
        return provider + "  •  " + email + "  •  Secure IMAP/SMTP";
    }
}
