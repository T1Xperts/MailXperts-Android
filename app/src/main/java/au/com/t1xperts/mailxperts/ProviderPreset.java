package au.com.t1xperts.mailxperts;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Known secure server presets. OAuth providers still require an app registration. */
final class ProviderPreset {
    static final String T1XPERTS = "T1XPERTS";
    static final String GMAIL = "GMAIL";
    static final String YAHOO = "YAHOO";
    static final String OUTLOOK = "OUTLOOK";
    static final String ICLOUD = "ICLOUD";
    static final String CUSTOM = "CUSTOM";

    static final class Definition {
        final String id;
        final String name;
        final String imapHost;
        final int imapPort;
        final String smtpHost;
        final int smtpPort;
        final String smtpSecurity;
        final String help;
        final boolean oauthRequired;

        Definition(String id, String name, String imapHost, int imapPort,
                   String smtpHost, int smtpPort, String smtpSecurity,
                   String help, boolean oauthRequired) {
            this.id = id;
            this.name = name;
            this.imapHost = imapHost;
            this.imapPort = imapPort;
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
            this.smtpSecurity = smtpSecurity;
            this.help = help;
            this.oauthRequired = oauthRequired;
        }

        @Override public String toString() { return name; }
    }

    private static final List<Definition> DEFINITIONS = Arrays.asList(
            new Definition(T1XPERTS, "T1Xperts / hosted IMAP", "t1xperts.com.au", 993,
                    "t1xperts.com.au", 465, AccountConfig.SMTP_SSL,
                    "Use the mailbox password supplied by the mail administrator.", false),
            new Definition(GMAIL, "Google Gmail", "imap.gmail.com", 993,
                    "smtp.gmail.com", 465, AccountConfig.SMTP_SSL,
                    "Use a Google app password with 2-Step Verification. Sign in with Google/OAuth will be added after the T1Xperts Google client is registered.", false),
            new Definition(YAHOO, "Yahoo Mail", "imap.mail.yahoo.com", 993,
                    "smtp.mail.yahoo.com", 465, AccountConfig.SMTP_SSL,
                    "Generate a Yahoo third-party app password, then enter it below.", false),
            new Definition(ICLOUD, "Apple iCloud Mail", "imap.mail.me.com", 993,
                    "smtp.mail.me.com", 587, AccountConfig.SMTP_STARTTLS,
                    "Use an Apple app-specific password.", false),
            new Definition(OUTLOOK, "Microsoft Outlook / Hotmail / MSN / Live", "outlook.office365.com", 993,
                    "smtp-mail.outlook.com", 587, AccountConfig.SMTP_STARTTLS,
                    "Microsoft requires OAuth2/Modern Auth. T1Xperts must register the app before this provider can be connected.", true),
            new Definition(CUSTOM, "Other / custom IMAP", "", 993,
                    "", 465, AccountConfig.SMTP_SSL,
                    "Enter the secure IMAP and SMTP settings supplied by your provider.", false)
    );

    private ProviderPreset() {}

    static List<Definition> all() { return DEFINITIONS; }

    static Definition find(String id) {
        for (Definition definition : DEFINITIONS) if (definition.id.equals(id)) return definition;
        return DEFINITIONS.get(DEFINITIONS.size() - 1);
    }

    static int indexOf(String id) {
        for (int i = 0; i < DEFINITIONS.size(); i++) if (DEFINITIONS.get(i).id.equals(id)) return i;
        return DEFINITIONS.size() - 1;
    }

    static String infer(String email, String imapHost) {
        String address = email == null ? "" : email.toLowerCase(Locale.ROOT);
        String host = imapHost == null ? "" : imapHost.toLowerCase(Locale.ROOT);
        if (address.endsWith("@gmail.com") || host.contains("gmail.com")) return GMAIL;
        if (address.endsWith("@yahoo.com") || address.contains("@yahoo.") || host.contains("yahoo.com")) return YAHOO;
        if (address.endsWith("@outlook.com") || address.endsWith("@hotmail.com")
                || address.endsWith("@msn.com") || address.endsWith("@live.com")
                || host.contains("office365.com") || host.contains("outlook.com")) return OUTLOOK;
        if (address.endsWith("@icloud.com") || address.endsWith("@me.com") || host.contains("mail.me.com")) return ICLOUD;
        if (host.contains("t1xperts.com.au")) return T1XPERTS;
        return CUSTOM;
    }
}
