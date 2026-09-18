package au.com.t1xperts.mailxperts;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OutgoingAttachmentMimeTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test public void outgoingMessageBuildsMixedMultipartWithNormalisedRecipients() throws Exception {
        File file = temp.newFile("invoice.txt");
        Files.write(file.toPath(), "invoice-content".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        AttachmentRef attachment = new AttachmentRef(
                file.getAbsolutePath(), "invoice.txt", "text/plain", file.length());

        AccountConfig account = new AccountConfig();
        account.email = "sender@example.com";

        MimeMessage message = MailAttachmentRepository.buildMessage(
                Session.getInstance(new Properties()),
                account,
                "alpha@example.com; beta@example.com",
                "",
                "",
                "Invoice",
                "<p>Hello</p>",
                "Hello",
                Collections.singletonList(attachment));

        InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(Message.RecipientType.TO);
        assertEquals(2, recipients.length);

        Object content = message.getContent();
        assertTrue(content instanceof Multipart);
        Multipart mixed = (Multipart) content;
        assertEquals(2, mixed.getCount());

        Part body = mixed.getBodyPart(0);
        assertTrue(body.isMimeType("multipart/alternative"));

        Part attached = mixed.getBodyPart(1);
        assertEquals(Part.ATTACHMENT, attached.getDisposition());
        assertEquals("invoice.txt", attached.getFileName());
        assertTrue(attached.isMimeType("text/plain"));
    }
}
