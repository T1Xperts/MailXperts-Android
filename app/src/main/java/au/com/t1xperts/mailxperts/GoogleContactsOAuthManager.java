package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Explicit Google People API authorization. Tokens are short-lived and never persisted here. */
final class GoogleContactsOAuthManager {
    interface Callback {
        void onAuthorized(String bearer);
        void onError(String message);
    }

    private GoogleContactsOAuthManager() {}

    static void begin(Activity activity, int requestCode, boolean write, Callback callback) {
        Identity.getAuthorizationClient(activity).authorize(request(write))
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        PendingIntent pending = result.getPendingIntent();
                        if (pending == null) {
                            callback.onError("Google Contacts consent is unavailable.");
                            return;
                        }
                        try {
                            activity.startIntentSenderForResult(pending.getIntentSender(), requestCode,
                                    null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException error) {
                            callback.onError("Could not open Google Contacts consent.");
                        }
                    } else {
                        deliver(result, write, callback);
                    }
                })
                .addOnFailureListener(error -> callback.onError("Google Contacts authorization failed."));
    }

    static void finish(Activity activity, Intent data, boolean write, Callback callback) {
        try {
            deliver(Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(data), write, callback);
        } catch (Exception error) {
            callback.onError("Google Contacts authorization did not complete.");
        }
    }

    static String acquireSilently(Context context, boolean write) throws Exception {
        AuthorizationResult result = Tasks.await(
                Identity.getAuthorizationClient(context).authorize(request(write)),
                30, TimeUnit.SECONDS);
        if (result.hasResolution() || !hasRequiredScope(result, write)) {
            throw new IllegalStateException("CONSENT_REQUIRED");
        }
        String bearer = result.getAccessToken();
        if (bearer == null || bearer.trim().isEmpty()) throw new IllegalStateException("AUTH_REJECTED");
        return bearer;
    }

    private static AuthorizationRequest request(boolean write) {
        String scope = write ? GoogleContactsScopes.WRITE : GoogleContactsScopes.READ;
        return AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(scope)))
                .setOptOutIncludingGrantedScopes(true)
                .build();
    }

    private static void deliver(AuthorizationResult result, boolean write, Callback callback) {
        if (!hasRequiredScope(result, write)) {
            callback.onError("Google Contacts permission was not granted.");
            return;
        }
        String bearer = result == null ? null : result.getAccessToken();
        if (bearer == null || bearer.trim().isEmpty()) callback.onError("Google Contacts authorization returned no credential.");
        else callback.onAuthorized(bearer);
    }

    static boolean containsRequiredScope(List<String> scopes, boolean write) {
        if (scopes == null) return false;
        for (String scope : scopes) {
            if (write && GoogleContactsScopes.WRITE.equals(scope)) return true;
            if (!write && (GoogleContactsScopes.READ.equals(scope) || GoogleContactsScopes.WRITE.equals(scope))) return true;
        }
        return false;
    }

    private static boolean hasRequiredScope(AuthorizationResult result, boolean write) {
        return result != null && containsRequiredScope(result.getGrantedScopes(), write);
    }
}
