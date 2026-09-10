from pathlib import Path

path = Path('app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java')
text = path.read_text(encoding='utf-8')

old_reply = '''    private void reply() {
        if (loaded == null) return;
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", accountId);
        String replyTo = headerValue("Reply-To");
        intent.putExtra("to", extract(replyTo.isEmpty() ? loaded.from : replyTo));
        String subject = loaded.subject == null ? "" : loaded.subject;
        intent.putExtra("subject", subject.toLowerCase(Locale.ROOT).startsWith("re:") ? subject : "Re: " + subject);
        startActivity(intent);
    }
'''

new_reply = '''    private void reply() {
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
'''

old_reply_all = '''    private void replyAll() {
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
        intent.putExtra("subject", subject.toLowerCase(Locale.ROOT).startsWith("re:") ? subject : "Re: " + subject);
        startActivity(intent);
    }
'''

new_reply_all = '''    private void replyAll() {
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
'''

old_forward = '''    private void forward() {
        if (loaded == null) return;
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("account_id", accountId);
        String subject = loaded.subject == null ? "" : loaded.subject;
        intent.putExtra("subject", subject.toLowerCase(Locale.ROOT).startsWith("fwd:") ? subject : "Fwd: " + subject);
        String plain = Html.fromHtml(loaded.html == null ? "" : loaded.html,
                Html.FROM_HTML_MODE_LEGACY).toString();
        String date = loaded.date == null ? "" : DateFormat.getDateTimeInstance().format(loaded.date);
        String forwarded = "<hr><p><strong>Forwarded message</strong><br>"
                + "From: " + TextUtils.htmlEncode(loaded.from == null ? "" : loaded.from) + "<br>"
                + "Date: " + TextUtils.htmlEncode(date) + "<br>"
                + "Subject: " + TextUtils.htmlEncode(subject) + "<br>"
                + "To: " + TextUtils.htmlEncode(loaded.to == null ? "" : loaded.to) + "</p>"
                + "<div style='white-space:pre-wrap'>" + TextUtils.htmlEncode(plain) + "</div>";
        intent.putExtra("initial_html", forwarded);
        startActivity(intent);
    }
'''

new_forward = '''    private void forward() {
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
'''

for name, old, new in [
    ('reply', old_reply, new_reply),
    ('replyAll', old_reply_all, new_reply_all),
    ('forward', old_forward, new_forward),
]:
    if old not in text:
        raise SystemExit(f'Expected {name} block was not found; refusing a blind patch')
    text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('MX-QA-025 patch applied successfully')
