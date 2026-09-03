package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class InboxActivity extends Activity {
    private static final int MAX_PARALLEL_ACCOUNTS = 3;
    private static final long FOREGROUND_REFRESH_INTERVAL_MS = 5L * 60L * 1_000L;
    private static final long PROGRESS_RENDER_THROTTLE_MS = 200L;

    private final ExecutorService coordinator = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mailxperts-sync-coordinator");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    private final AtomicBoolean syncing = new AtomicBoolean(false);
    private final AtomicLong lastProgressRenderAt = new AtomicLong(0L);
    private final ConcurrentHashMap<String, MailRepository.SyncToken> activeTokens =
            new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Future<AccountResult>> accountFutures =
            new CopyOnWriteArrayList<>();
    private final ArrayList<MailRepository.Summary> messages = new ArrayList<>();
    private final ArrayList<AccountConfig> accounts = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AccountConfig account;
    private String accountId;
    private String kind;
    private boolean allAccounts;
    private boolean smartOnly;
    private MailAdapter adapter;
    private ProgressBar progress;
    private TextView status;
    private Button refreshButton;
    private Button loadOlderButton;
    private LocalStore localStore;
    private ExecutorService accountExecutor;
    private volatile Future<?> coordinatorFuture;
    private volatile boolean canLoadOlder = true;
    private volatile boolean allAtMaximum;
    private boolean firstResume = true;
    private volatile boolean destroyed;

    private final Runnable periodicRefresh = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            startSync(false);
            mainHandler.postDelayed(this, FOREGROUND_REFRESH_INTERVAL_MS);
        }
    };

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        accountId = getIntent().getStringExtra("account_id");
        kind = getIntent().getStringExtra("folder_kind");
        if (kind == null) kind = MailRepository.INBOX;
        smartOnly = getIntent().getBooleanExtra("smart_only", false);

        SecureStore store = new SecureStore(this);
        List<AccountConfig> usable = MailboxScope.usable(store);
        allAccounts = MailboxScope.isAll(accountId);
        if (allAccounts) {
            accounts.addAll(usable);
        } else {
            account = MailboxScope.find(usable, accountId);
            if (account != null) accounts.add(account);
        }
        if (accounts.isEmpty()) { finish(); return; }

        localStore = new LocalStore(this);
        accountExecutor = Executors.newFixedThreadPool(
                Math.min(MAX_PARALLEL_ACCOUNTS, accounts.size()), runnable -> {
                    Thread thread = new Thread(runnable, "mailxperts-account-sync");
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    return thread;
                });

        LinearLayout root = Ui.vertical(this);
        root.addView(buildHeader());
        TextView who = Ui.text(this, allAccounts
                ? "All Accounts • " + accounts.size() + " connected mailboxes"
                : account.email);
        who.setTextColor(Ui.muted(this));
        root.addView(who);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        refreshButton = Ui.secondaryButton(this, "Refresh", v -> startSync(false));
        actions.addView(refreshButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        loadOlderButton = Ui.secondaryButton(this,
                allAccounts ? "Load 1,000 older/account" : "Load 1,000 older",
                v -> startSync(true));
        LinearLayout.LayoutParams olderParams =
                new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f);
        olderParams.setMargins(Ui.dp(this, 8), 0, 0, 0);
        actions.addView(loadOlderButton, olderParams);
        root.addView(actions);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        status = Ui.text(this, "Opening cached mail…");
        status.setTextColor(Ui.muted(this));
        root.addView(status);

        ListView list = new ListView(this);
        list.setDividerHeight(0);
        adapter = new MailAdapter(messages);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> open(messages.get(position)));
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, root);
        startSync(false);
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button menu = Ui.compactButton(this, "☰");
        menu.setContentDescription("Open navigation menu");
        menu.setOnClickListener(v -> Navigation.show(this, menu, accountId));
        header.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        String base = smartOnly ? "Smart Priority"
                : MailRepository.SENT.equals(kind) ? "Sent"
                : MailRepository.JUNK.equals(kind) ? "Spam / Junk" : "Inbox";
        TextView title = Ui.title(this, allAccounts ? "Unified " + base : base);
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(title,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button compose = Ui.compactButton(this, "✎");
        compose.setContentDescription("Compose email");
        compose.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra("account_id", accountId);
            startActivity(intent);
        });
        header.addView(compose,
                new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        return header;
    }

    private void startSync(boolean loadOlder) {
        if (destroyed || !syncing.compareAndSet(false, true)) return;
        setSyncControls(true);
        lastProgressRenderAt.set(0L);
        coordinatorFuture = coordinator.submit(() -> runSync(loadOlder));
    }

    private void runSync(boolean loadOlder) {
        ArrayList<AccountWork> work = new ArrayList<>();
        LinkedHashMap<String, Integer> targets = new LinkedHashMap<>();
        ArrayList<String> errors = new ArrayList<>();
        int successes = 0;
        try {
            for (AccountConfig configured : accounts) {
                LocalStore.SyncState state = localStore.getSyncState(configured.id, kind);
                int target = loadOlder
                        ? SyncPlanner.nextRequestedLimit(state.requestedLimit)
                        : SyncPlanner.clampRequestedLimit(state.requestedLimit);
                targets.put(configured.id, target);
                work.add(new AccountWork(configured, target, state));
                localStore.saveSyncState(configured.id, kind, target,
                        state.uidValidity, state.lastSuccessfulSyncAt, state.serverMessageCount);
            }
            List<MailRepository.Summary> cached = combinedSnapshot(targets);
            postSnapshot(cached, cached.isEmpty()
                    ? "Connecting securely…"
                    : "Showing " + cached.size() + " cached messages • checking for updates…");

            ExecutorCompletionService<AccountResult> completion =
                    new ExecutorCompletionService<>(accountExecutor);
            for (AccountWork item : work) {
                accountFutures.add(completion.submit(() -> syncAccount(item, targets)));
            }

            for (int completed = 0; completed < work.size(); completed++) {
                Future<AccountResult> future = completion.take();
                accountFutures.remove(future);
                AccountResult result = future.get();
                if (result.error == null) successes++;
                else errors.add(result.accountLabel + ": " + result.error);
            }

            recalculatePagingState();
            List<MailRepository.Summary> finalSnapshot = combinedSnapshot(targets);
            String summary = smartOnly
                    ? "Showing " + finalSnapshot.size() + " priority messages"
                    : "Showing " + finalSnapshot.size() + " messages • sync complete";
            if (allAccounts) summary += " • " + successes + "/" + accounts.size() + " accounts";
            if (!errors.isEmpty()) summary += "\n" + errors.size() + " account(s) could not sync";
            postResult(finalSnapshot, summary, successes == 0 && !errors.isEmpty()
                    ? errors.get(0) : null);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception error) {
            if (!destroyed) {
                List<MailRepository.Summary> cached = combinedSnapshot(targets);
                String prefix = cached.isEmpty() ? "" : "Showing " + cached.size() + " cached messages • ";
                postResult(cached, prefix + friendlyError(error), friendlyError(error));
            }
        } finally {
            for (MailRepository.SyncToken token : activeTokens.values()) token.cancel();
            for (Future<AccountResult> future : accountFutures) future.cancel(true);
            accountFutures.clear();
            activeTokens.clear();
            syncing.set(false);
            postToUi(() -> setSyncControls(false));
        }
    }

    private AccountResult syncAccount(AccountWork work, Map<String, Integer> targets) {
        AccountConfig configured = work.account;
        MailRepository.SyncToken token = new MailRepository.SyncToken();
        activeTokens.put(configured.id, token);
        try {
            LocalStore.CacheStats stats = localStore.cacheStats(configured.id, kind);
            MailRepository.SyncRequest request = new MailRepository.SyncRequest(
                    work.target, stats.count, stats.newestUid, stats.oldestUid,
                    work.previous.uidValidity, work.target > work.previous.requestedLimit);
            MailRepository.SyncResult result = MailRepository.syncFolder(
                    configured, kind, request, new MailRepository.SyncObserver() {
                        @Override public void onMailboxOpened(
                                long uidValidity, boolean cacheMustReset, int serverMessageCount) {
                            if (cacheMustReset) localStore.clearCached(configured.id, kind);
                            localStore.saveSyncState(configured.id, kind, work.target,
                                    uidValidity, work.previous.lastSuccessfulSyncAt,
                                    serverMessageCount);
                        }

                        @Override public void onBatch(
                                List<MailRepository.Summary> batch,
                                int processed, int expected, String phase) {
                            localStore.replaceCachedRange(configured.id, kind, batch);
                            if (shouldRenderProgress(processed, expected)) {
                                postProgress(combinedSnapshot(targets),
                                        configured.displayName() + " • " + phase,
                                        processed, expected);
                            }
                        }
                    }, token);

            localStore.trimCached(configured.id, kind, work.target);
            localStore.saveSyncState(configured.id, kind, work.target,
                    result.uidValidity, System.currentTimeMillis(), result.serverMessageCount);
            return new AccountResult(configured.displayName(), null);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return new AccountResult(configured.displayName(), "Sync cancelled");
        } catch (Exception error) {
            return new AccountResult(configured.displayName(), friendlyError(error));
        } finally {
            activeTokens.remove(configured.id, token);
        }
    }

    private boolean shouldRenderProgress(int processed, int expected) {
        if (processed <= SyncPlanner.QUICK_BATCH_SIZE || (expected > 0 && processed >= expected)) {
            lastProgressRenderAt.set(System.currentTimeMillis());
            return true;
        }
        long now = System.currentTimeMillis();
        long previous = lastProgressRenderAt.get();
        return now - previous >= PROGRESS_RENDER_THROTTLE_MS
                && lastProgressRenderAt.compareAndSet(previous, now);
    }

    private List<MailRepository.Summary> combinedSnapshot(Map<String, Integer> targets) {
        ArrayList<MailRepository.Summary> fetched = new ArrayList<>();
        for (AccountConfig configured : accounts) {
            Integer requested = targets.get(configured.id);
            int limit = requested == null
                    ? localStore.getSyncState(configured.id, kind).requestedLimit : requested;
            fetched.addAll(localStore.listCached(
                    configured.id, configured.displayName(), kind, limit));
        }
        fetched.sort((left, right) -> Long.compare(
                right.date == null ? 0L : right.date.getTime(),
                left.date == null ? 0L : left.date.getTime()));
        if (!smartOnly) return fetched;

        ArrayList<MailRepository.Summary> priority = new ArrayList<>();
        for (MailRepository.Summary message : fetched) {
            MailIntelligence.Result intelligence = MailIntelligence.analyse(
                    message.from, message.subject, "", message.date);
            if (intelligence.important) priority.add(message);
        }
        return priority;
    }

    private void recalculatePagingState() {
        boolean more = false;
        boolean maximum = true;
        for (AccountConfig configured : accounts) {
            LocalStore.SyncState state = localStore.getSyncState(configured.id, kind);
            int target = SyncPlanner.clampRequestedLimit(state.requestedLimit);
            maximum &= target >= SyncPlanner.MAX_MESSAGE_LIMIT;
            if (target < SyncPlanner.MAX_MESSAGE_LIMIT
                    && (state.serverMessageCount < 0 || state.serverMessageCount > target)) {
                more = true;
            }
        }
        canLoadOlder = more;
        allAtMaximum = maximum;
    }

    private void postProgress(List<MailRepository.Summary> snapshot, String phase,
                              int processed, int expected) {
        postToUi(() -> {
            applySnapshot(snapshot);
            progress.setVisibility(View.VISIBLE);
            if (expected > 0) {
                progress.setIndeterminate(false);
                progress.setMax(expected);
                progress.setProgress(Math.min(processed, expected));
                status.setText(phase + " • " + processed + " of " + expected
                        + " • showing " + snapshot.size());
            } else {
                progress.setIndeterminate(true);
                status.setText(phase + " • " + processed + " checked"
                        + " • showing " + snapshot.size());
            }
        });
    }

    private void postSnapshot(List<MailRepository.Summary> snapshot, String message) {
        postToUi(() -> {
            applySnapshot(snapshot);
            status.setText(message);
        });
    }

    private void postResult(List<MailRepository.Summary> snapshot, String message, String toast) {
        postToUi(() -> {
            applySnapshot(snapshot);
            status.setText(message);
            if (toast != null) Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
        });
    }

    private void applySnapshot(List<MailRepository.Summary> snapshot) {
        messages.clear();
        messages.addAll(snapshot);
        adapter.notifyDataSetChanged();
    }

    private void setSyncControls(boolean active) {
        if (destroyed || refreshButton == null) return;
        refreshButton.setEnabled(!active);
        loadOlderButton.setEnabled(!active && canLoadOlder);
        if (allAtMaximum) {
            loadOlderButton.setText("5,000-message limit reached");
        } else if (!canLoadOlder && !active) {
            loadOlderButton.setText("All available mail loaded");
        } else {
            loadOlderButton.setText(allAccounts
                    ? "Load 1,000 older/account" : "Load 1,000 older");
        }
        if (active) {
            progress.setIndeterminate(true);
            progress.setVisibility(View.VISIBLE);
            status.setText("Opening cached mail…");
        } else {
            progress.setVisibility(View.GONE);
        }
    }

    private String friendlyError(Exception error) {
        if (error instanceof SocketTimeoutException
                || (error.getMessage() != null
                && error.getMessage().toLowerCase().contains("timed out"))) {
            return "The mail server took too long. Cached mail remains available; tap Refresh to retry.";
        }
        return "Sync paused: " + MailRepository.safe(error);
    }

    private void open(MailRepository.Summary message) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("account_id", message.accountId);
        intent.putExtra("folder_kind", message.folderKind);
        intent.putExtra("uid", message.uid);
        startActivity(intent);
    }

    private void postToUi(Runnable runnable) {
        mainHandler.post(() -> {
            if (!destroyed && !isFinishing()) runnable.run();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        mainHandler.removeCallbacks(periodicRefresh);
        mainHandler.postDelayed(periodicRefresh, FOREGROUND_REFRESH_INTERVAL_MS);
        if (firstResume) firstResume = false;
        else startSync(false);
    }

    @Override protected void onPause() {
        mainHandler.removeCallbacks(periodicRefresh);
        super.onPause();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        mainHandler.removeCallbacksAndMessages(null);
        for (MailRepository.SyncToken token : activeTokens.values()) token.cancel();
        for (Future<AccountResult> future : accountFutures) future.cancel(true);
        Future<?> future = coordinatorFuture;
        if (future != null) future.cancel(true);
        coordinator.shutdownNow();
        if (accountExecutor != null) accountExecutor.shutdownNow();
        if (localStore != null) localStore.close();
        super.onDestroy();
    }

    private static final class AccountWork {
        final AccountConfig account;
        final int target;
        final LocalStore.SyncState previous;

        AccountWork(AccountConfig account, int target, LocalStore.SyncState previous) {
            this.account = account;
            this.target = target;
            this.previous = previous;
        }
    }

    private static final class AccountResult {
        final String accountLabel;
        final String error;

        AccountResult(String accountLabel, String error) {
            this.accountLabel = accountLabel;
            this.error = error;
        }
    }

    private final class MailAdapter extends BaseAdapter {
        private final List<MailRepository.Summary> items;

        MailAdapter(List<MailRepository.Summary> items) { this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) {
            MailRepository.Summary message = items.get(position);
            return (((long) message.accountId.hashCode()) << 32) ^ message.uid;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout card = convertView instanceof LinearLayout
                    ? (LinearLayout) convertView : Ui.card(InboxActivity.this);
            card.removeAllViews();
            MailRepository.Summary message = items.get(position);
            if (allAccounts) {
                TextView accountBadge = Ui.label(InboxActivity.this,
                        "ACCOUNT • " + (message.accountLabel.isEmpty()
                                ? "Email account" : message.accountLabel));
                accountBadge.setTextColor(Ui.teal(InboxActivity.this));
                card.addView(accountBadge);
            }
            TextView correspondent = Ui.text(InboxActivity.this,
                    (message.seen ? "" : "●  ")
                            + (message.from == null || message.from.isEmpty()
                            ? "Unknown sender" : message.from));
            correspondent.setTextSize(15);
            correspondent.setTypeface(Typeface.DEFAULT,
                    message.seen ? Typeface.NORMAL : Typeface.BOLD);
            correspondent.setTextColor(message.seen
                    ? Ui.textColor(InboxActivity.this) : Ui.teal(InboxActivity.this));
            card.addView(correspondent);
            TextView subject = Ui.text(InboxActivity.this,
                    message.subject == null || message.subject.trim().isEmpty()
                            ? "(No subject)" : message.subject);
            subject.setTextSize(17);
            subject.setTypeface(Typeface.DEFAULT,
                    message.seen ? Typeface.NORMAL : Typeface.BOLD);
            card.addView(subject);
            MailIntelligence.Result intelligence = MailIntelligence.analyse(
                    message.from, message.subject, "", message.date);
            if (!intelligence.label.isEmpty()) {
                String label = "★ " + intelligence.label;
                if (intelligence.hasDueDate()) {
                    label += intelligence.isOverdue() ? " • overdue" : " • due date detected";
                }
                TextView badge = Ui.text(InboxActivity.this, label);
                badge.setTextColor(intelligence.isOverdue()
                        ? Ui.error(InboxActivity.this) : Ui.teal(InboxActivity.this));
                badge.setTextSize(12);
                badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                card.addView(badge);
            }
            if (message.date != null) {
                TextView date = Ui.text(InboxActivity.this,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(message.date));
                date.setTextColor(Ui.muted(InboxActivity.this));
                date.setTextSize(12);
                card.addView(date);
            }
            return card;
        }
    }
}
