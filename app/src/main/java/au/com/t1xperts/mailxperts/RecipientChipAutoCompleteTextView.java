package au.com.t1xperts.mailxperts;

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
