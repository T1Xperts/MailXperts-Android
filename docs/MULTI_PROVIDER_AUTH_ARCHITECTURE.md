# MailXperts Multi-Provider Authentication Architecture

Status: Architecture baseline for MX-QA-030
Date: 3 Oct 2026

## Objective

Replace provider-specific password assumptions with a provider-neutral authentication layer suitable for a modern multi-provider email client. Gmail and Microsoft accounts use OAuth 2.0 as the primary sign-in path. Providers that still support or require app-specific passwords remain supported through a controlled credential fallback. Custom IMAP/SMTP remains available for hosted and business mail systems.

## Architectural principles

1. Authentication is a provider capability, not a single password field.
2. OAuth 2.0 / OpenID Connect is preferred whenever the provider supports it.
3. IMAP/SMTP transport is separate from authentication. OAuth tokens may authenticate standard IMAP/SMTP through SASL XOAUTH2.
4. Provider APIs may be used where they materially improve capabilities, but the core MailXperts mailbox model must not depend on a single provider API.
5. Passwords, app-specific passwords, access tokens and refresh tokens are secrets. They must never be logged or embedded in source control.
6. Long-lived secrets are protected using Android Keystore-backed encryption and separated from ordinary account metadata.
7. Access tokens are short lived, refreshed transparently, and never treated as permanent credentials.
8. Revocation, expiry, re-consent and provider policy changes are first-class states with actionable user recovery.
9. Least-privilege scopes are requested. New scopes require explicit design and privacy review.
10. Provider-specific code sits behind adapters so Gmail, Microsoft, Yahoo, iCloud and custom IMAP do not leak authentication logic into mailbox UI or message-processing code.

## Target components

### AuthProvider
Provider-neutral interface responsible for:
- authentication method discovery
- interactive sign-in
- silent token refresh
- credential validity state
- sign-out / revoke local credentials
- user-facing recovery action

Suggested providers:
- GoogleOAuthProvider
- MicrosoftOAuthProvider
- AppPasswordProvider
- PasswordProvider

### AuthCredential
Account authentication metadata must represent the credential type rather than assuming `password`.

Suggested fields:
- authType: OAUTH2, APP_PASSWORD, PASSWORD
- providerId
- accountEmail / principal
- accessToken (secret store only, optional and short lived)
- refreshToken (secret store only)
- expiresAt
- grantedScopes
- providerAccountId where available
- lastAuthErrorCode / reauthRequired state without secret material

### TokenVault
A dedicated Android Keystore-backed secret store for OAuth refresh tokens, access tokens and password/app-password fallbacks. Ordinary account metadata may remain in the existing account store, but secret lifecycle must be separated from UI persistence.

### MailTransportAuth
Transport-level authentication adapter that obtains a current credential and applies it to the selected protocol.

For OAuth-capable IMAP/SMTP providers:
- obtain/refresh access token
- authenticate via SASL XOAUTH2
- do not substitute access tokens into ordinary password logic

### ProviderCapabilities
Replace the current single `oauthRequired` flag with capability metadata, for example:
- preferredAuthMethod
- supportedAuthMethods
- IMAP support
- SMTP support
- provider API support
- contacts/calendar capability
- special folder behaviour
- server presets

## Provider strategy

| Provider | Primary authentication | Mail transport | Fallback |
|---|---|---|---|
| Google Gmail / Workspace | OAuth 2.0 / Sign in with Google | IMAP + SMTP using XOAUTH2 initially | App Password only where Google account policy permits it |
| Microsoft Outlook.com / Microsoft 365 | Microsoft identity OAuth 2.0 | OAuth-enabled IMAP/SMTP initially; Microsoft Graph may be introduced for richer provider-specific capabilities | No normal-password Basic Auth dependency |
| Yahoo | OAuth where implemented/supported | IMAP/SMTP | Provider-generated app password where supported |
| Apple iCloud Mail | Apple-supported account/app-specific-password route | IMAP/SMTP | App-specific password |
| T1Xperts / hosted IMAP | Mailbox credential | IMAP/SMTP over TLS | Provider/admin-defined |
| Other custom IMAP | Capability-driven credential | IMAP/SMTP over TLS | Provider-defined |

## Google implementation baseline

- Register a T1Xperts Android OAuth client and required Google OAuth consent configuration.
- Use Authorization Code with PKCE / platform-appropriate Google identity libraries.
- Request only required scopes.
- For full Gmail IMAP/SMTP access, use the Gmail mail scope required by XOAUTH2 integration.
- Exchange the OAuth access token with Gmail IMAP/SMTP through SASL XOAUTH2.
- Implement refresh-token renewal and re-consent handling.
- Keep the existing App Password path only as an optional fallback until explicitly retired.
- Do not ask for the normal Google account password.

## Microsoft implementation baseline

- Register MailXperts in Microsoft Entra ID.
- Use Microsoft Authentication Library / OAuth authorization-code flow suitable for Android.
- Support Outlook.com and Microsoft 365 delegated user authentication.
- For protocol transport, request delegated IMAP/SMTP permissions and use SASL XOAUTH2.
- Request `offline_access` when refresh tokens are required.
- Evaluate Microsoft Graph as the preferred provider API for richer Microsoft-specific mail, contact and calendar functionality without coupling the generic mail core to Graph.
- Do not build new Basic Auth dependencies.

## iCloud / app-password providers

- Keep app-specific password support behind `AppPasswordProvider`.
- Treat iCloud as IMAP/SMTP transport with provider-specific server and username rules.
- Store app-specific passwords in the secret vault, never logs or analytics.
- Make the UI explicitly distinguish an app-specific password from the user's Apple/Google/Yahoo account password.

## Account setup UX

### Add Account
1. Choose provider.
2. If OAuth is preferred, display provider-native action such as `Continue with Google` or `Continue with Microsoft`.
3. Complete provider consent in the system browser/provider SDK.
4. Return to MailXperts and validate mailbox connectivity.
5. Persist non-secret account metadata and protected tokens separately.
6. Open the mailbox.

For custom/app-password providers, show only the credential fields actually needed by that provider.

The user should not need to understand IMAP/SMTP hosts for Google or Microsoft under normal setup.

## Error model

Use structured authentication errors instead of raw provider/server text:
- NETWORK_UNAVAILABLE
- TLS_FAILURE
- AUTH_REJECTED
- TOKEN_EXPIRED
- TOKEN_REFRESH_FAILED
- CONSENT_REQUIRED
- PROVIDER_POLICY_BLOCK
- SMTP_DISABLED_BY_TENANT
- IMAP_DISABLED_BY_TENANT
- SERVER_UNAVAILABLE
- CONFIGURATION_ERROR

UI maps these to safe, actionable messages. Raw diagnostics may be kept in sanitised development logs without credentials/tokens.

## Migration

Existing accounts must continue to work during the transition.

- Existing password/app-password accounts retain their current credential type.
- Gmail users may migrate from APP_PASSWORD to OAUTH2 without deleting local mail/settings.
- Microsoft accounts move to OAUTH2.
- Account IDs and local cache ownership remain stable through auth migration.
- Signing identity/package identity are unaffected.

## Security requirements

- No client secrets suitable for confidential-server use may be embedded in the APK.
- Use PKCE for public mobile-client authorization flows.
- Validate redirect URI/app-link handling.
- Use state/nonce where required by the provider flow.
- Tokens and passwords are encrypted at rest using Android Keystore-backed keys.
- Never print credentials, bearer tokens, refresh tokens, authorization codes or app passwords.
- Redact provider error payloads before analytics/telemetry.
- Sign-out removes local tokens; provider revocation is offered where supported.
- Threat-model account takeover, token theft, malicious redirects, intent interception and backup/restore behaviour.

## Delivery phases

### Phase A - Provider-neutral auth core
- Refactor AccountConfig away from mandatory password semantics.
- Add auth type/capabilities/provider adapter interfaces.
- Add TokenVault and migration from existing SecureStore credentials.
- Add structured auth result/error model.

### Phase B - Google OAuth2
- Google app registration / consent configuration.
- Continue with Google UI.
- OAuth authorization and token refresh.
- Gmail IMAP + SMTP XOAUTH2.
- Device receive/send/background-sync QA.
- App Password retained as optional fallback.

### Phase C - Microsoft OAuth2
- Entra app registration.
- Continue with Microsoft UI.
- OAuth authorization / refresh.
- Outlook.com + Microsoft 365 IMAP/SMTP XOAUTH2.
- Tenant-policy aware errors.
- Device receive/send/background-sync QA.

### Phase D - Remaining providers
- Formalise Yahoo and iCloud app-password providers.
- Preserve T1Xperts/custom IMAP password support.
- Provider capability matrix and automated regression tests.

### Phase E - Optional provider APIs
- Gmail API or Microsoft Graph only for capabilities that justify provider-specific APIs.
- Contacts/calendar integration must use separate scopes and explicit consent.
- Core mail operations must remain behind provider-neutral interfaces.

## Definition of Done for MX-QA-030

- Gmail account can be added through OAuth without the user's Google password.
- Gmail receive, send and background sync work on a physical Android device.
- Microsoft Outlook.com account can be added through OAuth.
- Microsoft 365 delegated account can be added subject to tenant policy.
- Microsoft receive, send and background sync work on a physical Android device.
- OAuth refresh survives access-token expiry without manual re-login under normal conditions.
- Revoked/expired consent produces an actionable re-auth flow.
- No password/token is logged or committed.
- Existing T1Xperts/custom IMAP accounts continue to work.
- iCloud/app-password flows remain functional.
- Account upgrade/migration preserves local settings, cache and account identity.
- Unit, integration, regression, device QA and Product Owner UAT pass.

## Historical note

PR #17 added useful Gmail App Password validation, whitespace normalisation and safer diagnostics. Device testing on 3 Oct 2026 showed that the App Password route still did not successfully connect the tested Gmail account. That work is retained as fallback hardening, but it is no longer considered the target architecture for Gmail authentication.
