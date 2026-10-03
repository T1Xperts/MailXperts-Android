package au.com.t1xperts.mailxperts;

/** In-memory OAuth material. Persisted only through CredentialVault. */
final class OAuthCredential {
    final String accessToken;
    final String refreshToken;
    final long expiresAtMillis;

    OAuthCredential(String accessToken, String refreshToken, long expiresAtMillis) {
        this.accessToken = accessToken == null ? "" : accessToken;
        this.refreshToken = refreshToken == null ? "" : refreshToken;
        this.expiresAtMillis = Math.max(0L, expiresAtMillis);
    }

    boolean hasAccessToken() {
        return !accessToken.isEmpty();
    }

    boolean hasRefreshToken() {
        return !refreshToken.isEmpty();
    }

    boolean isUsableNow() {
        return hasAccessToken() && (expiresAtMillis <= 0L
                || expiresAtMillis > System.currentTimeMillis() + 60_000L);
    }
}
