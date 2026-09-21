package au.com.t1xperts.mailxperts;

/** Normalises and validates Google's current App Password format without logging the secret. */
final class GmailAppPassword {
    private static final int EXPECTED_LENGTH = 16;

    private GmailAppPassword() {}

    static String normalise(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!Character.isWhitespace(c)) out.append(c);
        }
        return out.toString();
    }

    static boolean isValid(String raw) {
        return normalise(raw).length() == EXPECTED_LENGTH;
    }
}
