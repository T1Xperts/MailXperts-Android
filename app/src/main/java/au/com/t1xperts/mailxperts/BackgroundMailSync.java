package au.com.t1xperts.mailxperts;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.List;

/** Incrementally refreshes the local inbox cache without loading message bodies. */
final class BackgroundMailSync {
    private static final String CHANNEL = "mailxperts_new_mail";

    private BackgroundMailSync() {}

    static void run(Context context, String accountId, MailRepository.SyncToken token)
            throws Exception {
        AccountConfig account = new SecureStore(context).load(accountId);
        if (!accountId.equals(account.id) || !account.isUsable() || !account.syncEnabled
                || SyncPolicy.normalizeInterval(account.syncIntervalMinutes) == SyncPolicy.MANUAL) {
            return;
        }

        LocalStore local = new LocalStore(context);
        try {
            LocalStore.SyncState previous = local.getSyncState(account.id, MailRepository.INBOX);
            LocalStore.CacheStats before = local.cacheStats(account.id, MailRepository.INBOX);
            NewMailCollector collector = new NewMailCollector(before.newestUid,
                    previous.lastSuccessfulSyncAt > 0L);
            MailRepository.SyncRequest request = new MailRepository.SyncRequest(
                    previous.requestedLimit, before.count, before.newestUid, before.oldestUid,
                    previous.uidValidity, false, SyncPlanner.PERIODIC_BACKFILL_BUDGET);
            MailRepository.SyncResult result = MailRepository.syncFolder(
                    account, MailRepository.INBOX, request, new MailRepository.SyncObserver() {
                        @Override public void onMailboxOpened(
                                long uidValidity, boolean cacheMustReset, int serverMessageCount) {
                            if (cacheMustReset) {
                                local.clearCached(account.id, MailRepository.INBOX);
                                collector.suppressNotification();
                            }
                            local.saveSyncState(account.id, MailRepository.INBOX,
                                    previous.requestedLimit, uidValidity,
                                    previous.lastSuccessfulSyncAt, serverMessageCount);
                        }

                        @Override public void onBatch(List<MailRepository.Summary> batch,
                                                      int processed, int expected, String phase) {
                            local.replaceCachedRange(account.id, MailRepository.INBOX, batch);
                            collector.accept(batch, local, account.id);
                        }
                    }, token);
            local.trimCached(account.id, MailRepository.INBOX, previous.requestedLimit);
            local.saveSyncState(account.id, MailRepository.INBOX, previous.requestedLimit,
                    result.uidValidity, System.currentTimeMillis(), result.serverMessageCount);
            if (account.notificationsEnabled && collector.count > 0 && collector.newest != null) {
                notifyUser(context, account, collector.newest, collector.count);
            }
        } finally {
            local.close();
        }
    }

    private static void notifyUser(Context context, AccountConfig account,
                                   MailRepository.Summary first, int count) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "New MailXperts email", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Notifications for newly received email");
        manager.createNotificationChannel(channel);

        Intent open = new Intent(context, InboxActivity.class);
        open.putExtra("account_id", account.id);
        open.putExtra("folder_kind", MailRepository.INBOX);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent action = PendingIntent.getActivity(context, account.id.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String subject = first.subject == null || first.subject.trim().isEmpty()
                ? "(No subject)" : first.subject;
        String title = count == 1 ? "New email — " + account.displayName()
                : count + " new emails — " + account.displayName();
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(subject)
                .setStyle(new Notification.BigTextStyle().bigText(subject + "\n" + first.from))
                .setContentIntent(action)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        manager.notify(account.id.hashCode(), notification);
    }

    private static final class NewMailCollector {
        final long previousNewestUid;
        boolean enabled;
        int count;
        MailRepository.Summary newest;

        NewMailCollector(long previousNewestUid, boolean enabled) {
            this.previousNewestUid = previousNewestUid;
            this.enabled = enabled && previousNewestUid > 0L;
        }

        void suppressNotification() {
            enabled = false;
            count = 0;
            newest = null;
        }

        void accept(List<MailRepository.Summary> batch, LocalStore local, String accountId) {
            if (!enabled) return;
            for (MailRepository.Summary message : batch) {
                if (message.uid <= previousNewestUid) continue;
                if (local.isHidden(accountId, MailRepository.INBOX, message.uid)) continue;
                count++;
                if (newest == null || message.uid > newest.uid) newest = message;
            }
        }
    }
}
