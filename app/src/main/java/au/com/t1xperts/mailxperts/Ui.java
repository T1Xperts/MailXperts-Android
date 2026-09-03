package au.com.t1xperts.mailxperts;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    private Ui() {}

    /**
     * Installs app content inside a protected safe area.
     *
     * Android 15+ enforces edge-to-edge layout for target API 35. Some vendor
     * builds also report transient or zero visible insets while the window is
     * starting. Use the stable system-bar/cutout dimensions and add a small
     * visual gutter so menu, title and compose controls never touch the phone
     * status icons.
     */
    static void setContentView(Activity activity, View content) {
        if (Build.VERSION.SDK_INT >= 30) {
            final int baseLeft = content.getPaddingLeft();
            final int baseTop = content.getPaddingTop();
            final int baseRight = content.getPaddingRight();
            final int baseBottom = content.getPaddingBottom();
            content.setOnApplyWindowInsetsListener(new SafeAreaInsetsListener(
                    baseLeft, baseTop, baseRight, baseBottom, dp(activity, 12)));
        }
        activity.setContentView(content);
        if (Build.VERSION.SDK_INT >= 30) content.requestApplyInsets();
    }

    @TargetApi(Build.VERSION_CODES.R)
    private static final class SafeAreaInsetsListener implements View.OnApplyWindowInsetsListener {
        private final int baseLeft;
        private final int baseTop;
        private final int baseRight;
        private final int baseBottom;
        private final int headerGutter;

        SafeAreaInsetsListener(int baseLeft, int baseTop, int baseRight, int baseBottom,
                               int headerGutter) {
            this.baseLeft = baseLeft;
            this.baseTop = baseTop;
            this.baseRight = baseRight;
            this.baseBottom = baseBottom;
            this.headerGutter = headerGutter;
        }

        @Override public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
            int types = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
            android.graphics.Insets safe;
            try {
                safe = windowInsets.getInsetsIgnoringVisibility(types);
            } catch (IllegalArgumentException ignored) {
                safe = windowInsets.getInsets(types);
            }
            view.setPadding(
                    baseLeft + safe.left,
                    baseTop + safe.top + headerGutter,
                    baseRight + safe.right,
                    baseBottom + safe.bottom);
            return windowInsets;
        }
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static int background(Context context) { return Color.parseColor(ThemeManager.isDark(context) ? "#05090B" : "#FAFCFD"); }
    static int panel(Context context) { return Color.parseColor(ThemeManager.isDark(context) ? "#10181C" : "#EDF7F7"); }
    static int panelSoft(Context context) { return Color.parseColor(ThemeManager.isDark(context) ? "#162227" : "#E4F1F2"); }
    static int textColor(Context context) { return Color.parseColor(ThemeManager.isDark(context) ? "#F4FFFF" : "#062A31"); }
    static int muted(Context context) { return Color.parseColor(ThemeManager.isDark(context) ? "#A8B6BA" : "#527078"); }
    static int teal(Context context) { return Color.parseColor(ThemeManager.isDark(context) ? "#00E6D2" : "#008F87"); }
    static int error(Context context) { return Color.parseColor(ThemeManager.isDark(context) ? "#FF7676" : "#C62828"); }

    static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(background(context));
        layout.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 18));
        return layout;
    }

    static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        card.setBackground(rounded(panel(context), teal(context), 1, 16, context));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(context, 8), 0, dp(context, 8));
        card.setLayoutParams(params);
        return card;
    }

    static LinearLayout card(Context context, View child) {
        LinearLayout card = card(context);
        card.addView(child);
        return card;
    }

    static TextView title(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(textColor(context));
        view.setTextSize(26);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, 0, 0, dp(context, 10));
        return view;
    }

    static TextView label(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(teal(context));
        view.setTextSize(13);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(context, 8), 0, dp(context, 4));
        return view;
    }

    static TextView text(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(textColor(context));
        view.setTextSize(15);
        return view;
    }

    static EditText edit(Context context, String hint) {
        EditText edit = new EditText(context);
        edit.setHint(hint);
        edit.setHintTextColor(muted(context));
        edit.setTextColor(textColor(context));
        edit.setSingleLine(true);
        edit.setTextSize(16);
        edit.setPadding(dp(context, 12), dp(context, 11), dp(context, 12), dp(context, 11));
        edit.setBackground(rounded(panel(context), Color.parseColor(ThemeManager.isDark(context) ? "#31535A" : "#B7D6D8"), 1, 12, context));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(context, 8));
        edit.setLayoutParams(params);
        return edit;
    }

    static EditText multiLine(Context context, String hint, int lines) {
        EditText edit = edit(context, hint);
        edit.setSingleLine(false);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setMinLines(lines);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return edit;
    }

    static EditText password(Context context) {
        EditText edit = edit(context, "Email account password");
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return edit;
    }

    static Button button(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(ThemeManager.isDark(context) ? Color.rgb(0, 38, 35) : Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(teal(context), teal(context), 1, 14, context));
        button.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 52));
        params.setMargins(0, dp(context, 8), 0, dp(context, 4));
        button.setLayoutParams(params);
        return button;
    }

    static Button secondaryButton(Context context, String text, View.OnClickListener listener) {
        Button button = button(context, text);
        button.setTextColor(teal(context));
        button.setBackground(rounded(panel(context), teal(context), 1, 14, context));
        button.setOnClickListener(listener);
        return button;
    }

    static Button compactButton(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(teal(context));
        button.setTextSize(16);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        button.setBackground(rounded(panel(context), teal(context), 1, 12, context));
        return button;
    }

    static void setEnabled(Button button, boolean enabled, String enabledText, String busyText) {
        button.setEnabled(enabled);
        button.setText(enabled ? enabledText : busyText);
        button.setAlpha(enabled ? 1f : 0.55f);
    }

    private static GradientDrawable rounded(int fill, int stroke, int strokeWidth, int radius, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radius));
        drawable.setStroke(dp(context, strokeWidth), stroke);
        return drawable;
    }
}
