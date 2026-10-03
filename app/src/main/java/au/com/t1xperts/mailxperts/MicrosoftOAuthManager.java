package au.com.t1xperts.mailxperts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.GrantTypeValues;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Microsoft identity OAuth2 authorization for Outlook.com and Microsoft 365 mail. */
final class MicrosoftOAuthManager {
    private static final String AUTH_ENDPOINT =
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private static final String TOKEN_ENDPOINT =
            "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String IMAP_SCOPE =
            "https://outlook.office.com/IMAP.AccessAsUser.All";
    private static final String SMTP_SCOPE =
            "https://outlook.office.com/SMTP.Send";
    private static final String OFFLINE_SCOPE = "offline_access";

    interface Callback {
        void onAuthorized(OAuthCredential credential);
        void onError(String message);
    }

    private MicrosoftOAuthManager() {}

    static boolean isConfigured() {
        return BuildConfig.MX_MICROSOFT_CLIENT_ID != null
                && !BuildConfig.MX_MICROSOFT_CLIENT_ID.trim().isEmpty()
                && BuildConfig.MX_MICROSOFT_REDIRECT_URI != null
                && !BuildConfig.MX_MICROSOFT_REDIRECT_URI.trim().isEmpty();
    }

    static void begin(Activity activity, int requestCode, Callback callback) {
        if (!isConfigured()) {
            callback.onError("Microsoft OAuth is not configured for this build. Register the MailXperts Android public client and supply MX_MICROSOFT_CLIENT_ID / MX_MICROSOFT_REDIRECT_URI.");
            return;
        }
        AuthorizationService service = new AuthorizationService(activity);
        AuthorizationRequest request = new AuthorizationRequest.Builder(
                configuration(),
                BuildConfig.MX_MICROSOFT_CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(BuildConfig.MX_MICROSOFT_REDIRECT_URI))
                .setScopes(Arrays.asList(OFFLINE_SCOPE, IMAP_SCOPE, SMTP_SCOPE))
                .build();
        activity.startActivityForResult(service.getAuthorizationRequestIntent(request), requestCode);
    }

    static void finish(Context context, Intent data, Callback callback) {
        AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
        AuthorizationException authorizationError = AuthorizationException.fromIntent(data);
        if (response == null) {
            callback.onError("Microsoft authorization failed: " + safe(authorizationError));
            return;
        }
        AuthorizationService service = new AuthorizationService(context);
        service.performTokenRequest(response.createTokenExchangeRequest(), (tokenResponse, tokenError) -> {
            try {
                if (tokenResponse == null || tokenResponse.accessToken == null
                        || tokenResponse.accessToken.trim().isEmpty()) {
                    callback.onError("Microsoft token exchange failed: " + safe(tokenError));
                    return;
                }
                callback.onAuthorized(toCredential(tokenResponse, ""));
            } finally {
                service.dispose();
            }
        });
    }

    static OAuthCredential refreshBlocking(Context context, OAuthCredential current) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("CONFIGURATION_ERROR: Microsoft OAuth client registration is missing.");
        }
        if (current == null || !current.hasRefreshToken()) {
            throw new IllegalStateException("CONSENT_REQUIRED: Reconnect this Microsoft account.");
        }

        TokenRequest request = new TokenRequest.Builder(
                configuration(), BuildConfig.MX_MICROSOFT_CLIENT_ID)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(current.refreshToken)
                .setScopes(Arrays.asList(OFFLINE_SCOPE, IMAP_SCOPE, SMTP_SCOPE))
                .build();

        AuthorizationService service = new AuthorizationService(context);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<OAuthCredential> result = new AtomicReference<>();
        AtomicReference<AuthorizationException> error = new AtomicReference<>();
        service.performTokenRequest(request, (response, exception) -> {
            if (response != null && response.accessToken != null) {
                result.set(toCredential(response, current.refreshToken));
            } else {
                error.set(exception);
            }
            latch.countDown();
        });

        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("TOKEN_REFRESH_FAILED: Microsoft token refresh timed out.");
            }
            OAuthCredential refreshed = result.get();
            if (refreshed == null) {
                throw new IllegalStateException("TOKEN_REFRESH_FAILED: " + safe(error.get()));
            }
            return refreshed;
        } finally {
            service.dispose();
        }
    }

    private static AuthorizationServiceConfiguration configuration() {
        return new AuthorizationServiceConfiguration(
                Uri.parse(AUTH_ENDPOINT), Uri.parse(TOKEN_ENDPOINT));
    }

    private static OAuthCredential toCredential(TokenResponse response, String existingRefreshToken) {
        String refresh = response.refreshToken == null || response.refreshToken.isEmpty()
                ? existingRefreshToken : response.refreshToken;
        long expiry = response.accessTokenExpirationTime == null
                ? System.currentTimeMillis() + 50L * 60L * 1000L
                : response.accessTokenExpirationTime;
        return new OAuthCredential(response.accessToken, refresh, expiry);
    }

    private static String safe(Throwable error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        return message == null || message.trim().isEmpty()
                ? (error == null ? "Unknown error" : error.getClass().getSimpleName())
                : message;
    }
}
