package au.com.t1xperts.mailxperts;

import android.content.Context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/** Serialises lifecycle-independent server Draft updates while local data remains authoritative. */
final class DraftSyncDispatcher {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mailxperts-draft-sync");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final ConcurrentHashMap<Long, Long> LATEST = new ConcurrentHashMap<>();

    private DraftSyncDispatcher() {}

    static void enqueue(Context context, long localId, long expectedUpdatedAt) {
        if (localId <= 0L) return;
        Context application = context.getApplicationContext();
        long generation = GENERATION.incrementAndGet();
        LATEST.put(localId, generation);
        EXECUTOR.execute(() -> sync(application, localId, expectedUpdatedAt, generation));
    }

    static void cancel(long localId) {
        if (localId > 0L) LATEST.remove(localId);
    }

    private static void sync(Context context, long localId, long expectedUpdatedAt, long generation) {
        if (!isCurrent(localId, generation)) return;
        LocalStore db = new LocalStore(context);
        try {
            LocalStore.LocalMessage message = db.get(localId);
            if (message == null || message.updatedAt != expectedUpdatedAt
                    || !LocalStore.DRAFT.equals(message.type)) return;
            AccountConfig account = new SecureStore(context).load(message.accountId);
            if (!message.accountId.equals(account.id) || !account.isUsable()
                    || !account.syncDraftsToServer) return;

            long newUid = MailRepository.saveServerDraft(account, message);
            if (!isCurrent(localId, generation)) {
                if (newUid > 0L) deleteQuietly(account, newUid);
                return;
            }
            LocalStore.LocalMessage latest = db.get(localId);
            if (latest == null || latest.updatedAt != expectedUpdatedAt
                    || !LocalStore.DRAFT.equals(latest.type)) {
                if (newUid > 0L) deleteQuietly(account, newUid);
                return;
            }
            long resolvedUid = newUid > 0L ? newUid : latest.serverUid;
            if (!db.updateDraftSyncResult(localId, expectedUpdatedAt, resolvedUid, "")
                    && newUid > 0L) {
                deleteQuietly(account, newUid);
            }
        } catch (Exception error) {
            LocalStore.LocalMessage latest = db.get(localId);
            if (isCurrent(localId, generation) && latest != null
                    && latest.updatedAt == expectedUpdatedAt) {
                db.updateDraftSyncResult(localId, expectedUpdatedAt, latest.serverUid,
                        "Server Drafts sync: " + MailRepository.safe(error));
            }
        } finally {
            LATEST.remove(localId, generation);
            db.close();
        }
    }

    private static boolean isCurrent(long localId, long generation) {
        Long current = LATEST.get(localId);
        return current != null && current == generation;
    }

    private static void deleteQuietly(AccountConfig account, long uid) {
        try { MailRepository.deleteServerDraft(account, uid); }
        catch (Exception ignored) {}
    }
}
