package au.com.t1xperts.mailxperts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.List;

final class NotificationScheduler {
    private static final long INTERVAL = 15L * 60L * 1000L;
    private NotificationScheduler() {}

    private static PendingIntent pending(Context context, String accountId, int flags) {
        Intent intent = new Intent(context, MailSyncReceiver.class);
        intent.putExtra("account_id", accountId);
        return PendingIntent.getBroadcast(context, accountId.hashCode(), intent, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static void cancel(Context context, String accountId) {
        if (accountId == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = pending(context, accountId, PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarms.cancel(pending);
            pending.cancel();
        }
    }

    static void update(Context context, AccountConfig account) {
        if (account == null || account.id == null || !account.syncEnabled || !account.notificationsEnabled || !account.isUsable()) {
            if (account != null) cancel(context, account.id);
            return;
        }
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = pending(context, account.id, PendingIntent.FLAG_UPDATE_CURRENT);
        alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL, INTERVAL, pending);
    }

    static void updateAll(Context context) {
        List<AccountConfig> accounts = new SecureStore(context).loadAll();
        for (AccountConfig account : accounts) update(context, account);
    }
}
