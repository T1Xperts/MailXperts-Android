package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.content.SharedPreferences;

final class AiPreferences {
    private static final String PREFS = "mailxperts_ai_v1";
    private static final String KEY_GATEWAY = "gateway";
    private static final String KEY_CLOUD_AI_CONSENT = "cloud_ai_consent";
    private final SharedPreferences prefs;

    AiPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    String gateway() {
        return prefs.getString(KEY_GATEWAY, "");
    }

    void setGateway(String value) {
        prefs.edit().putString(KEY_GATEWAY, AiPolicy.normaliseGateway(value)).apply();
    }

    /** Explicit, revocable consent. Defaults off for existing and new installations. */
    boolean cloudAiAllowed() {
        return prefs.getBoolean(KEY_CLOUD_AI_CONSENT, false);
    }

    void setCloudAiAllowed(boolean allowed) {
        prefs.edit().putBoolean(KEY_CLOUD_AI_CONSENT, allowed).apply();
    }

    void clear() {
        prefs.edit().remove(KEY_GATEWAY).putBoolean(KEY_CLOUD_AI_CONSENT, false).apply();
    }
}
