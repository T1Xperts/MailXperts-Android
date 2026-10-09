package au.com.t1xperts.mailxperts;

/** Provider-neutral cloud contact projection used by Google People and CardDAV connectors. */
final class CloudContactRecord {
    final String provider;
    final String remoteId;
    final String etag;
    final String name;
    final String email;
    final boolean deleted;

    CloudContactRecord(String provider, String remoteId, String etag,
                       String name, String email, boolean deleted) {
        this.provider = safe(provider);
        this.remoteId = safe(remoteId);
        this.etag = safe(etag);
        this.name = safe(name);
        this.email = safe(email);
        this.deleted = deleted;
    }

    RecipientDirectory.Entry asDirectoryEntry() {
        return new RecipientDirectory.Entry(name, email, 1, System.currentTimeMillis(),
                System.currentTimeMillis(), 0, 0,
                "google".equals(provider) ? "Google Contacts" : "iCloud / CardDAV");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
