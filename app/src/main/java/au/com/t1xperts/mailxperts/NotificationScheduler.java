package au.com.t1xperts.mailxperts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;

import java.util.List;

/** Schedules battery-aware, network-constrained mailbox refresh jobs. */
final class NotificationScheduler {
    private static final long MINIMUM_FLEX_MS = 5L * 60L * 1_000L;
    private static final int JOB_NAMESPACE = 0x4D000000;

    private NotificationScheduler() {}

    private static int jobId(String accountId) {
        return JOB_NAMESPACE | (accountId.hashCode() & 0x00FF_FFFF);
    }

    private static PendingIntent legacyPending(Context context, String accountId, int flags) {
        Intent intent = new Intent(context, MailSyncReceiver.class);
        intent.putExtra("account_id", accountId);
        return PendingIntent.getBroadcast(context, accountId.hashCode(), intent,
                flags | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void cancelLegacyAlarm(Context context, String accountId) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = legacyPending(context, accountId, PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarms.cancel(pending);
            pending.cancel();
        }
    }

    static void cancel(Context context, String accountId) {
        if (accountId == null || accountId.isEmpty()) return;
        cancelLegacyAlarm(context, accountId);
        JobScheduler jobs = context.getSystemService(JobScheduler.class);
        if (jobs != null) jobs.cancel(jobId(accountId));
    }

    static boolean update(Context context, AccountConfig account) {
        if (account == null || account.id == null || account.id.isEmpty()) return false;
        try {
            cancelLegacyAlarm(context, account.id);
            JobScheduler jobs = context.getSystemService(JobScheduler.class);
            if (jobs == null) return false;

            int intervalMinutes = SyncPolicy.normalizeInterval(account.syncIntervalMinutes);
            if (!account.syncEnabled || intervalMinutes == SyncPolicy.MANUAL
                    || !account.isUsable()) {
                jobs.cancel(jobId(account.id));
                return true;
            }

            long interval = SyncPolicy.intervalMillis(intervalMinutes);
            long flex = Math.max(MINIMUM_FLEX_MS, interval / 10L);
            JobInfo existing = jobs.getPendingJob(jobId(account.id));
            if (existing != null && existing.isPeriodic()
                    && existing.getIntervalMillis() == interval
                    && existing.getFlexMillis() == Math.min(interval, flex)) {
                // Reopening the app must not reset an already-correct periodic schedule.
                return true;
            }
            PersistableBundle extras = new PersistableBundle();
            extras.putString("account_id", account.id);
            JobInfo job = new JobInfo.Builder(jobId(account.id),
                    new ComponentName(context, MailSyncJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setPeriodic(interval, Math.min(interval, flex))
                    .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .setExtras(extras)
                    .build();
            return jobs.schedule(job) == JobScheduler.RESULT_SUCCESS;
        } catch (RuntimeException schedulingError) {
            // Account persistence and manual refresh must survive an OEM scheduler failure.
            return false;
        }
    }

    static void updateAll(Context context) {
        List<AccountConfig> accounts = new SecureStore(context).loadAll();
        for (AccountConfig account : accounts) update(context, account);
    }
}
