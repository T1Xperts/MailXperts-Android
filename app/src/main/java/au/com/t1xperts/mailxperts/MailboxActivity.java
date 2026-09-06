package au.com.t1xperts.mailxperts;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MailboxActivity extends Activity {
    private static final int CHOICE_ALL = 0;
    private static final int CHOICE_ACCOUNT = 1;
    private static final int CHOICE_ADD = 2;
    private static final int CHOICE_MANAGE = 3;

    private String accountId;
    private AccountConfig account;
    private boolean allAccounts;
    private SecureStore store;
    private LocalStore local;
    private Button drafts;
    private Button outbox;
    private Button scheduled;
    private EditText search;
    private List<AccountConfig> accounts;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        store = new SecureStore(this);
        accounts = MailboxScope.usable(store);
        if (accounts.isEmpty()) {
            startActivity(new Intent(this, AccountsActivity.class));
            finish();
            return;
        }

        String requested = getIntent().getStringExtra("account_id");
        if (requested == null || requested.isEmpty()) requested = store.getSelectedId();
        allAccounts = MailboxScope.isAll(requested);
        account = allAccounts ? null : MailboxScope.find(accounts, requested);
        if (!allAccounts && account == null) {
            allAccounts = accounts.size() > 1;
            account = allAccounts ? null : accounts.get(0);
        }
        accountId = allAccounts ? MailboxScope.ALL_ACCOUNTS : account.id;
        store.setSelectedId(accountId);
        local = new LocalStore(this);
        for (AccountConfig configured : accounts) NotificationScheduler.update(this, configured);
        requestNotificationPermission();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = Ui.vertical(this);
        scroll.addView(root);
        root.addView(buildHeader());

        String subtitleText = allAccounts
                ? "All Accounts  •  " + accounts.size() + " connected  •  Unified mailboxes"
                : ProviderPreset.find(account.provider).name + "  •  Secure IMAP/SMTP";
        TextView subtitle = Ui.text(this, subtitleText);
        subtitle.setTextColor(Ui.muted(this));
        root.addView(subtitle);
        root.addView(buildSearch());

        LinearLayout server = Ui.card(this);
        server.addView(Ui.label(this, allAccounts
                ? "UNIFIED SERVER MAILBOXES — CACHE-FIRST • UP TO 1,000 PER ACCOUNT"
                : "SERVER MAILBOXES — CACHE-FIRST • UP TO 1,000 MESSAGES"));
        server.addView(Ui.secondaryButton(this,
                allAccounts ? "Unified Inbox — all received email" : "Inbox — received email",
                v -> openServer(MailRepository.INBOX)));
        server.addView(Ui.secondaryButton(this,
                allAccounts ? "Unified Sent — all sent email" : "Sent — sent email",
                v -> openServer(MailRepository.SENT)));
        server.addView(Ui.secondaryButton(this,
                allAccounts ? "Unified Spam / Junk — all accounts" : "Spam / Junk — provider folder",
                v -> openServer(MailRepository.JUNK)));
        server.addView(Ui.secondaryButton(this,
                allAccounts ? "★ Unified Smart Priority — bills, finance & security"
                        : "★ Smart Priority — bills, finance & security",
                v -> openPriority()));
        root.addView(server);

        LinearLayout localCard = Ui.card(this);
        localCard.addView(Ui.label(this, allAccounts ? "UNIFIED LOCAL & SCHEDULED" : "LOCAL & SCHEDULED"));
        drafts = Ui.secondaryButton(this, "Drafts", v -> openLocal(LocalStore.DRAFT));
        outbox = Ui.secondaryButton(this, "Outbox — manual retry", v -> openLocal(LocalStore.OUTBOX));
        scheduled = Ui.secondaryButton(this, "Scheduled & recurring", v -> openLocal(LocalStore.SCHEDULED));
        localCard.addView(drafts);
        localCard.addView(outbox);
        localCard.addView(scheduled);
        root.addView(localCard);

        Button compose = Ui.button(this, "✎  Compose email");
        compose.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra("account_id", accountId);
            startActivity(intent);
        });
        root.addView(compose);
        TextView version = Ui.text(this, "MailXperts v1.5.1  •  Powered by T1Xperts");
        version.setTextColor(Ui.muted(this));
        version.setTextSize(12);
        version.setGravity(android.view.Gravity.CENTER);
        version.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        root.addView(version);
        Ui.setContentView(this, scroll);
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button menu = Ui.compactButton(this, "☰");
        menu.setContentDescription("Open navigation menu");
        menu.setOnClickListener(v -> Navigation.show(this, menu, accountId));
        header.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, "MailXperts");
        title.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 8), 0);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Spinner spinner = accountSpinner();
        header.addView(spinner, new LinearLayout.LayoutParams(Ui.dp(this, 168), Ui.dp(this, 52)));
        return header;
    }

    private View buildSearch() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        search = Ui.edit(this, allAccounts ? "Search all Inbox and Sent mail" : "Search Inbox and Sent");
        search.setSingleLine(true);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                openSearch();
                return true;
            }
            return false;
        });
        row.addView(search, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        Button go = Ui.compactButton(this, "⌕");
        go.setContentDescription("Search email");
        go.setOnClickListener(v -> openSearch());
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 52));
        goParams.setMargins(Ui.dp(this, 8), 0, 0, Ui.dp(this, 8));
        row.addView(go, goParams);
        return row;
    }

    private Spinner accountSpinner() {
        ArrayList<AccountChoice> choices = new ArrayList<>();
        choices.add(new AccountChoice(CHOICE_ALL, null, "All Accounts"));
        int selectedIndex = allAccounts ? 0 : 1;
        for (int i = 0; i < accounts.size(); i++) {
            AccountConfig configured = accounts.get(i);
            choices.add(new AccountChoice(CHOICE_ACCOUNT, configured, configured.displayName()));
            if (!allAccounts && configured.id.equals(accountId)) selectedIndex = i + 1;
        }
        choices.add(new AccountChoice(CHOICE_ADD, null, "＋ Add new mail account"));
        choices.add(new AccountChoice(CHOICE_MANAGE, null, "Manage accounts"));
        final int activeIndex = selectedIndex;

        ArrayAdapter<AccountChoice> adapter = new ArrayAdapter<AccountChoice>(
                this, android.R.layout.simple_spinner_item, choices) {
            private TextView decorate(View view, int position, boolean dropdown) {
                TextView text = (TextView) view;
                text.setTextColor(Ui.textColor(MailboxActivity.this));
                text.setTextSize(dropdown ? 15 : 13);
                text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                int vertical = dropdown ? Ui.dp(MailboxActivity.this, 15) : 0;
                text.setPadding(Ui.dp(MailboxActivity.this, 10), vertical,
                        Ui.dp(MailboxActivity.this, 8), vertical);
                if (dropdown) text.setBackgroundColor(Ui.panel(MailboxActivity.this));
                return text;
            }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getView(position, convertView, parent), position, false);
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getDropDownView(position, convertView, parent), position, true);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIndex, false);
        spinner.setPopupBackgroundResource(android.R.color.transparent);
        final boolean[] ready = {false};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!ready[0]) return;
                AccountChoice choice = choices.get(position);
                if (choice.type == CHOICE_ADD) {
                    spinner.setSelection(activeIndex, false);
                    startActivity(new Intent(MailboxActivity.this, SettingsActivity.class));
                    return;
                }
                if (choice.type == CHOICE_MANAGE) {
                    spinner.setSelection(activeIndex, false);
                    startActivity(new Intent(MailboxActivity.this, AccountsActivity.class));
                    return;
                }
                String destination = choice.type == CHOICE_ALL
                        ? MailboxScope.ALL_ACCOUNTS : choice.account.id;
                if (destination.equals(accountId)) return;
                store.setSelectedId(destination);
                Intent intent = new Intent(MailboxActivity.this, MailboxActivity.class);
                intent.putExtra("account_id", destination);
                startActivity(intent);
                finish();
            }
        });
        spinner.post(() -> ready[0] = true);
        return spinner;
    }

    @Override protected void onResume() {
        super.onResume();
        if (drafts != null) {
            drafts.setText((allAccounts ? "Unified Drafts (" : "Drafts (")
                    + local.count(accountId, LocalStore.DRAFT) + ")");
            outbox.setText((allAccounts ? "Unified Outbox — manual retry (" : "Outbox — manual retry (")
                    + local.count(accountId, LocalStore.OUTBOX) + ")");
            scheduled.setText((allAccounts ? "Unified Scheduled & recurring (" : "Scheduled & recurring (")
                    + local.count(accountId, LocalStore.SCHEDULED) + ")");
        }
    }

    private void openSearch() {
        String query = search.getText().toString().trim();
        if (query.isEmpty()) { search.setError("Enter a word, address or phrase"); return; }
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("query", query);
        startActivity(intent);
    }

    private void openServer(String kind) {
        Intent intent = new Intent(this, InboxActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("folder_kind", kind);
        startActivity(intent);
    }

    private void openPriority() {
        Intent intent = new Intent(this, InboxActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("folder_kind", MailRepository.INBOX);
        intent.putExtra("smart_only", true);
        startActivity(intent);
    }

    private void openLocal(String type) {
        Intent intent = new Intent(this, LocalFolderActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("local_type", type);
        startActivity(intent);
    }

    private void requestNotificationPermission() {
        boolean enabled = false;
        for (AccountConfig configured : accounts) {
            if (configured.notificationsEnabled) { enabled = true; break; }
        }
        if (Build.VERSION.SDK_INT >= 33 && enabled
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1203);
        }
    }

    private static final class AccountChoice {
        final int type;
        final AccountConfig account;
        final String label;
        AccountChoice(int type, AccountConfig account, String label) {
            this.type = type;
            this.account = account;
            this.label = label;
        }
        @Override public String toString() { return label; }
    }
}
