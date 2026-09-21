package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.RecipientStringTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

/** Pure-Java search specification shared by server search, local search and JVM QA. */
final class MailSearchSpec {
    static final String MODE_ALL = "ALL_WORDS";
    static final String MODE_ANY = "ANY_WORD";
    static final String MODE_EXACT = "EXACT_PHRASE";

    static final String SCOPE_CURRENT = "CURRENT";
    static final String SCOPE_INBOX = "INBOX";
    static final String SCOPE_SENT = "SENT";
    static final String SCOPE_JUNK = "JUNK";
    static final String SCOPE_ALL_SERVER = "ALL_SERVER";
    static final String SCOPE_DRAFTS = "DRAFTS";
    static final String SCOPE_OUTBOX = "OUTBOX";
    static final String SCOPE_SCHEDULED = "SCHEDULED";
    static final String SCOPE_ALL = "ALL";

    final String query;
    final String mode;
    final String folderScope;
    final String currentFolder;

    MailSearchSpec(String query, String mode, String folderScope, String currentFolder) {
        this.query = query == null ? "" : query.trim();
        this.mode = MODE_ANY.equals(mode) || MODE_EXACT.equals(mode) ? mode : MODE_ALL;
        this.folderScope = folderScope == null ? SCOPE_ALL_SERVER : folderScope;
        this.currentFolder = currentFolder == null || currentFolder.isEmpty()
                ? MailRepository.INBOX : currentFolder;
    }

    SearchTerm serverTerm() {
        List<String> tokens = tokens();
        if (tokens.isEmpty()) return fieldTerm("");
        if (MODE_EXACT.equals(mode)) return fieldTerm(cleanToken(query));
        SearchTerm[] terms = new SearchTerm[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) terms[i] = fieldTerm(tokens.get(i));
        return MODE_ANY.equals(mode) ? new OrTerm(terms) : new AndTerm(terms);
    }

    List<String> serverFolders() {
        ArrayList<String> out = new ArrayList<>();
        if (SCOPE_CURRENT.equals(folderScope)) {
            if (isServerFolder(currentFolder)) out.add(currentFolder);
        } else if (SCOPE_INBOX.equals(folderScope)) {
            out.add(MailRepository.INBOX);
        } else if (SCOPE_SENT.equals(folderScope)) {
            out.add(MailRepository.SENT);
        } else if (SCOPE_JUNK.equals(folderScope)) {
            out.add(MailRepository.JUNK);
        } else if (SCOPE_ALL_SERVER.equals(folderScope) || SCOPE_ALL.equals(folderScope)) {
            out.add(MailRepository.INBOX);
            out.add(MailRepository.SENT);
            out.add(MailRepository.JUNK);
        }
        return out;
    }

    List<String> localFolders() {
        ArrayList<String> out = new ArrayList<>();
        if (SCOPE_CURRENT.equals(folderScope)) {
            if (isLocalFolder(currentFolder)) out.add(currentFolder);
        } else if (SCOPE_DRAFTS.equals(folderScope)) out.add(LocalStore.DRAFT);
        else if (SCOPE_OUTBOX.equals(folderScope)) out.add(LocalStore.OUTBOX);
        else if (SCOPE_SCHEDULED.equals(folderScope)) out.add(LocalStore.SCHEDULED);
        else if (SCOPE_ALL.equals(folderScope)) {
            out.add(LocalStore.DRAFT);
            out.add(LocalStore.OUTBOX);
            out.add(LocalStore.SCHEDULED);
        }
        return out;
    }

    boolean matchesLocal(LocalStore.LocalMessage message) {
        return matchesLocal(message, "");
    }

    boolean matchesLocal(LocalStore.LocalMessage message, String attachmentNames) {
        if (message == null) return false;
        String haystack = (safe(message.to) + " " + safe(message.cc) + " " + safe(message.bcc) + " "
                + safe(message.subject) + " " + htmlText(message.html) + " "
                + safe(attachmentNames)).toLowerCase(Locale.ROOT);
        if (MODE_EXACT.equals(mode)) {
            String exact = cleanToken(query).toLowerCase(Locale.ROOT);
            return !exact.isEmpty() && haystack.contains(exact);
        }
        List<String> tokens = tokens();
        if (tokens.isEmpty()) return false;
        if (MODE_ANY.equals(mode)) {
            for (String token : tokens) if (haystack.contains(token.toLowerCase(Locale.ROOT))) return true;
            return false;
        }
        for (String token : tokens) {
            if (!haystack.contains(token.toLowerCase(Locale.ROOT))) return false;
        }
        return true;
    }

    private List<String> tokens() {
        ArrayList<String> out = new ArrayList<>();
        String cleaned = query.replaceAll("[\\s,;]+", " ").trim();
        if (cleaned.isEmpty()) return out;
        for (String token : cleaned.split(" ")) {
            String value = cleanToken(token);
            if (!value.isEmpty()) out.add(value);
        }
        return out;
    }

    private static String cleanToken(String token) {
        if (token == null) return "";
        return token.replace("*", "").trim();
    }

    private static SearchTerm fieldTerm(String value) {
        return new OrTerm(new SearchTerm[]{
                new SubjectTerm(value),
                new FromStringTerm(value),
                new RecipientStringTerm(Message.RecipientType.TO, value),
                new RecipientStringTerm(Message.RecipientType.CC, value),
                new RecipientStringTerm(Message.RecipientType.BCC, value),
                new BodyTerm(value)
        });
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /** Lightweight pure-Java HTML-to-searchable-text conversion for local JVM tests. */
    private static String htmlText(String html) {
        if (html == null || html.isEmpty()) return "";
        return html
                .replaceAll("(?is)<\\s*(script|style)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }


    private static boolean isLocalFolder(String folder) {
        return LocalStore.DRAFT.equals(folder)
                || LocalStore.OUTBOX.equals(folder)
                || LocalStore.SCHEDULED.equals(folder);
    }

    private static boolean isServerFolder(String folder) {
        return MailRepository.INBOX.equals(folder)
                || MailRepository.SENT.equals(folder)
                || MailRepository.JUNK.equals(folder);
    }
}
