package au.com.t1xperts.mailxperts;

import java.util.Locale;

/** Pure-Java allow-list for links that may leave the email WebView. */
final class ExternalLinkPolicy {
    private ExternalLinkPolicy() {}

    static boolean isAllowedScheme(String scheme) {
        if (scheme == null) return false;
        String value = scheme.trim().toLowerCase(Locale.ROOT);
        return "http".equals(value)
                || "https".equals(value)
                || "mailto".equals(value)
                || "tel".equals(value)
                || "sms".equals(value)
                || "geo".equals(value);
    }
}
