package au.com.t1xperts.mailxperts;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Read-only bridge to Android's Contacts provider.
 *
 * Google, Exchange and CardDAV/iCloud contacts that the user has already synchronised into
 * Android are surfaced through the same provider. MailXperts does not collect provider passwords.
 */
final class DeviceContactDirectory {
    private static final int MAX_DEVICE_EMAILS = 5000;

    private DeviceContactDirectory() {}

    static boolean canRead(Context context) {
        return context != null
                && context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static List<RecipientDirectory.Entry> load(Context context) {
        ArrayList<RecipientDirectory.Entry> empty = new ArrayList<>();
        if (!canRead(context)) return empty;

        LinkedHashMap<String, RecipientDirectory.Entry> unique = new LinkedHashMap<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS
        };
        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " COLLATE NOCASE ASC")) {
            if (cursor == null) return empty;
            int nameColumn = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME);
            int emailColumn = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Email.ADDRESS);
            while (cursor.moveToNext() && unique.size() < MAX_DEVICE_EMAILS) {
                String name = nameColumn >= 0 ? cursor.getString(nameColumn) : "";
                String email = emailColumn >= 0 ? cursor.getString(emailColumn) : "";
                if (email == null) continue;
                email = email.trim();
                if (email.isEmpty()) continue;
                List<RecipientDirectory.Entry> parsed =
                        RecipientDirectory.parseAddresses(email);
                if (parsed.isEmpty()) continue;
                String canonical = parsed.get(0).email;
                String key = canonical.toLowerCase(Locale.ROOT);
                if (unique.containsKey(key)) continue;
                unique.put(key, new RecipientDirectory.Entry(
                        name == null ? "" : name,
                        canonical,
                        1,
                        0L,
                        0L,
                        0,
                        0,
                        "Android Contacts"));
            }
        } catch (SecurityException ignored) {
            return empty;
        } catch (Exception ignored) {
            return empty;
        }
        return new ArrayList<>(unique.values());
    }
}
