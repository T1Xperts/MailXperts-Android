package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Local-only learned recipient history. Nothing is uploaded or synced off device. */
final class RecipientHistory {
    private static final String PREFS = "mailxperts_recipient_history_v1";
    private static final String KEY_ENTRIES = "entries";
    private static final int MAX_ENTRIES = 250;

    private final SharedPreferences preferences;

    RecipientHistory(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized void learn(String... rawFields) {
        List<RecipientDirectory.Entry> incoming = RecipientDirectory.parseAddresses(rawFields);
        if (incoming.isEmpty()) return;
        LinkedHashMap<String, RecipientDirectory.Entry> merged = new LinkedHashMap<>();
        for (RecipientDirectory.Entry existing : load()) {
            merged.put(existing.email.toLowerCase(Locale.ROOT), existing);
        }
        long now = System.currentTimeMillis();
        for (RecipientDirectory.Entry learned : incoming) {
            String key = learned.email.toLowerCase(Locale.ROOT);
            RecipientDirectory.Entry existing = merged.get(key);
            if (existing == null) {
                learned.lastUsed = now;
                merged.put(key, learned);
            } else {
                String name = existing.name.isEmpty() ? learned.name : existing.name;
                RecipientDirectory.Entry updated = new RecipientDirectory.Entry(
                        name, existing.email, existing.count + 1, now);
                merged.put(key, updated);
            }
        }
        ArrayList<RecipientDirectory.Entry> ordered = new ArrayList<>(merged.values());
        ordered.sort((a, b) -> {
            int byTime = Long.compare(b.lastUsed, a.lastUsed);
            return byTime != 0 ? byTime : Integer.compare(b.count, a.count);
        });
        if (ordered.size() > MAX_ENTRIES) {
            ordered = new ArrayList<>(ordered.subList(0, MAX_ENTRIES));
        }
        save(ordered);
    }

    synchronized List<String> suggestions(String query, int limit) {
        return RecipientDirectory.suggestions(load(), query, limit);
    }

    synchronized void clear() {
        preferences.edit().remove(KEY_ENTRIES).apply();
    }

    private List<RecipientDirectory.Entry> load() {
        ArrayList<RecipientDirectory.Entry> out = new ArrayList<>();
        String stored = preferences.getString(KEY_ENTRIES, "[]");
        try {
            JSONArray array = new JSONArray(stored == null ? "[]" : stored);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String email = item.optString("email", "").trim();
                if (email.isEmpty()) continue;
                out.add(new RecipientDirectory.Entry(
                        item.optString("name", ""),
                        email,
                        Math.max(1, item.optInt("count", 1)),
                        Math.max(0L, item.optLong("lastUsed", 0L))));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private void save(List<RecipientDirectory.Entry> entries) {
        JSONArray array = new JSONArray();
        for (RecipientDirectory.Entry entry : entries) {
            try {
                JSONObject item = new JSONObject();
                item.put("name", entry.name);
                item.put("email", entry.email);
                item.put("count", entry.count);
                item.put("lastUsed", entry.lastUsed);
                array.put(item);
            } catch (Exception ignored) {}
        }
        preferences.edit().putString(KEY_ENTRIES, array.toString()).apply();
    }
}
