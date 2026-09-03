package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class AppearanceActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        LinearLayout root = Ui.vertical(this);
        root.addView(Ui.title(this, "Appearance"));
        TextView note = Ui.text(this, "Choose the premium white day theme, premium black night theme, or follow your phone automatically.");
        note.setTextColor(Ui.muted(this));
        root.addView(note);

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        RadioButton automatic = option("Automatic — follow phone", ThemeManager.AUTO);
        RadioButton light = option("Premium white — day", ThemeManager.LIGHT);
        RadioButton dark = option("Premium black — night", ThemeManager.DARK);
        group.addView(automatic);
        group.addView(light);
        group.addView(dark);
        String selected = ThemeManager.mode(this);
        if (ThemeManager.LIGHT.equals(selected)) light.setChecked(true);
        else if (ThemeManager.DARK.equals(selected)) dark.setChecked(true);
        else automatic.setChecked(true);
        root.addView(Ui.card(this, group));

        Button save = Ui.button(this, "Apply theme");
        save.setOnClickListener(v -> {
            RadioButton checked = group.findViewById(group.getCheckedRadioButtonId());
            ThemeManager.setMode(this, checked == null ? ThemeManager.AUTO : String.valueOf(checked.getTag()));
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        root.addView(save);
        root.addView(Ui.secondaryButton(this, "Back", v -> finish()));
        Ui.setContentView(this, root);
    }

    private RadioButton option(String text, String value) {
        RadioButton button = new RadioButton(this);
        button.setId(android.view.View.generateViewId());
        button.setText(text);
        button.setTag(value);
        button.setTextColor(Ui.textColor(this));
        button.setTextSize(16);
        button.setPadding(Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12));
        return button;
    }
}
