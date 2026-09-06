package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalFolderActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<LocalStore.LocalMessage> items = new ArrayList<>();
    private String accountId;
    private String type;
    private boolean allAccounts;
    private SecureStore store;
    private LocalStore db;
    private Adapter adapter;
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        accountId = getIntent().getStringExtra("account_id");
        type = getIntent().getStringExtra("local_type");
        if (type == null) type = LocalStore.DRAFT;
        allAccounts = MailboxScope.isAll(accountId);
        store = new SecureStore(this);
        db = new LocalStore(this);

        LinearLayout root = Ui.vertical(this);
        root.addView(buildHeader());
        status = Ui.text(this, subtitle());
        status.setTextColor(Ui.muted(this));
        root.addView(status);
        ListView list = new ListView(this);
        list.setDividerHeight(0);
        adapter = new Adapter(items);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> edit(items.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            LocalStore.LocalMessage message = items.get(position);
            confirmDelete(message);
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, root);
    }

    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button menu = Ui.compactButton(this, "☰");
        menu.setContentDescription("Open navigation menu");
        menu.setOnClickListener(v -> Navigation.show(this, menu, accountId));
        header.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, (allAccounts ? "Unified " : "") + title());
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button compose = Ui.compactButton(this, "✎");
        compose.setContentDescription("Compose email");
        compose.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra("account_id", accountId);
            startActivity(intent);
        });
        header.addView(compose, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        return header;
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private String title() {
        if (LocalStore.OUTBOX.equals(type)) return "Outbox";
        if (LocalStore.SCHEDULED.equals(type)) return "Scheduled";
        return "Drafts";
    }

    private String subtitle() {
        String scope = allAccounts ? " across all accounts." : ".";
        if (LocalStore.OUTBOX.equals(type)) {
            return "Failed sends stay here" + scope
                    + " They are never retried automatically — tap Retry now manually.";
        }
        if (LocalStore.SCHEDULED.equals(type)) {
            return "Scheduled emails" + scope
                    + " send automatically. Recurring items reschedule after each occurrence.";
        }
        return "Unsent emails saved for later editing" + scope;
    }

    private void refresh() {
        items.clear();
        items.addAll(db.list(accountId, type));
        if (adapter != null) adapter.notifyDataSetChanged();
        if (status != null) status.setText(subtitle() + "\n" + items.size() + " item(s)");
    }

    private AccountConfig accountFor(LocalStore.LocalMessage message) {
        AccountConfig configured = store.load(message.accountId);
        return message.accountId != null && message.accountId.equals(configured.id) && configured.isUsable()
                ? configured : null;
    }

    private void edit(LocalStore.LocalMessage message) {
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", message.accountId);
        intent.putExtra("local_id", message.id);
        startActivity(intent);
    }

    private void confirmDelete(LocalStore.LocalMessage message) {
        AccountConfig configured = accountFor(message);
        boolean deleteRemoteDraft = message.serverUid > 0L && configured != null
                && configured.syncDraftsToServer;
        new AlertDialog.Builder(this)
                .setTitle("Delete this item?")
                .setMessage(deleteRemoteDraft
                        ? "This local item and its synchronised server Draft copy will be deleted."
                        : "This item will be deleted from this device. No server Draft will be changed.")
                .setPositiveButton("Delete", (dialog, which) ->
                        deleteItem(message, configured, deleteRemoteDraft))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem(LocalStore.LocalMessage message, AccountConfig configured,
                            boolean deleteRemoteDraft) {
        if (LocalStore.SCHEDULED.equals(message.type)) Scheduler.cancel(this, message.id);
        DraftSyncDispatcher.cancel(message.id);
        if (!deleteRemoteDraft) {
            db.delete(message.id);
            refresh();
            return;
        }
        status.setText("Deleting local and server Draft…");
        executor.execute(() -> {
            try {
                MailRepository.deleteServerDraft(configured, message.serverUid);
                db.delete(message.id);
                runOnUiThread(this::refresh);
            } catch (Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Server Draft deletion failed: "
                            + MailRepository.safe(error), Toast.LENGTH_LONG).show();
                    refresh();
                });
            }
        });
    }

    private void retry(LocalStore.LocalMessage message, Button button) {
        AccountConfig configured = accountFor(message);
        if (configured == null) {
            Toast.makeText(this, "The sending account is unavailable. Open Edit and choose another From account.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        button.setEnabled(false);
        button.setText("Retrying…");
        DraftSyncDispatcher.cancel(message.id);
        executor.execute(() -> {
            try {
                MailRepository.SendResult result = MailRepository.sendHtml(configured,
                        message.to, message.cc, message.bcc, message.subject, message.html);
                String draftWarning = "";
                if (message.serverUid > 0L) {
                    try { MailRepository.deleteServerDraft(configured, message.serverUid); }
                    catch (Exception ignored) {
                        draftWarning = " • old server Draft may require manual deletion";
                    }
                }
                db.delete(message.id);
                String toast = (result.sentCopySaved
                        ? "Sent and saved in Sent"
                        : "Sent; Sent-folder warning: " + result.sentCopyWarning) + draftWarning;
                runOnUiThread(() -> {
                    Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
                    refresh();
                });
            } catch (Exception error) {
                message.lastError = MailRepository.safe(error);
                db.save(message);
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText("Retry now");
                    Toast.makeText(this, "Still not sent: " + message.lastError, Toast.LENGTH_LONG).show();
                    refresh();
                });
            }
        });
    }

    private String accountLabel(LocalStore.LocalMessage message) {
        AccountConfig configured = store.load(message.accountId);
        if (message.accountId != null && message.accountId.equals(configured.id)) {
            String email = configured.email == null || configured.email.isEmpty() ? "" : " • " + configured.email;
            return configured.displayName() + email;
        }
        return "Account unavailable";
    }

    private final class Adapter extends BaseAdapter {
        private final List<LocalStore.LocalMessage> data;
        Adapter(List<LocalStore.LocalMessage> data) { this.data = data; }
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return data.get(position).id; }
        @Override public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
            LocalStore.LocalMessage message = data.get(position);
            LinearLayout row = Ui.card(LocalFolderActivity.this);
            if (allAccounts) {
                TextView accountBadge = Ui.label(LocalFolderActivity.this,
                        "FROM • " + accountLabel(message));
                accountBadge.setTextColor(Ui.teal(LocalFolderActivity.this));
                row.addView(accountBadge);
            }
            TextView subject = Ui.text(LocalFolderActivity.this,
                    message.subject == null || message.subject.isEmpty() ? "(No subject)" : message.subject);
            subject.setTextSize(16);
            row.addView(subject);
            TextView to = Ui.text(LocalFolderActivity.this, "To: " + message.to);
            to.setTextColor(Ui.muted(LocalFolderActivity.this));
            to.setTextSize(13);
            row.addView(to);
            if (LocalStore.OUTBOX.equals(type)
                    && message.lastError != null && !message.lastError.isEmpty()) {
                TextView error = Ui.text(LocalFolderActivity.this, "Last error: " + message.lastError);
                error.setTextColor(Ui.error(LocalFolderActivity.this));
                error.setTextSize(12);
                row.addView(error);
            }
            if (LocalStore.DRAFT.equals(type)
                    && message.lastError != null && !message.lastError.isEmpty()) {
                TextView warning = Ui.text(LocalFolderActivity.this, message.lastError);
                warning.setTextColor(Ui.error(LocalFolderActivity.this));
                warning.setTextSize(12);
                row.addView(warning);
            }
            if (LocalStore.SCHEDULED.equals(type)) {
                TextView next = Ui.text(LocalFolderActivity.this,
                        "Next: " + DateFormat.getDateTimeInstance().format(message.scheduledAt)
                                + " · " + message.recurrence);
                next.setTextColor(Ui.muted(LocalFolderActivity.this));
                next.setTextSize(12);
                row.addView(next);
            }
            LinearLayout buttons = new LinearLayout(LocalFolderActivity.this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            Button edit = Ui.compactButton(LocalFolderActivity.this, "Edit");
            edit.setOnClickListener(v -> edit(message));
            buttons.addView(edit, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (LocalStore.OUTBOX.equals(type)) {
                Button retry = Ui.compactButton(LocalFolderActivity.this, "Retry now");
                retry.setOnClickListener(v -> retry(message, retry));
                buttons.addView(retry, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            if (LocalStore.SCHEDULED.equals(type)) {
                Button cancel = Ui.compactButton(LocalFolderActivity.this, "Cancel");
                cancel.setOnClickListener(v -> confirmDelete(message));
                buttons.addView(cancel, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            row.addView(buttons);
            return row;
        }
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        if (db != null) db.close();
        super.onDestroy();
    }
}
