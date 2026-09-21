package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * Converts bounded data:image URLs from the rich editor into normal CID inline MIME parts.
 * Data URIs are convenient while editing but are not rendered reliably by many mail clients.
 */
final class InlineImageCid {
    static final int MAX_INLINE_IMAGES = 12;
    static final int MAX_INLINE_IMAGE_BYTES = 512_000;

    private static final Pattern DATA_IMAGE = Pattern.compile(
            "(?is)(\\bsrc\\s*=\\s*)([\"'])(data:image/(png|jpeg|jpg|gif|webp);base64,([a-z0-9+/=\\s]+))\\2");

    static final class Image {
        final String cid;
        final String mimeType;
        final byte[] bytes;

        Image(String cid, String mimeType, byte[] bytes) {
            this.cid = cid;
            this.mimeType = mimeType;
            this.bytes = bytes;
        }
    }

    static final class Prepared {
        final String html;
        final List<Image> images;

        Prepared(String html, List<Image> images) {
            this.html = html == null ? "" : html;
            this.images = Collections.unmodifiableList(images);
        }
    }

    private InlineImageCid() {}

    static Prepared prepare(String html) {
        String input = html == null ? "" : html;
        Matcher matcher = DATA_IMAGE.matcher(input);
        StringBuffer output = new StringBuffer();
        ArrayList<Image> images = new ArrayList<>();
        while (matcher.find()) {
            if (images.size() >= MAX_INLINE_IMAGES) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            try {
                String subtype = matcher.group(4).toLowerCase(Locale.ROOT);
                String mime = "image/" + ("jpg".equals(subtype) ? "jpeg" : subtype);
                String payload = matcher.group(5).replaceAll("\\s+", "");
                byte[] decoded = Base64.getDecoder().decode(payload);
                if (decoded.length == 0 || decoded.length > MAX_INLINE_IMAGE_BYTES) {
                    matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
                    continue;
                }
                String cid = "mailxperts-inline-" + (images.size() + 1) + "@local";
                images.add(new Image(cid, mime, decoded));
                String replacement = matcher.group(1) + matcher.group(2)
                        + "cid:" + cid + matcher.group(2);
                matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
            } catch (IllegalArgumentException malformed) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(output);
        return new Prepared(output.toString(), images);
    }

    static void addParts(MimeMultipart related, Prepared prepared) throws Exception {
        for (Image image : prepared.images) {
            MimeBodyPart part = new MimeBodyPart();
            part.setDataHandler(new DataHandler(new ByteArrayDataSource(image.bytes, image.mimeType)));
            part.setHeader("Content-ID", "<" + image.cid + ">");
            part.setDisposition(Part.INLINE);
            related.addBodyPart(part);
        }
    }
}
