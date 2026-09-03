package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class AccountsActivity extends Activity {
    private SecureStore store;
    private final ArrayList<AccountConfig> accounts = new ArrayList<>();
    private AccountAdapter adapter;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        store = new SecureStore(this);
        LinearLayout root = Ui.vertical(this);
        root.addView(Ui.title(this, "MailXperts Accounts"));
        TextView note = Ui.text(this, "Add multiple secure IMAP/SMTP accounts. Credentials are encrypted by Android Keystore on this phone.");
        note.setTextColor(Ui.muted(this));
        root.addView(note);
        Button add = Ui.button(this, "+ Add email account");
        add.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        root.addView(add);
        Button unified = Ui.secondaryButton(this, "All Accounts — Unified mailboxes", v -> openAll());
        root.addView(unified);
        ListView list = new ListView(this);
        list.setDividerHeight(0);
        adapter = new AccountAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> open(accounts.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            AccountConfig account = accounts.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Remove account?")
                    .setMessage(account.email + "\nLocal drafts and Outbox items will remain until removed.")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        store.delete(account.id);
                        LocalStore local = new LocalStore(this);
                        try { local.clearServerDataForAccount(account.id); }
                        finally { local.close(); }
                        NotificationScheduler.cancel(this, account.id);
                        refresh();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(Ui.secondaryButton(this, "Appearance", v -> startActivity(new Intent(this, AppearanceActivity.class))));
        Ui.setContentView(this, root);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void open(AccountConfig account) {
        store.setSelectedId(account.id);
        Intent intent = new Intent(this, MailboxActivity.class);
        intent.putExtra("account_id", account.id);
        startActivity(intent);
        finish();
    }

    private void openAll() {
        store.setSelectedId(MailboxScope.ALL_ACCOUNTS);
        Intent intent = new Intent(this, MailboxActivity.class);
        intent.putExtra("account_id", MailboxScope.ALL_ACCOUNTS);
        startActivity(intent);
        finish();
    }

    private void refresh() {
        accounts.clear();
        accounts.addAll(store.loadAll());
        adapter.notifyDataSetChanged();
        if (accounts.isEmpty()) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        }
    }

    private final class AccountAdapter extends BaseAdapter {
        @Override public int getCount() { return accounts.size(); }
        @Override public Object getItem(int position) { return accounts.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout card = Ui.card(AccountsActivity.this);
            AccountConfig account = accounts.get(position);
            TextView name = Ui.text(AccountsActivity.this, account.displayName());
            name.setTextSize(17);
            name.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            card.addView(name);
            TextView email = Ui.text(AccountsActivity.this, account.email);
            email.setTextColor(Ui.muted(AccountsActivity.this));
            card.addView(email);
            return card;
        }
    }
}
