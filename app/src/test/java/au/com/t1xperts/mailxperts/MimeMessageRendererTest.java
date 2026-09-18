package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MimeMessageRendererTest {
    private final Session session = Session.getInstance(new Properties());

    @Test public void multipartAlternativePrefersHtmlOverPlainText() throws Exception {
        MimeMultipart alternative = new MimeMultipart("alternative");

        MimeBodyPart plain = new MimeBodyPart();
        plain.setText("PLAIN_ONLY_MARKER", "UTF-8");
        alternative.addBodyPart(plain);

        MimeBodyPart html = new MimeBodyPart();
        html.setContent("<p><strong>HTML_MARKER</strong></p>", "text/html; charset=UTF-8");
        alternative.addBodyPart(html);

        MimeMessage message = messageWith(alternative);
        String rendered = MimeMessageRenderer.render(message);

        assertTrue(rendered.contains("HTML_MARKER"));
        assertFalse(rendered.contains("PLAIN_ONLY_MARKER"));
    }

    @Test public void multipartRelatedResolvesCidImageToBoundedDataUri() throws Exception {
        MimeMultipart related = new MimeMultipart("related");

        MimeBodyPart html = new MimeBodyPart();
        html.setContent("<p>Logo</p><img src=\"cid:logo-123\">",
                "text/html; charset=UTF-8");
        related.addBodyPart(html);

        MimeBodyPart image = new MimeBodyPart();
        image.setDataHandler(new DataHandler(
                new ByteArrayDataSource(new byte[]{1, 2, 3, 4}, "image/png")));
        image.setHeader("Content-ID", "<logo-123>");
        image.setDisposition(Part.INLINE);
        image.setFileName("logo.png");
        related.addBodyPart(image);

        MimeMessage message = messageWith(related);
        String rendered = MimeMessageRenderer.render(message);

        assertTrue(rendered.contains("data:image/png;base64,AQIDBA=="));
        assertFalse(rendered.toLowerCase().contains("cid:logo-123"));
    }

    @Test public void mixedMessageKeepsBodyButDoesNotRenderAttachmentAsBody() throws Exception {
        MimeMultipart alternative = new MimeMultipart("alternative");
        MimeBodyPart plain = new MimeBodyPart();
        plain.setText("plain fallback", "UTF-8");
        alternative.addBodyPart(plain);
        MimeBodyPart html = new MimeBodyPart();
        html.setContent("<div>BODY_MARKER</div>", "text/html; charset=UTF-8");
        alternative.addBodyPart(html);

        MimeBodyPart content = new MimeBodyPart();
        content.setContent(alternative);

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setText("ATTACHMENT_BODY_MUST_NOT_RENDER", "UTF-8");
        attachment.setFileName("report.txt");
        attachment.setDisposition(Part.ATTACHMENT);

        MimeMultipart mixed = new MimeMultipart("mixed");
        mixed.addBodyPart(content);
        mixed.addBodyPart(attachment);

        String rendered = MimeMessageRenderer.render(messageWith(mixed));
        assertTrue(rendered.contains("BODY_MARKER"));
        assertFalse(rendered.contains("ATTACHMENT_BODY_MUST_NOT_RENDER"));
    }

    @Test public void nestedMixedRelatedAlternativeRendersRootAndCid() throws Exception {
        MimeMultipart alternative = new MimeMultipart("alternative");
        MimeBodyPart plain = new MimeBodyPart();
        plain.setText("fallback", "UTF-8");
        alternative.addBodyPart(plain);
        MimeBodyPart rich = new MimeBodyPart();
        rich.setContent("<div>NESTED_HTML<img src='CID:nested-logo'></div>",
                "text/html; charset=UTF-8");
        alternative.addBodyPart(rich);

        MimeBodyPart alternativeContainer = new MimeBodyPart();
        alternativeContainer.setContent(alternative);

        MimeBodyPart image = new MimeBodyPart();
        image.setDataHandler(new DataHandler(
                new ByteArrayDataSource(new byte[]{9, 8, 7}, "image/jpeg")));
        image.setHeader("Content-ID", "<nested-logo>");
        image.setDisposition(Part.INLINE);

        MimeMultipart related = new MimeMultipart("related");
        related.addBodyPart(alternativeContainer);
        related.addBodyPart(image);

        MimeBodyPart relatedContainer = new MimeBodyPart();
        relatedContainer.setContent(related);

        MimeMultipart mixed = new MimeMultipart("mixed");
        mixed.addBodyPart(relatedContainer);

        String rendered = MimeMessageRenderer.render(messageWith(mixed));
        assertTrue(rendered.contains("NESTED_HTML"));
        assertTrue(rendered.contains("data:image/jpeg;base64,CQgH"));
    }

    @Test public void remoteHttpAndHttpsImageUrlsRemainAvailableForWebView() throws Exception {
        MimeBodyPart html = new MimeBodyPart();
        html.setContent("<img src='https://cdn.example/logo.png'>"
                        + "<img src='http://legacy.example/banner.jpg'>",
                "text/html; charset=UTF-8");

        MimeMessage message = new MimeMessage(session);
        message.setContent(html.getContent(), html.getContentType());
        message.saveChanges();

        String rendered = MimeMessageRenderer.render(message);
        assertTrue(rendered.contains("https://cdn.example/logo.png"));
        assertTrue(rendered.contains("http://legacy.example/banner.jpg"));
    }

    private MimeMessage messageWith(MimeMultipart multipart) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setContent(multipart);
        message.saveChanges();
        return message;
    }
}
