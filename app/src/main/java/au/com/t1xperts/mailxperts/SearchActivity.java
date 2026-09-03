package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<MailRepository.Summary> results = new ArrayList<>();
    private final ArrayList<AccountConfig> accounts = new ArrayList<>();
    private String accountId;
    private boolean allAccounts;
    private EditText query;
    private ProgressBar progress;
    private TextView status;
    private ResultAdapter adapter;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        accountId = getIntent().getStringExtra("account_id");
        SecureStore store = new SecureStore(this);
        List<AccountConfig> usable = MailboxScope.usable(store);
        allAccounts = MailboxScope.isAll(accountId);
        if (allAccounts) {
            accounts.addAll(usable);
        } else {
            AccountConfig account = MailboxScope.find(usable, accountId);
            if (account != null) accounts.add(account);
        }
        if (accounts.isEmpty()) { finish(); return; }

        LinearLayout root = Ui.vertical(this);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button menu = Ui.compactButton(this, "☰");
        menu.setContentDescription("Open navigation menu");
        menu.setOnClickListener(v -> Navigation.show(this, menu, accountId));
        header.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, allAccounts ? "Search all email" : "Search email");
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        query = Ui.edit(this, "Search subject, sender, recipient or message body");
        query.setText(getIntent().getStringExtra("query"));
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
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(Ui.dp(this, 92), Ui.dp(this, 52));
        goParams.setMargins(Ui.dp(this, 8), 0, 0, Ui.dp(this, 8));
        searchRow.addView(go, goParams);
        root.addView(searchRow);
        status = Ui.text(this, allAccounts
                ? "Search runs securely on Inbox and Sent across every connected account."
                : "Search runs securely on both Inbox and Sent.");
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
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, root);
        if (query.getText().toString().trim().length() > 0) runSearch();
    }

    private void runSearch() {
        String text = query.getText().toString().trim();
        if (text.isEmpty()) { query.setError("Enter a search term"); return; }
        progress.setVisibility(View.VISIBLE);
        status.setText(allAccounts ? "Searching all Inbox and Sent mailboxes…" : "Searching Inbox and Sent…");
        executor.execute(() -> {
            ArrayList<MailRepository.Summary> found = new ArrayList<>();
            ArrayList<String> errors = new ArrayList<>();
            int successfulAccounts = 0;
            for (AccountConfig account : accounts) {
                try {
                    found.addAll(MailRepository.search(account, text, 500));
                    successfulAccounts++;
                } catch (Exception error) {
                    errors.add(account.displayName() + ": " + MailRepository.safe(error));
                }
            }
            found.sort((left, right) -> Long.compare(
                    right.date == null ? 0L : right.date.getTime(),
                    left.date == null ? 0L : left.date.getTime()));
            final int successCount = successfulAccounts;
            runOnUiThread(() -> {
                results.clear();
                results.addAll(found);
                adapter.notifyDataSetChanged();
                progress.setVisibility(View.GONE);
                String summary = found.size() + " matching messages";
                if (allAccounts) summary += " • " + successCount + "/" + accounts.size() + " accounts";
                if (!errors.isEmpty()) summary += "\n" + errors.size() + " account(s) could not be searched";
                status.setText(summary);
                if (successCount == 0 && !errors.isEmpty()) {
                    Toast.makeText(this, errors.get(0), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void open(MailRepository.Summary message) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("account_id", message.accountId);
        intent.putExtra("folder_kind", message.folderKind);
        intent.putExtra("uid", message.uid);
        startActivity(intent);
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private final class ResultAdapter extends BaseAdapter {
        private final List<MailRepository.Summary> items;
        ResultAdapter(List<MailRepository.Summary> items) { this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) {
            MailRepository.Summary message = items.get(position);
            return (((long) message.accountId.hashCode()) << 32) ^ message.uid;
        }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            MailRepository.Summary message = items.get(position);
            LinearLayout card = Ui.card(SearchActivity.this);
            String folderName = MailRepository.SENT.equals(message.folderKind) ? "SENT" : "INBOX";
            if (allAccounts && !message.accountLabel.isEmpty()) {
                folderName += " • " + message.accountLabel;
            }
            TextView folder = Ui.label(SearchActivity.this, folderName);
            folder.setTextColor(Ui.teal(SearchActivity.this));
            card.addView(folder);
            TextView subject = Ui.text(SearchActivity.this,
                    message.subject == null || message.subject.trim().isEmpty() ? "(No subject)" : message.subject);
            subject.setTextSize(17);
            subject.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(subject);
            TextView correspondent = Ui.text(SearchActivity.this, message.from == null ? "" : message.from);
            correspondent.setTextColor(Ui.muted(SearchActivity.this));
            card.addView(correspondent);
            if (message.date != null) {
                TextView date = Ui.text(SearchActivity.this,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(message.date));
                date.setTextColor(Ui.muted(SearchActivity.this));
                date.setTextSize(12);
                card.addView(date);
            }
            return card;
        }
    }
}
