package au.com.t1xperts.mailxperts;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailSyncReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "mailxperts_new_mail";
    private static final String PREFS = "mailxperts_notification_state_v1";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override public void onReceive(final Context context, Intent intent) {
        final String accountId = intent == null ? null : intent.getStringExtra("account_id");
        if (accountId == null) return;
        final PendingResult pending = goAsync();
        EXECUTOR.execute(new Runnable() {
            @Override public void run() {
                try { sync(context.getApplicationContext(), accountId); }
                finally { pending.finish(); }
            }
        });
    }

    private static void sync(Context context, String accountId) {
        AccountConfig account = new SecureStore(context).load(accountId);
        if (!accountId.equals(account.id) || !account.isUsable() || !account.syncEnabled || !account.notificationsEnabled) return;
        try {
            List<MailRepository.Summary> messages = MailRepository.fetchFolder(account, MailRepository.INBOX, 25);
            if (messages.isEmpty()) return;
            SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long previous = preferences.getLong("uid_" + account.id, -1L);
            long newest = messages.get(0).uid;
            preferences.edit().putLong("uid_" + account.id, newest).apply();
            if (previous < 0L || newest == previous) return;
            int count = 0;
            MailRepository.Summary first = messages.get(0);
            for (MailRepository.Summary message : messages) {
                if (message.uid == previous) break;
                count++;
            }
            if (count > 0) notifyUser(context, account, first, count);
        } catch (Exception ignored) {}
    }

    private static void notifyUser(Context context, AccountConfig account, MailRepository.Summary first, int count) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "New MailXperts email", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications for newly received email");
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, InboxActivity.class);
        open.putExtra("account_id", account.id);
        open.putExtra("folder_kind", MailRepository.INBOX);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent action = PendingIntent.getActivity(context, account.id.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String subject = first.subject == null || first.subject.trim().isEmpty() ? "(No subject)" : first.subject;
        String title = count == 1 ? "New email — " + account.displayName() : count + " new emails — " + account.displayName();
        Notification.BigTextStyle style = new Notification.BigTextStyle().bigText(subject + "\n" + first.from);
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(subject)
                .setStyle(style)
                .setContentIntent(action)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        manager.notify(account.id.hashCode(), notification);
    }
}
