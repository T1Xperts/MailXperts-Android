package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.Window;

final class ThemeManager {
    static final String AUTO = "AUTO";
    static final String LIGHT = "LIGHT";
    static final String DARK = "DARK";
    private static final String PREFS = "mailxperts_ui_v1";
    private static final String KEY_MODE = "theme_mode";

    private ThemeManager() {}

    static String mode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, AUTO);
    }

    static void setMode(Context context, String mode) {
        if (!LIGHT.equals(mode) && !DARK.equals(mode)) mode = AUTO;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode).apply();
    }

    static boolean isDark(Context context) {
        String selected = mode(context);
        if (DARK.equals(selected)) return true;
        if (LIGHT.equals(selected)) return false;
        int mask = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mask == Configuration.UI_MODE_NIGHT_YES;
    }

    static void apply(Activity activity) {
        boolean dark = isDark(activity);
        try {
            activity.setTheme(dark ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        } catch (Throwable ignored) {
            // The manifest theme remains a safe fallback.
        }
        try {
            Window window = activity.getWindow();
            if (window == null) return;
            window.setStatusBarColor(Ui.background(activity));
            window.setNavigationBarColor(Ui.background(activity));
            int flags = window.getDecorView().getSystemUiVisibility();
            if (dark) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        } catch (Throwable ignored) {
            // Window decoration must never prevent the mailbox from opening.
        }
    }
}
