package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

import javax.mail.internet.InternetAddress;

/** Normalises common mobile address delimiters before JavaMail strict parsing. */
final class RecipientNormalizer {
    private RecipientNormalizer() {}

    static String normalise(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return "";
        try {
            // Preserve valid RFC group syntax such as Friends: a@example.com, b@example.com;
            InternetAddress.parse(trimmed, true);
            return trimmed;
        } catch (Exception ignored) {
            String normalised = trimmed.replace(';', ',').replace('\r', ',').replace('\n', ',');
            ArrayList<String> cleaned = new ArrayList<>();
            try {
                InternetAddress[] parsed = InternetAddress.parse(normalised, true);
                LinkedHashSet<String> seen = new LinkedHashSet<>();
                for (InternetAddress address : parsed) {
                    address.validate();
                    String key = address.getAddress() == null
                            ? address.toString().toLowerCase(Locale.ROOT)
                            : address.getAddress().toLowerCase(Locale.ROOT);
                    if (seen.add(key)) cleaned.add(address.toUnicodeString());
                }
                return String.join(", ", cleaned);
            } catch (Exception error) {
                throw new IllegalArgumentException("Invalid recipient address. Check To/Cc/Bcc entries.", error);
            }
        }
    }
}
