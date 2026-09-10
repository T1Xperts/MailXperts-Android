package au.com.t1xperts.mailxperts;

import java.util.Locale;

/**
 * Builds the quoted conversation section inserted below a new reply/reply-all
 * or forward. Kept free of Android dependencies so the behaviour can be
 * exhaustively unit-tested on CI.
 */
final class ReplyForwardFormatter {
    private ReplyForwardFormatter() {}

    static String replySubject(String subject) {
        String value = subject == null ? "" : subject.trim();
        if (value.toLowerCase(Locale.ROOT).startsWith("re:")) return value;
        return "Re: " + value;
    }

    static String forwardSubject(String subject) {
        String value = subject == null ? "" : subject.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("fwd:") || lower.startsWith("fw:")) return value;
        return "Fwd: " + value;
    }

    static String replyChain(String from, String to, String cc, String date,
                             String subject, String originalHtml) {
        StringBuilder html = new StringBuilder(768);
        html.append("<div data-mailxperts-quoted=\"reply\" style=\"margin-top:18px\">")
                .append("<hr style=\"border:0;border-top:1px solid #b8b8b8;margin:14px 0\">")
                .append("<div style=\"font-size:0.92em;line-height:1.45\">")
                .append("<strong>Original message</strong><br>")
                .append("<strong>From:</strong> ").append(escape(from)).append("<br>")
                .append("<strong>Sent:</strong> ").append(escape(date)).append("<br>")
                .append("<strong>To:</strong> ").append(escape(to)).append("<br>");
        if (!blank(cc)) {
            html.append("<strong>Cc:</strong> ").append(escape(cc)).append("<br>");
        }
        html.append("<strong>Subject:</strong> ").append(escape(subject))
                .append("</div>")
                .append("<blockquote style=\"margin:12px 0 0 8px;padding-left:12px;border-left:2px solid #b8b8b8\">")
                .append(bodyOrPlaceholder(originalHtml))
                .append("</blockquote></div>");
        return html.toString();
    }

    static String forwardChain(String from, String to, String cc, String date,
                               String subject, String originalHtml) {
        StringBuilder html = new StringBuilder(768);
        html.append("<div data-mailxperts-quoted=\"forward\" style=\"margin-top:18px\">")
                .append("<hr style=\"border:0;border-top:1px solid #b8b8b8;margin:14px 0\">")
                .append("<div style=\"font-size:0.92em;line-height:1.45\">")
                .append("<strong>---------- Forwarded message ----------</strong><br>")
                .append("<strong>From:</strong> ").append(escape(from)).append("<br>")
                .append("<strong>Date:</strong> ").append(escape(date)).append("<br>")
                .append("<strong>Subject:</strong> ").append(escape(subject)).append("<br>")
                .append("<strong>To:</strong> ").append(escape(to)).append("<br>");
        if (!blank(cc)) {
            html.append("<strong>Cc:</strong> ").append(escape(cc)).append("<br>");
        }
        html.append("</div><div style=\"margin-top:12px\">")
                .append(bodyOrPlaceholder(originalHtml))
                .append("</div></div>");
        return html.toString();
    }

    private static String bodyOrPlaceholder(String html) {
        if (blank(html)) return "<p><em>(No message body)</em></p>";
        // Compose's editor has JavaScript enabled, so never inject untrusted
        // message HTML verbatim. Reuse the app's active-content sanitizer while
        // retaining safe nested quoted history, tables, links and formatting.
        String safe = SignatureHtml.sanitise(html);
        return blank(safe) ? "<p><em>(No message body)</em></p>" : safe;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }
}
