package au.com.t1xperts.mailxperts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduledEmailReceiver extends BroadcastReceiver {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override public void onReceive(final Context context, Intent intent) {
        final long id = intent == null ? -1L : intent.getLongExtra("local_id", -1L);
        if (id < 0L) return;
        final PendingResult pending = goAsync();
        EXECUTOR.execute(new Runnable() {
            @Override public void run() {
                try { send(context.getApplicationContext(), id); }
                finally { pending.finish(); }
            }
        });
    }

    private static void send(Context context, long id) {
        LocalStore db = new LocalStore(context);
        try {
            LocalStore.LocalMessage message = db.get(id);
            if (message == null || !LocalStore.SCHEDULED.equals(message.type)) return;
            AccountConfig account = new SecureStore(context).load(message.accountId);
            ArrayList<AttachmentRef> attachments = LocalAttachmentStore.load(context, message.id);
            if (!message.accountId.equals(account.id) || !account.isUsable()) {
                copyToOutbox(context, db, message, attachments,
                        "Scheduled send failed: account credentials are unavailable.");
                finishOccurrence(context, db, message, false);
                return;
            }
            boolean sent = false;
            try {
                message.to = RecipientNormalizer.normalise(message.to);
                message.cc = RecipientNormalizer.normalise(message.cc);
                message.bcc = RecipientNormalizer.normalise(message.bcc);
                MailAttachmentRepository.sendHtmlWithAttachments(account,
                        message.to, message.cc, message.bcc,
                        message.subject, message.html, attachments);
                sent = true;
            } catch (Exception error) {
                copyToOutbox(context, db, message, attachments,
                        "Scheduled send failed: " + MailRepository.safe(error));
            }
            if (sent && message.serverUid > 0L) {
                try {
                    MailRepository.deleteServerDraft(account, message.serverUid);
                    message.serverUid = 0L;
                } catch (Exception ignored) {
                    // Sending succeeded; a Draft-cleanup failure must never cause a duplicate send.
                }
            }
            finishOccurrence(context, db, message, sent);
        } finally {
            db.close();
        }
    }

    private static void copyToOutbox(Context context, LocalStore db,
                                     LocalStore.LocalMessage source,
                                     ArrayList<AttachmentRef> attachments, String error) {
        LocalStore.LocalMessage outbox = new LocalStore.LocalMessage();
        outbox.accountId = source.accountId;
        outbox.type = LocalStore.OUTBOX;
        outbox.to = source.to;
        outbox.cc = source.cc;
        outbox.bcc = source.bcc;
        outbox.subject = source.subject;
        outbox.html = source.html;
        outbox.serverUid = source.serverUid;
        outbox.lastError = error;
        db.save(outbox);
        LocalAttachmentStore.save(context, outbox.id, attachments);
    }

    private static void finishOccurrence(Context context, LocalStore db,
                                         LocalStore.LocalMessage message, boolean sent) {
        long next = Scheduler.nextRun(message.scheduledAt, message.recurrence);
        if (next <= 0L) {
            db.delete(message.id);
            LocalAttachmentStore.delete(context, message.id, sent);
            return;
        }
        message.scheduledAt = next;
        message.lastError = "";
        db.save(message);
        Scheduler.schedule(context, message);
    }
}
