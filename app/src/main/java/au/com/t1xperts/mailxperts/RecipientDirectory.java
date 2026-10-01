package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.InternetAddress;

/** Pure-Java contact parsing/ranking shared by Smart Contacts and composer autocomplete. */
final class RecipientDirectory {
    static final class Entry {
        final String name;
        final String email;
        int count;
        long lastUsed;
        long firstSeen;
        int incomingCount;
        int outgoingCount;
        String source;

        Entry(String name, String email, int count, long lastUsed) {
            this(name, email, count, lastUsed, lastUsed, 0, Math.max(1, count), "MailXperts");
        }

        Entry(String name, String email, int count, long lastUsed, long firstSeen,
              int incomingCount, int outgoingCount, String source) {
            this.name = name == null ? "" : name.trim();
            this.email = email == null ? "" : email.trim();
            this.count = Math.max(1, count);
            this.lastUsed = Math.max(0L, lastUsed);
            this.firstSeen = Math.max(0L, firstSeen);
            this.incomingCount = Math.max(0, incomingCount);
            this.outgoingCount = Math.max(0, outgoingCount);
            this.source = source == null || source.trim().isEmpty()
                    ? "MailXperts" : source.trim();
        }

        Entry copy() {
            return new Entry(name, email, count, lastUsed, firstSeen,
                    incomingCount, outgoingCount, source);
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
                    Entry existing = unique.get(key);
                    if (existing == null) {
                        unique.put(key, new Entry(personal, email, 1, 0L));
                    } else if (existing.name.isEmpty() && !personal.isEmpty()) {
                        unique.put(key, new Entry(personal, existing.email, existing.count,
                                existing.lastUsed, existing.firstSeen, existing.incomingCount,
                                existing.outgoingCount, existing.source));
                    }
                }
            } catch (Exception ignored) {
                // Invalid entries are rejected by the composer; never learn them.
            }
        }
        return new ArrayList<>(unique.values());
    }

    static boolean isLearnableEmail(String email) {
        if (email == null) return false;
        String value = email.trim().toLowerCase(Locale.ROOT);
        int at = value.lastIndexOf('@');
        if (at <= 0 || at >= value.length() - 1) return false;
        String local = value.substring(0, at)
                .replace(".", "")
                .replace("_", "")
                .replace("-", "");
        return !(local.equals("noreply")
                || local.equals("donotreply")
                || local.equals("mailerdaemon")
                || local.equals("postmaster")
                || local.equals("bounce")
                || local.equals("bounces")
                || local.equals("notifications")
                || local.equals("notification"));
    }

    static List<Entry> mergeEntries(List<Entry> primary, List<Entry> secondary) {
        LinkedHashMap<String, Entry> merged = new LinkedHashMap<>();
        addMerged(merged, primary);
        addMerged(merged, secondary);
        return new ArrayList<>(merged.values());
    }

    private static void addMerged(Map<String, Entry> merged, List<Entry> entries) {
        if (entries == null) return;
        for (Entry candidate : entries) {
            if (candidate == null || candidate.email == null || candidate.email.trim().isEmpty()) continue;
            String key = candidate.email.trim().toLowerCase(Locale.ROOT);
            Entry existing = merged.get(key);
            if (existing == null) {
                merged.put(key, candidate.copy());
                continue;
            }
            String preferredName = !existing.name.isEmpty() ? existing.name : candidate.name;
            String preferredSource = existing.source.equals(candidate.source)
                    ? existing.source
                    : existing.source + " + " + candidate.source;
            merged.put(key, new Entry(
                    preferredName,
                    existing.email,
                    Math.max(existing.count, candidate.count),
                    Math.max(existing.lastUsed, candidate.lastUsed),
                    earliest(existing.firstSeen, candidate.firstSeen),
                    Math.max(existing.incomingCount, candidate.incomingCount),
                    Math.max(existing.outgoingCount, candidate.outgoingCount),
                    preferredSource));
        }
    }

    private static long earliest(long a, long b) {
        if (a <= 0L) return b;
        if (b <= 0L) return a;
        return Math.min(a, b);
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
