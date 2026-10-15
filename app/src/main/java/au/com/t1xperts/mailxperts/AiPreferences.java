package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.content.SharedPreferences;

final class AiPreferences {
    private static final String PREFS = "mailxperts_ai_v1";
    private static final String KEY_GATEWAY = "gateway";
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

    void clear() {
        prefs.edit().remove(KEY_GATEWAY).apply();
    }
}
