from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def write(path, content):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f"Patch target not found: {label}")
    if text.count(old) != 1:
        raise RuntimeError(f"Patch target not unique: {label} ({text.count(old)})")
    return text.replace(old, new, 1)


def regex_once(text, pattern, replacement, label):
    out, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise RuntimeError(f"Regex patch target not found/unique: {label} ({count})")
    return out


# MX-QA-006: allow http/https signature images to render in the secure rich editor preview.
compose_path = "app/src/main/java/au/com/t1xperts/mailxperts/ComposeActivity.java"
compose = read(compose_path)
compose = replace_once(
    compose,
    """        settings.setBlockNetworkLoads(true);\n        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);""",
    """        // Remote images are required for HTML signatures and quoted email content.\n        // Navigation, file/content access and persistent web storage remain blocked.\n        settings.setBlockNetworkLoads(false);\n        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);\n        settings.setSafeBrowsingEnabled(true);""",
    "Compose rich-editor remote images",
)
compose = regex_once(
    compose,
    r"    private boolean normaliseRecipientFields\(\) \{.*?\n    \}\n\n    private void saveDraft",
    """    private boolean normaliseRecipientFields() {\n        try {\n            RecipientSet.Fields recipients = RecipientSet.normalise(\n                    to.getText().toString(), cc.getText().toString(), bcc.getText().toString());\n            to.setText(recipients.to);\n            cc.setText(recipients.cc);\n            bcc.setText(recipients.bcc);\n            return true;\n        } catch (IllegalArgumentException error) {\n            to.setError(\"Check recipient addresses\");\n            status.setTextColor(Ui.error(this));\n            status.setText(error.getMessage());\n            return false;\n        }\n    }\n\n    private void saveDraft""",
    "Compose recipient de-duplication",
)
write(compose_path, compose)

signature_path = "app/src/main/java/au/com/t1xperts/mailxperts/SignatureEditorActivity.java"
signature = read(signature_path)
signature = replace_once(
    signature,
    """                        + \"keeps an http/https link in the outgoing signature. Linked images \"\n                        + \"are blocked in this secure editor preview and load in compatible \"\n                        + \"mail clients after sending.\");""",
    """                        + \"keeps an http/https link in the outgoing signature. Linked images \"\n                        + \"are previewed securely here while navigation, scripts outside the editor, \"\n                        + \"local file access and persistent web storage remain restricted.\");""",
    "Signature editor help text",
)
signature = replace_once(
    signature,
    """        settings.setBlockNetworkLoads(true);\n        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);""",
    """        settings.setBlockNetworkLoads(false);\n        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);\n        settings.setSafeBrowsingEnabled(true);""",
    "Signature editor remote images",
)
write(signature_path, signature)

# MX-QA-017: chip-style recipient entry with To/Cc/Bcc cross-field duplicate prevention.
ui_path = "app/src/main/java/au/com/t1xperts/mailxperts/Ui.java"
ui = read(ui_path)
ui = regex_once(
    ui,
    r"    static MultiAutoCompleteTextView recipientEdit\(Context context, String hint\) \{.*?\n    \}\n\n    static EditText multiLine",
    """    static MultiAutoCompleteTextView recipientEdit(Context context, String hint) {\n        MultiAutoCompleteTextView edit = new RecipientChipAutoCompleteTextView(context);\n        edit.setHint(hint);\n        edit.setHintTextColor(muted(context));\n        edit.setTextColor(textColor(context));\n        edit.setTextSize(16);\n        edit.setSingleLine(false);\n        edit.setMaxLines(3);\n        edit.setHorizontallyScrolling(false);\n        edit.setThreshold(1);\n        edit.setTokenizer(new RecipientChipAutoCompleteTextView.RecipientTokenizer(context));\n        edit.setInputType(InputType.TYPE_CLASS_TEXT\n                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS\n                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);\n        edit.setPadding(dp(context, 12), dp(context, 11), dp(context, 12), dp(context, 11));\n        edit.setBackground(rounded(panel(context),\n                Color.parseColor(ThemeManager.isDark(context) ? \"#31535A\" : \"#B7D6D8\"),\n                1, 12, context));\n        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(\n                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);\n        params.setMargins(0, 0, 0, dp(context, 8));\n        edit.setLayoutParams(params);\n        return edit;\n    }\n\n    static EditText multiLine""",
    "Recipient chip input",
)
write(ui_path, ui)

adapter_path = "app/src/main/java/au/com/t1xperts/mailxperts/RecipientSuggestionAdapter.java"
adapter = read(adapter_path)
adapter = replace_once(
    adapter,
    """            @Override public CharSequence convertResultToString(Object resultValue) {\n                return resultValue == null ? \"\" : resultValue.toString();\n            }""",
    """            @Override public CharSequence convertResultToString(Object resultValue) {\n                String value = resultValue == null ? \"\" : resultValue.toString();\n                return RecipientChipStyler.style(context, value);\n            }""",
    "Autocomplete suggestion chip",
)
write(adapter_path, adapter)

# MX-QA-022: never use raw provider/server text as the primary user-facing auth error.
settings_path = "app/src/main/java/au/com/t1xperts/mailxperts/SettingsActivity.java"
settings = read(settings_path)
settings = replace_once(
    settings,
    "status.setText(prefix + MailRepository.safe(error));",
    "status.setText(prefix + ProviderErrorMessage.forAccount(current, error));",
    "Settings friendly provider errors",
)
write(settings_path, settings)

oauth_path = "app/src/main/java/au/com/t1xperts/mailxperts/OAuthConnectActivity.java"
oauth = read(oauth_path)
oauth = replace_once(
    oauth,
    """                showError(\"OAuth sign-in completed, but mailbox validation failed: \"\n                        + MailRepository.safe(error));""",
    """                showError(ProviderErrorMessage.forAccount(account, error));""",
    "OAuth mailbox validation error",
)
oauth = replace_once(
    oauth,
    """            status.setText(message == null ? \"Authentication failed.\" : message);""",
    """            status.setText(ProviderErrorMessage.forProvider(\n                    account == null ? \"\" : account.provider, message));""",
    "OAuth friendly error surface",
)
write(oauth_path, oauth)

# Release identity advances so beta.5 upgrades beta.4 in place.
gradle_path = "app/build.gradle"
gradle = read(gradle_path)
gradle = replace_once(gradle, "versionCode 18", "versionCode 19", "versionCode 19")
gradle = replace_once(gradle, "versionName '1.6.0-beta.4'", "versionName '1.6.0-beta.5'", "versionName beta.5")
write(gradle_path, gradle)

release_test_path = "app/src/test/java/au/com/t1xperts/mailxperts/ReleaseIdentityContractTest.java"
release_test = read(release_test_path)
release_test = replace_once(release_test, "v1.6.0-beta.3 versionCode", "v1.6.0-beta.4 versionCode", "release contract message")
release_test = replace_once(release_test, 'gradle.contains("versionCode 18")', 'gradle.contains("versionCode 19")', "release code assertion")
release_test = replace_once(release_test, 'gradle.contains("versionName \'1.6.0-beta.4\'")', 'gradle.contains("versionName \'1.6.0-beta.5\'")', "release name assertion")
write(release_test_path, release_test)

# New implementation helpers.
write("app/src/main/java/au/com/t1xperts/mailxperts/RecipientChipStyler.java", r'''package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ReplacementSpan;

/** Rounded recipient token styling used by To/Cc/Bcc fields. */
final class RecipientChipStyler {
    private RecipientChipStyler() {}

    static CharSequence style(Context context, String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return "";
        SpannableString styled = new SpannableString(text);
        styled.setSpan(new ChipSpan(context), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
    }

    static final class ChipSpan extends ReplacementSpan {
        private final int fill;
        private final int stroke;
        private final int textColor;
        private final float pad;
        private final float radius;

        ChipSpan(Context context) {
            fill = Ui.panelSoft(context);
            stroke = Ui.teal(context);
            textColor = Ui.textColor(context);
            pad = Ui.dp(context, 7);
            radius = Ui.dp(context, 10);
        }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end,
                                     Paint.FontMetricsInt fm) {
            return Math.round(paint.measureText(text, start, end) + pad * 2f);
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                                   float x, int top, int y, int bottom, Paint paint) {
            float width = paint.measureText(text, start, end) + pad * 2f;
            RectF rect = new RectF(x, top + 2, x + width, bottom - 2);
            int oldColor = paint.getColor();
            Paint.Style oldStyle = paint.getStyle();
            float oldStroke = paint.getStrokeWidth();
            paint.setColor(fill);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setColor(stroke);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(textColor);
            canvas.drawText(text, start, end, x + pad, y, paint);
            paint.setColor(oldColor);
            paint.setStyle(oldStyle);
            paint.setStrokeWidth(oldStroke);
        }
    }
}
''')

write("app/src/main/java/au/com/t1xperts/mailxperts/RecipientChipAutoCompleteTextView.java", r'''package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.MultiAutoCompleteTextView;

/** Multi-recipient input that turns completed comma/semicolon/newline tokens into visual chips. */
final class RecipientChipAutoCompleteTextView extends MultiAutoCompleteTextView {
    private boolean styling;

    RecipientChipAutoCompleteTextView(Context context) {
        super(context);
        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { styleCompleted(s); }
        });
    }

    private void styleCompleted(Editable text) {
        if (styling || text == null) return;
        styling = true;
        try {
            RecipientChipStyler.ChipSpan[] old = text.getSpans(
                    0, text.length(), RecipientChipStyler.ChipSpan.class);
            for (RecipientChipStyler.ChipSpan span : old) text.removeSpan(span);
            int tokenStart = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == ',' || c == ';' || c == '\n') {
                    applySpan(text, tokenStart, i);
                    tokenStart = i + 1;
                }
            }
        } finally {
            styling = false;
        }
    }

    private void applySpan(Editable text, int start, int end) {
        while (start < end && Character.isWhitespace(text.charAt(start))) start++;
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) end--;
        if (end <= start) return;
        text.setSpan(new RecipientChipStyler.ChipSpan(getContext()), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /** Tokenizer accepts comma, semicolon and newline, while suggestions terminate with comma. */
    static final class RecipientTokenizer implements Tokenizer {
        private final Context context;
        RecipientTokenizer(Context context) { this.context = context.getApplicationContext(); }

        @Override public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0) {
                char c = text.charAt(i - 1);
                if (c == ',' || c == ';' || c == '\n') break;
                i--;
            }
            while (i < cursor && Character.isWhitespace(text.charAt(i))) i++;
            return i;
        }

        @Override public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            while (i < text.length()) {
                char c = text.charAt(i);
                if (c == ',' || c == ';' || c == '\n') return i;
                i++;
            }
            return text.length();
        }

        @Override public CharSequence terminateToken(CharSequence text) {
            String trimmed = text == null ? "" : text.toString().trim();
            if (trimmed.isEmpty()) return "";
            CharSequence chip = RecipientChipStyler.style(context, trimmed);
            SpannableString out = new SpannableString(chip + ", ");
            if (chip instanceof Spanned) {
                TextUtils.copySpansFrom((Spanned) chip, 0, chip.length(), Object.class,
                        out, 0);
            }
            return out;
        }
    }
}
''')

write("app/src/main/java/au/com/t1xperts/mailxperts/RecipientSet.java", r'''package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

import javax.mail.internet.InternetAddress;

/** Normalises To/Cc/Bcc and prevents the same mailbox appearing in multiple fields. */
final class RecipientSet {
    private RecipientSet() {}

    static final class Fields {
        final String to;
        final String cc;
        final String bcc;
        Fields(String to, String cc, String bcc) {
            this.to = to;
            this.cc = cc;
            this.bcc = bcc;
        }
    }

    static Fields normalise(String to, String cc, String bcc) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        return new Fields(clean(to, seen), clean(cc, seen), clean(bcc, seen));
    }

    private static String clean(String raw, LinkedHashSet<String> seen) {
        String normalised = RecipientNormalizer.normalise(raw);
        if (normalised.isEmpty()) return "";
        try {
            InternetAddress[] parsed = InternetAddress.parse(normalised, true);
            ArrayList<String> kept = new ArrayList<>();
            for (InternetAddress address : parsed) {
                address.validate();
                String mailbox = address.getAddress() == null
                        ? address.toString() : address.getAddress();
                String key = mailbox.trim().toLowerCase(Locale.ROOT);
                if (seen.add(key)) kept.add(address.toUnicodeString());
            }
            return String.join(", ", kept);
        } catch (Exception error) {
            throw new IllegalArgumentException(
                    "Invalid recipient address. Check To/Cc/Bcc entries.", error);
        }
    }
}
''')

write("app/src/main/java/au/com/t1xperts/mailxperts/ProviderErrorMessage.java", r'''package au.com.t1xperts.mailxperts;

import java.util.Locale;

/** Converts provider/server failures into safe, actionable user-facing explanations. */
final class ProviderErrorMessage {
    private ProviderErrorMessage() {}

    static String forAccount(AccountConfig account, Throwable error) {
        String provider = account == null ? "" : account.provider;
        boolean oauth = account != null && AuthType.isOAuth(account.authType);
        String raw = error == null ? "" : error.getMessage();
        return describe(provider, oauth, raw);
    }

    static String forProvider(String provider, String raw) {
        return describe(provider, true, raw);
    }

    private static String describe(String provider, boolean oauth, String raw) {
        String message = raw == null ? "" : raw.trim();
        String lower = message.toLowerCase(Locale.ROOT);
        boolean gmail = ProviderPreset.GMAIL.equals(provider);
        boolean outlook = ProviderPreset.OUTLOOK.equals(provider);

        if (lower.contains("cancelled") || lower.contains("canceled")) {
            return "Authorization was cancelled. No account changes were made.";
        }
        if (lower.contains("does not use oauth")) return "This provider does not use OAuth in MailXperts.";
        if (lower.contains("not configured") && outlook) {
            return "Microsoft sign-in is not configured for this build yet. Add the registered Microsoft client ID and redirect URI, then rebuild MailXperts.";
        }
        if (lower.contains("invalid_grant") || lower.contains("token has been expired")
                || lower.contains("token expired") || lower.contains("revoked")) {
            if (gmail) return "Google authorization has expired or was revoked. Reconnect this account with Continue with Google.";
            if (outlook) return "Microsoft authorization has expired or was revoked. Reconnect this account with Continue with Microsoft.";
            return "The provider authorization expired or was revoked. Reconnect this account and try again.";
        }
        if (lower.contains("authenticationfailed") || lower.contains("authentication failed")
                || lower.contains("invalid credentials") || lower.contains("username and password not accepted")
                || lower.contains("web login required") || lower.contains("535") || lower.contains("534")) {
            if (gmail && oauth) {
                return "Google rejected the mailbox sign-in. Reconnect with Continue with Google and approve Gmail access.";
            }
            if (gmail) {
                return "Google rejected the App Password. Use Continue with Google (recommended), or create a fresh 16-character App Password after enabling 2-Step Verification.";
            }
            if (outlook) {
                return "Microsoft rejected the mailbox sign-in. Reconnect with Continue with Microsoft.";
            }
            return "The mail provider rejected the sign-in. Check the account credentials or reconnect the provider.";
        }
        if (lower.contains("unknownhost") || lower.contains("unknown host")
                || lower.contains("connectexception") || lower.contains("connection refused")
                || lower.contains("timed out") || lower.contains("timeout")
                || lower.contains("no route to host")) {
            return "Could not reach the mail server. Check your internet connection and the IMAP/SMTP server address, then try again.";
        }
        if (lower.contains("sslhandshake") || lower.contains("certificate")
                || lower.contains("pkix") || lower.contains("handshake_failure")) {
            return "MailXperts could not verify the server's secure TLS connection. Check the server hostname and your device date/time.";
        }
        if (lower.contains("imap") && lower.contains("disabled")) {
            return "IMAP access is disabled for this mailbox or blocked by the provider policy. Enable IMAP access or ask the mailbox administrator.";
        }
        if (gmail && lower.contains("app password")) {
            return "Use Continue with Google (recommended). If you use the fallback App Password method, create a fresh 16-character Google App Password after enabling 2-Step Verification.";
        }
        if (message.isEmpty()) return "Mail account connection failed. Check the account settings and try again.";
        return "Mail account connection failed. Check the account settings or reconnect the provider, then try again.";
    }
}
''')

# Tests/evidence for all selected backlog items.
write("app/src/test/java/au/com/t1xperts/mailxperts/RecipientSetTest.java", r'''package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecipientSetTest {
    @Test public void duplicateRecipientsAreRemovedAcrossToCcBcc() {
        RecipientSet.Fields fields = RecipientSet.normalise(
                "Alice <alice@example.com>; bob@example.com",
                "ALICE@example.com, carol@example.com",
                "bob@example.com, dave@example.com");
        assertTrue(fields.to.toLowerCase().contains("alice@example.com"));
        assertTrue(fields.to.toLowerCase().contains("bob@example.com"));
        assertFalse(fields.cc.toLowerCase().contains("alice@example.com"));
        assertTrue(fields.cc.toLowerCase().contains("carol@example.com"));
        assertFalse(fields.bcc.toLowerCase().contains("bob@example.com"));
        assertEquals("dave@example.com", fields.bcc);
    }
}
''')

write("app/src/test/java/au/com/t1xperts/mailxperts/ProviderErrorMessageTest.java", r'''package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProviderErrorMessageTest {
    @Test public void gmailOAuthFailureIsActionableAndDoesNotExposeRawServerText() {
        AccountConfig account = new AccountConfig();
        account.provider = ProviderPreset.GMAIL;
        account.authType = AuthType.OAUTH2;
        String message = ProviderErrorMessage.forAccount(account,
                new Exception("AUTHENTICATIONFAILED 535 invalid credentials token=secret"));
        assertTrue(message.contains("Continue with Google"));
        assertFalse(message.contains("535"));
        assertFalse(message.contains("secret"));
    }

    @Test public void networkFailureIsExplainedWithoutExceptionName() {
        String message = ProviderErrorMessage.forProvider(
                ProviderPreset.GMAIL, "java.net.UnknownHostException: imap.gmail.com");
        assertTrue(message.contains("Could not reach the mail server"));
        assertFalse(message.contains("UnknownHostException"));
    }
}
''')

write("app/src/test/java/au/com/t1xperts/mailxperts/BacklogCompletionContractTest.java", r'''package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Release contracts for MX-QA-006/011/012/013/014/015/017/019/021/022/023/026. */
public class BacklogCompletionContractTest {
    @Test public void selectedBacklogCapabilitiesArePresent() throws Exception {
        String compose = src("ComposeActivity.java");
        String signature = src("SignatureEditorActivity.java");
        String message = src("MessageActivity.java");
        String navigation = src("Navigation.java");
        String mailbox = src("MailboxActivity.java");
        String settings = src("SettingsActivity.java");
        String search = src("SearchActivity.java");
        String searchSpec = src("MailSearchSpec.java");
        String manifest = project("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml");

        // 006 HTML signature image reliability
        assertTrue(compose.contains("setBlockNetworkLoads(false)"));
        assertTrue(signature.contains("setBlockNetworkLoads(false)"));
        assertTrue(signature.contains("MIXED_CONTENT_COMPATIBILITY_MODE"));

        // 011 message details
        assertTrue(message.contains("Show message details"));
        assertTrue(message.contains("Reply-To"));
        assertTrue(message.contains("Message-ID"));

        // 012 safe sender HTML under app themes
        String htmlPolicy = src("EmailHtmlPolicy.java");
        assertTrue(htmlPolicy.contains("color-scheme:light"));
        assertTrue(htmlPolicy.contains("background:#ffffff"));

        // 013 full-height structured drawer
        assertTrue(navigation.contains("Gravity.START"));
        assertTrue(navigation.contains("WindowManager.LayoutParams.MATCH_PARENT"));
        assertTrue(navigation.contains("PEOPLE"));
        assertTrue(navigation.contains("ACCOUNTS & APP"));

        // 014 compact Smart Priority
        assertTrue(mailbox.contains("Smart Priority"));
        assertTrue(message.contains("smartStatus.setMaxLines(2)"));

        // 015 settings hierarchy/progressive disclosure
        assertTrue(settings.contains("ACCOUNT IDENTITY"));
        assertTrue(settings.contains("CACHE-FIRST SYNCHRONISATION"));
        assertTrue(settings.contains("Show advanced server settings"));
        assertTrue(settings.contains("SIGNATURE"));

        // 017 recipient chips/autocomplete/dedup
        assertTrue(compose.contains("RecipientSuggestionAdapter"));
        assertTrue(compose.contains("RecipientSet.normalise"));
        assertTrue(src("RecipientChipAutoCompleteTextView.java").contains("ChipSpan"));

        // 019 Android share-to-compose
        assertTrue(manifest.contains("android.intent.action.SEND"));
        assertTrue(manifest.contains("android.intent.action.SEND_MULTIPLE"));
        assertTrue(compose.contains("importSharedAttachments"));

        // 021 quick account switcher
        assertTrue(mailbox.contains("accountSpinner()"));
        assertTrue(mailbox.contains("Add new mail account"));
        assertTrue(mailbox.contains("AccountIdentity.dropdown"));

        // 022 friendly provider auth errors
        assertTrue(settings.contains("ProviderErrorMessage.forAccount"));
        assertTrue(src("OAuthConnectActivity.java").contains("ProviderErrorMessage"));

        // 023 clear server-delete behaviour
        assertTrue(settings.contains("Safe default: server deletion is off"));
        assertTrue(settings.contains("Current Delete behaviour"));
        assertTrue(message.contains("Delete — move to server Trash"));
        assertTrue(message.contains("Delete on this device"));

        // 026 advanced scoped/token search
        assertTrue(search.contains("All server folders"));
        assertTrue(search.contains("All Accounts"));
        assertTrue(search.contains("All words"));
        assertTrue(search.contains("Exact phrase"));
        assertTrue(searchSpec.contains("MODE_ANY"));
        assertTrue(searchSpec.contains("SCOPE_ALL"));
    }

    private static String src(String file) throws Exception {
        return project("src/main/java/au/com/t1xperts/mailxperts/" + file,
                "app/src/main/java/au/com/t1xperts/mailxperts/" + file);
    }

    private static String project(String modulePath, String rootPath) throws Exception {
        Path path = Paths.get(modulePath);
        if (!Files.exists(path)) path = Paths.get(rootPath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
''')

write("RELEASE-NOTES-v1.6.0-beta.5.md", '''# MailXperts v1.6.0-beta.5\n\n## Consolidated backlog completion\n\nThis beta consolidates MX-QA-006, 011, 012, 013, 014, 015, 017, 019, 021, 022, 023 and 026.\n\n- HTML signatures can preview remote http/https logos/images in the secure rich editor while local file/content access remains blocked.\n- Expandable sender/recipient/message metadata remains available from message view.\n- HTML mail uses a neutral light sender-content canvas so app dark/light chrome does not corrupt sender formatting.\n- Full-height structured navigation drawer is covered by release contracts.\n- Smart Priority presentation remains compact.\n- Account/settings forms use grouped sections and collapsed advanced server settings.\n- To/Cc/Bcc now use chip-style autocomplete tokens and cross-field duplicate prevention.\n- Android SEND/SEND_MULTIPLE share intents open MailXperts compose with shared text/files.\n- Inbox header provides direct multi-account switching with actual email identity.\n- Provider authentication errors are mapped to safe, actionable messages instead of raw server text.\n- Server-delete mode is explicit, safe-by-default and consistent between settings and message actions.\n- Advanced search supports account/folder scope, all/any/exact matching, wildcard-style partial matching and origin-labelled results.\n\n## Release identity\n\n- versionName: `1.6.0-beta.5`\n- versionCode: `19`\n- package: `au.com.t1xperts.mailxperts`\n\nDevice QA/UAT remains required before Product Owner lifecycle closure.\n''')

print("Consolidated backlog patch applied successfully.")
