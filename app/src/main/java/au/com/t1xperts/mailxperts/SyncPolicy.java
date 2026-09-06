package au.com.t1xperts.mailxperts;

/** Validated user-selectable mailbox refresh policy. */
final class SyncPolicy {
    static final int MANUAL = 0;
    static final int DEFAULT_INTERVAL_MINUTES = 15;
    static final int[] INTERVAL_MINUTES = {MANUAL, 15, 30, 60, 120, 360, 720, 1_440};
    static final String[] INTERVAL_LABELS = {
            "Manual only", "Every 15 minutes", "Every 30 minutes", "Every hour",
            "Every 2 hours", "Every 6 hours", "Every 12 hours", "Every 24 hours"
    };

    private SyncPolicy() {}

    static int normalizeInterval(int value) {
        for (int allowed : INTERVAL_MINUTES) if (value == allowed) return value;
        return DEFAULT_INTERVAL_MINUTES;
    }

    static int indexOf(int value) {
        int normalized = normalizeInterval(value);
        for (int index = 0; index < INTERVAL_MINUTES.length; index++) {
            if (INTERVAL_MINUTES[index] == normalized) return index;
        }
        return 1;
    }

    static long intervalMillis(int value) {
        int normalized = normalizeInterval(value);
        return normalized <= 0 ? 0L : normalized * 60_000L;
    }

    static boolean isDue(long lastSuccessfulSyncAt, long now, int intervalMinutes) {
        long interval = intervalMillis(intervalMinutes);
        return interval > 0L && (lastSuccessfulSyncAt <= 0L
                || now - lastSuccessfulSyncAt >= interval);
    }
}
