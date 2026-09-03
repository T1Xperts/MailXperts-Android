package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openApp = () -> {
        if (isFinishing()) return;
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    };

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        try {
            ImageView splash = new ImageView(this);
            splash.setScaleType(ImageView.ScaleType.CENTER_CROP);
            splash.setContentDescription("MailXperts Smart Email Client");
            splash.setImageResource(ThemeManager.isDark(this)
                    ? R.drawable.mailxperts_splash_dark
                    : R.drawable.mailxperts_splash_light);
            setContentView(splash);
        } catch (Throwable ignored) {
            TextView fallback = new TextView(this);
            fallback.setText("MailXperts\nSmart Email Client");
            fallback.setGravity(Gravity.CENTER);
            fallback.setTextSize(28f);
            fallback.setTextColor(Ui.teal(this));
            fallback.setBackgroundColor(Ui.background(this));
            setContentView(fallback);
        }
        try {
            handler.postDelayed(openApp, 1100L);
        } catch (Throwable ignored) {
            openApp.run();
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(openApp);
        super.onDestroy();
    }
}
