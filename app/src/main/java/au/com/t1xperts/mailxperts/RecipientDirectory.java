package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.InternetAddress;

/** Pure-Java recipient parsing/ranking used by the Android autocomplete store and JVM tests. */
final class RecipientDirectory {
    static final class Entry {
        final String name;
        final String email;
        int count;
        long lastUsed;

        Entry(String name, String email, int count, long lastUsed) {
            this.name = name == null ? "" : name.trim();
            this.email = email == null ? "" : email.trim();
            this.count = Math.max(1, count);
            this.lastUsed = Math.max(0L, lastUsed);
        }

        String label() {
            return name.isEmpty() ? email : name + " <" + email + ">";
        }
    }

    private RecipientDirectory() {}

    static List<Entry> parseAddresses(String... rawFields) {
        LinkedHashMap<String, Entry> unique = new LinkedHashMap<>();
        if (rawFields == null) return new ArrayList<>();
        for (String raw : rawFields) {
            if (raw == null || raw.trim().isEmpty()) continue;
            try {
                String normalised = RecipientNormalizer.normalise(raw);
                for (InternetAddress address : InternetAddress.parse(normalised, false)) {
                    String email = address.getAddress() == null ? "" : address.getAddress().trim();
                    if (email.isEmpty()) continue;
                    String key = email.toLowerCase(Locale.ROOT);
                    String personal = address.getPersonal() == null ? "" : address.getPersonal().trim();
                    unique.putIfAbsent(key, new Entry(personal, email, 1, 0L));
                }
            } catch (Exception ignored) {
                // Invalid entries are rejected by the composer; never learn them.
            }
        }
        return new ArrayList<>(unique.values());
    }

    static List<String> suggestions(List<Entry> entries, String query, int limit) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        while (needle.startsWith(",") || needle.startsWith(";")) needle = needle.substring(1).trim();
        int safeLimit = Math.max(1, Math.min(20, limit));
        ArrayList<Entry> matches = new ArrayList<>();
        if (entries != null) {
            for (Entry entry : entries) {
                if (entry == null || entry.email.isEmpty()) continue;
                String email = entry.email.toLowerCase(Locale.ROOT);
                String name = entry.name.toLowerCase(Locale.ROOT);
                if (needle.isEmpty() || email.contains(needle) || name.contains(needle)) matches.add(entry);
            }
        }
        final String q = needle;
        matches.sort(Comparator
                .comparingInt((Entry e) -> prefixRank(e, q))
                .thenComparing((Entry e) -> -e.count)
                .thenComparing((Entry e) -> -e.lastUsed)
                .thenComparing(e -> e.email.toLowerCase(Locale.ROOT)));
        ArrayList<String> out = new ArrayList<>();
        for (Entry entry : matches) {
            out.add(entry.label());
            if (out.size() >= safeLimit) break;
        }
        return out;
    }

    private static int prefixRank(Entry entry, String query) {
        if (query.isEmpty()) return 1;
        String name = entry.name.toLowerCase(Locale.ROOT);
        String email = entry.email.toLowerCase(Locale.ROOT);
        if (name.startsWith(query) || email.startsWith(query)) return 0;
        return 1;
    }
}
