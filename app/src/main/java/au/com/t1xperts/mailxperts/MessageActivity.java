package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AccountConfig account;
    private String accountId;
    private String kind;
    private long uid;
    private TextView header;
    private TextView smartStatus;
    private WebView body;
    private ProgressBar progress;
    private Button reply;
    private Button spam;
    private Button delete;
    private Button externalReport;
    private Button reminder;
    private MailRepository.FullMessage loaded;
    private MailIntelligence.Result intelligence;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        uid = getIntent().getLongExtra("uid", -1);
        accountId = getIntent().getStringExtra("account_id");
        kind = getIntent().getStringExtra("folder_kind");
        if (kind == null) kind = MailRepository.INBOX;
        account = new SecureStore(this).load(accountId);
        if (uid < 0 || !account.isUsable()) { finish(); return; }

        LinearLayout root = Ui.vertical(this);
        root.addView(Ui.title(this, "Message"));
        header = Ui.text(this, "Loading…");
        root.addView(header);
        smartStatus = Ui.text(this, "");
        smartStatus.setTextColor(Ui.teal(this));
        smartStatus.setVisibility(View.GONE);
        smartStatus.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 6));
        root.addView(smartStatus);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        reply = Ui.compactButton(this, "Reply");
        reply.setEnabled(false);
        reply.setOnClickListener(v -> reply());
        actions.addView(reply, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        spam = Ui.compactButton(this, MailRepository.JUNK.equals(kind) ? "Not spam" : "Mark spam");
        spam.setEnabled(false);
        spam.setOnClickListener(v -> confirmSpam());
        LinearLayout.LayoutParams spamParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        spamParams.setMargins(Ui.dp(this, 6), 0, 0, 0);
        actions.addView(spam, spamParams);
        Button back = Ui.compactButton(this, "Back");
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        backParams.setMargins(Ui.dp(this, 6), 0, 0, 0);
        actions.addView(back, backParams);
        root.addView(actions);

        delete = Ui.secondaryButton(this,
                account.deleteFromServer ? "Delete — move to server Trash" : "Delete on this device",
                v -> confirmDelete());
        delete.setEnabled(false);
        root.addView(delete);

        LinearLayout smartActions = new LinearLayout(this);
        smartActions.setOrientation(LinearLayout.HORIZONTAL);
        externalReport = Ui.secondaryButton(this, "External abuse report…", v -> reportOptions());
        externalReport.setEnabled(false);
        smartActions.addView(externalReport, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        reminder = Ui.secondaryButton(this, "Add due-date reminder", v -> addReminder());
        reminder.setEnabled(false);
        reminder.setVisibility(View.GONE);
        LinearLayout.LayoutParams reminderParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f);
        reminderParams.setMargins(Ui.dp(this, 6), 0, 0, 0);
        smartActions.addView(reminder, reminderParams);
        root.addView(smartActions);

        if (MailRepository.SENT.equals(kind)) {
            spam.setVisibility(View.GONE);
            externalReport.setVisibility(View.GONE);
        }

        progress = new ProgressBar(this);
        root.addView(progress);
        body = new WebView(this);
        body.setBackgroundColor(Ui.background(this));
        WebSettings settings = body.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setBlockNetworkLoads(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        body.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null || url.trim().isEmpty()) return false;
                Uri target = Uri.parse(url);
                String scheme = target.getScheme();
                if (scheme == null || "about".equalsIgnoreCase(scheme)
                        || "data".equalsIgnoreCase(scheme)) {
                    return false;
                }
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                        || "mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme)
                        || "sms".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, target));
                    } catch (ActivityNotFoundException error) {
                        Toast.makeText(MessageActivity.this,
                                "No compatible app is installed to open this link.",
                                Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                // Do not let unknown/custom schemes execute inside the email WebView.
                return true;
            }
        });
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, root);
        load();
    }

    private void load() {
        executor.execute(() -> {
            try {
                MailRepository.FullMessage message = MailRepository.fetchMessage(
                        account, kind, uid, account.syncReadState);
                markCachedSeen();
                MailIntelligence.Result result = MailIntelligence.analyse(
                        message.from, message.subject, message.html, message.date);
                runOnUiThread(() -> {
                    loaded = message;
                    intelligence = result;
                    progress.setVisibility(View.GONE);
                    String date = message.date == null ? "" : "\n" + DateFormat.getDateTimeInstance().format(message.date);
                    header.setText((message.subject == null || message.subject.isEmpty() ? "(No subject)" : message.subject)
                            + "\nFrom: " + message.from + "\nTo: " + message.to + date);
                    reply.setEnabled(true);
                    spam.setEnabled(!MailRepository.SENT.equals(kind));
                    externalReport.setEnabled(!MailRepository.SENT.equals(kind));
                    delete.setEnabled(true);
                    showIntelligence(result);
                    body.loadDataWithBaseURL("https://mailxperts.local/", wrap(message.html),
                            "text/html", "UTF-8", null);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    header.setText("Unable to load message.");
                    Toast.makeText(this, MailRepository.safe(error), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showIntelligence(MailIntelligence.Result result) {
        if (result == null || result.label.isEmpty()) return;
        String text = "★ Smart Priority: " + result.label + "\n" + result.explanation;
        if (result.hasDueDate()) {
            text += "\nDetected date: " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(result.dueAt));
            reminder.setVisibility(View.VISIBLE);
            reminder.setEnabled(true);
        }
        smartStatus.setText(text);
        smartStatus.setTextColor(result.isOverdue() ? Ui.error(this) : Ui.teal(this));
        smartStatus.setVisibility(View.VISIBLE);
    }

    private void reply() {
        if (loaded == null) return;
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("to", extract(loaded.from));
        String subject = loaded.subject == null ? "" : loaded.subject;
        intent.putExtra("subject", subject.toLowerCase().startsWith("re:") ? subject : "Re: " + subject);
        startActivity(intent);
    }

    private void confirmSpam() {
        boolean markSpam = !MailRepository.JUNK.equals(kind);
        new AlertDialog.Builder(this)
                .setTitle(markSpam ? "Mark as spam?" : "Mark as not spam?")
                .setMessage(markSpam
                        ? "The message will move to the provider's Spam/Junk folder and receive junk flags. This is the normal feedback path used by compatible mail providers."
                        : "The message will move back to Inbox and receive a not-junk flag.")
                .setPositiveButton(markSpam ? "Mark spam" : "Not spam", (dialog, which) -> setSpam(markSpam))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setSpam(boolean markSpam) {
        spam.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                MailRepository.setSpam(account, kind, uid, markSpam);
                removeCachedSummary();
                runOnUiThread(() -> {
                    Toast.makeText(this, markSpam ? "Moved to Spam/Junk" : "Moved to Inbox", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    spam.setEnabled(true);
                    Toast.makeText(this, "Spam action failed: " + MailRepository.safe(error), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void confirmDelete() {
        boolean onServer = account.deleteFromServer;
        new AlertDialog.Builder(this)
                .setTitle(onServer ? "Move to server Trash?" : "Hide on this device?")
                .setMessage(onServer
                        ? "This message will be removed from this folder on the mail server and moved to its Trash folder when supported."
                        : "This message will disappear from MailXperts on this device, but it will remain unchanged on the mail server. You can change this policy in Account Settings.")
                .setPositiveButton("Delete", (dialog, which) -> deleteMessage(onServer))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(boolean onServer) {
        delete.setEnabled(false);
        if (!onServer) {
            LocalStore cache = new LocalStore(this);
            try { cache.hideCached(accountId, kind, uid); }
            finally { cache.close(); }
            Toast.makeText(this, "Hidden on this device — server copy retained",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                MailRepository.deleteMessage(account, kind, uid);
                removeCachedSummary();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Moved to server Trash", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    delete.setEnabled(true);
                    Toast.makeText(this, "Delete failed: " + MailRepository.safe(error),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void markCachedSeen() {
        LocalStore cache = new LocalStore(this);
        try { cache.markCachedSeen(accountId, kind, uid); }
        catch (RuntimeException ignored) {}
        finally { cache.close(); }
    }

    private void removeCachedSummary() {
        LocalStore cache = new LocalStore(this);
        try { cache.deleteCached(accountId, kind, uid); }
        catch (RuntimeException ignored) {}
        finally { cache.close(); }
    }

    private void reportOptions() {
        if (loaded == null) return;
        new AlertDialog.Builder(this)
                .setTitle("External abuse reporting")
                .setMessage("Full email headers can contain personal and routing information. Nothing is sent automatically. Review the generated draft, and use Cloudflare only when the sender/domain is related to Cloudflare Email Service.")
                .setPositiveButton("Cloudflare draft", (dialog, which) -> createCloudflareDraft())
                .setNeutralButton("Share report", (dialog, which) -> shareReport())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String reportText() {
        return "Possible email abuse report\n\n"
                + "From: " + loaded.from + "\n"
                + "Subject: " + loaded.subject + "\n"
                + "Received: " + (loaded.date == null ? "Unknown" : DateFormat.getDateTimeInstance().format(loaded.date)) + "\n\n"
                + "Full headers:\n" + loaded.headers;
    }

    private void createCloudflareDraft() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:mailabuse@cloudflare.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Possible email abuse: " + (loaded.subject == null ? "(No subject)" : loaded.subject));
        intent.putExtra(Intent.EXTRA_TEXT, reportText());
        launch(intent, "Create Cloudflare abuse report");
    }

    private void shareReport() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Possible email abuse report");
        intent.putExtra(Intent.EXTRA_TEXT, reportText());
        launch(Intent.createChooser(intent, "Share abuse report"), "Share abuse report");
    }

    private void launch(Intent intent, String title) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No compatible app is installed for: " + title, Toast.LENGTH_LONG).show();
        }
    }

    private void addReminder() {
        if (loaded == null || intelligence == null || !intelligence.hasDueDate()) return;
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, intelligence.dueAt);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, intelligence.dueAt + 30L * 60L * 1000L);
        intent.putExtra(CalendarContract.Events.TITLE, "MailXperts reminder: "
                + (loaded.subject == null ? "Payment due" : loaded.subject));
        intent.putExtra(CalendarContract.Events.DESCRIPTION,
                "Detected from email by MailXperts. Verify sender and amount before paying.\nFrom: " + loaded.from);
        launch(intent, "Add calendar reminder");
    }

    private String wrap(String html) {
        boolean dark = ThemeManager.isDark(this);
        String background = dark ? "#05090b" : "#fafcfd";
        String text = dark ? "#f4ffff" : "#062a31";
        String muted = dark ? "#a8b6ba" : "#527078";
        String teal = dark ? "#00e6d2" : "#008f87";
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{background:" + background + ";color:" + text + ";font-family:sans-serif;line-height:1.45;padding:12px}"
                + "a{color:" + teal + "}img{max-width:100%;height:auto}blockquote{border-left:3px solid " + teal
                + ";padding-left:10px;color:" + muted + "}table{max-width:100%}</style></head><body>"
                + (html == null ? "" : html) + "</body></html>";
    }

    private String extract(String from) {
        if (from == null) return "";
        int left = from.lastIndexOf('<');
        int right = from.lastIndexOf('>');
        return left >= 0 && right > left ? from.substring(left + 1, right).trim() : from.trim();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        if (body != null) body.destroy();
        super.onDestroy();
    }
}
