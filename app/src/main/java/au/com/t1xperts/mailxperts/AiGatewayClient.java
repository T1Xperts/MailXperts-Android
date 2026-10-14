package au.com.t1xperts.mailxperts;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

final class AiGatewayClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final AiPreferences preferences;
    private final AiSecretVault vault;
    private final OkHttpClient http;

    AiGatewayClient(Context context) {
        preferences = new AiPreferences(context);
        vault = new AiSecretVault(context);
        http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(75, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS)
                .build();
    }

    boolean configured() {
        String endpoint = preferences.gateway();
        String token = vault.token();
        if (endpoint == null || endpoint.trim().isEmpty() || token.isEmpty()) return false;
        try { AiPolicy.normaliseGateway(endpoint); return true; }
        catch (IllegalArgumentException ignored) { return false; }
    }

    Result execute(String task, JSONObject payload) throws Exception {
        if (!AiPolicy.isAllowedTask(task)) throw new IllegalArgumentException("Unsupported AI task.");
        String endpoint = AiPolicy.normaliseGateway(preferences.gateway());
        String token = vault.token();
        if (token.isEmpty()) throw new IllegalStateException("MailXperts AI is not configured.");

        JSONObject requestJson = payload == null ? new JSONObject() : new JSONObject(payload.toString());
        requestJson.put("task", task);
        Request request = new Request.Builder()
                .url(endpoint + "/v1/mail/tasks")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .post(RequestBody.create(requestJson.toString(), JSON))
                .build();

        try (Response response = http.newCall(request).execute()) {
            String raw = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                String message = "MailXperts AI request failed (HTTP " + response.code() + ").";
                try {
                    JSONObject error = new JSONObject(raw);
                    String safe = error.optString("message", "").trim();
                    if (!safe.isEmpty()) message = safe;
                } catch (Exception ignored) {}
                throw new IOException(message);
            }
            JSONObject json = new JSONObject(raw);
            return new Result(json.optString("text", ""),
                    strings(json.optJSONArray("keywords")),
                    strings(json.optJSONArray("message_keys")));
        }
    }

    private static List<String> strings(JSONArray array) {
        if (array == null || array.length() == 0) return Collections.emptyList();
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty()) out.add(value);
        }
        return Collections.unmodifiableList(out);
    }

    static final class Result {
        final String text;
        final List<String> keywords;
        final List<String> messageKeys;
        Result(String text, List<String> keywords, List<String> messageKeys) {
            this.text = text == null ? "" : text;
            this.keywords = keywords == null ? Collections.emptyList() : keywords;
            this.messageKeys = messageKeys == null ? Collections.emptyList() : messageKeys;
        }
    }
}
