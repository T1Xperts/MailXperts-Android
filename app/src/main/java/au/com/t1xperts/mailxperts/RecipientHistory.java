package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Local-first Smart Contacts store.
 *
 * Learned contacts stay on-device. External contact access is mediated through the Android
 * Contacts provider and explicit system UI; this class never uploads address-book data.
 */
final class RecipientHistory {
    private static final String PREFS = "mailxperts_recipient_history_v1";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_SEEN_MESSAGES = "seen_messages";
    private static final String KEY_BLOCKED = "blocked";
    private static final int MAX_ENTRIES = 1000;
    private static final int MAX_SEEN_MESSAGES = 5000;

    private final SharedPreferences preferences;

    RecipientHistory(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Compatibility entry point: composer/senders are outgoing interactions. */
    synchronized void learn(String... rawFields) {
        learnOutgoing(rawFields);
    }

    synchronized void learnOutgoing(String... rawFields) {
        learnInternal(false, "MailXperts sent", "", rawFields);
    }

    synchronized void learnIncoming(String... rawFields) {
        learnInternal(true, "MailXperts received", "", rawFields);
    }

    synchronized void learnMessage(String accountId, String folderKind, long uid,
                                   String... rawFields) {
        learnMessage(accountId, folderKind, uid, "", rawFields);
    }

    synchronized void learnMessage(String accountId, String folderKind, long uid,
                                   String excludedEmail, String... rawFields) {
        if (uid <= 0L) {
            learnInternal(!MailRepository.SENT.equals(folderKind),
                    MailRepository.SENT.equals(folderKind)
                            ? "MailXperts sent" : "MailXperts received",
                    excludedEmail, rawFields);
            return;
        }
        String messageKey = safe(accountId) + "|" + safe(folderKind) + "|" + uid;
        LinkedHashSet<String> seen = loadSet(KEY_SEEN_MESSAGES);
        if (seen.contains(messageKey)) return;
        if (MailRepository.SENT.equals(folderKind)) {
            learnInternal(false, "MailXperts sent", excludedEmail, rawFields);
        } else {
            learnInternal(true, "MailXperts received", excludedEmail, rawFields);
        }
        seen.add(messageKey);
        trimSet(seen, MAX_SEEN_MESSAGES);
        saveSet(KEY_SEEN_MESSAGES, seen);
    }

    synchronized void clearSeenForAccountFolder(String accountId, String folderKind) {
        String prefix = safe(accountId) + "|" + safe(folderKind) + "|";
        LinkedHashSet<String> seen = loadSet(KEY_SEEN_MESSAGES);
        boolean changed = false;
        ArrayList<String> snapshot = new ArrayList<>(seen);
        for (String value : snapshot) {
            if (value.startsWith(prefix)) {
                seen.remove(value);
                changed = true;
            }
        }
        if (changed) saveSet(KEY_SEEN_MESSAGES, seen);
    }

    synchronized void importEntries(List<RecipientDirectory.Entry> entries, String source) {
        if (entries == null || entries.isEmpty()) return;
        LinkedHashMap<String, RecipientDirectory.Entry> merged = index(load());
        long now = System.currentTimeMillis();
        for (RecipientDirectory.Entry incoming : entries) {
            if (incoming == null || !RecipientDirectory.isLearnableEmail(incoming.email)) continue;
            String key = incoming.email.toLowerCase(Locale.ROOT);
            if (isBlockedKey(key)) continue;
            RecipientDirectory.Entry existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new RecipientDirectory.Entry(
                        incoming.name, incoming.email, 1, now, now,
                        0, 0, source == null ? "Device contacts" : source));
            } else {
                String name = existing.name.isEmpty() ? incoming.name : existing.name;
                String mergedSource = existing.source.contains("Device")
                        ? existing.source : existing.source + " + Device contacts";
                merged.put(key, new RecipientDirectory.Entry(
                        name, existing.email, existing.count, existing.lastUsed,
                        existing.firstSeen, existing.incomingCount, existing.outgoingCount,
                        mergedSource));
            }
        }
        save(orderedAndTrim(merged));
    }

    synchronized List<String> suggestions(String query, int limit) {
        return RecipientDirectory.suggestions(load(), query, limit);
    }

    synchronized List<RecipientDirectory.Entry> entries() {
        ArrayList<RecipientDirectory.Entry> copy = new ArrayList<>();
        for (RecipientDirectory.Entry entry : load()) copy.add(entry.copy());
        return copy;
    }

    synchronized boolean hasLearned(String email) {
        if (email == null) return false;
        return index(load()).containsKey(email.trim().toLowerCase(Locale.ROOT));
    }

    synchronized void remove(String email) {
        if (email == null) return;
        String key = email.trim().toLowerCase(Locale.ROOT);
        LinkedHashMap<String, RecipientDirectory.Entry> merged = index(load());
        if (merged.remove(key) != null) save(new ArrayList<>(merged.values()));
    }

    synchronized void block(String email) {
        if (email == null || email.trim().isEmpty()) return;
        String key = email.trim().toLowerCase(Locale.ROOT);
        remove(key);
        LinkedHashSet<String> blocked = loadSet(KEY_BLOCKED);
        blocked.add(key);
        saveSet(KEY_BLOCKED, blocked);
    }

    synchronized void unblock(String email) {
        if (email == null) return;
        LinkedHashSet<String> blocked = loadSet(KEY_BLOCKED);
        if (blocked.remove(email.trim().toLowerCase(Locale.ROOT))) saveSet(KEY_BLOCKED, blocked);
    }

    synchronized boolean isBlocked(String email) {
        return email != null && isBlockedKey(email.trim().toLowerCase(Locale.ROOT));
    }

    synchronized void clear() {
        preferences.edit()
                .remove(KEY_ENTRIES)
                .remove(KEY_SEEN_MESSAGES)
                .apply();
    }

    private void learnInternal(boolean incomingDirection, String source,
                               String excludedEmail, String... rawFields) {
        List<RecipientDirectory.Entry> incoming = RecipientDirectory.parseAddresses(rawFields);
        if (incoming.isEmpty()) return;
        LinkedHashMap<String, RecipientDirectory.Entry> merged = index(load());
        long now = System.currentTimeMillis();
        for (RecipientDirectory.Entry learned : incoming) {
            if (!RecipientDirectory.isLearnableEmail(learned.email)) continue;
            String key = learned.email.toLowerCase(Locale.ROOT);
            String excluded = excludedEmail == null ? "" : excludedEmail.trim().toLowerCase(Locale.ROOT);
            if (!excluded.isEmpty() && key.equals(excluded)) continue;
            if (isBlockedKey(key)) continue;
            RecipientDirectory.Entry existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new RecipientDirectory.Entry(
                        learned.name, learned.email, 1, now, now,
                        incomingDirection ? 1 : 0,
                        incomingDirection ? 0 : 1,
                        source));
            } else {
                String name = existing.name.isEmpty() ? learned.name : existing.name;
                String mergedSource = existing.source.contains(source)
                        ? existing.source : existing.source + " + " + source;
                merged.put(key, new RecipientDirectory.Entry(
                        name,
                        existing.email,
                        existing.count + 1,
                        now,
                        existing.firstSeen > 0L ? existing.firstSeen : now,
                        existing.incomingCount + (incomingDirection ? 1 : 0),
                        existing.outgoingCount + (incomingDirection ? 0 : 1),
                        mergedSource));
            }
        }
        save(orderedAndTrim(merged));
    }

    private LinkedHashMap<String, RecipientDirectory.Entry> index(
            List<RecipientDirectory.Entry> entries) {
        LinkedHashMap<String, RecipientDirectory.Entry> merged = new LinkedHashMap<>();
        for (RecipientDirectory.Entry existing : entries) {
            merged.put(existing.email.toLowerCase(Locale.ROOT), existing);
        }
        return merged;
    }

    private ArrayList<RecipientDirectory.Entry> orderedAndTrim(
            Map<String, RecipientDirectory.Entry> merged) {
        ArrayList<RecipientDirectory.Entry> ordered = new ArrayList<>(merged.values());
        ordered.sort((a, b) -> {
            int byTime = Long.compare(b.lastUsed, a.lastUsed);
            return byTime != 0 ? byTime : Integer.compare(b.count, a.count);
        });
        if (ordered.size() > MAX_ENTRIES) {
            ordered = new ArrayList<>(ordered.subList(0, MAX_ENTRIES));
        }
        return ordered;
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
                long lastUsed = Math.max(0L, item.optLong("lastUsed", 0L));
                int count = Math.max(1, item.optInt("count", 1));
                out.add(new RecipientDirectory.Entry(
                        item.optString("name", ""),
                        email,
                        count,
                        lastUsed,
                        Math.max(0L, item.optLong("firstSeen", lastUsed)),
                        Math.max(0, item.optInt("incomingCount", 0)),
                        Math.max(0, item.optInt("outgoingCount", count)),
                        item.optString("source", "MailXperts")));
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
                item.put("firstSeen", entry.firstSeen);
                item.put("incomingCount", entry.incomingCount);
                item.put("outgoingCount", entry.outgoingCount);
                item.put("source", entry.source);
                array.put(item);
            } catch (Exception ignored) {}
        }
        preferences.edit().putString(KEY_ENTRIES, array.toString()).apply();
    }

    private boolean isBlockedKey(String key) {
        return loadSet(KEY_BLOCKED).contains(key);
    }

    private LinkedHashSet<String> loadSet(String key) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String stored = preferences.getString(key, "[]");
        try {
            JSONArray array = new JSONArray(stored == null ? "[]" : stored);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "").trim();
                if (!value.isEmpty()) out.add(value);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private void saveSet(String key, Set<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        preferences.edit().putString(key, array.toString()).apply();
    }

    private static void trimSet(LinkedHashSet<String> values, int max) {
        while (values.size() > max) {
            String first = values.iterator().next();
            values.remove(first);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
