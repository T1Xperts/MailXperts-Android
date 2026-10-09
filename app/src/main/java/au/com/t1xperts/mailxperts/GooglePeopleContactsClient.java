package au.com.t1xperts.mailxperts;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Minimal Google People REST client for explicit Smart Contacts cloud sync. */
final class GooglePeopleContactsClient {
    static final class SyncResult {
        final List<CloudContactRecord> records;
        final String nextSyncToken;
        final boolean syncTokenExpired;

        SyncResult(List<CloudContactRecord> records, String nextSyncToken, boolean expired) {
            this.records = records;
            this.nextSyncToken = nextSyncToken == null ? "" : nextSyncToken;
            this.syncTokenExpired = expired;
        }
    }

    private static final String API = "https://people.googleapis.com/v1/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient();

    SyncResult fetchAll(String bearer, String syncToken) throws Exception {
        ArrayList<CloudContactRecord> out = new ArrayList<>();
        String pageToken = "";
        String nextSyncToken = "";
        boolean incremental = syncToken != null && !syncToken.trim().isEmpty();
        do {
            StringBuilder url = new StringBuilder(API)
                    .append("people/me/connections?personFields=names,emailAddresses,metadata")
                    .append("&pageSize=1000&requestSyncToken=true");
            if (incremental) url.append("&syncToken=").append(enc(syncToken));
            if (!pageToken.isEmpty()) url.append("&pageToken=").append(enc(pageToken));
            Response response = execute(new Request.Builder().url(url.toString())
                    .header("Authorization", "Bearer " + bearer).get().build());
            if (response.code() == 410 && incremental) {
                response.close();
                return new SyncResult(new ArrayList<>(), "", true);
            }
            String body = bodyOrThrow(response);
            JSONObject json = new JSONObject(body);
            JSONArray connections = json.optJSONArray("connections");
            if (connections != null) {
                for (int i = 0; i < connections.length(); i++) {
                    JSONObject person = connections.optJSONObject(i);
                    if (person == null) continue;
                    parsePerson(person, out);
                }
            }
            pageToken = json.optString("nextPageToken", "");
            String candidate = json.optString("nextSyncToken", "");
            if (!candidate.isEmpty()) nextSyncToken = candidate;
        } while (!pageToken.isEmpty());
        return new SyncResult(out, nextSyncToken, false);
    }

    CloudContactRecord create(String bearer, String name, String email) throws Exception {
        JSONObject payload = personPayload(name, email);
        Request request = new Request.Builder()
                .url(API + "people:createContact?personFields=names,emailAddresses,metadata")
                .header("Authorization", "Bearer " + bearer)
                .post(RequestBody.create(payload.toString(), JSON))
                .build();
        JSONObject created = new JSONObject(bodyOrThrow(execute(request)));
        return firstRecord(created);
    }

    CloudContactRecord update(String bearer, CloudContactRecord remote,
                              String name, String email) throws Exception {
        if (remote == null || remote.remoteId.isEmpty()) return create(bearer, name, email);
        JSONObject latest = getPerson(bearer, remote.remoteId);
        JSONObject payload = personPayload(name, email);
        payload.put("resourceName", remote.remoteId);
        payload.put("etag", latest.optString("etag", remote.etag));
        JSONObject metadata = latest.optJSONObject("metadata");
        if (metadata != null) payload.put("metadata", metadata);
        String url = API + remote.remoteId + ":updateContact"
                + "?updatePersonFields=names,emailAddresses&personFields=names,emailAddresses,metadata";
        Request request = new Request.Builder().url(url)
                .header("Authorization", "Bearer " + bearer)
                .patch(RequestBody.create(payload.toString(), JSON)).build();
        return firstRecord(new JSONObject(bodyOrThrow(execute(request))));
    }

    void delete(String bearer, String remoteId) throws Exception {
        if (remoteId == null || remoteId.trim().isEmpty()) return;
        Request request = new Request.Builder().url(API + remoteId + ":deleteContact")
                .header("Authorization", "Bearer " + bearer).delete().build();
        bodyOrThrow(execute(request));
    }

    private JSONObject getPerson(String bearer, String remoteId) throws Exception {
        String url = API + remoteId + "?personFields=names,emailAddresses,metadata";
        Request request = new Request.Builder().url(url)
                .header("Authorization", "Bearer " + bearer).get().build();
        return new JSONObject(bodyOrThrow(execute(request)));
    }

    private static JSONObject personPayload(String name, String email) throws Exception {
        JSONObject payload = new JSONObject();
        JSONArray emails = new JSONArray();
        emails.put(new JSONObject().put("value", email == null ? "" : email.trim()));
        payload.put("emailAddresses", emails);
        String display = name == null ? "" : name.trim();
        if (!display.isEmpty()) {
            payload.put("names", new JSONArray().put(new JSONObject().put("displayName", display)));
        }
        return payload;
    }

    private static void parsePerson(JSONObject person, List<CloudContactRecord> out) {
        boolean deleted = false;
        JSONObject metadata = person.optJSONObject("metadata");
        if (metadata != null) deleted = metadata.optBoolean("deleted", false);
        String remoteId = person.optString("resourceName", "");
        String etag = person.optString("etag", "");
        String name = "";
        JSONArray names = person.optJSONArray("names");
        if (names != null && names.length() > 0 && names.optJSONObject(0) != null) {
            name = names.optJSONObject(0).optString("displayName", "");
        }
        JSONArray emails = person.optJSONArray("emailAddresses");
        if (emails == null || emails.length() == 0) return;
        for (int i = 0; i < emails.length(); i++) {
            JSONObject emailItem = emails.optJSONObject(i);
            String email = emailItem == null ? "" : emailItem.optString("value", "").trim();
            if (email.isEmpty()) continue;
            out.add(new CloudContactRecord(CloudContactStore.GOOGLE, remoteId, etag,
                    name, email, deleted));
        }
    }

    private static CloudContactRecord firstRecord(JSONObject person) {
        ArrayList<CloudContactRecord> records = new ArrayList<>();
        parsePerson(person, records);
        return records.isEmpty() ? new CloudContactRecord(
                CloudContactStore.GOOGLE,
                person.optString("resourceName", ""), person.optString("etag", ""),
                "", "", false) : records.get(0);
    }

    private Response execute(Request request) throws Exception {
        return http.newCall(request).execute();
    }

    private static String bodyOrThrow(Response response) throws Exception {
        try (Response safe = response) {
            String body = safe.body() == null ? "" : safe.body().string();
            if (!safe.isSuccessful()) {
                throw new IllegalStateException("Google People request failed (HTTP " + safe.code() + ")");
            }
            return body;
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
