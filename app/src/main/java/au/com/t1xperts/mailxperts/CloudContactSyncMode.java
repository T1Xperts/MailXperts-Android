package au.com.t1xperts.mailxperts;

/** Explicit user-selected cloud contact synchronisation behaviour. */
enum CloudContactSyncMode {
    DISCONNECTED,
    READ_ONLY,
    SELECTED_PUSH,
    TWO_WAY;

    boolean canRead() {
        return this != DISCONNECTED;
    }

    boolean canPushSelected() {
        return this == SELECTED_PUSH || this == TWO_WAY;
    }

    static CloudContactSyncMode parse(String value) {
        if (value == null || value.trim().isEmpty()) return DISCONNECTED;
        try {
            return valueOf(value.trim());
        } catch (Exception ignored) {
            return DISCONNECTED;
        }
    }
}
