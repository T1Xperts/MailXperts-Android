package au.com.t1xperts.mailxperts;

import android.text.Html;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Privacy-preserving, on-device rules for useful mailbox triage. */
final class MailIntelligence {
    static final class Result {
        final String category;
        final String label;
        final String explanation;
        final boolean important;
        final long dueAt;

        Result(String category, String label, String explanation, boolean important, long dueAt) {
            this.category = category;
            this.label = label;
            this.explanation = explanation;
            this.important = important;
            this.dueAt = dueAt;
        }

        boolean hasDueDate() { return dueAt > 0L; }
        boolean isOverdue() { return dueAt > 0L && dueAt < System.currentTimeMillis(); }
    }

    private static final String[] BILL = {
            "payment due", "due date", "amount due", "pay by", "overdue", "invoice",
            "utility bill", "credit card", "loan payment", "mortgage", "statement ready"
    };
    private static final String[] SECURITY = {
            "security alert", "unusual activity", "new sign-in", "password reset",
            "verification code", "one-time code", "one time code", "otp", "account locked"
    };
    private static final String[] TRAVEL = {
            "boarding pass", "flight", "itinerary", "hotel booking", "reservation", "check-in"
    };
    private static final String[] DELIVERY = {
            "out for delivery", "tracking number", "parcel", "package", "shipped", "delivery"
    };
    private static final String[] RECEIPT = {
            "receipt", "order confirmed", "order confirmation", "tax invoice", "purchase"
    };

    private static final Pattern ISO_DATE = Pattern.compile("(?i)(?:due|pay(?:ment)?\\s+by)[^0-9]{0,20}(20\\d{2})[-/](\\d{1,2})[-/](\\d{1,2})");
    private static final Pattern LOCAL_DATE = Pattern.compile("(?i)(?:due(?:\\s+(?:on|by))?|pay(?:ment)?\\s+by)[^0-9]{0,20}(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?");
    private static final Pattern NAMED_DATE = Pattern.compile("(?i)(?:due(?:\\s+(?:on|by))?|pay(?:ment)?\\s+by)[^a-z0-9]{0,20}(\\d{1,2})\\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s+(20\\d{2}))?");

    private MailIntelligence() {}

    static Result analyse(String from, String subject, String html, Date received) {
        String plain = html == null ? "" : Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        String text = ((from == null ? "" : from) + " " + (subject == null ? "" : subject) + " " + plain)
                .toLowerCase(Locale.ROOT);
        long due = extractDueDate(text, received);
        if (containsAny(text, BILL) || due > 0L) {
            return new Result("BILL", "Bill / payment", "Possible bill or payment deadline — verify the sender before acting.", true, due);
        }
        if (containsAny(text, SECURITY)) {
            return new Result("SECURITY", "Security", "Possible account-security notice — verify links and sender details.", true, 0L);
        }
        if (containsAny(text, TRAVEL)) {
            return new Result("TRAVEL", "Travel", "Travel or booking information detected on this device.", true, 0L);
        }
        if (containsAny(text, DELIVERY)) {
            return new Result("DELIVERY", "Delivery", "Delivery or tracking information detected on this device.", false, 0L);
        }
        if (containsAny(text, RECEIPT)) {
            return new Result("RECEIPT", "Receipt", "Receipt or purchase information detected on this device.", false, 0L);
        }
        return new Result("GENERAL", "", "", false, 0L);
    }

    private static boolean containsAny(String text, String[] values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static long extractDueDate(String text, Date received) {
        Matcher iso = ISO_DATE.matcher(text);
        if (iso.find()) return date(integer(iso.group(1)), integer(iso.group(2)), integer(iso.group(3)));

        Matcher local = LOCAL_DATE.matcher(text);
        if (local.find()) {
            int day = integer(local.group(1));
            int month = integer(local.group(2));
            int year = year(local.group(3), received);
            return date(year, month, day);
        }

        Matcher named = NAMED_DATE.matcher(text);
        if (named.find()) {
            int day = integer(named.group(1));
            int month = month(named.group(2));
            int year = year(named.group(3), received);
            return date(year, month, day);
        }
        return 0L;
    }

    private static int year(String raw, Date received) {
        if (raw != null && !raw.isEmpty()) {
            int value = integer(raw);
            return value < 100 ? 2000 + value : value;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(received == null ? new Date() : received);
        return calendar.get(Calendar.YEAR);
    }

    private static int month(String raw) {
        try {
            Date parsed = new SimpleDateFormat("MMM", Locale.ENGLISH).parse(raw.substring(0, 3));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return calendar.get(Calendar.MONTH) + 1;
        } catch (ParseException | RuntimeException ignored) { return 0; }
    }

    private static long date(int year, int month, int day) {
        if (year < 2000 || month < 1 || month > 12 || day < 1 || day > 31) return 0L;
        Calendar calendar = Calendar.getInstance();
        calendar.setLenient(false);
        calendar.set(year, month - 1, day, 9, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        try { return calendar.getTimeInMillis(); }
        catch (IllegalArgumentException ignored) { return 0L; }
    }

    private static int integer(String value) {
        try { return Integer.parseInt(value); }
        catch (Exception ignored) { return 0; }
    }
}
