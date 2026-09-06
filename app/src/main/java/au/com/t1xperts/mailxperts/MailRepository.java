package au.com.t1xperts.mailxperts;

import android.text.Html;
import android.text.TextUtils;

import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.BodyTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.RecipientStringTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

final class MailRepository {
    static final String INBOX = "INBOX";
    static final String SENT = "SENT";
    static final String JUNK = "JUNK";
    static final String DRAFTS = "DRAFTS";
    static final String TRASH = "TRASH";
    private static final int IMAP_CONNECT_TIMEOUT_MS = 10_000;
    private static final int IMAP_READ_TIMEOUT_MS = 15_000;
    private static final int IMAP_WRITE_TIMEOUT_MS = 15_000;
    private static final int OVERALL_SYNC_TIMEOUT_SECONDS = 60;
    private static final ScheduledExecutorService SYNC_WATCHDOG =
            Executors.newScheduledThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "mailxperts-sync-watchdog");
                thread.setDaemon(true);
                return thread;
            });

    static final class Summary {
        final long uid;
        final String from;
        final String subject;
        final Date date;
        final boolean seen;
        final String folderKind;
        final String accountId;
        final String accountLabel;

        Summary(long uid, String from, String subject, Date date, boolean seen) {
            this(uid, from, subject, date, seen, INBOX, "", "");
        }

        Summary(long uid, String from, String subject, Date date, boolean seen, String folderKind) {
            this(uid, from, subject, date, seen, folderKind, "", "");
        }

        Summary(long uid, String from, String subject, Date date, boolean seen, String folderKind,
                String accountId, String accountLabel) {
            this.uid = uid;
            this.from = from;
            this.subject = subject;
            this.date = date;
            this.seen = seen;
            this.folderKind = folderKind;
            this.accountId = accountId == null ? "" : accountId;
            this.accountLabel = accountLabel == null ? "" : accountLabel;
        }
    }

    static final class FullMessage {
        final long uid;
        final String from;
        final String to;
        final String subject;
        final Date date;
        final String html;
        final String headers;

        FullMessage(long uid, String from, String to, String subject, Date date,
                    String html, String headers) {
            this.uid = uid;
            this.from = from;
            this.to = to;
            this.subject = subject;
            this.date = date;
            this.html = html;
            this.headers = headers;
        }
    }

    static final class SendResult {
        final boolean sentCopySaved;
        final String sentCopyWarning;
        SendResult(boolean saved, String warning) { sentCopySaved = saved; sentCopyWarning = warning; }
    }

    static final class SyncRequest {
        final int requestedLimit;
        final int cachedCount;
        final long newestCachedUid;
        final long oldestCachedUid;
        final long knownUidValidity;
        final boolean loadOlder;
        final int maxBackfillMessages;

        SyncRequest(int requestedLimit, int cachedCount, long newestCachedUid,
                    long oldestCachedUid, long knownUidValidity, boolean loadOlder) {
            this(requestedLimit, cachedCount, newestCachedUid, oldestCachedUid,
                    knownUidValidity, loadOlder, Integer.MAX_VALUE);
        }

        SyncRequest(int requestedLimit, int cachedCount, long newestCachedUid,
                    long oldestCachedUid, long knownUidValidity, boolean loadOlder,
                    int maxBackfillMessages) {
            this.requestedLimit = SyncPlanner.clampRequestedLimit(requestedLimit);
            this.cachedCount = Math.max(0, cachedCount);
            this.newestCachedUid = Math.max(0L, newestCachedUid);
            this.oldestCachedUid = Math.max(0L, oldestCachedUid);
            this.knownUidValidity = Math.max(0L, knownUidValidity);
            this.loadOlder = loadOlder;
            this.maxBackfillMessages = Math.max(0, maxBackfillMessages);
        }
    }

    static final class SyncResult {
        final int serverMessageCount;
        final int processedMessageCount;
        final long uidValidity;

        SyncResult(int serverMessageCount, int processedMessageCount, long uidValidity) {
            this.serverMessageCount = serverMessageCount;
            this.processedMessageCount = processedMessageCount;
            this.uidValidity = uidValidity;
        }
    }

    interface SyncObserver {
        void onMailboxOpened(long uidValidity, boolean cacheMustReset, int serverMessageCount)
                throws Exception;
        void onBatch(List<Summary> batch, int processed, int expected, String phase)
                throws Exception;
    }

    /** Cooperative cancellation that can also close an IMAP connection blocked in I/O. */
    static final class SyncToken {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean timedOut = new AtomicBoolean(false);
        private volatile Store store;
        private volatile Folder folder;

        void attach(Store attachedStore, Folder attachedFolder) {
            store = attachedStore;
            folder = attachedFolder;
            if (cancelled.get() || timedOut.get()) abortConnectionAsync();
        }

        void cancel() {
            if (cancelled.compareAndSet(false, true)) abortConnectionAsync();
        }

        private void timeout() {
            if (timedOut.compareAndSet(false, true)) abortConnectionAsync();
        }

        void throwIfStopped() throws Exception {
            if (timedOut.get()) {
                throw new SocketTimeoutException(
                        "Mailbox sync exceeded " + OVERALL_SYNC_TIMEOUT_SECONDS + " seconds.");
            }
            if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Mailbox sync was cancelled.");
            }
        }

        private void abortConnectionAsync() {
            SYNC_WATCHDOG.execute(() -> closeQuietly(folder, store));
        }
    }

    private MailRepository() {}

    /**
     * Synchronises one mailbox incrementally. A small first batch reaches the UI quickly, while
     * the remainder is fetched in bounded batches and can resume from the oldest cached UID.
     */
    static SyncResult syncFolder(AccountConfig account, String kind, SyncRequest request,
                                 SyncObserver observer, SyncToken token) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder folder = null;
        ScheduledFuture<?> timeout = SYNC_WATCHDOG.schedule(
                token::timeout, OVERALL_SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        int processed = 0;
        try {
            token.throwIfStopped();
            store = session.getStore("imaps");
            token.attach(store, null);
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            token.throwIfStopped();

            folder = resolveFolder(store, kind, false);
            token.attach(store, folder);
            if (folder == null || !folder.exists()) {
                observer.onMailboxOpened(0L, true, 0);
                return new SyncResult(0, 0, 0L);
            }
            folder.open(Folder.READ_ONLY);
            token.throwIfStopped();
            if (!(folder instanceof UIDFolder)) {
                throw new MessagingException("The selected server folder does not support IMAP UIDs.");
            }

            UIDFolder uidFolder = (UIDFolder) folder;
            int total = folder.getMessageCount();
            long uidValidity = uidFolder.getUIDValidity();
            boolean cacheMustReset = total == 0 || (request.knownUidValidity > 0L
                    && uidValidity > 0L && request.knownUidValidity != uidValidity);
            observer.onMailboxOpened(uidValidity, cacheMustReset, total);
            if (total <= 0) return new SyncResult(0, 0, uidValidity);

            int cachedCount = cacheMustReset ? 0 : request.cachedCount;
            long newestCachedUid = cacheMustReset ? 0L : request.newestCachedUid;
            long oldestCachedUid = cacheMustReset ? 0L : request.oldestCachedUid;

            if (!cacheMustReset && !request.loadOlder && cachedCount > 0
                    && newestCachedUid > 0L
                    && uidFolder.getMessageByUID(newestCachedUid) == null) {
                // The newest checkpoint was deleted or moved. Rebuild so it cannot remain stale.
                cacheMustReset = true;
                cachedCount = 0;
                newestCachedUid = 0L;
                oldestCachedUid = 0L;
                observer.onMailboxOpened(uidValidity, true, total);
            }

            if (cachedCount <= 0 || newestCachedUid <= 0L || oldestCachedUid <= 0L) {
                int expected = SyncPlanner.limitBackfill(
                        Math.min(total, request.requestedLimit), request.maxBackfillMessages);
                processed += fetchRanges(account, kind, folder, uidFolder,
                        SyncPlanner.newestFirst(total, expected,
                                SyncPlanner.QUICK_BATCH_SIZE,
                                SyncPlanner.BACKGROUND_BATCH_SIZE),
                        0L, false, expected, "Loading recent mail", observer, token);
            } else if (request.loadOlder) {
                processed += fetchOlder(account, kind, folder, uidFolder, oldestCachedUid,
                        SyncPlanner.limitBackfill(request.requestedLimit - cachedCount,
                                request.maxBackfillMessages),
                        request.requestedLimit, uidValidity, total, observer, token);
            } else {
                // Refresh is proportional to new mail, not to the entire cached window.
                processed += fetchRanges(account, kind, folder, uidFolder,
                        SyncPlanner.newestFirst(total,
                                Math.min(total, request.requestedLimit),
                                SyncPlanner.QUICK_BATCH_SIZE,
                                SyncPlanner.BACKGROUND_BATCH_SIZE),
                        newestCachedUid, true, 0, "Checking for new mail", observer, token);

                // Resume an interrupted initial backfill without re-fetching completed batches.
                if (cachedCount < request.requestedLimit) {
                    processed += fetchOlder(account, kind, folder, uidFolder, oldestCachedUid,
                            SyncPlanner.limitBackfill(request.requestedLimit - cachedCount,
                                    request.maxBackfillMessages),
                            request.requestedLimit, uidValidity, total, observer, token);
                }
            }

            token.throwIfStopped();
            return new SyncResult(total, processed, uidValidity);
        } catch (Exception error) {
            token.throwIfStopped();
            throw error;
        } finally {
            timeout.cancel(false);
            closeQuietly(folder, store);
        }
    }

    private static int fetchOlder(AccountConfig account, String kind, Folder folder,
                                  UIDFolder uidFolder, long oldestCachedUid,
                                  int requestedCount, int requestedLimit, long uidValidity,
                                  int total, SyncObserver observer, SyncToken token) throws Exception {
        if (requestedCount <= 0) return 0;
        token.throwIfStopped();
        Message anchor = uidFolder.getMessageByUID(oldestCachedUid);
        token.throwIfStopped();

        if (anchor == null || anchor.getMessageNumber() <= 1) {
            if (anchor == null) {
                // A deleted anchor cannot safely define the older window; rebuild the bounded cache.
                observer.onMailboxOpened(uidValidity, true, total);
                int expected = Math.min(total, requestedLimit);
                return fetchRanges(account, kind, folder, uidFolder,
                        SyncPlanner.newestFirst(total, expected,
                                SyncPlanner.QUICK_BATCH_SIZE,
                                SyncPlanner.BACKGROUND_BATCH_SIZE),
                        0L, false, expected, "Rebuilding mailbox cache", observer, token);
            }
            return 0;
        }

        int endSequence = anchor.getMessageNumber() - 1;
        int expected = Math.min(requestedCount, endSequence);
        return fetchRanges(account, kind, folder, uidFolder,
                SyncPlanner.newestFirst(endSequence, expected,
                        SyncPlanner.BACKGROUND_BATCH_SIZE,
                        SyncPlanner.BACKGROUND_BATCH_SIZE),
                0L, false, expected, "Loading older mail", observer, token);
    }

    private static int fetchRanges(AccountConfig account, String kind, Folder folder,
                                   UIDFolder uidFolder, List<SyncPlanner.Range> ranges,
                                   long stopAtOrBelowUid, boolean stopOnOverlap,
                                   int expected, String phase,
                                   SyncObserver observer, SyncToken token) throws Exception {
        int processed = 0;
        for (SyncPlanner.Range range : ranges) {
            token.throwIfStopped();
            Message[] serverMessages = folder.getMessages(
                    range.startInclusive, range.endInclusive);
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            profile.add(UIDFolder.FetchProfileItem.UID);
            folder.fetch(serverMessages, profile);
            token.throwIfStopped();

            ArrayList<Summary> batch = new ArrayList<>(serverMessages.length);
            boolean overlapFound = false;
            for (int index = serverMessages.length - 1; index >= 0; index--) {
                Message message = serverMessages[index];
                long uid = uidFolder.getUID(message);
                if (uid <= 0L) continue;
                if (stopAtOrBelowUid > 0L && uid <= stopAtOrBelowUid) overlapFound = true;
                String correspondent = SENT.equals(kind)
                        ? addresses(message.getRecipients(Message.RecipientType.TO))
                        : addresses(message.getFrom());
                batch.add(new Summary(uid, correspondent, message.getSubject(),
                        message.getReceivedDate() != null
                                ? message.getReceivedDate() : message.getSentDate(),
                        message.isSet(Flags.Flag.SEEN), kind,
                        account.id, account.displayName()));
            }

            processed += batch.size();
            if (!batch.isEmpty()) {
                observer.onBatch(Collections.unmodifiableList(batch), processed, expected, phase);
            }
            if (stopOnOverlap && overlapFound) break;
        }
        return processed;
    }

    static List<Summary> fetchFolder(AccountConfig account, String kind, int limit) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder folder = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            folder = resolveFolder(store, kind, false);
            if (folder == null || !folder.exists()) return Collections.emptyList();
            folder.open(Folder.READ_ONLY);
            int total = folder.getMessageCount();
            if (total <= 0) return Collections.emptyList();
            int start = limit > 0 ? Math.max(1, total - limit + 1) : 1;
            Message[] messages = folder.getMessages(start, total);
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            folder.fetch(messages, profile);
            UIDFolder uidFolder = (UIDFolder) folder;
            ArrayList<Summary> out = new ArrayList<>();
            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                String correspondent = SENT.equals(kind)
                        ? addresses(message.getRecipients(Message.RecipientType.TO))
                        : addresses(message.getFrom());
                out.add(new Summary(uidFolder.getUID(message), correspondent, message.getSubject(),
                        message.getReceivedDate() != null ? message.getReceivedDate() : message.getSentDate(),
                        message.isSet(Flags.Flag.SEEN), kind, account.id, account.displayName()));
            }
            return out;
        } finally {
            closeQuietly(folder, store);
        }
    }

    static List<Summary> search(AccountConfig account, String query, int limitPerFolder) throws Exception {
        String clean = query == null ? "" : query.trim();
        if (clean.isEmpty()) return Collections.emptyList();
        Session session = imapSession(account);
        Store store = null;
        ArrayList<Summary> out = new ArrayList<>();
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            SearchTerm term = new OrTerm(new SearchTerm[]{new SubjectTerm(clean), new FromStringTerm(clean),
                    new RecipientStringTerm(Message.RecipientType.TO, clean),
                    new RecipientStringTerm(Message.RecipientType.CC, clean), new BodyTerm(clean)});
            searchFolder(account, store, INBOX, term, limitPerFolder, out);
            searchFolder(account, store, SENT, term, limitPerFolder, out);
            out.sort((left, right) -> Long.compare(
                    right.date == null ? 0L : right.date.getTime(),
                    left.date == null ? 0L : left.date.getTime()));
            return out;
        } finally {
            if (store != null && store.isConnected()) try { store.close(); } catch (Exception ignored) {}
        }
    }

    private static void searchFolder(AccountConfig account, Store store, String kind,
                                     SearchTerm term, int limit,
                                     List<Summary> out) throws Exception {
        Folder folder = null;
        try {
            folder = resolveFolder(store, kind, false);
            if (folder == null || !folder.exists()) return;
            folder.open(Folder.READ_ONLY);
            Message[] matches = folder.search(term);
            if (matches == null || matches.length == 0) return;
            int start = limit > 0 ? Math.max(0, matches.length - limit) : 0;
            Message[] chosen = new Message[matches.length - start];
            System.arraycopy(matches, start, chosen, 0, chosen.length);
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            folder.fetch(chosen, profile);
            UIDFolder uidFolder = (UIDFolder) folder;
            for (int i = chosen.length - 1; i >= 0; i--) {
                Message message = chosen[i];
                String correspondent = SENT.equals(kind)
                        ? addresses(message.getRecipients(Message.RecipientType.TO))
                        : addresses(message.getFrom());
                out.add(new Summary(uidFolder.getUID(message), correspondent, message.getSubject(),
                        message.getReceivedDate() != null ? message.getReceivedDate() : message.getSentDate(),
                        message.isSet(Flags.Flag.SEEN), kind, account.id, account.displayName()));
            }
        } finally {
            try { if (folder != null && folder.isOpen()) folder.close(false); } catch (Exception ignored) {}
        }
    }

    static FullMessage fetchMessage(AccountConfig account, String kind, long uid) throws Exception {
        return fetchMessage(account, kind, uid, true);
    }

    static FullMessage fetchMessage(
            AccountConfig account, String kind, long uid, boolean markReadOnServer) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder folder = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            folder = resolveFolder(store, kind, false);
            if (folder == null || !folder.exists()) throw new MessagingException("Mailbox folder is unavailable.");
            folder.open(markReadOnServer ? Folder.READ_WRITE : Folder.READ_ONLY);
            UIDFolder uidFolder = (UIDFolder) folder;
            Message message = uidFolder.getMessageByUID(uid);
            if (message == null) throw new MessagingException("Message is no longer available.");
            if (markReadOnServer) message.setFlag(Flags.Flag.SEEN, true);
            return new FullMessage(uid, addresses(message.getFrom()),
                    addresses(message.getRecipients(Message.RecipientType.TO)), message.getSubject(),
                    message.getReceivedDate() != null ? message.getReceivedDate() : message.getSentDate(),
                    extractBestBody(message), fullHeaders(message));
        } finally {
            closeQuietly(folder, store);
        }
    }

    /** Moves a message between Inbox and the provider's special-use Junk folder. */
    static void setSpam(AccountConfig account, String sourceKind, long uid, boolean spam) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder source = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            source = resolveFolder(store, sourceKind, false);
            if (source == null || !source.exists()) throw new MessagingException("Source mailbox is unavailable.");
            Folder destination = resolveFolder(store, spam ? JUNK : INBOX, true);
            if (destination == null || (!destination.exists() && !destination.create(Folder.HOLDS_MESSAGES))) {
                throw new MessagingException("The provider did not expose a Spam/Junk folder.");
            }
            source.open(Folder.READ_WRITE);
            Message message = ((UIDFolder) source).getMessageByUID(uid);
            if (message == null) throw new MessagingException("Message is no longer available.");
            message.setFlag(Flags.Flag.SEEN, true);
            setUserFlag(message, "$Junk", spam);
            setUserFlag(message, "Junk", spam);
            setUserFlag(message, "$NotJunk", !spam);
            if (source.getFullName().equalsIgnoreCase(destination.getFullName())) return;
            try {
                if (source instanceof IMAPFolder) {
                    ((IMAPFolder) source).moveMessages(new Message[]{message}, destination);
                } else {
                    source.copyMessages(new Message[]{message}, destination);
                    message.setFlag(Flags.Flag.DELETED, true);
                    source.expunge();
                }
            } catch (MessagingException moveError) {
                source.copyMessages(new Message[]{message}, destination);
                message.setFlag(Flags.Flag.DELETED, true);
                source.expunge();
            }
        } finally {
            closeQuietly(source, store);
        }
    }

    /** Moves the selected server message to Trash, falling back to IMAP deletion if necessary. */
    static void deleteMessage(AccountConfig account, String sourceKind, long uid) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder source = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            source = resolveFolder(store, sourceKind, false);
            if (source == null || !source.exists()) {
                throw new MessagingException("Source mailbox is unavailable.");
            }
            source.open(Folder.READ_WRITE);
            Message message = ((UIDFolder) source).getMessageByUID(uid);
            if (message == null) throw new MessagingException("Message is no longer available.");

            Folder trash = resolveFolder(store, TRASH, true);
            if (trash != null && (!trash.exists() && !trash.create(Folder.HOLDS_MESSAGES))) {
                trash = null;
            }
            if (trash == null || source.getFullName().equalsIgnoreCase(trash.getFullName())) {
                message.setFlag(Flags.Flag.DELETED, true);
                source.expunge();
                return;
            }
            moveMessages(source, trash, new Message[]{message});
        } finally {
            closeQuietly(source, store);
        }
    }

    /** Appends or replaces one IMAP Draft after its local copy has already been saved. */
    static long saveServerDraft(AccountConfig account, LocalStore.LocalMessage local) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder drafts = null;
        try {
            MimeMessage message = buildMessage(session, account, local.to, local.cc, local.bcc,
                    local.subject, local.html);
            message.setFlag(Flags.Flag.DRAFT, true);
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            drafts = resolveFolder(store, DRAFTS, true);
            if (drafts == null || (!drafts.exists() && !drafts.create(Folder.HOLDS_MESSAGES))) {
                throw new MessagingException("The provider did not expose a Drafts folder.");
            }
            drafts.open(Folder.READ_WRITE);
            long newUid = 0L;
            if (drafts instanceof IMAPFolder) {
                AppendUID[] result = ((IMAPFolder) drafts).appendUIDMessages(
                        new Message[]{message});
                if (result != null && result.length > 0 && result[0] != null) {
                    newUid = result[0].uid;
                }
            } else {
                drafts.appendMessages(new Message[]{message});
            }
            if (newUid <= 0L && drafts instanceof UIDFolder
                    && message.getMessageID() != null) {
                // UIDPLUS is optional. Resolve the just-appended Draft by its unique Message-ID
                // so later edits can replace it instead of creating remote duplicates.
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

    static void deleteServerDraft(AccountConfig account, long uid) throws Exception {
        if (uid <= 0L) return;
        Session session = imapSession(account);
        Store store = null;
        Folder drafts = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            drafts = resolveFolder(store, DRAFTS, false);
            if (drafts == null || !drafts.exists()) return;
            drafts.open(Folder.READ_WRITE);
            Message message = ((UIDFolder) drafts).getMessageByUID(uid);
            if (message != null) {
                message.setFlag(Flags.Flag.DELETED, true);
                drafts.expunge();
            }
        } finally {
            closeQuietly(drafts, store);
        }
    }

    static SendResult sendHtml(AccountConfig account, String to, String cc, String bcc,
                               String subject, String html) throws Exception {
        Session session = smtpSession(account);
        MimeMessage message = buildMessage(session, account, to, cc, bcc, subject, html);
        Transport transport = null;
        try {
            transport = session.getTransport("smtp");
            transport.connect(account.smtpHost, account.smtpPort, account.username, account.password);
            Address[] recipients = message.getAllRecipients();
            if (recipients == null || recipients.length == 0) throw new MessagingException("No recipients were specified.");
            transport.sendMessage(message, recipients);
        } finally {
            if (transport != null && transport.isConnected()) try { transport.close(); } catch (Exception ignored) {}
        }
        if (ProviderPreset.GMAIL.equals(account.provider)) return new SendResult(true, "");
        try {
            appendToSent(account, message);
            return new SendResult(true, "");
        } catch (Exception error) {
            return new SendResult(false, safe(error));
        }
    }

    static void testConnections(AccountConfig account) throws Exception {
        Session imap = imapSession(account);
        Store store = null;
        try {
            store = imap.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
        } finally {
            if (store != null && store.isConnected()) store.close();
        }
        Session smtp = smtpSession(account);
        Transport transport = null;
        try {
            transport = smtp.getTransport("smtp");
            transport.connect(account.smtpHost, account.smtpPort, account.username, account.password);
        } finally {
            if (transport != null && transport.isConnected()) transport.close();
        }
    }

    private static MimeMessage buildMessage(Session session, AccountConfig account, String to,
                                            String cc, String bcc, String subject, String html) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(account.email));
        addRecipients(message, Message.RecipientType.TO, to);
        addRecipients(message, Message.RecipientType.CC, cc);
        addRecipients(message, Message.RecipientType.BCC, bcc);
        message.setSubject(subject == null ? "" : subject, "UTF-8");
        message.setSentDate(new Date());
        MimeMultipart alternative = new MimeMultipart("alternative");
        MimeBodyPart plain = new MimeBodyPart();
        plain.setText(toPlainText(html), "UTF-8");
        alternative.addBodyPart(plain);
        MimeBodyPart rich = new MimeBodyPart();
        rich.setContent(html == null ? "" : html, "text/html; charset=UTF-8");
        alternative.addBodyPart(rich);
        message.setContent(alternative);
        message.saveChanges();
        return message;
    }

    private static void appendToSent(AccountConfig account, MimeMessage message) throws Exception {
        Session session = imapSession(account);
        Store store = null;
        Folder sent = null;
        try {
            store = session.getStore("imaps");
            store.connect(account.imapHost, account.imapPort, account.username, account.password);
            sent = resolveFolder(store, SENT, true);
            if (!sent.exists() && !sent.create(Folder.HOLDS_MESSAGES)) {
                throw new MessagingException("Could not create Sent folder.");
            }
            message.setFlag(Flags.Flag.SEEN, true);
            sent.appendMessages(new Message[]{message});
        } finally {
            closeQuietly(sent, store);
        }
    }

    private static Folder resolveFolder(Store store, String kind, boolean createIfMissing) throws Exception {
        if (INBOX.equals(kind)) return store.getFolder("INBOX");
        if (SENT.equals(kind)) {
            Folder special = findBySpecialUse(store, "\\Sent");
            if (special != null) return special;
            String[] candidates = {"Sent", "INBOX.Sent", "Sent Items", "Sent Messages", "INBOX/Sent"};
            Folder found = findExisting(store, candidates, "sent");
            if (found != null) return found;
            Folder fallback = store.getFolder("Sent");
            if (createIfMissing && !fallback.exists()) fallback.create(Folder.HOLDS_MESSAGES);
            return fallback;
        }
        if (JUNK.equals(kind)) {
            Folder special = findBySpecialUse(store, "\\Junk");
            if (special != null) return special;
            String[] candidates = {"Junk", "Spam", "INBOX.Junk", "INBOX.Spam", "[Gmail]/Spam", "Bulk Mail"};
            Folder found = findExisting(store, candidates, "junk", "spam", "bulk mail");
            if (found != null) return found;
            Folder fallback = store.getFolder("Junk");
            if (createIfMissing && !fallback.exists()) fallback.create(Folder.HOLDS_MESSAGES);
            return fallback;
        }
        if (DRAFTS.equals(kind)) {
            Folder special = findBySpecialUse(store, "\\Drafts");
            if (special != null) return special;
            String[] candidates = {"Drafts", "INBOX.Drafts", "Draft", "[Gmail]/Drafts"};
            Folder found = findExisting(store, candidates, "draft");
            if (found != null) return found;
            Folder fallback = store.getFolder("Drafts");
            if (createIfMissing && !fallback.exists()) fallback.create(Folder.HOLDS_MESSAGES);
            return fallback;
        }
        if (TRASH.equals(kind)) {
            Folder special = findBySpecialUse(store, "\\Trash");
            if (special != null) return special;
            String[] candidates = {"Trash", "Deleted Items", "Deleted Messages", "INBOX.Trash", "[Gmail]/Trash"};
            Folder found = findExisting(store, candidates, "trash", "deleted");
            if (found != null) return found;
            Folder fallback = store.getFolder("Trash");
            if (createIfMissing && !fallback.exists()) fallback.create(Folder.HOLDS_MESSAGES);
            return fallback;
        }
        return store.getFolder(kind);
    }

    private static void moveMessages(Folder source, Folder destination, Message[] messages)
            throws MessagingException {
        try {
            if (source instanceof IMAPFolder) {
                ((IMAPFolder) source).moveMessages(messages, destination);
            } else {
                source.copyMessages(messages, destination);
                for (Message message : messages) message.setFlag(Flags.Flag.DELETED, true);
                source.expunge();
            }
        } catch (MessagingException moveError) {
            source.copyMessages(messages, destination);
            for (Message message : messages) message.setFlag(Flags.Flag.DELETED, true);
            source.expunge();
        }
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
                for (String fragment : fragments) if (name.endsWith(fragment) || name.contains(fragment)) return folder;
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
        properties.put("mail.imaps.connectiontimeout", String.valueOf(IMAP_CONNECT_TIMEOUT_MS));
        properties.put("mail.imaps.timeout", String.valueOf(IMAP_READ_TIMEOUT_MS));
        properties.put("mail.imaps.writetimeout", String.valueOf(IMAP_WRITE_TIMEOUT_MS));
        properties.put("mail.imaps.connectionpoolsize", "1");
        return Session.getInstance(properties);
    }

    private static void addRecipients(MimeMessage message, Message.RecipientType type, String raw)
            throws MessagingException {
        if (raw != null && !raw.trim().isEmpty()) {
            message.setRecipients(type, InternetAddress.parse(raw.trim(), true));
        }
    }

    private static void setUserFlag(Message message, String flag, boolean value) {
        try { message.setFlags(new Flags(flag), value); } catch (Exception ignored) {}
    }

    private static String addresses(Address[] addresses) {
        if (addresses == null) return "";
        StringBuilder out = new StringBuilder();
        for (Address address : addresses) {
            if (out.length() > 0) out.append(", ");
            out.append(address);
        }
        return out.toString();
    }

    private static String fullHeaders(Message message) {
        StringBuilder out = new StringBuilder();
        try {
            Enumeration<?> headers = message.getAllHeaders();
            while (headers.hasMoreElements()) {
                Header header = (Header) headers.nextElement();
                out.append(header.getName()).append(": ").append(header.getValue()).append("\n");
            }
        } catch (Exception ignored) {}
        return out.toString();
    }

    private static String extractBestBody(Part part) throws Exception {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part.getFileName() != null) return "";
        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            return content == null ? "" : content.toString();
        }
        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            String plain = content == null ? "" : content.toString();
            return "<div style='white-space:pre-wrap'>" + TextUtils.htmlEncode(plain) + "</div>";
        }
        if (part.isMimeType("multipart/alternative")) {
            Multipart multipart = (Multipart) part.getContent();
            String fallback = "";
            for (int i = 0; i < multipart.getCount(); i++) {
                Part child = multipart.getBodyPart(i);
                if (child.isMimeType("text/html")) return extractBestBody(child);
                if (fallback.isEmpty() && child.isMimeType("text/plain")) fallback = extractBestBody(child);
            }
            return fallback;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                String child = extractBestBody(multipart.getBodyPart(i));
                if (!child.isEmpty()) {
                    if (out.length() > 0) out.append("<hr>");
                    out.append(child);
                }
            }
            return out.toString();
        }
        return "";
    }

    private static String toPlainText(String html) {
        return html == null || html.isEmpty() ? ""
                : Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
    }

    private static void closeQuietly(Folder folder, Store store) {
        try { if (folder != null && folder.isOpen()) folder.close(false); } catch (Exception ignored) {}
        try { if (store != null && store.isConnected()) store.close(); } catch (Exception ignored) {}
    }

    static String safe(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName() : message;
    }
}
