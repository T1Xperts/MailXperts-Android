# MailXperts Multi-Provider Authentication Architecture

Status: **Implementation baseline merged in v1.6.0-beta.3; provider activation and live device UAT pending**
Date: 3 Oct 2026
Release notes: `RELEASE-NOTES-v1.6.0-beta.3.md`

## Objective
Replace provider-specific password assumptions with a provider-neutral authentication layer suitable for a modern multi-provider email client. Gmail and Microsoft accounts use OAuth 2.0 as the primary sign-in path. Providers that still support or require app-specific passwords remain supported through a controlled credential fallback. Custom IMAP/SMTP remains available for hosted and business mail systems.

## Architectural principles
1. Authentication is a provider capability, not a single password field.
2. OAuth 2.0 is preferred whenever the provider supports it.
3. IMAP/SMTP transport is separate from authentication; OAuth-backed protocol transport uses SASL XOAUTH2.
4. Provider APIs may be added for richer features without coupling the core mailbox model to one provider.
5. Passwords, app passwords and OAuth tokens are secrets and must never be logged or committed.
6. Long-lived secrets are protected using Android Keystore-backed encryption and separated from ordinary account metadata.
7. Expiry, revocation, re-consent and provider policy changes are first-class recovery states.
8. Provider-specific code sits behind provider-aware auth/transport components.

## Implemented in v1.6.0-beta.3
- `AuthType`: `OAUTH2`, `APP_PASSWORD`, `PASSWORD`.
- OAuth-capable `AccountConfig` without mandatory password semantics.
- Provider authentication capability metadata in `ProviderPreset`.
- Android Keystore-backed AES-GCM `CredentialVault` for OAuth material.
- `MailAuth` XOAUTH2 bridge for IMAP and SMTP.
- `GoogleOAuthManager` using Google Identity authorization for the Gmail mail scope.
- `MicrosoftOAuthManager` using Authorization Code + PKCE with delegated IMAP/SMTP scopes and refresh-token renewal.
- `OAuthConnectActivity` with `Continue with Google` and `Continue with Microsoft` flows and post-auth mailbox validation.
- Settings UI prefers OAuth for Gmail and requires OAuth for Microsoft while retaining Gmail App Password fallback.
- T1Xperts/custom IMAP and Yahoo/iCloud app-password paths preserved.
- Existing account IDs, settings, signatures and local cache ownership preserved during auth migration.

## Provider strategy
| Provider | Primary authentication | Transport | Fallback |
|---|---|---|---|
| Google Gmail / Workspace | OAuth2 / Continue with Google | IMAP + SMTP XOAUTH2 | App Password where policy permits |
| Outlook.com / Microsoft 365 | Microsoft identity OAuth2 | IMAP + SMTP XOAUTH2 | No normal-password Basic Auth dependency |
| Yahoo | Provider app password / future OAuth adapter | IMAP/SMTP | App password |
| Apple iCloud Mail | App-specific password | IMAP/SMTP | App-specific password |
| T1Xperts / hosted IMAP | Mailbox credential | IMAP/SMTP TLS | Provider/admin-defined |
| Other custom IMAP | Capability-driven | IMAP/SMTP TLS | Provider-defined |

## External activation gates
### Google
- Register the MailXperts Android OAuth application for package `au.com.t1xperts.mailxperts` and the production signing identity.
- Configure consent and Gmail mail scope `https://mail.google.com/` as required by Google.
- Run physical-device sign-in, receive, send, background sync and re-consent QA.

### Microsoft
- Register a Microsoft Entra public-client application.
- Supply build-time `MX_MICROSOFT_CLIENT_ID` and matching `MX_MICROSOFT_REDIRECT_URI`.
- Configure delegated IMAP/SMTP permissions and consent subject to tenant policy.
- Run physical-device Outlook.com/Microsoft 365 sign-in, receive, send, refresh and background-sync QA.

No confidential OAuth client secret is embedded in the Android application.

## QA evidence
- PR #21 merged as `16153dc02ee14a62e9a613bf412a5f47267bbc45`.
- PR Build MailXperts Android #53 / run `37122256291`: PASS.
- PR P0 QA and APK Build #33 / run `37122256332`: PASS.
- Main Build MailXperts Android #54 / run `37122426566`: `validate-debug` PASS.
- Main P0 QA and APK Build #34 / run `37122426634`: PASS, including APK identity verification and artifact upload.
- Protected signed-release failed closed because repository secret `MX_KEYSTORE_BASE64` is absent. No incorrectly signed production artifact was published.

## Remaining Definition of Done gates
- [x] Provider-neutral authentication model implemented.
- [x] Keystore-backed OAuth credential vault implemented.
- [x] Gmail OAuth + XOAUTH2 code path implemented.
- [x] Gmail App Password fallback retained.
- [x] Microsoft OAuth2/PKCE + XOAUTH2 code path implemented.
- [x] Microsoft refresh-token logic implemented.
- [x] Existing standards-based provider paths preserved in code/regression.
- [x] Pull-request and main automated QA passed.
- [ ] Google provider registration/consent activation.
- [ ] Gmail OAuth physical-device receive/send/background-sync QA.
- [ ] Microsoft Entra public-client activation in the build.
- [ ] Outlook.com/Microsoft 365 physical-device receive/send/background-sync QA.
- [ ] Token refresh/re-consent live-provider QA.
- [ ] Cross-provider physical-device regression.
- [ ] Product Owner UAT for MX-QA-030.

## Historical note
PR #17 added Gmail App Password validation, whitespace normalisation and safer diagnostics. Device testing on 3 Oct 2026 showed the tested Gmail account still could not connect through that fallback route, so App Password is no longer the target Gmail architecture.
