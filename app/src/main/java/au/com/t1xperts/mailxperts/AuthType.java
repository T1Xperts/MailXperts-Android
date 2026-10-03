package au.com.t1xperts.mailxperts;

/** Authentication methods supported by MailXperts provider adapters. */
final class AuthType {
    static final String PASSWORD = "PASSWORD";
    static final String APP_PASSWORD = "APP_PASSWORD";
    static final String OAUTH2 = "OAUTH2";

    private AuthType() {}

    static boolean isOAuth(String value) {
        return OAUTH2.equals(value);
    }

    static boolean requiresPassword(String value) {
        return !isOAuth(value);
    }

    static String normalise(String value) {
        if (OAUTH2.equals(value) || APP_PASSWORD.equals(value) || PASSWORD.equals(value)) {
            return value;
        }
        return PASSWORD;
    }
}
