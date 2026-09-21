package au.com.t1xperts.mailxperts;

/**
 * Renders sender HTML on a neutral light email canvas so dark/light app chrome cannot
 * invert or overwrite sender formatting. Explicit sender colours/backgrounds remain intact.
 */
final class EmailHtmlPolicy {
    private EmailHtmlPolicy() {}

    static String wrapForDisplay(String html) {
        return "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<meta name='color-scheme' content='only light'>"
                + "<meta name='supported-color-schemes' content='light'>"
                + "<style>html,body{color-scheme:light;background:#ffffff;color:#111111;}"
                + "body{font-family:sans-serif;line-height:1.45;padding:12px;margin:0;overflow-wrap:anywhere;}"
                + "a{color:#007a73}img{max-width:100%;height:auto}table{max-width:100%}"
                + "blockquote{border-left:3px solid #008f87;padding-left:10px}</style>"
                + "</head><body>" + (html == null ? "" : html) + "</body></html>";
    }
}
