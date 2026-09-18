package au.com.t1xperts.mailxperts;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;

/**
 * Standards-aware MIME body renderer.
 *
 * The renderer:
 * - prefers the richest supported body from multipart/alternative;
 * - renders only the root body from multipart/related;
 * - collects CID inline images and resolves cid: URLs to bounded data URIs;
 * - skips true attachments while preserving non-attachment body parts in multipart/mixed; and
 * - safely escapes plain-text bodies.
 */
final class MimeMessageRenderer {
    private static final int MAX_INLINE_RESOURCE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_TOTAL_INLINE_RESOURCE_BYTES = 12 * 1024 * 1024;
    private static final Pattern CID_REFERENCE =
            Pattern.compile("(?i)cid:<?([^\\s\\\"'<>]+)>?");

    private MimeMessageRenderer() {}

    static String render(Part root) throws Exception {
        if (root == null) return "";
        RenderContext context = new RenderContext();
        collectInlineResources(root, context);
        String html = renderBody(root);
        return replaceCidReferences(html, context.resources);
    }

    private static String renderBody(Part part) throws Exception {
        if (part == null || MimePartClassifier.isAttachment(part)
                || MimePartClassifier.isInlineCidImage(part)) {
            return "";
        }

        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            return content instanceof String ? (String) content
                    : content == null ? "" : content.toString();
        }

        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            String plain = content instanceof String ? (String) content
                    : content == null ? "" : content.toString();
            return "<div style='white-space:pre-wrap'>"
                    + escapeHtml(plain) + "</div>";
        }

        if (part.isMimeType("multipart/alternative")) {
            Multipart multipart = (Multipart) part.getContent();
            // RFC 2046 orders alternatives from least to most faithful. Prefer the last
            // supported non-empty representation.
            for (int i = multipart.getCount() - 1; i >= 0; i--) {
                String candidate = renderBody(multipart.getBodyPart(i));
                if (!candidate.isEmpty()) return candidate;
            }
            return "";
        }

        if (part.isMimeType("multipart/related")) {
            Multipart multipart = (Multipart) part.getContent();
            if (multipart.getCount() == 0) return "";
            int rootIndex = relatedRootIndex(part, multipart);
            return renderBody(multipart.getBodyPart(rootIndex));
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                String child = renderBody(multipart.getBodyPart(i));
                if (child.isEmpty()) continue;
                if (out.length() > 0) out.append("<hr>");
                out.append(child);
            }
            return out.toString();
        }

        if (part.isMimeType("message/rfc822")) {
            Object content = part.getContent();
            if (content instanceof Part) {
                String nested = renderBody((Part) content);
                if (!nested.isEmpty()) {
                    return "<blockquote class='mailxperts-forwarded-message'>"
                            + nested + "</blockquote>";
                }
            }
        }

        return "";
    }

    private static int relatedRootIndex(Part parent, Multipart multipart) {
        try {
            ContentType type = new ContentType(parent.getContentType());
            String start = MimePartClassifier.normaliseContentId(type.getParameter("start"));
            if (!start.isEmpty()) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    if (start.equals(MimePartClassifier.contentId(multipart.getBodyPart(i)))) return i;
                }
            }
        } catch (Exception ignored) {
            // RFC 2387 defaults the root to the first body part when start is absent/invalid.
        }
        return 0;
    }

    private static void collectInlineResources(Part part, RenderContext context) throws Exception {
        if (part == null || MimePartClassifier.isAttachment(part)) return;

        if (MimePartClassifier.isInlineCidImage(part)) {
            String cid = MimePartClassifier.contentId(part);
            if (cid.isEmpty() || context.resources.containsKey(cid)) return;
            byte[] bytes = readBounded(part, MAX_INLINE_RESOURCE_BYTES);
            if (bytes == null || bytes.length == 0
                    || context.totalInlineBytes + bytes.length > MAX_TOTAL_INLINE_RESOURCE_BYTES) {
                return;
            }
            String mimeType = baseMimeType(part.getContentType());
            if (!mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) return;
            String dataUri = "data:" + mimeType + ";base64,"
                    + Base64.getEncoder().encodeToString(bytes);
            context.resources.put(cid, dataUri);
            context.totalInlineBytes += bytes.length;
            return;
        }

        if (part.isMimeType("multipart/*")) {
            Object content = part.getContent();
            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    collectInlineResources(multipart.getBodyPart(i), context);
                }
            }
            return;
        }

        if (part.isMimeType("message/rfc822")) {
            Object content = part.getContent();
            if (content instanceof Part) collectInlineResources((Part) content, context);
        }
    }

    private static byte[] readBounded(Part part, int maxBytes) throws Exception {
        int declared = part.getSize();
        if (declared > maxBytes) return null;
        try (InputStream input = part.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream(
                     declared > 0 ? Math.min(declared, maxBytes) : 8192)) {
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) return null;
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String replaceCidReferences(String html, Map<String, String> resources) {
        if (html == null || html.isEmpty() || resources.isEmpty()) return html == null ? "" : html;
        Matcher matcher = CID_REFERENCE.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = MimePartClassifier.normaliseContentId(matcher.group(1));
            String replacement = resources.get(key);
            if (replacement == null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String baseMimeType(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "application/octet-stream";
        try {
            return new ContentType(raw).getBaseType();
        } catch (Exception ignored) {
            int semicolon = raw.indexOf(';');
            return (semicolon >= 0 ? raw.substring(0, semicolon) : raw).trim();
        }
    }

    private static String escapeHtml(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder out = new StringBuilder(value.length() + 32);
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

    private static final class RenderContext {
        final Map<String, String> resources = new LinkedHashMap<>();
        int totalInlineBytes;
    }
}
