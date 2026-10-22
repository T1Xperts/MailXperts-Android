package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.internet.InternetAddress;

/** P0 end-user AI workspace: AI-001 through AI-007. */
public class AiAssistantActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<MessageRef> recent = new ArrayList<>();
    private SecureStore store;
    private AiGatewayClient ai;
    private AiPreferences preferences;
    private AiSecretVault vault;
    private String accountId;
    private LinearLayout messageList;
    private TextView selectedLabel;
    private TextView result;
    private TextView status;
    private EditText question;
    private EditText search;
    private EditText rewriteText;
    private EditText rewriteInstruction;
    private MessageRef selected;
    private MailRepository.FullMessage selectedFull;

    @Override protected void onCreate(Bundle state) {
        ThemeManager.apply(this);
        super.onCreate(state);
        store = new SecureStore(this);
        ai = new AiGatewayClient(this);
        preferences = new AiPreferences(this);
        vault = new AiSecretVault(this);
        accountId = getIntent().getStringExtra("account_id");
        if (accountId == null || accountId.isEmpty()) accountId = store.getSelectedId();

        LinearLayout root = Ui.vertical(this);
        root.addView(header());

        LinearLayout gatewayCard = Ui.card(this);
        gatewayCard.addView(Ui.label(this, "MAILXPERTS AI"));
        status = Ui.text(this, "");
        gatewayCard.addView(status);
        gatewayCard.addView(Ui.secondaryButton(this, "AI gateway settings", v -> configureGateway()));
        root.addView(gatewayCard);

        LinearLayout searchCard = Ui.card(this);
        searchCard.addView(Ui.label(this, "AI MAILBOX"));
        search = Ui.edit(this, "Ask to find mail, e.g. invoices due this month");
        searchCard.addView(search);
        LinearLayout searchActions = new LinearLayout(this);
        searchActions.setOrientation(LinearLayout.HORIZONTAL);
        Button find = Ui.compactButton(this, "Natural search");
        find.setOnClickListener(v -> naturalSearch());
        Button priority = Ui.compactButton(this, "What needs attention?");
        priority.setOnClickListener(v -> priorityInbox());
        searchActions.addView(find, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f));
        LinearLayout.LayoutParams priorityParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f);
        priorityParams.setMargins(Ui.dp(this, 8), 0, 0, 0);
        searchActions.addView(priority, priorityParams);
        searchCard.addView(searchActions);
        root.addView(searchCard);

        LinearLayout selectedCard = Ui.card(this);
        selectedCard.addView(Ui.label(this, "SELECTED MESSAGE / THREAD"));
        selectedLabel = Ui.text(this, "Select a recent or search-result message below.");
        selectedCard.addView(selectedLabel);
        question = Ui.edit(this, "Ask a question about this message/thread");
        selectedCard.addView(question);
        selectedCard.addView(actionRow("Summarise", "Ask AI", "Extract actions"));
        Button draftReply = Ui.secondaryButton(this, "Draft contextual reply", v -> draftReply());
        selectedCard.addView(draftReply);
        root.addView(selectedCard);

        LinearLayout resultCard = Ui.card(this);
        resultCard.addView(Ui.label(this, "AI RESULT"));
        result = Ui.text(this, "AI output will appear here.");
        result.setTextIsSelectable(true);
        resultCard.addView(result);
        root.addView(resultCard);

        LinearLayout rewriteCard = Ui.card(this);
        rewriteCard.addView(Ui.label(this, "AI REWRITE"));
        rewriteText = Ui.multiLine(this, "Paste or type a draft to improve", 4);
        rewriteInstruction = Ui.edit(this, "Rewrite style: clearer, shorter, formal, friendly...");
        rewriteCard.addView(rewriteText);
        rewriteCard.addView(rewriteInstruction);
        rewriteCard.addView(Ui.secondaryButton(this, "Rewrite draft", v -> rewrite()));
        root.addView(rewriteCard);

        TextView listTitle = Ui.label(this, "RECENT MAIL");
        root.addView(listTitle);
        ScrollView scroll = new ScrollView(this);
        messageList = new LinearLayout(this);
        messageList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messageList);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Ui.setContentView(this, root);
        refreshGatewayStatus();
        loadRecent();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View header() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button back = Ui.compactButton(this, "‹");
        back.setContentDescription("Back");
        back.setOnClickListener(v -> finish());
        row.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView title = Ui.title(this, "MailXperts AI");
        title.setPadding(Ui.dp(this, 10), 0, 0, 0);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button refresh = Ui.compactButton(this, "↻");
        refresh.setContentDescription("Refresh recent mail");
        refresh.setOnClickListener(v -> loadRecent());
        row.addView(refresh, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        return row;
    }

    private View actionRow(String first, String second, String third) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button summarize = Ui.compactButton(this, first);
        summarize.setOnClickListener(v -> messageTask("summarize", ""));
        Button ask = Ui.compactButton(this, second);
        ask.setOnClickListener(v -> {
            String prompt = question.getText().toString().trim();
            if (prompt.isEmpty()) { question.setError("Enter a question"); return; }
            messageTask("ask", prompt);
        });
        Button actions = Ui.compactButton(this, third);
        actions.setOnClickListener(v -> messageTask("extract_actions", ""));
        row.addView(summarize, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f);
        middle.setMargins(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        row.addView(ask, middle);
        row.addView(actions, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f));
        return row;
    }

    private List<AccountConfig> scopedAccounts() {
        List<AccountConfig> usable = MailboxScope.usable(store);
        if (MailboxScope.isAll(accountId)) return usable;
        AccountConfig account = MailboxScope.find(usable, accountId);
        if (account == null) return usable.isEmpty() ? Collections.emptyList() : Collections.singletonList(usable.get(0));
        return Collections.singletonList(account);
    }

    private void loadRecent() {
        setStatus("Loading recent mail…", false);
        executor.execute(() -> {
            ArrayList<MessageRef> loaded = new ArrayList<>();
            String error = "";
            for (AccountConfig account : scopedAccounts()) {
                try {
                    for (MailRepository.Summary summary : MailRepository.fetchFolder(account, MailRepository.INBOX, 25)) {
                        loaded.add(new MessageRef(account, summary));
                    }
                } catch (Exception problem) {
                    error = MailRepository.safe(problem);
                }
            }
            loaded.sort(Comparator.comparingLong(MessageRef::time).reversed());
            if (loaded.size() > 50) loaded.subList(50, loaded.size()).clear();
            String finalError = error;
            runOnUiThread(() -> {
                recent.clear();
                recent.addAll(loaded);
                renderMessages(recent, "Recent mail");
                setStatus(finalError.isEmpty() ? "Ready • " + recent.size() + " recent messages" : finalError,
                        !finalError.isEmpty());
            });
        });
    }

    private void renderMessages(List<MessageRef> messages, String label) {
        messageList.removeAllViews();
        TextView heading = Ui.text(this, label + " • " + messages.size());
        heading.setTextColor(Ui.muted(this));
        messageList.addView(heading);
        for (MessageRef ref : messages) {
            String subject = ref.summary.subject == null || ref.summary.subject.trim().isEmpty()
                    ? "(No subject)" : ref.summary.subject.trim();
            String prefix = ref.summary.seen ? "" : "● ";
            Button button = Ui.secondaryButton(this,
                    prefix + subject + "\n" + safe(ref.summary.from) + accountSuffix(ref), v -> select(ref));
            button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            messageList.addView(button);
        }
    }

    private String accountSuffix(MessageRef ref) {
        return MailboxScope.isAll(accountId) ? " • " + ref.account.displayName() : "";
    }

    private void select(MessageRef ref) {
        selected = ref;
        selectedFull = null;
        selectedLabel.setText("Loading: " + safe(ref.summary.subject));
        executor.execute(() -> {
            try {
                MailRepository.FullMessage full = MailRepository.fetchMessage(
                        ref.account, ref.summary.folderKind, ref.summary.uid, false);
                runOnUiThread(() -> {
                    if (selected != ref) return;
                    selectedFull = full;
                    selectedLabel.setText((full.subject == null ? "(No subject)" : full.subject)
                            + "\nFrom: " + safe(full.from));
                    result.setText("Message selected. Choose an AI action.");
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus(MailRepository.safe(error), true));
            }
        });
    }

    private void messageTask(String task, String instruction) {
        if (!ensureMessage() || !ensureAi()) return;
        setStatus("MailXperts AI is working…", false);
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("instruction", AiPolicy.clamp(instruction, 4000));
                payload.put("message", messageJson(selected, selectedFull));
                AiGatewayClient.Result response = ai.execute(task, payload);
                runOnUiThread(() -> {
                    result.setText(response.text.isEmpty() ? "No AI response returned." : response.text);
                    setStatus("AI complete", false);
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus(error.getMessage(), true));
            }
        });
    }

    private void draftReply() {
        if (!ensureMessage() || !ensureAi()) return;
        setStatus("Drafting reply…", false);
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("message", messageJson(selected, selectedFull));
                AiGatewayClient.Result response = ai.execute("draft_reply", payload);
                runOnUiThread(() -> openDraft(response.text));
            } catch (Exception error) {
                runOnUiThread(() -> setStatus(error.getMessage(), true));
            }
        });
    }

    private void openDraft(String draftText) {
        if (draftText == null || draftText.trim().isEmpty()) {
            setStatus("AI returned an empty draft.", true);
            return;
        }
        result.setText(draftText);
        String recipient = extractAddress(selectedFull.from);
        String subject = ReplyForwardFormatter.replySubject(selectedFull.subject == null ? "" : selectedFull.subject);
        String date = selectedFull.date == null ? "" : DateFormat.getDateTimeInstance().format(selectedFull.date);
        String aiHtml = "<p>" + TextUtils.htmlEncode(draftText).replace("\n", "<br>") + "</p>";
        String chain = ReplyForwardFormatter.replyChain(selectedFull.from, selectedFull.to, "", date,
                selectedFull.subject == null ? "" : selectedFull.subject, selectedFull.html);
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", selected.account.id);
        intent.putExtra("to", recipient);
        intent.putExtra("subject", subject);
        intent.putExtra("initial_html", aiHtml + chain);
        startActivity(intent);
        setStatus("AI draft opened in Compose for review before sending.", false);
    }

    private void naturalSearch() {
        String request = search.getText().toString().trim();
        if (request.isEmpty()) { search.setError("Describe what you want to find"); return; }
        if (!ensureAi()) return;
        setStatus("Understanding your search…", false);
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("instruction", AiPolicy.clamp(request, 2000));
                AiGatewayClient.Result plan = ai.execute("search_plan", payload);
                String query = plan.keywords.isEmpty() ? request : TextUtils.join(" ", plan.keywords);
                ArrayList<MessageRef> matches = new ArrayList<>();
                for (AccountConfig account : scopedAccounts()) {
                    List<MailRepository.Summary> found = MailRepository.search(account, query, 30);
                    for (MailRepository.Summary summary : found) matches.add(new MessageRef(account, summary));
                }
                matches.sort(Comparator.comparingLong(MessageRef::time).reversed());
                runOnUiThread(() -> {
                    renderMessages(matches, "AI search results for “" + request + "”");
                    setStatus("Search complete • " + matches.size() + " matches", false);
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus(error.getMessage(), true));
            }
        });
    }

    private void priorityInbox() {
        if (!ensureAi()) return;
        if (recent.isEmpty()) { setStatus("Load recent mail first.", true); return; }
        setStatus("Ranking what needs attention…", false);
        executor.execute(() -> {
            try {
                JSONArray mailbox = new JSONArray();
                Map<String, MessageRef> byKey = new HashMap<>();
                int count = Math.min(40, recent.size());
                for (int i = 0; i < count; i++) {
                    MessageRef ref = recent.get(i);
                    String key = ref.key();
                    byKey.put(key, ref);
                    JSONObject item = new JSONObject();
                    item.put("key", key);
                    item.put("from", AiPolicy.clamp(ref.summary.from, 500));
                    item.put("subject", AiPolicy.clamp(ref.summary.subject, 1000));
                    item.put("date", ref.summary.date == null ? "" : ref.summary.date.toString());
                    item.put("unread", !ref.summary.seen);
                    mailbox.put(item);
                }
                JSONObject payload = new JSONObject();
                payload.put("mailbox", mailbox);
                AiGatewayClient.Result response = ai.execute("priority", payload);
                ArrayList<MessageRef> ranked = new ArrayList<>();
                for (String key : response.messageKeys) {
                    MessageRef ref = byKey.get(key);
                    if (ref != null && !ranked.contains(ref)) ranked.add(ref);
                }
                if (ranked.isEmpty()) ranked.addAll(recent.subList(0, Math.min(10, recent.size())));
                runOnUiThread(() -> {
                    renderMessages(ranked, "AI Priority • needs attention");
                    result.setText(response.text.isEmpty()
                            ? "Priority ranking generated from recent mailbox metadata." : response.text);
                    setStatus("AI Priority complete", false);
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus(error.getMessage(), true));
            }
        });
    }

    private void rewrite() {
        String draft = rewriteText.getText().toString().trim();
        if (draft.isEmpty()) { rewriteText.setError("Enter a draft to rewrite"); return; }
        if (!ensureAi()) return;
        String instruction = rewriteInstruction.getText().toString().trim();
        if (instruction.isEmpty()) instruction = "Make this clearer and more concise while preserving meaning.";
        String finalInstruction = instruction;
        setStatus("Rewriting draft…", false);
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("draft", AiPolicy.clamp(draft, 30000));
                payload.put("instruction", AiPolicy.clamp(finalInstruction, 2000));
                AiGatewayClient.Result response = ai.execute("rewrite", payload);
                runOnUiThread(() -> {
                    rewriteText.setText(response.text);
                    result.setText(response.text);
                    setStatus("Rewrite complete", false);
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus(error.getMessage(), true));
            }
        });
    }

    private JSONObject messageJson(MessageRef ref, MailRepository.FullMessage full) throws Exception {
        JSONObject message = new JSONObject();
        message.put("key", ref.key());
        message.put("from", AiPolicy.clamp(full.from, 1000));
        message.put("to", AiPolicy.clamp(full.to, 1000));
        message.put("subject", AiPolicy.clamp(full.subject, 2000));
        message.put("date", full.date == null ? "" : full.date.toString());
        String plain = Html.fromHtml(full.html == null ? "" : full.html,
                Html.FROM_HTML_MODE_LEGACY).toString();
        message.put("body", AiPolicy.clamp(plain, 60000));
        return message;
    }

    private boolean ensureMessage() {
        if (selected == null || selectedFull == null) {
            setStatus("Select a message first.", true);
            return false;
        }
        return true;
    }

    private boolean ensureAi() {
        if (!preferences.cloudAiAllowed()) {
            setStatus("Cloud AI is off. Enable it only after reviewing the privacy notice.", true);
            configureGateway();
            return false;
        }
        if (ai.configured()) return true;
        setStatus("Configure the MailXperts AI gateway first.", true);
        configureGateway();
        return false;
    }

    private void configureGateway() {
        LinearLayout form = Ui.vertical(this);
        EditText endpoint = Ui.edit(this, "https://ai.example.com");
        endpoint.setText(preferences.gateway());
        EditText token = Ui.edit(this, vault.token().isEmpty()
                ? "Gateway access token" : "Gateway token saved • leave blank to keep it");
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(Ui.text(this, "The OpenAI API key stays on your server. This app stores only the MailXperts gateway token in Android Keystore."));
        form.addView(Ui.text(this, "PRIVACY NOTICE: When Cloud AI is on, the subject, sender, message body and other selected mailbox context can be transmitted to your configured MailXperts AI gateway and its external AI model provider. Cloud AI is not processed on-device. You can turn it off at any time."));
        CheckBox cloudConsent = new CheckBox(this);
        cloudConsent.setText("I agree to send selected mailbox content to the configured Cloud AI service");
        cloudConsent.setChecked(preferences.cloudAiAllowed());
        form.addView(cloudConsent);
        form.addView(endpoint);
        form.addView(token);
        new AlertDialog.Builder(this)
                .setTitle("MailXperts AI gateway")
                .setView(form)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        preferences.setGateway(endpoint.getText().toString());
                        String supplied = token.getText().toString().trim();
                        if (!supplied.isEmpty()) vault.saveToken(supplied);
                        preferences.setCloudAiAllowed(cloudConsent.isChecked());
                        refreshGatewayStatus();
                        Toast.makeText(this, "AI gateway saved", Toast.LENGTH_SHORT).show();
                    } catch (Exception error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton("Disconnect", (dialog, which) -> {
                    preferences.clear();
                    vault.clear();
                    refreshGatewayStatus();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshGatewayStatus() {
        String endpoint = preferences.gateway();
        status.setText(!preferences.cloudAiAllowed()
                ? "Cloud AI disabled • No selected email content is sent to the AI gateway"
                : ai.configured()
                    ? "Cloud AI enabled; gateway configured • " + endpoint
                    : "Cloud AI enabled but gateway not configured");
        status.setTextColor(preferences.cloudAiAllowed() && ai.configured()
                ? Ui.teal(this) : Ui.muted(this));
    }

    private void setStatus(String message, boolean error) {
        if (status == null) return;
        status.setText(message == null ? "" : message);
        status.setTextColor(error ? Ui.error(this) : Ui.muted(this));
    }

    private static String extractAddress(String value) {
        try {
            InternetAddress[] parsed = InternetAddress.parse(value == null ? "" : value, false);
            if (parsed.length > 0 && parsed[0].getAddress() != null) return parsed[0].getAddress();
        } catch (Exception ignored) {}
        return value == null ? "" : value;
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private static final class MessageRef {
        final AccountConfig account;
        final MailRepository.Summary summary;
        MessageRef(AccountConfig account, MailRepository.Summary summary) {
            this.account = account;
            this.summary = summary;
        }
        long time() { return summary.date == null ? 0L : summary.date.getTime(); }
        String key() { return account.id + "|" + summary.folderKind + "|" + summary.uid; }
    }
}
