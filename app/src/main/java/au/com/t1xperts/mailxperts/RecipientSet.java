package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

import javax.mail.internet.InternetAddress;

/** Normalises To/Cc/Bcc and prevents the same mailbox appearing in multiple fields. */
final class RecipientSet {
    private RecipientSet() {}

    static final class Fields {
        final String to;
        final String cc;
        final String bcc;
        Fields(String to, String cc, String bcc) {
            this.to = to;
            this.cc = cc;
            this.bcc = bcc;
        }
    }

    static Fields normalise(String to, String cc, String bcc) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        return new Fields(clean(to, seen), clean(cc, seen), clean(bcc, seen));
    }

    private static String clean(String raw, LinkedHashSet<String> seen) {
        String normalised = RecipientNormalizer.normalise(raw);
        if (normalised.isEmpty()) return "";
        try {
            InternetAddress[] parsed = InternetAddress.parse(normalised, true);
            ArrayList<String> kept = new ArrayList<>();
            for (InternetAddress address : parsed) {
                address.validate();
                String mailbox = address.getAddress() == null
                        ? address.toString() : address.getAddress();
                String key = mailbox.trim().toLowerCase(Locale.ROOT);
                if (seen.add(key)) kept.add(address.toUnicodeString());
            }
            return String.join(", ", kept);
        } catch (Exception error) {
            throw new IllegalArgumentException(
                    "Invalid recipient address. Check To/Cc/Bcc entries.", error);
        }
    }
}
