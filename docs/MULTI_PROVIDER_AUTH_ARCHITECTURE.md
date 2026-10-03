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
Provider-neutral interface responsible for authentication method discovery, interactive sign-in, silent refresh, credential state, sign-out and recovery.

Suggested providers: GoogleOAuthProvider, MicrosoftOAuthProvider, AppPasswordProvider and PasswordProvider.

### AuthCredential
Account authentication metadata represents the credential type rather than assuming `password`.

Suggested fields: authType, providerId, principal, token expiry, granted scopes, provider account ID and re-authentication state. Secret token/password values live only in the protected secret store.

### TokenVault
A dedicated Android Keystore-backed secret store for OAuth refresh tokens, access tokens and password/app-password fallbacks. Ordinary account metadata remains separate.

### MailTransportAuth
Transport-level adapter obtains a current credential and applies it to the selected protocol. OAuth-capable IMAP/SMTP providers authenticate using SASL XOAUTH2, not by substituting access tokens into normal password logic.

### ProviderCapabilities
Replace the current single `oauthRequired` flag with capability metadata such as preferred/supported auth methods, IMAP/SMTP support, provider API support, contacts/calendar capability, special folder behaviour and server presets.

## Provider strategy

| Provider | Primary authentication | Mail transport | Fallback |
|---|---|---|---|
| Google Gmail / Workspace | OAuth 2.0 / Sign in with Google | IMAP + SMTP using XOAUTH2 initially | App Password only where account policy permits it |
| Microsoft Outlook.com / Microsoft 365 | Microsoft identity OAuth 2.0 | OAuth-enabled IMAP/SMTP initially; Microsoft Graph may be introduced for richer provider capabilities | No normal-password Basic Auth dependency |
| Yahoo | OAuth where implemented/supported | IMAP/SMTP | Provider-generated app password where supported |
| Apple iCloud Mail | Apple-supported account/app-specific-password route | IMAP/SMTP | App-specific password |
| T1Xperts / hosted IMAP | Mailbox credential | IMAP/SMTP over TLS | Provider/admin-defined |
| Other custom IMAP | Capability-driven credential | IMAP/SMTP over TLS | Provider-defined |

## Google implementation baseline

- Register T1Xperts Android OAuth client and Google OAuth consent configuration.
- Use Authorization Code with PKCE / platform-appropriate Google identity libraries.
- Request only required scopes.
- Authenticate Gmail IMAP/SMTP with OAuth access tokens through SASL XOAUTH2.
- Implement refresh-token renewal and re-consent handling.
- Keep App Password as optional fallback during transition.
- Never request the normal Google account password.

## Microsoft implementation baseline

- Register MailXperts in Microsoft Entra ID.
- Use Microsoft Authentication Library / OAuth authorization-code flow suitable for Android.
- Support Outlook.com and Microsoft 365 delegated user authentication.
- Use delegated IMAP/SMTP OAuth permissions and SASL XOAUTH2 for protocol transport.
- Request offline access when refresh tokens are required.
- Evaluate Microsoft Graph for richer Microsoft-specific mail/contact/calendar capabilities while retaining a provider-neutral core.
- Do not create new Basic Auth dependencies.

## iCloud / app-password providers

- Keep app-specific password support behind AppPasswordProvider.
- Treat iCloud as IMAP/SMTP with provider-specific server and username rules.
- Store app-specific passwords in the secret vault, never logs or analytics.
- Explicitly distinguish app-specific passwords from the user's main account password.

## Account setup UX

1. Choose provider.
2. OAuth providers show `Continue with Google` or `Continue with Microsoft`.
3. Complete provider consent in the system browser/provider SDK.
4. Return to MailXperts and validate mailbox connectivity.
5. Persist non-secret metadata and protected tokens separately.
6. Open mailbox.

Custom/app-password providers show only fields required by that provider. Normal Google/Microsoft setup must not expose IMAP/SMTP host fields unless troubleshooting/advanced settings are opened.

## Error model

Use structured authentication errors rather than raw provider text: NETWORK_UNAVAILABLE, TLS_FAILURE, AUTH_REJECTED, TOKEN_EXPIRED, TOKEN_REFRESH_FAILED, CONSENT_REQUIRED, PROVIDER_POLICY_BLOCK, SMTP_DISABLED_BY_TENANT, IMAP_DISABLED_BY_TENANT, SERVER_UNAVAILABLE and CONFIGURATION_ERROR.

UI maps these to safe, actionable messages. Sanitised diagnostics may be logged without credentials/tokens.

## Migration

- Existing password/app-password accounts retain their credential type.
- Gmail accounts may migrate from APP_PASSWORD to OAUTH2 without deleting local mail/settings.
- Microsoft accounts move to OAUTH2.
- Account IDs and local cache ownership remain stable through migration.
- Package/signing identity remain unchanged.

## Security requirements

- No confidential-client secret embedded in APK.
- PKCE for public mobile-client flows.
- Validate redirect URI/app-link handling and provider state/nonce requirements.
- Tokens/passwords encrypted at rest with Android Keystore-backed keys.
- Never log passwords, app passwords, bearer tokens, refresh tokens or authorization codes.
- Redact provider error payloads before telemetry.
- Sign-out clears local credentials; provider revocation offered where supported.
- Threat-model token theft, malicious redirects, intent interception and backup/restore.

## Delivery phases

### Phase A - Provider-neutral auth core
Refactor AccountConfig away from mandatory password semantics; add auth type/capabilities/provider adapters; add TokenVault; add structured auth result/error model.

### Phase B - Google OAuth2
Google registration/consent, Continue with Google, OAuth/token refresh, Gmail IMAP/SMTP XOAUTH2, receive/send/background-sync QA, App Password fallback retained.

### Phase C - Microsoft OAuth2
Entra registration, Continue with Microsoft, OAuth/token refresh, Outlook.com and Microsoft 365 IMAP/SMTP XOAUTH2, tenant-policy errors, receive/send/background-sync QA.

### Phase D - Remaining providers
Formalise Yahoo and iCloud app-password adapters; preserve T1Xperts/custom IMAP password support; complete capability matrix and regression tests.

### Phase E - Optional provider APIs
Use Gmail API or Microsoft Graph only where provider-specific capabilities justify them. Contacts/calendar use separate scopes and explicit consent. Core mail operations remain behind provider-neutral interfaces.

## Definition of Done for MX-QA-030

- Gmail can be added through OAuth without the user's Google password.
- Gmail receive, send and background sync pass on a physical Android device.
- Outlook.com can be added through Microsoft OAuth.
- Microsoft 365 delegated accounts can be added subject to tenant policy.
- Microsoft receive, send and background sync pass on device.
- Token refresh works across access-token expiry.
- Revoked/expired consent triggers actionable re-authentication.
- No password/token is logged or committed.
- Existing T1Xperts/custom IMAP continues working.
- iCloud/app-password flows remain functional.
- Migration preserves local settings, cache and account identity.
- Unit, integration, regression, device QA and Product Owner UAT pass.

## Historical note

PR #17 added Gmail App Password validation, whitespace normalisation and safer diagnostics. Device testing on 3 Oct 2026 showed the tested Gmail account still could not connect through the App Password route. That hardening remains useful fallback behaviour, but App Password is no longer the target architecture for Gmail.
