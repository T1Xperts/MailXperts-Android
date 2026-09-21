package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
    private static final int CHOICE_ALL = 0;
    private static final int CHOICE_ACCOUNT = 1;
    private static final int CHOICE_ADD = 2;
    private static final int CHOICE_MANAGE = 3;
    private static final int MAX_PARALLEL_ACCOUNTS = 3;
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
    private SecureStore secureStore;
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
    private long foregroundRefreshIntervalMs;

    private final Runnable periodicRefresh = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            startSync(false, false);
            schedulePeriodicRefresh();
        }
    };

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        accountId = getIntent().getStringExtra("account_id");
        kind = getIntent().getStringExtra("folder_kind");
        if (kind == null) kind = MailRepository.INBOX;
        smartOnly = getIntent().getBooleanExtra("smart_only", false);

        secureStore = new SecureStore(this);
        List<AccountConfig> usable = MailboxScope.usable(secureStore);
        allAccounts = MailboxScope.isAll(accountId);
        if (allAccounts) {
            accounts.addAll(usable);
        } else {
            account = MailboxScope.find(usable, accountId);
            if (account != null) accounts.add(account);
        }
        if (accounts.isEmpty()) { finish(); return; }
        foregroundRefreshIntervalMs = shortestAutomaticInterval();

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
        refreshButton = Ui.secondaryButton(this, "Refresh", v -> startSync(false, true));
        actions.addView(refreshButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        loadOlderButton = Ui.secondaryButton(this,
                allAccounts ? "Load 1,000 older/account" : "Load 1,000 older",
                v -> startSync(true, true));
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
        startSync(false, false);
    }

    private View buildHeader() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

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
        title.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 6), 0);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        header.addView(title,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button search = Ui.compactButton(this, "⌕");
        search.setContentDescription("Search this mailbox");
        search.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra("account_id", accountId);
            intent.putExtra("folder_kind", kind);
            startActivity(intent);
        });
        header.addView(search,
                new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        Button compose = Ui.compactButton(this, "✎");
        compose.setContentDescription("Compose email");
        compose.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra("account_id", accountId);
            startActivity(intent);
        });
        header.addView(compose,
                new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        container.addView(header);

        Spinner switcher = accountSpinner();
        container.addView(switcher, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));
        return container;
    }

    private Spinner accountSpinner() {
        ArrayList<AccountChoice> choices = new ArrayList<>();
        choices.add(new AccountChoice(CHOICE_ALL, null, "All Accounts"));
        int selectedIndex = allAccounts ? 0 : 1;
        List<AccountConfig> switchAccounts = MailboxScope.usable(secureStore);
        for (int i = 0; i < switchAccounts.size(); i++) {
            AccountConfig configured = switchAccounts.get(i);
            choices.add(new AccountChoice(CHOICE_ACCOUNT, configured, configured.displayName()));
            if (!allAccounts && configured.id.equals(accountId)) selectedIndex = i + 1;
        }
        choices.add(new AccountChoice(CHOICE_ADD, null, "＋ Add account"));
        choices.add(new AccountChoice(CHOICE_MANAGE, null, "Manage accounts"));
        final int activeIndex = selectedIndex;

        ArrayAdapter<AccountChoice> adapter = new ArrayAdapter<AccountChoice>(
                this, android.R.layout.simple_spinner_item, choices) {
            private TextView decorate(View view, boolean dropdown) {
                TextView text = (TextView) view;
                text.setTextColor(Ui.textColor(InboxActivity.this));
                text.setTextSize(dropdown ? 15 : 12);
                text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                int vertical = dropdown ? Ui.dp(InboxActivity.this, 14) : 0;
                text.setPadding(Ui.dp(InboxActivity.this, 8), vertical,
                        Ui.dp(InboxActivity.this, 8), vertical);
                if (dropdown) text.setBackgroundColor(Ui.panel(InboxActivity.this));
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
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIndex, false);
        final boolean[] ready = {false};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!ready[0]) return;
                AccountChoice choice = choices.get(position);
                if (choice.type == CHOICE_ADD) {
                    spinner.setSelection(activeIndex, false);
                    startActivity(new Intent(InboxActivity.this, SettingsActivity.class));
                    return;
                }
                if (choice.type == CHOICE_MANAGE) {
                    spinner.setSelection(activeIndex, false);
                    startActivity(new Intent(InboxActivity.this, AccountsActivity.class));
                    return;
                }
                String destination = choice.type == CHOICE_ALL
                        ? MailboxScope.ALL_ACCOUNTS : choice.account.id;
                if (destination.equals(accountId)) return;
                secureStore.setSelectedId(destination);
                Intent intent = new Intent(InboxActivity.this, InboxActivity.class);
                intent.putExtra("account_id", destination);
                intent.putExtra("folder_kind", kind);
                intent.putExtra("smart_only", smartOnly);
                startActivity(intent);
                finish();
            }
        });
        spinner.post(() -> ready[0] = true);
        return spinner;
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

    private void startSync(boolean loadOlder, boolean force) {
        if (destroyed || !syncing.compareAndSet(false, true)) return;
        setSyncControls(true);
        lastProgressRenderAt.set(0L);
        coordinatorFuture = coordinator.submit(() -> runSync(loadOlder, force));
    }

    private void runSync(boolean loadOlder, boolean force) {
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
                boolean automatic = configured.syncEnabled
                        && SyncPolicy.normalizeInterval(configured.syncIntervalMinutes)
                        != SyncPolicy.MANUAL;
                boolean due = SyncPolicy.isDue(state.lastSuccessfulSyncAt,
                        System.currentTimeMillis(), configured.syncIntervalMinutes);
                if (force || loadOlder || (automatic && due)) {
                    work.add(new AccountWork(configured, target, state));
                }
                localStore.saveSyncState(configured.id, kind, target,
                        state.uidValidity, state.lastSuccessfulSyncAt, state.serverMessageCount);
            }
            List<MailRepository.Summary> cached = combinedSnapshot(
                    targets, SyncPlanner.FIRST_CACHE_RENDER_LIMIT);
            postSnapshot(cached, cached.isEmpty()
                    ? (work.isEmpty() ? "No cached mail • tap Refresh to connect" : "Connecting securely…")
                    : "Showing cached mail instantly" + (work.isEmpty()
                    ? " • up to date" : " • checking for updates…"));

            if (work.isEmpty()) {
                recalculatePagingState();
                List<MailRepository.Summary> fullCache = combinedSnapshot(targets);
                postResult(fullCache, "Showing " + fullCache.size()
                        + " cached messages • automatic sync is not due", null);
                return;
            }

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
            if (allAccounts) summary += " • " + successes + "/" + work.size() + " refreshed accounts";
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
        return combinedSnapshot(targets, SyncPlanner.MAX_MESSAGE_LIMIT);
    }

    private List<MailRepository.Summary> combinedSnapshot(
            Map<String, Integer> targets, int maximumPerAccount) {
        ArrayList<MailRepository.Summary> fetched = new ArrayList<>();
        for (AccountConfig configured : accounts) {
            Integer requested = targets.get(configured.id);
            int limit = requested == null
                    ? localStore.getSyncState(configured.id, kind).requestedLimit : requested;
            fetched.addAll(localStore.listCached(configured.id, configured.displayName(), kind,
                    Math.min(limit, maximumPerAccount)));
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
        schedulePeriodicRefresh();
        if (firstResume) firstResume = false;
        else startSync(false, false);
    }

    @Override protected void onPause() {
        mainHandler.removeCallbacks(periodicRefresh);
        super.onPause();
    }

    private long shortestAutomaticInterval() {
        long shortest = Long.MAX_VALUE;
        for (AccountConfig configured : accounts) {
            if (!configured.syncEnabled) continue;
            long interval = SyncPolicy.intervalMillis(configured.syncIntervalMinutes);
            if (interval > 0L) shortest = Math.min(shortest, interval);
        }
        return shortest == Long.MAX_VALUE ? 0L : shortest;
    }

    private void schedulePeriodicRefresh() {
        if (!destroyed && foregroundRefreshIntervalMs > 0L) {
            mainHandler.postDelayed(periodicRefresh, foregroundRefreshIntervalMs);
        }
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
