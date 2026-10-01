package au.com.t1xperts.mailxperts;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Local-first Smart Contacts browser and device-contact bridge for MX-QA-027. */
public class SmartContactsActivity extends Activity {
    private static final int REQUEST_CONTACTS = 6201;

    private RecipientHistory history;
    private LinearLayout contactsContainer;
    private TextView status;
    private EditText search;
    private List<RecipientDirectory.Entry> deviceEntries = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        history = new RecipientHistory(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = Ui.vertical(this);
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        Button back = Ui.compactButton(this, "‹");
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, "Smart Contacts");
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        TextView description = Ui.text(this,
                "MailXperts learns legitimate people from sent and received mail locally. "
                        + "With permission, it also reads Android Contacts, including Google, "
                        + "Exchange or CardDAV/iCloud contacts already synchronised to this phone.");
        description.setTextColor(Ui.muted(this));
        root.addView(description);

        search = Ui.edit(this, "Search Smart Contacts");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                render();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        root.addView(search);

        LinearLayout actions = Ui.card(this);
        actions.addView(Ui.label(this, "CONTACT SOURCES"));
        Button deviceAccess = Ui.secondaryButton(this, "Enable / refresh phone contacts", v -> {
            if (DeviceContactDirectory.canRead(this)) {
                reloadDeviceContacts();
                Toast.makeText(this, "Phone contacts refreshed", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACTS);
            }
        });
        actions.addView(deviceAccess);

        actions.addView(Ui.secondaryButton(this,
                "Import phone contacts into local Smart Contacts",
                v -> importDeviceContacts()));

        actions.addView(Ui.secondaryButton(this,
                "Clear learned MailXperts contacts",
                v -> confirmClear()));
        root.addView(actions);

        status = Ui.text(this, "");
        status.setTextColor(Ui.muted(this));
        root.addView(status);

        contactsContainer = new LinearLayout(this);
        contactsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(contactsContainer);

        Ui.setContentView(this, scroll);
        reloadDeviceContacts();
    }

    @Override protected void onResume() {
        super.onResume();
        reloadDeviceContacts();
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CONTACTS) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            reloadDeviceContacts();
            Toast.makeText(this,
                    "Phone contacts enabled for Smart Contacts", Toast.LENGTH_SHORT).show();
        } else {
            render();
            Toast.makeText(this,
                    "MailXperts learned contacts still work without phone-contact permission",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void reloadDeviceContacts() {
        deviceEntries = DeviceContactDirectory.load(this);
        render();
    }

    private void importDeviceContacts() {
        if (!DeviceContactDirectory.canRead(this)) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACTS);
            return;
        }
        history.importEntries(deviceEntries, "Device contacts");
        render();
        Toast.makeText(this,
                "Phone contacts imported into local Smart Contacts",
                Toast.LENGTH_SHORT).show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Clear learned contacts?")
                .setMessage("This clears MailXperts learned history and message-learning markers. "
                        + "It does not delete contacts from your phone, Google or iCloud.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> {
                    history.clear();
                    render();
                })
                .show();
    }

    private void render() {
        if (contactsContainer == null) return;
        contactsContainer.removeAllViews();

        ArrayList<RecipientDirectory.Entry> allowedDevice = new ArrayList<>();
        for (RecipientDirectory.Entry entry : deviceEntries) {
            if (!history.isBlocked(entry.email)) allowedDevice.add(entry);
        }
        List<RecipientDirectory.Entry> localEntries = history.entries();
        ArrayList<RecipientDirectory.Entry> merged = new ArrayList<>(
                RecipientDirectory.mergeEntries(localEntries, allowedDevice));

        String needle = search == null ? "" : search.getText().toString().trim()
                .toLowerCase(Locale.ROOT);
        merged.removeIf(entry -> !needle.isEmpty()
                && !entry.name.toLowerCase(Locale.ROOT).contains(needle)
                && !entry.email.toLowerCase(Locale.ROOT).contains(needle));

        merged.sort(Comparator
                .comparingLong((RecipientDirectory.Entry e) -> e.lastUsed).reversed()
                .thenComparingInt((RecipientDirectory.Entry e) -> e.count).reversed()
                .thenComparing(e -> e.label().toLowerCase(Locale.ROOT)));

        status.setText(localEntries.size() + " learned • "
                + deviceEntries.size() + " phone-contact email addresses • "
                + merged.size() + " shown");

        if (merged.isEmpty()) {
            TextView empty = Ui.text(this,
                    "No contacts match yet. Send or receive email, or enable phone contacts.");
            empty.setTextColor(Ui.muted(this));
            contactsContainer.addView(empty);
            return;
        }

        int shown = 0;
        for (RecipientDirectory.Entry entry : merged) {
            if (shown++ >= 500) break;
            LinearLayout card = Ui.card(this);
            TextView name = Ui.text(this, entry.name.isEmpty() ? entry.email : entry.name);
            name.setTextSize(17);
            card.addView(name);
            if (!entry.name.isEmpty()) {
                TextView email = Ui.text(this, entry.email);
                email.setTextColor(Ui.teal(this));
                card.addView(email);
            }
            TextView details = Ui.text(this, details(entry));
            details.setTextColor(Ui.muted(this));
            details.setTextSize(12);
            card.addView(details);

            card.setOnClickListener(v -> compose(entry));
            card.setOnLongClickListener(v -> {
                showContactActions(entry);
                return true;
            });
            contactsContainer.addView(card);
        }
    }

    private String details(RecipientDirectory.Entry entry) {
        StringBuilder out = new StringBuilder(entry.source);
        if (entry.incomingCount > 0 || entry.outgoingCount > 0) {
            out.append(" • received ").append(entry.incomingCount)
                    .append(" • sent ").append(entry.outgoingCount);
        }
        if (entry.lastUsed > 0L) {
            out.append(" • last ")
                    .append(DateFormat.getDateTimeInstance(
                            DateFormat.SHORT, DateFormat.SHORT).format(new Date(entry.lastUsed)));
        }
        return out.toString();
    }

    private void showContactActions(RecipientDirectory.Entry entry) {
        String[] actions = history.hasLearned(entry.email)
                ? new String[]{"Compose email", "Add / update in phone contacts",
                "Remove learned copy", "Block future learning"}
                : new String[]{"Compose email", "Add / update in phone contacts",
                "Block future learning"};
        new AlertDialog.Builder(this)
                .setTitle(entry.label())
                .setItems(actions, (dialog, which) -> {
                    String action = actions[which];
                    if ("Compose email".equals(action)) compose(entry);
                    else if ("Add / update in phone contacts".equals(action)) openSystemContactEditor(entry);
                    else if ("Remove learned copy".equals(action)) {
                        history.remove(entry.email);
                        render();
                    } else if ("Block future learning".equals(action)) {
                        history.block(entry.email);
                        render();
                    }
                })
                .show();
    }

    private void compose(RecipientDirectory.Entry entry) {
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("to", entry.label());
        startActivity(intent);
    }

    private void openSystemContactEditor(RecipientDirectory.Entry entry) {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.NAME, entry.name);
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, entry.email);
        try {
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this,
                    "No contacts app is available to save this contact",
                    Toast.LENGTH_LONG).show();
        }
    }
}
