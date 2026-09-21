package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends Activity {
    private static final String[] FOLDER_LABELS = {
            "Current folder", "Inbox", "Sent", "Spam / Junk", "All server folders",
            "Drafts", "Outbox", "Scheduled", "All folders"
    };
    private static final String[] FOLDER_VALUES = {
            MailSearchSpec.SCOPE_CURRENT, MailSearchSpec.SCOPE_INBOX, MailSearchSpec.SCOPE_SENT,
            MailSearchSpec.SCOPE_JUNK, MailSearchSpec.SCOPE_ALL_SERVER,
            MailSearchSpec.SCOPE_DRAFTS, MailSearchSpec.SCOPE_OUTBOX,
            MailSearchSpec.SCOPE_SCHEDULED, MailSearchSpec.SCOPE_ALL
    };
    private static final String[] MODE_LABELS = {"All words", "Any word", "Exact phrase"};
    private static final String[] MODE_VALUES = {
            MailSearchSpec.MODE_ALL, MailSearchSpec.MODE_ANY, MailSearchSpec.MODE_EXACT
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<SearchHit> results = new ArrayList<>();
    private final ArrayList<AccountConfig> availableAccounts = new ArrayList<>();
    private String initialAccountId;
    private String currentFolder;
    private EditText query;
    private Spinner accountScope;
    private Spinner folderScope;
    private Spinner matchMode;
    private ProgressBar progress;
    private TextView status;
    private ResultAdapter adapter;
    private LocalStore localStore;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        initialAccountId = getIntent().getStringExtra("account_id");
        currentFolder = getIntent().getStringExtra("folder_kind");
        if (currentFolder == null || currentFolder.isEmpty()) currentFolder = MailRepository.INBOX;

        SecureStore secureStore = new SecureStore(this);
        availableAccounts.addAll(MailboxScope.usable(secureStore));
        if (availableAccounts.isEmpty()) { finish(); return; }
        localStore = new LocalStore(this);

        LinearLayout root = Ui.vertical(this);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button menu = Ui.compactButton(this, "☰");
        menu.setContentDescription("Open navigation menu");
        menu.setOnClickListener(v -> Navigation.show(this, menu, initialAccountId));
        header.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, "Search email");
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        query = Ui.edit(this, "Search sender, recipient, subject, body or local attachment");
        String initialQuery = getIntent().getStringExtra("query");
        query.setText(initialQuery == null ? "" : initialQuery);
        query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        query.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                runSearch();
                return true;
            }
            return false;
        });
        searchRow.addView(query, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        Button go = Ui.compactButton(this, "Search");
        go.setOnClickListener(v -> runSearch());
        LinearLayout.LayoutParams goParams =
                new LinearLayout.LayoutParams(Ui.dp(this, 92), Ui.dp(this, 52));
        goParams.setMargins(Ui.dp(this, 8), 0, 0, Ui.dp(this, 8));
        searchRow.addView(go, goParams);
        root.addView(searchRow);

        LinearLayout options = Ui.card(this);
        options.addView(Ui.label(this, "ACCOUNT SCOPE"));
        accountScope = new Spinner(this);
        ArrayList<String> accountLabels = new ArrayList<>();
        accountLabels.add("All Accounts");
        int selectedAccount = 0;
        for (int i = 0; i < availableAccounts.size(); i++) {
            AccountConfig account = availableAccounts.get(i);
            accountLabels.add(account.displayName() + " • " + account.email);
            if (!MailboxScope.isAll(initialAccountId) && account.id.equals(initialAccountId)) {
                selectedAccount = i + 1;
            }
        }
        accountScope.setAdapter(spinnerAdapter(accountLabels));
        accountScope.setSelection(selectedAccount, false);
        options.addView(accountScope);

        options.addView(Ui.label(this, "FOLDER SCOPE"));
        folderScope = new Spinner(this);
        folderScope.setAdapter(spinnerAdapter(java.util.Arrays.asList(FOLDER_LABELS)));
        folderScope.setSelection(MailboxScope.isAll(initialAccountId) ? 4 : 0, false);
        options.addView(folderScope);

        options.addView(Ui.label(this, "MATCHING"));
        matchMode = new Spinner(this);
        matchMode.setAdapter(spinnerAdapter(java.util.Arrays.asList(MODE_LABELS)));
        matchMode.setSelection(0, false);
        options.addView(matchMode);
        root.addView(options);

        status = Ui.text(this,
                "Search is case-insensitive. *wildcards are treated as practical partial matches.");
        status.setTextColor(Ui.muted(this));
        root.addView(status);
        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress);

        ListView list = new ListView(this);
        list.setDividerHeight(0);
        adapter = new ResultAdapter(results);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> open(results.get(position)));
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, root);

        if (query.getText().toString().trim().length() > 0) runSearch();
    }

    private ArrayAdapter<String> spinnerAdapter(List<String> labels) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, labels) {
            private TextView decorate(View view, boolean dropdown) {
                TextView text = (TextView) view;
                text.setTextColor(Ui.textColor(SearchActivity.this));
                text.setTextSize(14);
                text.setPadding(Ui.dp(SearchActivity.this, 12),
                        Ui.dp(SearchActivity.this, dropdown ? 13 : 9),
                        Ui.dp(SearchActivity.this, 12),
                        Ui.dp(SearchActivity.this, dropdown ? 13 : 9));
                if (dropdown) text.setBackgroundColor(Ui.panel(SearchActivity.this));
                return text;
            }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getView(position, convertView, parent), false);
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return decorate(super.getDropDownView(position, convertView, parent), true);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void runSearch() {
        String text = query.getText().toString().trim();
        if (text.isEmpty()) { query.setError("Enter a search term"); return; }

        int accountIndex = Math.max(0, accountScope.getSelectedItemPosition());
        String folderValue = FOLDER_VALUES[Math.max(0, folderScope.getSelectedItemPosition())];
        String modeValue = MODE_VALUES[Math.max(0, matchMode.getSelectedItemPosition())];
        MailSearchSpec spec = new MailSearchSpec(text, modeValue, folderValue, currentFolder);
        ArrayList<AccountConfig> targets = new ArrayList<>();
        if (accountIndex == 0) targets.addAll(availableAccounts);
        else targets.add(availableAccounts.get(accountIndex - 1));

        progress.setVisibility(View.VISIBLE);
        status.setText("Searching " + targets.size() + " account(s)…");
        executor.execute(() -> {
            ArrayList<SearchHit> found = new ArrayList<>();
            ArrayList<String> errors = new ArrayList<>();
            int successfulServerAccounts = 0;

            if (!spec.serverFolders().isEmpty()) {
                for (AccountConfig account : targets) {
                    try {
                        for (MailRepository.Summary summary :
                                MailRepository.search(account, spec, 500)) {
                            found.add(SearchHit.server(summary));
                        }
                        successfulServerAccounts++;
                    } catch (Exception error) {
                        errors.add(account.displayName() + ": " + MailRepository.safe(error));
                    }
                }
            }

            for (String localType : spec.localFolders()) {
                String scopeId = accountIndex == 0
                        ? MailboxScope.ALL_ACCOUNTS : targets.get(0).id;
                for (LocalStore.LocalMessage message : localStore.list(scopeId, localType)) {
                    String attachmentNames = localAttachmentNames(message.id);
                    if (!spec.matchesLocal(message, attachmentNames)) continue;
                    AccountConfig account = findAccount(message.accountId);
                    found.add(SearchHit.local(message,
                            account == null ? "Account unavailable" : account.displayName()));
                }
            }

            found.sort((left, right) -> Long.compare(right.sortTime, left.sortTime));
            final int serverSuccesses = successfulServerAccounts;
            runOnUiThread(() -> {
                results.clear();
                results.addAll(found);
                adapter.notifyDataSetChanged();
                progress.setVisibility(View.GONE);
                String summary = found.size() + " matching message(s)";
                if (!spec.serverFolders().isEmpty()) {
                    summary += " • server " + serverSuccesses + "/" + targets.size() + " account(s)";
                }
                if (!spec.localFolders().isEmpty()) summary += " • local folders included";
                if (!errors.isEmpty()) summary += "\n" + errors.size() + " server search error(s)";
                status.setText(summary);
                if (serverSuccesses == 0 && !errors.isEmpty() && found.isEmpty()) {
                    Toast.makeText(this, errors.get(0), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private String localAttachmentNames(long localMessageId) {
        StringBuilder names = new StringBuilder();
        for (AttachmentRef attachment : LocalAttachmentStore.load(this, localMessageId)) {
            if (names.length() > 0) names.append(' ');
            names.append(attachment.name);
        }
        return names.toString();
    }

    private AccountConfig findAccount(String id) {
        if (id == null) return null;
        for (AccountConfig account : availableAccounts) {
            if (id.equals(account.id)) return account;
        }
        return null;
    }

    private void open(SearchHit hit) {
        if (hit.server != null) {
            Intent intent = new Intent(this, MessageActivity.class);
            intent.putExtra("account_id", hit.server.accountId);
            intent.putExtra("folder_kind", hit.server.folderKind);
            intent.putExtra("uid", hit.server.uid);
            startActivity(intent);
            return;
        }
        if (hit.local != null) {
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra("account_id", hit.local.accountId);
            intent.putExtra("local_id", hit.local.id);
            startActivity(intent);
        }
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        if (localStore != null) localStore.close();
        super.onDestroy();
    }

    private static String folderLabel(String kind) {
        if (MailRepository.SENT.equals(kind)) return "SENT";
        if (MailRepository.JUNK.equals(kind)) return "SPAM / JUNK";
        if (LocalStore.DRAFT.equals(kind)) return "DRAFTS";
        if (LocalStore.OUTBOX.equals(kind)) return "OUTBOX";
        if (LocalStore.SCHEDULED.equals(kind)) return "SCHEDULED";
        return "INBOX";
    }

    private final class ResultAdapter extends BaseAdapter {
        private final List<SearchHit> items;
        ResultAdapter(List<SearchHit> items) { this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return items.get(position).stableId; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            SearchHit hit = items.get(position);
            LinearLayout card = Ui.card(SearchActivity.this);
            TextView scope = Ui.label(SearchActivity.this,
                    folderLabel(hit.folder) + " • " + hit.accountLabel);
            scope.setTextColor(Ui.teal(SearchActivity.this));
            card.addView(scope);

            TextView subject = Ui.text(SearchActivity.this,
                    hit.subject.isEmpty() ? "(No subject)" : hit.subject);
            subject.setTextSize(17);
            subject.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(subject);

            if (!hit.correspondent.isEmpty()) {
                TextView correspondent = Ui.text(SearchActivity.this, hit.correspondent);
                correspondent.setTextColor(Ui.muted(SearchActivity.this));
                card.addView(correspondent);
            }
            if (hit.date != null) {
                TextView date = Ui.text(SearchActivity.this,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(hit.date));
                date.setTextColor(Ui.muted(SearchActivity.this));
                date.setTextSize(12);
                card.addView(date);
            }
            return card;
        }
    }

    private static final class SearchHit {
        final long stableId;
        final String accountLabel;
        final String folder;
        final String subject;
        final String correspondent;
        final Date date;
        final long sortTime;
        final MailRepository.Summary server;
        final LocalStore.LocalMessage local;

        private SearchHit(long stableId, String accountLabel, String folder, String subject,
                          String correspondent, Date date, MailRepository.Summary server,
                          LocalStore.LocalMessage local) {
            this.stableId = stableId;
            this.accountLabel = accountLabel == null ? "" : accountLabel;
            this.folder = folder == null ? "" : folder;
            this.subject = subject == null ? "" : subject;
            this.correspondent = correspondent == null ? "" : correspondent;
            this.date = date;
            this.sortTime = date == null ? 0L : date.getTime();
            this.server = server;
            this.local = local;
        }

        static SearchHit server(MailRepository.Summary summary) {
            long stable = (((long) summary.accountId.hashCode()) << 32) ^ summary.uid;
            return new SearchHit(stable, summary.accountLabel, summary.folderKind,
                    summary.subject, summary.from, summary.date, summary, null);
        }

        static SearchHit local(LocalStore.LocalMessage message, String accountLabel) {
            Date date = new Date(Math.max(message.updatedAt, message.createdAt));
            String correspondents = (message.to == null ? "" : "To: " + message.to);
            return new SearchHit(-Math.abs(message.id), accountLabel, message.type,
                    message.subject, correspondents, date, null, message);
        }
    }
}
