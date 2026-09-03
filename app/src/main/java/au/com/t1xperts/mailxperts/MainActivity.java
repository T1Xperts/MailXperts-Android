package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        SecureStore store;
        try {
            store = new SecureStore(this);
        } catch (Throwable ignored) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
            return;
        }
        try {
            NotificationScheduler.updateAll(this);
        } catch (Throwable ignored) {
            // Background scheduling is optional and must not block startup.
        }
        Intent intent;
        if (store.hasAccounts()) {
            intent = new Intent(this, MailboxActivity.class);
            String selected = store.getSelectedId();
            if (selected == null || selected.isEmpty()) {
                selected = store.loadAll().size() > 1
                        ? MailboxScope.ALL_ACCOUNTS : store.loadAll().get(0).id;
            }
            intent.putExtra("account_id", selected);
        } else {
            intent = new Intent(this, SettingsActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
