package au.com.t1xperts.mailxperts;

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
