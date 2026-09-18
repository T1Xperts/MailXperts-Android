package au.com.t1xperts.mailxperts;

import javax.mail.MessagingException;
import javax.mail.Part;

/**
 * Shared MIME part classification rules used by both the renderer and attachment handling.
 * Inline CID images are message content, not downloadable attachments unless explicitly marked
 * as attachment by the sender.
 */
final class MimePartClassifier {
    private MimePartClassifier() {}

    static boolean isAttachment(Part part) throws MessagingException {
        if (part == null) return false;
        String disposition = part.getDisposition();
        if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) return true;
        if (Part.INLINE.equalsIgnoreCase(disposition)) return false;

        String contentId = contentId(part);
        if (!contentId.isEmpty() && part.isMimeType("image/*")) return false;

        String name = part.getFileName();
        return name != null && !name.trim().isEmpty();
    }

    static boolean isInlineCidImage(Part part) throws MessagingException {
        if (part == null || isAttachment(part) || !part.isMimeType("image/*")) return false;
        return !contentId(part).isEmpty();
    }

    static String contentId(Part part) throws MessagingException {
        if (part == null) return "";
        String[] values = part.getHeader("Content-ID");
        if (values == null || values.length == 0 || values[0] == null) return "";
        return normaliseContentId(values[0]);
    }

    static String normaliseContentId(String value) {
        if (value == null) return "";
        String out = value.trim();
        if (out.regionMatches(true, 0, "cid:", 0, 4)) out = out.substring(4).trim();
        while (out.startsWith("<") || out.startsWith(""") || out.startsWith("'")) {
            out = out.substring(1).trim();
        }
        while (out.endsWith(">") || out.endsWith(""") || out.endsWith("'")) {
            out = out.substring(0, out.length() - 1).trim();
        }
        return out.toLowerCase(java.util.Locale.ROOT);
    }
}
