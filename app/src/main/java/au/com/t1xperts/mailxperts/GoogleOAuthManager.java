package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/** Google Identity Services authorization for Gmail IMAP/SMTP XOAUTH2. */
final class GoogleOAuthManager {
    static final String MAIL_SCOPE = "https://mail.google.com/";
    private static final long ACCESS_TOKEN_CACHE_MS = 50L * 60L * 1000L;

    interface Callback {
        void onAuthorized(String email, String accessToken, long expiresAtMillis);
        void onError(String message);
    }

    private GoogleOAuthManager() {}

    static void begin(Activity activity, int requestCode, Callback callback) {
        AuthorizationClient client = Identity.getAuthorizationClient(activity);
        client.authorize(request())
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        PendingIntent pending = result.getPendingIntent();
                        if (pending == null) {
                            callback.onError("Google authorization requires user action but no consent screen is available.");
                            return;
                        }
                        try {
                            activity.startIntentSenderForResult(
                                    pending.getIntentSender(), requestCode, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException error) {
                            callback.onError("Could not open Google authorization: " + safe(error));
                        }
                        return;
                    }
                    deliver(result, callback);
                })
                .addOnFailureListener(error ->
                        callback.onError("Google authorization failed: " + safe(error)));
    }

    static void finish(Activity activity, Intent data, Callback callback) {
        try {
            AuthorizationResult result = Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(data);
            deliver(result, callback);
        } catch (Exception error) {
            callback.onError("Google authorization did not complete: " + safe(error));
        }
    }

    /**
     * Retrieves a fresh token without presenting UI. If consent is required, callers receive a
     * recoverable error and must route the user back through begin(...).
     */
    static OAuthCredential acquireSilently(Context context) throws Exception {
        AuthorizationResult result = Tasks.await(
                Identity.getAuthorizationClient(context).authorize(request()),
                30, TimeUnit.SECONDS);
        if (result.hasResolution()) {
            throw new IllegalStateException("CONSENT_REQUIRED: Reconnect this Gmail account with Google.");
        }
        String token = result.getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("AUTH_REJECTED: Google did not return an OAuth access token.");
        }
        return new OAuthCredential(token, "", System.currentTimeMillis() + ACCESS_TOKEN_CACHE_MS);
    }

    private static AuthorizationRequest request() {
        return AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(MAIL_SCOPE)))
                .build();
    }

    private static void deliver(AuthorizationResult result, Callback callback) {
        String token = result == null ? null : result.getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            callback.onError("Google did not return an OAuth access token.");
            return;
        }
        String email = "";
        try {
            if (result.toGoogleSignInAccount() != null
                    && result.toGoogleSignInAccount().getEmail() != null) {
                email = result.toGoogleSignInAccount().getEmail();
            }
        } catch (Exception ignored) {}
        callback.onAuthorized(email, token, System.currentTimeMillis() + ACCESS_TOKEN_CACHE_MS);
    }

    private static String safe(Throwable error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        return message == null || message.trim().isEmpty()
                ? (error == null ? "Unknown error" : error.getClass().getSimpleName())
                : message;
    }
}
