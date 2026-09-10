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
    private Button reminder;
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
        root.addView(header);
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
        body.setBackgroundColor(Ui.background(this));
        WebSettings settings = body.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
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
                    String date = message.date == null ? "" : "\n" + DateFormat.getDateTimeInstance().format(message.date);
                    header.setText((message.subject == null || message.subject.isEmpty() ? "(No subject)" : message.subject)
                            + "\nFrom: " + message.from + "\nTo: " + message.to + date);
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

    private void showOverflow(View anchor) {
        if (loaded == null) return;
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Print / Save as PDF");
        popup.getMenu().add(0, 2, 1, account.deleteFromServer
                ? "Delete — move to server Trash" : "Delete on this device");
        if (!MailRepository.SENT.equals(kind)) {
            popup.getMenu().add(0, 3, 2, MailRepository.JUNK.equals(kind) ? "Not spam" : "Mark spam");
            popup.getMenu().add(0, 4, 3, "External abuse report…");
        }
        popup.getMenu().add(0, 5, 4, "Share message");
        popup.getMenu().add(0, 6, 5, "View full headers");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: printMessage(); return true;
                case 2: confirmDelete(); return true;
                case 3: confirmSpam(); return true;
                case 4: reportOptions(); return true;
                case 5: shareMessage(); return true;
                case 6: viewHeaders(); return true;
                default: return false;
            }
        });
        popup.show();
    }

    private void reply() {
        if (loaded == null) return;
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", accountId);
        String replyTo = headerValue("Reply-To");
        intent.putExtra("to", extract(replyTo.isEmpty() ? loaded.from : replyTo));
        String subject = loaded.subject == null ? "" : loaded.subject;
        String date = loaded.date == null ? "" : DateFormat.getDateTimeInstance().format(loaded.date);
        intent.putExtra("subject", ReplyForwardFormatter.replySubject(subject));
        intent.putExtra("initial_html", ReplyForwardFormatter.replyChain(
                loaded.from, loaded.to, headerValue("Cc"), date, subject, loaded.html));
        startActivity(intent);
    }

    private void replyAll() {
        if (loaded == null) return;
        String replyTarget = headerValue("Reply-To");
        if (replyTarget.isEmpty()) replyTarget = loaded.from;
        String toRecipients = uniqueRecipients(replyTarget, loaded.to);
        String ccRecipients = uniqueRecipients(headerValue("Cc"));
        ccRecipients = subtractRecipients(ccRecipients, toRecipients);
        if (toRecipients.isEmpty()) toRecipients = extract(replyTarget);
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("to", toRecipients);
        if (!ccRecipients.isEmpty()) intent.putExtra("cc", ccRecipients);
        String subject = loaded.subject == null ? "" : loaded.subject;
        String date = loaded.date == null ? "" : DateFormat.getDateTimeInstance().format(loaded.date);
        intent.putExtra("subject", ReplyForwardFormatter.replySubject(subject));
        intent.putExtra("initial_html", ReplyForwardFormatter.replyChain(
                loaded.from, loaded.to, headerValue("Cc"), date, subject, loaded.html));
        startActivity(intent);
    }

    private void forward() {
        if (loaded == null) return;
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", accountId);
        String subject = loaded.subject == null ? "" : loaded.subject;
        String date = loaded.date == null ? "" : DateFormat.getDateTimeInstance().format(loaded.date);
        intent.putExtra("subject", ReplyForwardFormatter.forwardSubject(subject));
        intent.putExtra("initial_html", ReplyForwardFormatter.forwardChain(
                loaded.from, loaded.to, headerValue("Cc"), date, subject, loaded.html));
        startActivity(intent);
    }

    private void renderAttachments() {
        attachmentList.removeAllViews();
        if (incomingAttachments == null || incomingAttachments.isEmpty()) {
            attachmentList.setVisibility(View.GONE);
            return;
        }
        attachmentList.setVisibility(View.VISIBLE);
        attachmentList.addView(Ui.label(this, "ATTACHMENTS (" + incomingAttachments.size() + ")"));
        for (MailAttachmentRepository.IncomingAttachment attachment : incomingAttachments) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView label = Ui.text(this, "📎 " + attachment.fileName + " • "
                    + AttachmentStorage.displaySize(attachment.size));
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button more = Ui.compactButton(this, "⋮");
            more.setContentDescription("Attachment options for " + attachment.fileName);
            more.setOnClickListener(v -> attachmentOptions(attachment));
            row.addView(more, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 42)));
            attachmentList.addView(row);
        }
    }

    private void attachmentOptions(MailAttachmentRepository.IncomingAttachment attachment) {
        new AlertDialog.Builder(this)
                .setTitle(attachment.fileName)
                .setItems(new String[]{"Open", "Save", "Share"}, (dialog, which) -> {
                    if (which == 0) downloadForOpenOrShare(attachment, false);
                    else if (which == 1) saveAttachment(attachment);
                    else downloadForOpenOrShare(attachment, true);
                })
                .show();
    }

    private void saveAttachment(MailAttachmentRepository.IncomingAttachment attachment) {
        pendingSaveAttachment = attachment;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(attachment.mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, attachment.fileName);
        try { startActivityForResult(intent, SAVE_ATTACHMENT); }
        catch (ActivityNotFoundException error) {
            pendingSaveAttachment = null;
            Toast.makeText(this, "No document provider is available.", Toast.LENGTH_LONG).show();
        }
    }

    private void downloadForOpenOrShare(
            MailAttachmentRepository.IncomingAttachment attachment, boolean share) {
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                File directory = new File(getCacheDir(), "mailxperts_attachments");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("Could not create attachment cache.");
                }
                File file = new File(directory,
                        uid + "_" + attachment.index + "_" + AttachmentStorage.safeName(attachment.fileName));
                try (FileOutputStream output = new FileOutputStream(file)) {
                    MailAttachmentRepository.writeAttachment(account, kind, uid,
                            attachment.index, output);
                }
                Uri uri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", file);
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Intent intent = new Intent(share ? Intent.ACTION_SEND : Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (share) {
                        intent.setType(attachment.mimeType);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        launch(Intent.createChooser(intent, "Share attachment"), "Share attachment");
                    } else {
                        intent.setDataAndType(uri, attachment.mimeType);
                        launch(intent, "Open attachment");
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Attachment failed: " + MailRepository.safe(error),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SAVE_ATTACHMENT || resultCode != RESULT_OK || data == null
                || data.getData() == null || pendingSaveAttachment == null) return;
        Uri destination = data.getData();
        MailAttachmentRepository.IncomingAttachment attachment = pendingSaveAttachment;
        pendingSaveAttachment = null;
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(destination)) {
                if (output == null) throw new IllegalStateException("Could not open selected save location.");
                MailAttachmentRepository.writeAttachment(account, kind, uid,
                        attachment.index, output);
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Saved " + attachment.fileName, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Save failed: " + MailRepository.safe(error),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean openExternalLink(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        Intent intent;
        if ("http".equals(scheme) || "https".equals(scheme)) {
            intent = new Intent(Intent.ACTION_VIEW, uri);
        } else if ("mailto".equals(scheme)) {
            intent = new Intent(Intent.ACTION_SENDTO, uri);
        } else if ("tel".equals(scheme) || "sms".equals(scheme) || "geo".equals(scheme)) {
            intent = new Intent(Intent.ACTION_VIEW, uri);
        } else {
            Toast.makeText(this, "Blocked unsupported link type.", Toast.LENGTH_SHORT).show();
            return true;
        }
        launch(intent, "Open link");
        return true;
    }

    private void printMessage() {
        if (loaded == null) return;
        if (printWebView != null) printWebView.destroy();
        printWebView = new WebView(this);
        WebSettings settings = printWebView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        printWebView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                if (manager == null) {
                    Toast.makeText(MessageActivity.this, "Android print service is unavailable.", Toast.LENGTH_LONG).show();
                    return;
                }
                String jobName = "MailXperts - " + (loaded.subject == null ? "Message" : loaded.subject);
                PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(jobName);
                manager.print(jobName, adapter, new PrintAttributes.Builder().build());
            }
        });
        printWebView.loadDataWithBaseURL("https://mailxperts.local/", printableHtml(),
                "text/html", "UTF-8", null);
    }

    private String printableHtml() {
        StringBuilder attachments = new StringBuilder();
        if (incomingAttachments != null && !incomingAttachments.isEmpty()) {
            attachments.append("<hr><p><strong>Attachments:</strong></p><ul>");
            for (MailAttachmentRepository.IncomingAttachment attachment : incomingAttachments) {
                attachments.append("<li>").append(TextUtils.htmlEncode(attachment.fileName))
                        .append(" (" ).append(TextUtils.htmlEncode(AttachmentStorage.displaySize(attachment.size)))
                        .append(")</li>");
            }
            attachments.append("</ul>");
        }
        String date = loaded.date == null ? "" : DateFormat.getDateTimeInstance().format(loaded.date);
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width'>"
                + "<style>body{background:#fff;color:#111;font-family:sans-serif;line-height:1.45;margin:24px}"
                + "img{max-width:100%;height:auto}table{max-width:100%;border-collapse:collapse}a{color:#006b63}</style>"
                + "</head><body><h2>" + TextUtils.htmlEncode(loaded.subject == null ? "(No subject)" : loaded.subject) + "</h2>"
                + "<p><strong>From:</strong> " + TextUtils.htmlEncode(loaded.from == null ? "" : loaded.from) + "<br>"
                + "<strong>To:</strong> " + TextUtils.htmlEncode(loaded.to == null ? "" : loaded.to) + "<br>"
                + "<strong>Date:</strong> " + TextUtils.htmlEncode(date) + "</p><hr>"
                + (loaded.html == null ? "" : loaded.html) + attachments + "</body></html>";
    }

    private void shareMessage() {
        if (loaded == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, loaded.subject == null ? "" : loaded.subject);
        String text = "From: " + loaded.from + "\nTo: " + loaded.to + "\n\n"
                + Html.fromHtml(loaded.html == null ? "" : loaded.html,
                Html.FROM_HTML_MODE_LEGACY).toString();
        intent.putExtra(Intent.EXTRA_TEXT, text);
        launch(Intent.createChooser(intent, "Share message"), "Share message");
    }

    private void viewHeaders() {
        if (loaded == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Full message headers")
                .setMessage(loaded.headers == null || loaded.headers.trim().isEmpty()
                        ? "No headers available." : loaded.headers)
                .setPositiveButton("Close", null)
                .show();
    }

    private void confirmSpam() {
        boolean markSpam = !MailRepository.JUNK.equals(kind);
        new AlertDialog.Builder(this)
                .setTitle(markSpam ? "Mark as spam?" : "Mark as not spam?")
                .setMessage(markSpam
                        ? "The message will move to the provider's Spam/Junk folder and receive junk flags."
                        : "The message will move back to Inbox and receive a not-junk flag.")
                .setPositiveButton(markSpam ? "Mark spam" : "Not spam", (dialog, which) -> setSpam(markSpam))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setSpam(boolean markSpam) {
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

    private String headerValue(String wanted) {
        if (loaded == null || loaded.headers == null || wanted == null) return "";
        String prefix = wanted.toLowerCase(Locale.ROOT) + ":";
        String[] lines = loaded.headers.split("\\n");
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith(prefix)) return line.substring(line.indexOf(':') + 1).trim();
        }
        return "";
    }

    private String uniqueRecipients(String... rawValues) {
        LinkedHashMap<String, String> addresses = new LinkedHashMap<>();
        String selfEmail = account.email == null ? "" : account.email.toLowerCase(Locale.ROOT);
        String selfUser = account.username == null ? "" : account.username.toLowerCase(Locale.ROOT);
        for (String raw : rawValues) {
            if (raw == null || raw.trim().isEmpty()) continue;
            try {
                String normalised = RecipientNormalizer.normalise(raw);
                for (InternetAddress address : InternetAddress.parse(normalised, false)) {
                    String email = address.getAddress() == null ? "" : address.getAddress().trim().toLowerCase(Locale.ROOT);
                    if (email.isEmpty() || email.equals(selfEmail) || email.equals(selfUser)) continue;
                    addresses.putIfAbsent(email, address.toUnicodeString());
                }
            } catch (Exception ignored) {
                String fallback = extract(raw);
                String key = fallback.toLowerCase(Locale.ROOT);
                if (!fallback.isEmpty() && !key.equals(selfEmail) && !key.equals(selfUser)) {
                    addresses.putIfAbsent(key, fallback);
                }
            }
        }
        return String.join(", ", addresses.values());
    }

    private String subtractRecipients(String candidates, String alreadyUsed) {
        if (candidates == null || candidates.trim().isEmpty()) return "";
        Map<String, String> used = addressMap(alreadyUsed);
        Map<String, String> candidateMap = addressMap(candidates);
        for (String key : used.keySet()) candidateMap.remove(key);
        return String.join(", ", candidateMap.values());
    }

    private Map<String, String> addressMap(String raw) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.trim().isEmpty()) return out;
        try {
            for (InternetAddress address : InternetAddress.parse(RecipientNormalizer.normalise(raw), false)) {
                String email = address.getAddress() == null ? "" : address.getAddress().toLowerCase(Locale.ROOT);
                if (!email.isEmpty()) out.put(email, address.toUnicodeString());
            }
        } catch (Exception ignored) {}
        return out;
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
        if (printWebView != null) printWebView.destroy();
        super.onDestroy();
    }
}
