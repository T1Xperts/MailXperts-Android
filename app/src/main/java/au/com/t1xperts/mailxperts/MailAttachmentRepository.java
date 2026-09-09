package au.com.t1xperts.mailxperts;

import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.search.MessageIDTerm;

/**
 * Attachment-specific IMAP/SMTP operations kept separate from the cache-first message repository.
 * This limits regression risk in the fast inbox synchronisation path.
 */
final class MailAttachmentRepository {
    private static final long MAX_DOWNLOAD_BYTES = 100L * 1024L * 1024L;

    static final class IncomingAttachment {
        final int index;
        final String fileName;
        final String mimeType;
        final long size;

        IncomingAttachment(int index, String fileName, String mimeType, long size) {
            this.index = index;
            this.fileName = fileName == null || fileName.trim().isEmpty()
                    ? "attachment-" + (index + 1) : fileName;
            this.mimeType = mimeType == null || mimeType.trim().isEmpty()
                    ? "application/octet-stream" : mimeType;
            this.size = Math.max(0L, size);
        }
    }

    private MailAttachmentRepository() {}

    static List<IncomingAttachment> listAttachments(
            AccountConfig account, String kind, long uid) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder folder = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            folder = resolveFolder(store, kind, false);
            if (folder == null || !folder.exists()) return Collections.emptyList();
            folder.open(Folder.READ_ONLY);
            if (!(folder instanceof UIDFolder)) return Collections.emptyList();
            Message message = ((UIDFolder) folder).getMessageByUID(uid);
            if (message == null) return Collections.emptyList();
            ArrayList<Part> parts = new ArrayList<>();
            collectAttachmentParts(message, parts);
            ArrayList<IncomingAttachment> out = new ArrayList<>(parts.size());
            for (int i = 0; i < parts.size(); i++) out.add(info(parts.get(i), i));
            return out;
        } finally {
            closeQuietly(folder, store);
        }
    }

    static void writeAttachment(AccountConfig account, String kind, long uid,
                                int attachmentIndex, OutputStream output) throws Exception {
        if (output == null) throw new IOException("Attachment destination is unavailable.");
        Session session = imapSession(account);
        Store store = null;
        Folder folder = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            folder = resolveFolder(store, kind, false);
            if (folder == null || !folder.exists()) throw new MessagingException("Mailbox folder is unavailable.");
            folder.open(Folder.READ_ONLY);
            if (!(folder instanceof UIDFolder)) throw new MessagingException("Mailbox does not expose message UIDs.");
            Message message = ((UIDFolder) folder).getMessageByUID(uid);
            if (message == null) throw new MessagingException("Message is no longer available.");
            ArrayList<Part> parts = new ArrayList<>();
            collectAttachmentParts(message, parts);
            if (attachmentIndex < 0 || attachmentIndex >= parts.size()) {
                throw new MessagingException("Attachment is no longer available.");
            }
            long copied = 0L;
            try (InputStream input = parts.get(attachmentIndex).getInputStream()) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    copied += read;
                    if (copied > MAX_DOWNLOAD_BYTES) {
                        throw new IOException("Attachment exceeds the 100 MB MailXperts safety limit.");
                    }
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
        } finally {
            closeQuietly(folder, store);
        }
    }

    static MailRepository.SendResult sendHtmlWithAttachments(
            AccountConfig account, String to, String cc, String bcc,
            String subject, String html, List<AttachmentRef> attachments) throws Exception {
        List<AttachmentRef> valid = validAttachments(attachments);
        if (valid.isEmpty()) {
            return MailRepository.sendHtml(account,
                    RecipientNormalizer.normalise(to), RecipientNormalizer.normalise(cc),
                    RecipientNormalizer.normalise(bcc), subject, html);
        }
        Session session = smtpSession(account);
        MimeMessage message = buildMessage(session, account, to, cc, bcc, subject, html, valid);
        Transport transport = null;
        try {
            transport = session.getTransport("smtp");
            transport.connect(account.smtpHost, account.smtpPort, account.username, account.password);
            Address[] recipients = message.getAllRecipients();
            if (recipients == null || recipients.length == 0) {
                throw new MessagingException("No recipients were specified.");
            }
            transport.sendMessage(message, recipients);
        } finally {
            if (transport != null && transport.isConnected()) {
                try { transport.close(); } catch (Exception ignored) {}
            }
        }

        if (ProviderPreset.GMAIL.equals(account.provider)) {
            return new MailRepository.SendResult(true, "");
        }
        try {
            appendToSent(account, message);
            return new MailRepository.SendResult(true, "");
        } catch (Exception error) {
            return new MailRepository.SendResult(false, MailRepository.safe(error));
        }
    }

    static long saveServerDraftWithAttachments(
            AccountConfig account, LocalStore.LocalMessage local,
            List<AttachmentRef> attachments) throws Exception {
        List<AttachmentRef> valid = validAttachments(attachments);
        if (valid.isEmpty()) return MailRepository.saveServerDraft(account, local);
        Session session = imapSession(account);
        MimeMessage message = buildMessage(session, account, local.to, local.cc, local.bcc,
                local.subject, local.html, valid);
        message.setFlag(Flags.Flag.DRAFT, true);
        Store store = null;
        Folder drafts = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            drafts = resolveFolder(store, MailRepository.DRAFTS, true);
            if (drafts == null || (!drafts.exists() && !drafts.create(Folder.HOLDS_MESSAGES))) {
                throw new MessagingException("The provider did not expose a Drafts folder.");
            }
            drafts.open(Folder.READ_WRITE);
            long newUid = 0L;
            if (drafts instanceof IMAPFolder) {
                AppendUID[] result = ((IMAPFolder) drafts).appendUIDMessages(new Message[]{message});
                if (result != null && result.length > 0 && result[0] != null) newUid = result[0].uid;
            } else {
                drafts.appendMessages(new Message[]{message});
            }
            if (newUid <= 0L && drafts instanceof UIDFolder && message.getMessageID() != null) {
                Message[] matches = drafts.search(new MessageIDTerm(message.getMessageID()));
                UIDFolder uidFolder = (UIDFolder) drafts;
                for (Message match : matches) newUid = Math.max(newUid, uidFolder.getUID(match));
            }
            if (newUid > 0L && local.serverUid > 0L && local.serverUid != newUid) {
                Message previous = ((UIDFolder) drafts).getMessageByUID(local.serverUid);
                if (previous != null) {
                    previous.setFlag(Flags.Flag.DELETED, true);
                    drafts.expunge();
                }
            }
            return newUid;
        } finally {
            closeQuietly(drafts, store);
        }
    }

    private static MimeMessage buildMessage(Session session, AccountConfig account,
                                            String to, String cc, String bcc,
                                            String subject, String html,
                                            List<AttachmentRef> attachments) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(account.email));
        addRecipients(message, Message.RecipientType.TO, to);
        addRecipients(message, Message.RecipientType.CC, cc);
        addRecipients(message, Message.RecipientType.BCC, bcc);
        message.setSubject(subject == null ? "" : subject, "UTF-8");
        message.setSentDate(new Date());

        MimeMultipart alternative = new MimeMultipart("alternative");
        MimeBodyPart plain = new MimeBodyPart();
        plain.setText(android.text.Html.fromHtml(html == null ? "" : html,
                android.text.Html.FROM_HTML_MODE_LEGACY).toString(), "UTF-8");
        alternative.addBodyPart(plain);
        MimeBodyPart rich = new MimeBodyPart();
        rich.setContent(html == null ? "" : html, "text/html; charset=UTF-8");
        alternative.addBodyPart(rich);

        MimeMultipart mixed = new MimeMultipart("mixed");
        MimeBodyPart content = new MimeBodyPart();
        content.setContent(alternative);
        mixed.addBodyPart(content);
        for (AttachmentRef attachment : attachments) {
            MimeBodyPart part = new MimeBodyPart();
            File file = attachment.file();
            FileDataSource source = new FileDataSource(file);
            if (attachment.mimeType != null && !attachment.mimeType.isEmpty()) {
                source.setFileTypeMap(new javax.activation.FileTypeMap() {
                    @Override public String getContentType(File ignored) { return attachment.mimeType; }
                    @Override public String getContentType(String ignored) { return attachment.mimeType; }
                });
            }
            part.setDataHandler(new DataHandler(source));
            part.setFileName(MimeUtility.encodeText(attachment.name, "UTF-8", null));
            part.setDisposition(Part.ATTACHMENT);
            mixed.addBodyPart(part);
        }
        message.setContent(mixed);
        message.saveChanges();
        return message;
    }

    private static List<AttachmentRef> validAttachments(List<AttachmentRef> attachments)
            throws IOException {
        if (attachments == null || attachments.isEmpty()) return Collections.emptyList();
        ArrayList<AttachmentRef> out = new ArrayList<>();
        for (AttachmentRef attachment : attachments) {
            if (attachment == null) continue;
            if (!attachment.exists()) {
                throw new IOException("Attachment is no longer available: " + attachment.name);
            }
            out.add(attachment);
        }
        return out;
    }

    private static void addRecipients(MimeMessage message, Message.RecipientType type, String raw)
            throws Exception {
        String normalised = RecipientNormalizer.normalise(raw);
        if (!normalised.isEmpty()) message.setRecipients(type, InternetAddress.parse(normalised, true));
    }

    private static void appendToSent(AccountConfig account, MimeMessage message) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder sent = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            sent = resolveFolder(store, MailRepository.SENT, true);
            if (sent == null || (!sent.exists() && !sent.create(Folder.HOLDS_MESSAGES))) {
                throw new MessagingException("Could not create Sent folder.");
            }
            message.setFlag(Flags.Flag.SEEN, true);
            sent.appendMessages(new Message[]{message});
        } finally {
            closeQuietly(sent, store);
        }
    }

    private static IncomingAttachment info(Part part, int index) throws Exception {
        String name = part.getFileName();
        if (name != null) {
            try { name = MimeUtility.decodeText(name); } catch (Exception ignored) {}
        }
        String mime = "application/octet-stream";
        try { mime = new ContentType(part.getContentType()).getBaseType(); }
        catch (Exception ignored) {}
        return new IncomingAttachment(index, name, mime, Math.max(0, part.getSize()));
    }

    private static void collectAttachmentParts(Part part, List<Part> out) throws Exception {
        if (isAttachment(part)) {
            out.add(part);
            return;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                collectAttachmentParts(multipart.getBodyPart(i), out);
            }
        }
    }

    private static boolean isAttachment(Part part) throws MessagingException {
        String disposition = part.getDisposition();
        if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) return true;
        String name = part.getFileName();
        return name != null && !name.trim().isEmpty();
    }

    private static Session smtpSession(AccountConfig account) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", account.smtpHost);
        properties.put("mail.smtp.port", String.valueOf(account.smtpPort));
        properties.put("mail.smtp.auth", "true");
        boolean startTls = AccountConfig.SMTP_STARTTLS.equals(account.smtpSecurity);
        properties.put("mail.smtp.ssl.enable", String.valueOf(!startTls));
        properties.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        properties.put("mail.smtp.starttls.required", String.valueOf(startTls));
        properties.put("mail.smtp.ssl.checkserveridentity", "true");
        properties.put("mail.smtp.connectiontimeout", "15000");
        properties.put("mail.smtp.timeout", "30000");
        properties.put("mail.smtp.writetimeout", "30000");
        return Session.getInstance(properties, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(account.username, account.password);
            }
        });
    }

    private static Session imapSession(AccountConfig account) {
        Properties properties = new Properties();
        properties.put("mail.imaps.host", account.imapHost);
        properties.put("mail.imaps.port", String.valueOf(account.imapPort));
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.ssl.checkserveridentity", "true");
        properties.put("mail.imaps.connectiontimeout", "10000");
        properties.put("mail.imaps.timeout", "30000");
        properties.put("mail.imaps.writetimeout", "30000");
        return Session.getInstance(properties);
    }

    private static Folder resolveFolder(Store store, String kind, boolean createIfMissing)
            throws Exception {
        if (MailRepository.INBOX.equals(kind)) return store.getFolder("INBOX");
        if (MailRepository.SENT.equals(kind)) {
            Folder special = findBySpecialUse(store, "\\Sent");
            if (special != null) return special;
            Folder found = findExisting(store,
                    new String[]{"Sent", "INBOX.Sent", "Sent Items", "Sent Messages", "INBOX/Sent"},
                    "sent");
            if (found != null) return found;
            Folder fallback = store.getFolder("Sent");
            if (createIfMissing && !fallback.exists()) fallback.create(Folder.HOLDS_MESSAGES);
            return fallback;
        }
        if (MailRepository.JUNK.equals(kind)) {
            Folder special = findBySpecialUse(store, "\\Junk");
            if (special != null) return special;
            Folder found = findExisting(store,
                    new String[]{"Junk", "Spam", "INBOX.Junk", "INBOX.Spam", "[Gmail]/Spam", "Bulk Mail"},
                    "junk", "spam", "bulk mail");
            if (found != null) return found;
            return store.getFolder("Junk");
        }
        if (MailRepository.DRAFTS.equals(kind)) {
            Folder special = findBySpecialUse(store, "\\Drafts");
            if (special != null) return special;
            Folder found = findExisting(store,
                    new String[]{"Drafts", "INBOX.Drafts", "Draft", "[Gmail]/Drafts"}, "draft");
            if (found != null) return found;
            Folder fallback = store.getFolder("Drafts");
            if (createIfMissing && !fallback.exists()) fallback.create(Folder.HOLDS_MESSAGES);
            return fallback;
        }
        if (MailRepository.TRASH.equals(kind)) {
            Folder special = findBySpecialUse(store, "\\Trash");
            if (special != null) return special;
            Folder found = findExisting(store,
                    new String[]{"Trash", "Deleted Items", "Deleted Messages", "INBOX.Trash", "[Gmail]/Trash"},
                    "trash", "deleted");
            if (found != null) return found;
            return store.getFolder("Trash");
        }
        return store.getFolder(kind);
    }

    private static Folder findExisting(Store store, String[] candidates, String... fragments) {
        try {
            for (String candidate : candidates) {
                Folder folder = store.getFolder(candidate);
                if (folder.exists()) return folder;
            }
            Folder[] all = store.getDefaultFolder().list("*");
            if (all != null) for (Folder folder : all) {
                String name = folder.getFullName().toLowerCase(Locale.ROOT);
                for (String fragment : fragments) {
                    if (name.endsWith(fragment) || name.contains(fragment)) return folder;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Folder findBySpecialUse(Store store, String wanted) {
        try {
            Folder[] all = store.getDefaultFolder().list("*");
            if (all == null) return null;
            for (Folder folder : all) if (folder instanceof IMAPFolder) {
                String[] attributes = ((IMAPFolder) folder).getAttributes();
                if (attributes != null) for (String attribute : attributes) {
                    if (wanted.equalsIgnoreCase(attribute)) return folder;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void closeQuietly(Folder folder, Store store) {
        try { if (folder != null && folder.isOpen()) folder.close(false); } catch (Exception ignored) {}
        try { if (store != null && store.isConnected()) store.close(); } catch (Exception ignored) {}
    }
}
