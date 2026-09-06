package au.com.t1xperts.mailxperts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Compatibility receiver for alarms created by versions before v1.5. */
public class MailSyncReceiver extends BroadcastReceiver {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override public void onReceive(Context context, Intent intent) {
        String accountId = intent == null ? null : intent.getStringExtra("account_id");
        if (accountId == null || accountId.isEmpty()) return;
        PendingResult pending = goAsync();
        Context application = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                BackgroundMailSync.run(application, accountId, new MailRepository.SyncToken());
            } catch (Exception ignored) {
                // JobScheduler will handle future refreshes after the app migrates this account.
            } finally {
                pending.finish();
            }
        });
    }
}
