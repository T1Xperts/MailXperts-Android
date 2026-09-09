package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28));
        root.setBackgroundColor(Ui.background(this));

        try {
            ImageView logo = new ImageView(this);
            logo.setImageResource(R.drawable.mailxperts_icon);
            logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            logo.setContentDescription("MailXperts");
            root.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 164), Ui.dp(this, 164)));
        } catch (Throwable ignored) {
            // Text branding below remains a complete fallback if the icon cannot be decoded.
        }

        TextView title = new TextView(this);
        title.setText("MailXperts");
        title.setGravity(Gravity.CENTER);
        title.setTextSize(31f);
        title.setTextColor(Ui.teal(this));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = Ui.dp(this, 16);
        root.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Smart Email Client\nA T1Xperts App");
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextSize(16f);
        subtitle.setTextColor(Ui.muted(this));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = Ui.dp(this, 8);
        root.addView(subtitle, subtitleParams);

        setContentView(root);

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
