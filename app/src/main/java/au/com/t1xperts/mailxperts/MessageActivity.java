package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.internet.InternetAddress;

public class MessageActivity extends Activity {
    private static final int SAVE_ATTACHMENT = 4201;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AccountConfig account;
    private String accountId;
    private String kind;
    private long uid;
    private TextView header;
    private TextView smartStatus;
    private WebView body;
    private WebView printWebView;
    private ProgressBar progress;
    private Button reply;
    private Button replyAll;
    private Button forward;
    private Button overflow;
    private Button details;
    private Button reminder;
    private boolean detailsExpanded;
    private LinearLayout attachmentList;
    private MailRepository.FullMessage loaded;
    private MailIntelligence.Result intelligence;
    private List<MailAttachmentRepository.IncomingAttachment> incomingAttachments = new ArrayList<>();
    private MailAttachmentRepository.IncomingAttachment pendingSaveAttachment;

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
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button back = Ui.compactButton(this, "‹");
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, "Message");
        title.setPadding(Ui.dp(this, 8), 0, 0, 0);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        overflow = Ui.compactButton(this, "⋮");
        overflow.setContentDescription("Message options");
        overflow.setEnabled(false);
        overflow.setOnClickListener(this::showOverflow);
        top.addView(overflow, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        root.addView(top);

        header = Ui.text(this, "Loading…");
        header.setTextIsSelectable(true);
        header.setContentDescription("Message sender and recipient details. Long press to select and copy.");
        root.addView(header);
        details = Ui.compactButton(this, "Show message details");
        details.setEnabled(false);
        details.setOnClickListener(v -> {
            detailsExpanded = !detailsExpanded;
            renderMessageHeader();
        });
        root.addView(details, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 42)));
        smartStatus = Ui.text(this, "");
        smartStatus.setTextColor(Ui.teal(this));
        smartStatus.setVisibility(View.GONE);
        smartStatus.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 6));
        root.addView(smartStatus);

        reminder = Ui.secondaryButton(this, "Add due-date reminder", v -> addReminder());
        reminder.setEnabled(false);
        reminder.setVisibility(View.GONE);
        root.addView(reminder);

        progress = new ProgressBar(this);
        root.addView(progress);
        body = new WebView(this);
        body.setBackgroundColor(android.graphics.Color.WHITE);
        WebSettings settings = body.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setDomStorageEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setGeolocationEnabled(false);
        settings.setSaveFormData(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSafeBrowsingEnabled(true);
        // Remote HTML images (such as company logos and newsletters) must be allowed
        // to load; otherwise WebView renders each external image as a broken placeholder.
        // JavaScript, local file/content access and persistent web storage remain disabled.
        settings.setBlockNetworkLoads(false);
        // Permit legacy HTTP image sources as well as HTTPS sources in an HTML email.
        // Main-frame navigation still leaves the WebView through the explicit safe-scheme policy.
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        body.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openExternalLink(request == null ? null : request.getUrl());
            }
            @SuppressWarnings("deprecation")
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternalLink(url == null ? null : Uri.parse(url));
            }
        });
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        attachmentList = Ui.card(this);
        attachmentList.setVisibility(View.GONE);
        root.addView(attachmentList);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        reply = Ui.compactButton(this, "Reply");
        replyAll = Ui.compactButton(this, "Reply all");
        forward = Ui.compactButton(this, "Forward");
        reply.setEnabled(false);
        replyAll.setEnabled(false);
        forward.setEnabled(false);
        reply.setOnClickListener(v -> reply());
        replyAll.setOnClickListener(v -> replyAll());
        forward.setOnClickListener(v -> forward());
        actions.addView(reply, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f);
        middle.setMargins(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        actions.addView(replyAll, middle);
        actions.addView(forward, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f));
        root.addView(actions);

        Ui.setContentView(this, root);
        load();
    }

    private void load() {
        executor.execute(() -> {
            try {
                MailRepository.FullMessage message = MailRepository.fetchMessage(
                        account, kind, uid, account.syncReadState);
                List<MailAttachmentRepository.IncomingAttachment> attachments;
                try {
                    attachments = MailAttachmentRepository.listAttachments(account, kind, uid);
                } catch (Exception attachmentError) {
                    attachments = new ArrayList<>();
                }
                markCachedSeen();
                MailIntelligence.Result result = MailIntelligence.analyse(
                        message.from, message.subject, message.html, message.date);
                List<MailAttachmentRepository.IncomingAttachment> finalAttachments = attachments;
                runOnUiThread(() -> {
                    loaded = message;
                    intelligence = result;
                    incomingAttachments = finalAttachments;
                    progress.setVisibility(View.GONE);
                    renderMessageHeader();
                    details.setEnabled(true);
                    reply.setEnabled(true);
                    replyAll.setEnabled(true);
                    forward.setEnabled(true);
                    overflow.setEnabled(true);
                    showIntelligence(result);
                    renderAttachments();
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
        String text = "★ " + result.label;
        if (result.explanation != null && !result.explanation.trim().isEmpty()) {
            text += " • " + result.explanation.trim();
        }
        if (result.hasDueDate()) {
            text += " • " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(result.dueAt));
            reminder.setVisibility(View.VISIBLE);
            reminder.setEnabled(true);
        }
        smartStatus.setText(text);
        smartStatus.setMaxLines(2);
        smartStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        smartStatus.setTextSize(13);
        smartStatus.setTextColor(result.isOverdue() ? Ui.error(this) : Ui.teal(this));
        smartStatus.setVisibility(View.VISIBLE);
    }

    private void renderMessageHeader() {
        if (loaded == null) return;
        String subject = loaded.subject == null || loaded.subject.trim().isEmpty()
                ? "(No subject)" : loaded.subject.trim();
        StringBuilder text = new StringBuilder(subject)
                .append("\nFrom: ").append(loaded.from == null ? "" : loaded.from)
                .append("\nTo: ").append(loaded.to == null ? "" : loaded.to);
        if (loaded.date != null) {
            text.append("\n").append(DateFormat.getDateTimeInstance().format(loaded.date));
        }
        if (detailsExpanded) {
            appendHeaderDetail(text, "Cc");
            appendHeaderDetail(text, "Bcc");
            appendHeaderDetail(text, "Reply-To");
            appendHeaderDetail(text, "Message-ID");
            text.append("\nAccount: ").append(account.displayName())
                    .append(" <").append(account.email).append(">");
            text.append("\nFolder: ").append(kind);
        }
        header.setText(text.toString());
        if (details != null) {
            details.setText(detailsExpanded ? "Hide message details" : "Show message details");
        }
    }

    private void appendHeaderDetail(StringBuilder text, String key) {
        if (loaded == null || loaded.headers == null) return;
        String value = loaded.headers.get(key);
        if (value != null && !value.trim().isEmpty()) {
            text.append("\n").append(key).append(": ").append(value.trim());
        }
    }

    private String wrap(String html) {
        return EmailHtmlPolicy.wrapForDisplay(html);
    }

    private boolean openExternalLink(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        try {
            if ("http".equals(scheme) || "https".equals(scheme)) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } else if ("mailto".equals(scheme)) {
                startActivity(new Intent(Intent.ACTION_SENDTO, uri));
            } else if ("tel".equals(scheme)) {
                startActivity(new Intent(Intent.ACTION_DIAL, uri));
            } else {
                Toast.makeText(this, "Unsupported or unsafe link.", Toast.LENGTH_SHORT).show();
            }
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No app is available to open this link.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void showOverflow(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Reply");
        menu.getMenu().add("Reply all");
        menu.getMenu().add("Forward");
        menu.getMenu().add("Print / Save as PDF");
        menu.getMenu().add("Open in external browser");
        menu.getMenu().add(account.deleteFromServer
                ? "Delete — move to server Trash" : "Delete on this device");
        menu.setOnMenuItemClickListener(item -> {
            String action = item.getTitle().toString();
            if (action.startsWith("Reply all")) replyAll();
            else if (action.startsWith("Reply")) reply();
            else if (action.startsWith("Forward")) forward();
            else if (action.startsWith("Print")) printMessage();
            else if (action.startsWith("Open in")) openRenderedInBrowser();
            else if (action.startsWith("Delete")) deleteMessage();
            return true;
        });
        menu.show();
    }

    private void printMessage() {
        if (loaded == null) return;
        String html = buildPrintableHtml();
        printWebView = new WebView(this);
        printWebView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                PrintDocumentAdapter adapter = view.createPrintDocumentAdapter("MailXperts-email");
                manager.print("MailXperts email", adapter, new PrintAttributes.Builder().build());
            }
        });
        printWebView.loadDataWithBaseURL("https://mailxperts.local/", html, "text/html", "UTF-8", null);
    }

    private String buildPrintableHtml() {
        StringBuilder out = new StringBuilder();
        out.append("<html><body style='font-family:sans-serif'>")
                .append("<h2>").append(escape(loaded == null ? "" : loaded.subject)).append("</h2>")
                .append("<p><b>From:</b> ").append(escape(loaded == null ? "" : loaded.from)).append("<br>")
                .append("<b>To:</b> ").append(escape(loaded == null ? "" : loaded.to)).append("<br>");
        if (loaded != null && loaded.date != null) {
            out.append("<b>Date:</b> ").append(escape(DateFormat.getDateTimeInstance().format(loaded.date))).append("<br>");
        }
        out.append("</p>").append(loaded == null ? "" : wrap(loaded.html)).append("</body></html>");
        return out.toString();
    }

    private void openRenderedInBrowser() {
        if (loaded == null) return;
        try {
            File directory = new File(getCacheDir(), "shares");
            if (!directory.exists()) directory.mkdirs();
            File file = new File(directory, "mailxperts-email.html");
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(buildPrintableHtml().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/html");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open email with"));
        } catch (Exception error) {
            Toast.makeText(this, "Unable to open message externally.", Toast.LENGTH_LONG).show();
        }
    }

    private void reply() {
        if (loaded == null) return;
        Intent intent = baseComposeIntent("reply");
        intent.putExtra("to", loaded.replyTo == null || loaded.replyTo.trim().isEmpty()
                ? extractEmail(loaded.from) : extractEmail(loaded.replyTo));
        intent.putExtra("subject", prefixSubject("Re:", loaded.subject));
        intent.putExtra("body_html", buildQuotedReplyBody());
        startActivity(intent);
    }

    private void replyAll() {
        if (loaded == null) return;
        String replyTarget = loaded.replyTo == null || loaded.replyTo.trim().isEmpty()
                ? extractEmail(loaded.from) : extractEmail(loaded.replyTo);
        List<String> recipients = new ArrayList<>();
        addAddresses(recipients, replyTarget);
        addAddresses(recipients, loaded.to);
        addAddresses(recipients, headerValue("Cc"));
        removeOwnAddress(recipients);
        Intent intent = baseComposeIntent("reply_all");
        intent.putExtra("to", TextUtils.join(", ", recipients));
        intent.putExtra("subject", prefixSubject("Re:", loaded.subject));
        intent.putExtra("body_html", buildQuotedReplyBody());
        startActivity(intent);
    }

    private void forward() {
        if (loaded == null) return;
        Intent intent = baseComposeIntent("forward");
        intent.putExtra("subject", prefixSubject("Fwd:", loaded.subject));
        intent.putExtra("body_html", buildForwardBody());
        startActivity(intent);
    }

    private Intent baseComposeIntent(String mode) {
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("mode", mode);
        return intent;
    }

    private String buildQuotedReplyBody() {
        if (loaded == null) return "";
        String dateText = loaded.date == null ? "" : DateFormat.getDateTimeInstance().format(loaded.date);
        return "<p><br></p><p>On " + escape(dateText) + ", "
                + escape(loaded.from) + " wrote:</p><blockquote>"
                + (loaded.html == null ? "" : loaded.html) + "</blockquote>";
    }

    private String buildForwardBody() {
        if (loaded == null) return "";
        String dateText = loaded.date == null ? "" : DateFormat.getDateTimeInstance().format(loaded.date);
        return "<p><br></p><hr><p><b>Forwarded message</b><br>"
                + "From: " + escape(loaded.from) + "<br>"
                + "To: " + escape(loaded.to) + "<br>"
                + "Date: " + escape(dateText) + "<br>"
                + "Subject: " + escape(loaded.subject) + "</p>"
                + (loaded.html == null ? "" : loaded.html);
    }

    private String headerValue(String key) {
        if (loaded == null || loaded.headers == null) return "";
        String value = loaded.headers.get(key);
        return value == null ? "" : value;
    }

    private void addAddresses(List<String> target, String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        try {
            for (InternetAddress address : InternetAddress.parse(raw, false)) {
                String email = address.getAddress();
                if (email != null && !email.trim().isEmpty()) addUnique(target, email.trim());
            }
        } catch (Exception ignored) {
            for (String token : raw.split("[,;]")) {
                String email = extractEmail(token);
                if (!email.isEmpty()) addUnique(target, email);
            }
        }
    }

    private void addUnique(List<String> target, String email) {
        for (String existing : target) {
            if (existing.equalsIgnoreCase(email)) return;
        }
        target.add(email);
    }

    private void removeOwnAddress(List<String> target) {
        if (account == null || account.email == null) return;
        target.removeIf(value -> value.equalsIgnoreCase(account.email));
    }

    private String extractEmail(String value) {
        if (value == null) return "";
        try {
            InternetAddress[] parsed = InternetAddress.parse(value, false);
            if (parsed.length > 0 && parsed[0].getAddress() != null) return parsed[0].getAddress();
        } catch (Exception ignored) {}
        return value.replaceAll(".*<([^>]+)>.*", "$1").trim();
    }

    private String prefixSubject(String prefix, String value) {
        String subject = value == null ? "" : value.trim();
        if (subject.regionMatches(true, 0, prefix, 0, prefix.length())) return subject;
        return prefix + " " + subject;
    }

    private void markCachedSeen() {
        try { new MailCache(this).markSeen(account.id, kind, uid, true); }
        catch (Exception ignored) {}
    }

    private void deleteMessage() {
        if (loaded == null) return;
        String message = account.deleteFromServer
                ? "Move this message to the provider Trash folder? This changes the server mailbox."
                : "Remove the cached message from this device only? The server mailbox will not be changed.";
        new AlertDialog.Builder(this)
                .setTitle(account.deleteFromServer ? "Delete from server?" : "Delete on this device?")
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> executor.execute(() -> {
                    try {
                        if (account.deleteFromServer) {
                            MailRepository.moveToTrash(account, kind, uid);
                        }
                        new MailCache(this).remove(account.id, kind, uid);
                        runOnUiThread(() -> {
                            Toast.makeText(this, account.deleteFromServer
                                            ? "Moved to server Trash." : "Removed from this device only.",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } catch (Exception error) {
                        runOnUiThread(() -> Toast.makeText(this,
                                "Delete failed: " + MailRepository.safe(error), Toast.LENGTH_LONG).show());
                    }
                }))
                .show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        if (body != null) body.destroy();
        if (printWebView != null) printWebView.destroy();
    }

    private String escape(String value) {
        return Html.escapeHtml(value == null ? "" : value);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SAVE_ATTACHMENT && resultCode == RESULT_OK
                && data != null && data.getData() != null && pendingSaveAttachment != null) {
            MailAttachmentRepository.IncomingAttachment attachment = pendingSaveAttachment;
            pendingSaveAttachment = null;
            Uri target = data.getData();
            executor.execute(() -> {
                try {
                    try (OutputStream output = getContentResolver().openOutputStream(target)) {
                        if (output == null) throw new IllegalStateException("Cannot open selected destination");
                        MailAttachmentRepository.copyAttachmentTo(account, kind, uid, attachment.partPath, output);
                    }
                    runOnUiThread(() -> Toast.makeText(this, "Attachment saved.", Toast.LENGTH_SHORT).show());
                } catch (Exception error) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Save failed: " + MailRepository.safe(error), Toast.LENGTH_LONG).show());
                }
            });
        }
    }
}
