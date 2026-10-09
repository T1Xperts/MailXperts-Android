package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Non-secret provider state for MX-QA-027. Credentials are kept in ContactSecretVault. */
final class CloudContactStore {
    static final String GOOGLE = "google";
    static final String ICLOUD = "icloud";

    private static final String PREFS = "mailxperts_cloud_contacts_v1";
    private final SharedPreferences prefs;

    CloudContactStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized CloudContactSyncMode mode(String provider) {
        return CloudContactSyncMode.parse(prefs.getString("mode_" + provider, "DISCONNECTED"));
    }

    synchronized void setMode(String provider, CloudContactSyncMode mode) {
        CloudContactSyncMode safe = mode == null ? CloudContactSyncMode.DISCONNECTED : mode;
        prefs.edit().putString("mode_" + provider, safe.name()).apply();
    }

    synchronized String syncToken(String provider) {
        return prefs.getString("sync_token_" + provider, "");
    }

    synchronized void setSyncToken(String provider, String value) {
        prefs.edit().putString("sync_token_" + provider, safe(value)).apply();
    }

    synchronized String collectionUrl(String provider) {
        return prefs.getString("collection_url_" + provider, "");
    }

    synchronized void setCollectionUrl(String provider, String value) {
        prefs.edit().putString("collection_url_" + provider, safe(value)).apply();
    }

    synchronized void promote(String provider, String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) return;
        LinkedHashSet<String> values = promoted(provider);
        values.add(normalized);
        saveStringSet("promoted_" + provider, values);
    }

    synchronized void unpromote(String provider, String email) {
        LinkedHashSet<String> values = promoted(provider);
        if (values.remove(normalizeEmail(email))) saveStringSet("promoted_" + provider, values);
    }

    synchronized LinkedHashSet<String> promoted(String provider) {
        return loadStringSet("promoted_" + provider);
    }

    synchronized Map<String, CloudContactRecord> mappings(String provider) {
        LinkedHashMap<String, CloudContactRecord> out = new LinkedHashMap<>();
        String raw = prefs.getString("mappings_" + provider, "[]");
        try {
            JSONArray array = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                CloudContactRecord record = new CloudContactRecord(
                        provider,
                        item.optString("remoteId", ""),
                        item.optString("etag", ""),
                        item.optString("name", ""),
                        item.optString("email", ""),
                        item.optBoolean("deleted", false));
                String key = normalizeEmail(record.email);
                if (!key.isEmpty()) out.put(key, record);
            }
        } catch (Exception ignored) {}
        return out;
    }

    synchronized void replaceMappings(String provider, List<CloudContactRecord> records) {
        LinkedHashMap<String, CloudContactRecord> merged = new LinkedHashMap<>(mappings(provider));
        if (records != null) {
            for (CloudContactRecord record : records) {
                if (record == null) continue;
                if (record.deleted && !record.remoteId.isEmpty()) {
                    removeRemoteId(merged, record.remoteId);
                    continue;
                }
                String key = normalizeEmail(record.email);
                if (key.isEmpty()) continue;
                removeRemoteId(merged, record.remoteId);
                merged.put(key, record);
            }
        }
        saveMappings(provider, merged.values());
    }

    synchronized void putMapping(String provider, CloudContactRecord record) {
        if (record == null) return;
        LinkedHashMap<String, CloudContactRecord> merged = new LinkedHashMap<>(mappings(provider));
        if (record.deleted && !record.remoteId.isEmpty()) {
            removeRemoteId(merged, record.remoteId);
            saveMappings(provider, merged.values());
            return;
        }
        String key = normalizeEmail(record.email);
        if (key.isEmpty()) return;
        removeRemoteId(merged, record.remoteId);
        merged.put(key, record);
        saveMappings(provider, merged.values());
    }

    synchronized void removeMappingByRemoteId(String provider, String remoteId) {
        if (remoteId == null || remoteId.trim().isEmpty()) return;
        LinkedHashMap<String, CloudContactRecord> merged = new LinkedHashMap<>(mappings(provider));
        if (removeRemoteId(merged, remoteId.trim())) saveMappings(provider, merged.values());
    }

    synchronized void clearProvider(String provider) {
        prefs.edit()
                .remove("mode_" + provider)
                .remove("sync_token_" + provider)
                .remove("collection_url_" + provider)
                .remove("promoted_" + provider)
                .remove("mappings_" + provider)
                .apply();
    }

    private static boolean removeRemoteId(Map<String, CloudContactRecord> records, String remoteId) {
        if (remoteId == null || remoteId.trim().isEmpty()) return false;
        String target = remoteId.trim();
        String found = null;
        for (Map.Entry<String, CloudContactRecord> entry : records.entrySet()) {
            if (target.equals(entry.getValue().remoteId)) {
                found = entry.getKey();
                break;
            }
        }
        if (found == null) return false;
        records.remove(found);
        return true;
    }

    private void saveMappings(String provider, Iterable<CloudContactRecord> records) {
        JSONArray array = new JSONArray();
        for (CloudContactRecord record : records) {
            try {
                JSONObject item = new JSONObject();
                item.put("remoteId", record.remoteId);
                item.put("etag", record.etag);
                item.put("name", record.name);
                item.put("email", record.email);
                item.put("deleted", record.deleted);
                array.put(item);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString("mappings_" + provider, array.toString()).apply();
    }

    private LinkedHashSet<String> loadStringSet(String key) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String raw = prefs.getString(key, "[]");
        try {
            JSONArray array = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "").trim();
                if (!value.isEmpty()) out.add(value);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private void saveStringSet(String key, Set<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        prefs.edit().putString(key, array.toString()).apply();
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
