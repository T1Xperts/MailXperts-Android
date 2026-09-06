package au.com.t1xperts.mailxperts;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalises legacy encoded signatures and strips active content before storage. */
final class SignatureHtml {
    static final int MAX_HTML_CHARACTERS = 300_000;

    private static final Pattern ENCODED_MARKUP = Pattern.compile(
            "(?is)&(?:amp;)?lt;\\s*(?:!doctype|html|body|table|tbody|tr|td|div|p|span|a|img|style|br)\\b");
    private static final Pattern BODY = Pattern.compile(
            "(?is)<\\s*body(?:\\s[^>]*)?>(.*?)<\\s*/\\s*body\\s*>");
    private static final Pattern DANGEROUS_BLOCK = Pattern.compile(
            "(?is)<\\s*(script|iframe|object|embed|form|svg|math)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>");
    private static final Pattern DANGEROUS_TAG = Pattern.compile(
            "(?is)<\\s*/?\\s*(?:script|iframe|object|embed|form|input|button|meta|base|link|svg|math)\\b[^>]*>");
    private static final Pattern EVENT_ATTRIBUTE = Pattern.compile(
            "(?is)\\s+on[a-z0-9_-]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern URL_ATTRIBUTE = Pattern.compile(
            "(?is)\\b(href|src)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    private static final Pattern COMMENTS = Pattern.compile("(?is)<!--.*?-->");
    private static final Pattern STYLE_EXPRESSION = Pattern.compile(
            "(?is)(?:expression\\s*\\(|url\\s*\\(\\s*['\"]?\\s*(?:javascript|vbscript):)");

    private SignatureHtml() {}

    static String normaliseStored(String stored) {
        String value = stored == null ? "" : stored.trim();
        for (int pass = 0; pass < 2 && ENCODED_MARKUP.matcher(value).find(); pass++) {
            value = decodeEntities(value);
        }
        return sanitise(value);
    }

    static String sanitise(String html) {
        String value = html == null ? "" : html.replace("\u0000", "").trim();
        Matcher body = BODY.matcher(value);
        if (body.find()) value = body.group(1);
        value = COMMENTS.matcher(value).replaceAll("");
        String previous;
        do {
            previous = value;
            value = DANGEROUS_BLOCK.matcher(value).replaceAll("");
        } while (!value.equals(previous));
        value = DANGEROUS_TAG.matcher(value).replaceAll("");
        value = EVENT_ATTRIBUTE.matcher(value).replaceAll("");
        value = sanitiseUrls(value);
        value = STYLE_EXPRESSION.matcher(value).replaceAll("");
        return value.trim();
    }

    static boolean isWithinLimit(String html) {
        return html != null && html.length() <= MAX_HTML_CHARACTERS;
    }

    private static String sanitiseUrls(String html) {
        Matcher matcher = URL_ATTRIBUTE.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String attribute = matcher.group(1).toLowerCase(Locale.ROOT);
            String url = matcher.group(2) != null ? matcher.group(2)
                    : matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            url = url.trim();
            if (!allowedUrl(attribute, url)) {
                matcher.appendReplacement(out,
                        Matcher.quoteReplacement(attribute + "=\"\""));
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static boolean allowedUrl(String attribute, String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) return true;
        if ("href".equals(attribute)) {
            return lower.startsWith("mailto:") || lower.startsWith("tel:")
                    || lower.startsWith("#");
        }
        return lower.startsWith("cid:")
                || lower.matches("data:image/(?:png|jpeg|jpg|gif|webp);base64,[a-z0-9+/=\\s]+")
                || lower.isEmpty();
    }

    private static String decodeEntities(String value) {
        return value.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&amp;", "&");
    }
}
