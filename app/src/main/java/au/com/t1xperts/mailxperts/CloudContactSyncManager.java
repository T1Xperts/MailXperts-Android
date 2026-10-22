package au.com.t1xperts.mailxperts;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Pull-first, explicit-push synchronisation coordinator for MX-QA-027. */
final class CloudContactSyncManager {
    static final class Result {
        final String provider;
        final int pulled;
        final int pushed;
        final String message;

        Result(String provider, int pulled, int pushed, String message) {
            this.provider = provider;
            this.pulled = pulled;
            this.pushed = pushed;
            this.message = message == null ? "" : message;
        }
    }

    private final Context context;
    private final RecipientHistory history;
    private final CloudContactStore store;

    CloudContactSyncManager(Context context) {
        this.context = context.getApplicationContext();
        history = new RecipientHistory(this.context);
        store = new CloudContactStore(this.context);
    }

    Result syncGoogle() throws Exception {
        CloudContactSyncMode mode = store.mode(CloudContactStore.GOOGLE);
        if (!mode.canRead()) return new Result(CloudContactStore.GOOGLE, 0, 0, "Google Contacts is disconnected");
        boolean write = mode.canPushSelected();
        String bearer = GoogleContactsOAuthManager.acquireSilently(context, write);
        GooglePeopleContactsClient client = new GooglePeopleContactsClient();
        String syncToken = store.syncToken(CloudContactStore.GOOGLE);
        GooglePeopleContactsClient.SyncResult remote = client.fetchAll(bearer, syncToken);
        if (remote.syncTokenExpired) {
            store.setSyncToken(CloudContactStore.GOOGLE, "");
            remote = client.fetchAll(bearer, "");
        }
        int pulled = importRemote(CloudContactStore.GOOGLE, remote.records);
        if (!remote.nextSyncToken.isEmpty()) {
            store.setSyncToken(CloudContactStore.GOOGLE, remote.nextSyncToken);
        }
        int pushed = write ? pushGoogle(client, bearer) : 0;
        return new Result(CloudContactStore.GOOGLE, pulled, pushed,
                "Google Contacts sync complete");
    }

    Result syncICloud() throws Exception {
        CloudContactSyncMode mode = store.mode(CloudContactStore.ICLOUD);
        if (!mode.canRead()) return new Result(CloudContactStore.ICLOUD, 0, 0, "iCloud / CardDAV is disconnected");
        ContactSecretVault.CardDavCredential credential = new ContactSecretVault(context).loadICloud();
        if (!credential.configured()) throw new IllegalStateException("iCloud / CardDAV credentials are not configured");
        CardDavContactsClient client = new CardDavContactsClient();
        String collectionUrl = store.collectionUrl(CloudContactStore.ICLOUD);
        if (collectionUrl.isEmpty()) {
            CardDavContactsClient.Discovery discovery = client.discover(credential);
            collectionUrl = discovery.addressBookUrl;
            store.setCollectionUrl(CloudContactStore.ICLOUD, collectionUrl);
        }
        List<CloudContactRecord> remote = client.fetchAll(credential, collectionUrl);
        int pulled = importRemote(CloudContactStore.ICLOUD, remote);
        int pushed = mode.canPushSelected() ? pushICloud(client, credential, collectionUrl) : 0;
        return new Result(CloudContactStore.ICLOUD, pulled, pushed,
                "iCloud / CardDAV sync complete");
    }

    void promote(String provider, String email) {
        store.promote(provider, email);
    }

    void unpromote(String provider, String email) {
        store.unpromote(provider, email);
    }

    private int importRemote(String provider, List<CloudContactRecord> records) {
        ArrayList<RecipientDirectory.Entry> importable = new ArrayList<>();
        int pulled = 0;
        if (records != null) {
            for (CloudContactRecord record : records) {
                if (record == null) continue;
                if (record.deleted) {
                    store.removeMappingByRemoteId(provider, record.remoteId);
                    continue;
                }
                if (record.email.isEmpty()) continue;
                if (!RecipientDirectory.isLearnableEmail(record.email)) continue;
                importable.add(record.asDirectoryEntry());
                pulled++;
            }
        }
        if (!importable.isEmpty()) {
            history.importEntries(importable,
                    CloudContactStore.GOOGLE.equals(provider)
                            ? "Google Contacts" : "iCloud / CardDAV");
        }
        store.replaceMappings(provider, records);
        return pulled;
    }

    private int pushGoogle(GooglePeopleContactsClient client, String bearer) throws Exception {
        Map<String, CloudContactRecord> mapped = store.mappings(CloudContactStore.GOOGLE);
        LinkedHashSet<String> promoted = store.promoted(CloudContactStore.GOOGLE);
        int pushed = 0;
        for (RecipientDirectory.Entry local : history.entries()) {
            String key = CloudContactStore.normalizeEmail(local.email);
            if (!promoted.contains(key)) continue;
            CloudContactRecord existing = mapped.get(key);
            CloudContactRecord result = existing == null
                    ? client.create(bearer, local.name, local.email)
                    : client.update(bearer, existing, local.name, local.email);
            if (result != null && !result.email.isEmpty()) {
                store.putMapping(CloudContactStore.GOOGLE, result);
                pushed++;
            }
        }
        return pushed;
    }

    private int pushICloud(CardDavContactsClient client,
                           ContactSecretVault.CardDavCredential credential,
                           String collectionUrl) throws Exception {
        Map<String, CloudContactRecord> mapped = store.mappings(CloudContactStore.ICLOUD);
        LinkedHashSet<String> promoted = store.promoted(CloudContactStore.ICLOUD);
        int pushed = 0;
        for (RecipientDirectory.Entry local : history.entries()) {
            String key = CloudContactStore.normalizeEmail(local.email);
            if (!promoted.contains(key)) continue;
            CloudContactRecord result = client.upsert(
                    credential, collectionUrl, mapped.get(key), local.name, local.email);
            store.putMapping(CloudContactStore.ICLOUD, result);
            pushed++;
        }
        return pushed;
    }
}
