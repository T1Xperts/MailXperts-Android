package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Full-height left navigation drawer shared across mailbox screens. */
final class Navigation {
    private Navigation() {}

    static void show(Activity activity, View anchor, String accountId) {
        Dialog drawer = new Dialog(activity);
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        LinearLayout panel = Ui.vertical(activity);
        panel.setPadding(Ui.dp(activity, 18), Ui.dp(activity, 20),
                Ui.dp(activity, 18), Ui.dp(activity, 20));

        TextView title = Ui.title(activity, "MailXperts");
        panel.addView(title);
        TextView scope = Ui.text(activity, scopeLabel(activity, accountId));
        scope.setTextColor(Ui.muted(activity));
        scope.setTextSize(13);
        scope.setPadding(0, 0, 0, Ui.dp(activity, 12));
        panel.addView(scope);

        add(drawer, panel, activity, accountId, "Inbox",
                () -> openServer(activity, accountId, MailRepository.INBOX));
        add(drawer, panel, activity, accountId, "★ Smart Priority",
                () -> openPriority(activity, accountId));
        add(drawer, panel, activity, accountId, "Sent",
                () -> openServer(activity, accountId, MailRepository.SENT));
        add(drawer, panel, activity, accountId, "Spam / Junk",
                () -> openServer(activity, accountId, MailRepository.JUNK));

        panel.addView(Ui.label(activity, "LOCAL"));
        add(drawer, panel, activity, accountId, "Drafts",
                () -> openLocal(activity, accountId, LocalStore.DRAFT));
        add(drawer, panel, activity, accountId, "Outbox",
                () -> openLocal(activity, accountId, LocalStore.OUTBOX));
        add(drawer, panel, activity, accountId, "Scheduled",
                () -> openLocal(activity, accountId, LocalStore.SCHEDULED));

        panel.addView(Ui.label(activity, "ACCOUNTS & APP"));
        add(drawer, panel, activity, accountId, "＋ Add email account",
                () -> activity.startActivity(new Intent(activity, SettingsActivity.class)));
        add(drawer, panel, activity, accountId, "Manage accounts",
                () -> activity.startActivity(new Intent(activity, AccountsActivity.class)));
        if (!MailboxScope.isAll(accountId)) {
            add(drawer, panel, activity, accountId, "Account settings", () -> {
                Intent intent = new Intent(activity, SettingsActivity.class);
                intent.putExtra("account_id", accountId);
                activity.startActivity(intent);
            });
        }
        add(drawer, panel, activity, accountId, "Appearance",
                () -> activity.startActivity(new Intent(activity, AppearanceActivity.class)));

        Button close = Ui.compactButton(activity, "Close");
        close.setOnClickListener(v -> drawer.dismiss());
        panel.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 48)));

        scroll.addView(panel);
        drawer.setContentView(scroll);
        Window window = drawer.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.START);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.42f;
            window.setAttributes(params);
        }
        drawer.show();
        if (window != null) {
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int width = Math.min(Ui.dp(activity, 340), Math.round(screenWidth * 0.88f));
            window.setLayout(width, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.START);
        }
    }

    private static void add(Dialog drawer, LinearLayout panel, Activity activity,
                            String accountId, String label, Runnable action) {
        Button button = Ui.secondaryButton(activity, label, v -> {
            drawer.dismiss();
            action.run();
        });
        panel.addView(button);
    }

    private static String scopeLabel(Activity activity, String accountId) {
        if (MailboxScope.isAll(accountId)) return "All Accounts • Unified mailbox";
        try {
            AccountConfig account = new SecureStore(activity).load(accountId);
            if (account != null && account.isUsable()) {
                return account.displayName() + "\n" + account.email;
            }
        } catch (Throwable ignored) {}
        return "Mail account";
    }

    private static void openServer(Activity activity, String accountId, String kind) {
        Intent intent = new Intent(activity, InboxActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("folder_kind", kind);
        activity.startActivity(intent);
    }

    private static void openPriority(Activity activity, String accountId) {
        Intent intent = new Intent(activity, InboxActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("folder_kind", MailRepository.INBOX);
        intent.putExtra("smart_only", true);
        activity.startActivity(intent);
    }

    private static void openLocal(Activity activity, String accountId, String type) {
        Intent intent = new Intent(activity, LocalFolderActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("local_type", type);
        activity.startActivity(intent);
    }
}
