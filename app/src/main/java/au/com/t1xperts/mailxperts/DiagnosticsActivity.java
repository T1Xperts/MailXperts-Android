package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Local diagnostics. Nothing is sent to GitHub or any remote service automatically. */
public final class DiagnosticsActivity extends Activity {
    private static final int EXPORT_REPORT = 9301;
    private TextView history;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.vertical(this);
        scroll.addView(root);
        root.addView(Ui.title(this, "Diagnostics & Crash Reports"));
        TextView note = Ui.text(this,
                "MailXperts saves limited app errors and screen-navigation events locally. "
                + "It does not capture device-wide logcat, email content, attachments, passwords or tokens.");
        note.setTextColor(Ui.muted(this));
        root.addView(note);

        LinearLayout actions = Ui.card(this);
        actions.addView(Ui.secondaryButton(this, "Export sanitized report",
                v -> exportReport()));
        actions.addView(Ui.secondaryButton(this, "Refresh history", v -> refresh()));
        actions.addView(Ui.secondaryButton(this, "Delete local reports", v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete diagnostic history?")
                        .setMessage("Delete locally saved crash and error reports on this device?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            DiagnosticLogger.clear(this);
                            refresh();
                        }).show()));
        root.addView(actions);

        TextView sync = Ui.text(this,
                "Automatic private GitHub synchronization: NOT ACTIVE. "
                + "An authenticated reporting gateway and explicit user consent must be approved "
                + "before this can be enabled. Export is manual and never sends a report itself.");
        sync.setTextColor(Ui.muted(this));
        root.addView(Ui.card(this, sync));

        history = Ui.text(this, "");
        history.setTextSize(12);
        root.addView(Ui.card(this, history));
        root.addView(Ui.secondaryButton(this, "Back", v -> finish()));
        Ui.setContentView(this, scroll);
        refresh();
    }

    private void refresh() {
        if (history == null) return;
        history.setText(DiagnosticLogger.exportText(this));
    }

    private void exportReport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "MailXperts-diagnostics.txt");
        try {
            startActivityForResult(intent, EXPORT_REPORT);
        } catch (Exception error) {
            Toast.makeText(this, "No document exporter available", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != EXPORT_REPORT || result != RESULT_OK || data == null || data.getData() == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            if (out == null) throw new IllegalStateException("Document stream unavailable");
            out.write(DiagnosticLogger.exportText(this).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Sanitized diagnostic report exported", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            DiagnosticLogger.nonFatal(this, "DIAGNOSTICS_EXPORT_FAILED", "DIAGNOSTICS", error);
            Toast.makeText(this, "Could not save the diagnostic report", Toast.LENGTH_LONG).show();
        }
    }
}
