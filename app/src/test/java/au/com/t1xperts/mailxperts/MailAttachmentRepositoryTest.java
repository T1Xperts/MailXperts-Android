package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MailAttachmentRepositoryTest {
    @Test public void inlineCidImageIsNotPresentedAsDownloadAttachment() throws Exception {
        MimeBodyPart image = new MimeBodyPart();
        image.setDataHandler(new DataHandler(
                new ByteArrayDataSource(new byte[]{1, 2, 3}, "image/png")));
        image.setHeader("Content-ID", "<logo>");
        image.setDisposition(Part.INLINE);
        image.setFileName("logo.png");

        assertFalse(MailAttachmentRepository.isAttachment(image));
    }

    @Test public void explicitAttachmentAndFilenameOnlyAttachmentAreDetected() throws Exception {
        MimeBodyPart explicit = new MimeBodyPart();
        explicit.setDisposition(Part.ATTACHMENT);
        explicit.setFileName("invoice.pdf");
        assertTrue(MailAttachmentRepository.isAttachment(explicit));

        MimeBodyPart implicit = new MimeBodyPart();
        implicit.setText("report", "UTF-8");
        implicit.setFileName("report.txt");
        assertTrue(MailAttachmentRepository.isAttachment(implicit));
    }

    @Test public void nestedCollectionReturnsOnlyDownloadableAttachments() throws Exception {
        MimeMultipart related = new MimeMultipart("related");
        MimeBodyPart html = new MimeBodyPart();
        html.setContent("<img src='cid:logo'>", "text/html; charset=UTF-8");
        related.addBodyPart(html);

        MimeBodyPart inline = new MimeBodyPart();
        inline.setDataHandler(new DataHandler(
                new ByteArrayDataSource(new byte[]{1}, "image/png")));
        inline.setHeader("Content-ID", "<logo>");
        inline.setDisposition(Part.INLINE);
        inline.setFileName("logo.png");
        related.addBodyPart(inline);

        MimeBodyPart relatedContainer = new MimeBodyPart();
        relatedContainer.setContent(related);

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setText("download me", "UTF-8");
        attachment.setFileName("download.txt");
        attachment.setDisposition(Part.ATTACHMENT);

        MimeMultipart mixed = new MimeMultipart("mixed");
        mixed.addBodyPart(relatedContainer);
        mixed.addBodyPart(attachment);

        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setContent(mixed);
        message.saveChanges();

        List<Part> found = new ArrayList<>();
        MailAttachmentRepository.collectAttachmentParts(message, found);

        assertEquals(1, found.size());
        assertEquals("download.txt", found.get(0).getFileName());
    }
}
