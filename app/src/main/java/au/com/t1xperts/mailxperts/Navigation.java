package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.PopupMenu;

final class Navigation {
    private Navigation() {}

    static void show(Activity activity, View anchor, String accountId) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        menu.getMenu().add("Inbox");
        menu.getMenu().add("Smart Priority");
        menu.getMenu().add("Sent");
        menu.getMenu().add("Spam / Junk");
        menu.getMenu().add("Drafts");
        menu.getMenu().add("Outbox");
        menu.getMenu().add("Scheduled");
        menu.getMenu().add("+ Add email account");
        menu.getMenu().add("Accounts");
        menu.getMenu().add("Appearance");
        if (!MailboxScope.isAll(accountId)) menu.getMenu().add("Account settings");
        menu.setOnMenuItemClickListener(item -> {
            String name = item.getTitle().toString();
            if ("Inbox".equals(name)) openServer(activity, accountId, MailRepository.INBOX);
            else if ("Smart Priority".equals(name)) openPriority(activity, accountId);
            else if ("Sent".equals(name)) openServer(activity, accountId, MailRepository.SENT);
            else if ("Spam / Junk".equals(name)) openServer(activity, accountId, MailRepository.JUNK);
            else if ("Drafts".equals(name)) openLocal(activity, accountId, LocalStore.DRAFT);
            else if ("Outbox".equals(name)) openLocal(activity, accountId, LocalStore.OUTBOX);
            else if ("Scheduled".equals(name)) openLocal(activity, accountId, LocalStore.SCHEDULED);
            else if ("+ Add email account".equals(name)) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
            }
            else if ("Accounts".equals(name)) activity.startActivity(new Intent(activity, AccountsActivity.class));
            else if ("Appearance".equals(name)) activity.startActivity(new Intent(activity, AppearanceActivity.class));
            else if ("Account settings".equals(name)) {
                Intent intent = new Intent(activity, SettingsActivity.class);
                intent.putExtra("account_id", accountId);
                activity.startActivity(intent);
            }
            return true;
        });
        menu.show();
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
