package au.com.t1xperts.mailxperts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class RescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        NotificationScheduler.updateAll(context);
        LocalStore local = new LocalStore(context);
        List<AccountConfig> accounts = new SecureStore(context).loadAll();
        for (AccountConfig account : accounts) {
            List<LocalStore.LocalMessage> scheduled = local.list(account.id, LocalStore.SCHEDULED);
            for (LocalStore.LocalMessage message : scheduled) Scheduler.schedule(context, message);
        }
    }
}
