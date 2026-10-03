# MailXperts Multi-Provider Authentication Architecture

Status: **Implementation baseline merged in v1.6.0-beta.3; provider activation and live device UAT pending**
Date: 3 Oct 2026

## Objective

Replace provider-specific password assumptions with a provider-neutral authentication layer suitable for a modern multi-provider email client. Gmail and Microsoft accounts use OAuth 2.0 as the primary sign-in path. Providers that still support or require app-specific passwords remain supported through a controlled credential fallback. Custom IMAP/SMTP remains available for hosted and business mail systems.

## Architectural principles

1. Authentication is a provider capability, not a single password field.
2. OAuth 2.0 / OpenID Connect is preferred whenever the provider supports it.
3. IMAP/SMTP transport is separate from authentication. OAuth tokens authenticate standards-based IMAP/SMTP through SASL XOAUTH2.
4. Provider APIs may be used where they materially improve capabilities, but the core MailXperts mailbox model must not depend on a single provider API.
5. Passwords, app-specific passwords, access tokens and refresh tokens are secrets. They must never be logged or embedded in source control.
6. Long-lived secrets are protected using Android Keystore-backed encryption and separated from ordinary account metadata.
7. Access tokens are short lived, refreshed/reacquired through provider-aware flows, and never treated as permanent credentials.
8. Revocation, expiry, re-consent and provider policy changes are first-class states with actionable user recovery.
9. Least-privilege scopes are requested. New scopes require explicit design and privacy review.
10. Provider-specific code sits behind adapters so Gmail, Microsoft, Yahoo, iCloud and custom IMAP do not leak authentication logic into mailbox UI or message-processing code.

## Implemented in v1.6.0-beta.3

- `AuthType` supports `OAUTH2`, `APP_PASSWORD` and `PASSWORD`.
- `AccountConfig` no longer requires a password for OAuth-backed accounts.
- `ProviderPreset` defines preferred/supported authentication capabilities per provider.
- `CredentialVault` separates OAuth material from account metadata and encrypts it with an Android Keystore-managed AES-GCM key.
- `MailAuth` resolves provider credentials and enables JavaMail SASL XOAUTH2 for IMAP and SMTP.
- `GoogleOAuthManager` implements Google Identity authorization for the Gmail mail scope and silent access-token reacquisition when consent is still valid.
- `MicrosoftOAuthManager` implements Authorization Code + PKCE, delegated IMAP/SMTP scopes, offline access and refresh-token renewal.
- `OAuthConnectActivity` provides `Continue with Google` and `Continue with Microsoft` account flows and validates IMAP/SMTP after authorization.
- `SettingsActivity` presents OAuth as the recommended Gmail route and the required Microsoft route while retaining Gmail App Password fallback.
- Existing T1Xperts/custom IMAP accounts and Yahoo/iCloud app-password paths are preserved.
- Existing account IDs, settings, signatures and local cache ownership remain stable during authentication migration.

## Provider strategy

| Provider | Primary authentication | Mail transport | Fallback |
|---|---|---|---|
| Google Gmail / Workspace | OAuth 2.0 / Continue with Google | IMAP + SMTP using XOAUTH2 | App Password only where account policy permits it |
| Microsoft Outlook.com / Microsoft 365 | Microsoft identity OAuth 2.0 | OAuth-enabled IMAP/SMTP using XOAUTH2 | No normal-password Basic Auth dependency |
| Yahoo | Provider-supported app password / future OAuth adapter | IMAP/SMTP | Provider app password |
| Apple iCloud Mail | Apple-supported app-specific-password route | IMAP/SMTP | App-specific password |
| T1Xperts / hosted IMAP | Mailbox credential | IMAP/SMTP over TLS | Provider/admin-defined |
| Other custom IMAP | Capability-driven credential | IMAP/SMTP over TLS | Provider-defined |

## Google activation gate

The application implementation is present, but live Google authorization requires provider-side configuration:

- Google Cloud / Android OAuth application registration for package `au.com.t1xperts.mailxperts` and the production signing identity.
- Gmail mail scope (`https://mail.google.com/`) enabled and consent/verification configured as required by Google.
- Physical-device validation of sign-in, IMAP receive, SMTP send, background sync and re-consent behaviour.

The normal Google password is never requested for OAuth mode.

## Microsoft activation gate

The application implementation is present, but live Microsoft authorization requires:

- Microsoft Entra public-client application registration.
- Build-time `MX_MICROSOFT_CLIENT_ID` and matching `MX_MICROSOFT_REDIRECT_URI`.
- Delegated IMAP/SMTP permissions and consent subject to tenant policy.
- Physical-device validation of Outlook.com/Microsoft 365 sign-in, receive, send, refresh and background sync.

No confidential client secret is embedded in the Android APK.

## Account setup UX

Google:
`Add Account → Google Gmail → Continue with Google → consent → mailbox validation → Inbox`

Microsoft:
`Add Account → Outlook / Microsoft 365 → Continue with Microsoft → consent → mailbox validation → Inbox`

Custom/app-password providers show only their relevant credential/server path. Advanced server values remain available for standards-based troubleshooting.

## Security requirements

- No confidential-client secret embedded in APK.
- PKCE for Microsoft public mobile-client authorization.
- Provider redirect/consent configuration must match the production application identity.
- Tokens/passwords encrypted at rest using Android Keystore-backed keys.
- Never log passwords, app passwords, bearer tokens, refresh tokens or authorization codes.
- OAuth secrets are not persisted in ordinary account JSON.
- Deleting/forgetting account credentials clears the OAuth vault entry.
- Provider registration, scopes and consent settings are release/security artefacts, not source-code secrets.

## QA evidence

### Pull request
- PR #21: `MX-QA-030: Multi-provider OAuth authentication foundation`
- Merged to `main` as `16153dc02ee14a62e9a613bf412a5f47267bbc45`

### PR validation
- Build MailXperts Android #53 / run `37122256291`: PASS
- P0 QA and APK Build #33 / run `37122256332`: PASS

### Main validation
- Build MailXperts Android #54 / run `37122426566`: `validate-debug` PASS
- P0 QA and APK Build #34 / run `37122426634`: PASS, including APK identity verification and evidence upload
- Protected `signed-release` job failed closed before build because GitHub secret `MX_KEYSTORE_BASE64` is still missing. No incorrectly signed production artifact was published.

Automated tests include provider capability rules, OAuth account usability without passwords, legacy fallback preservation, token-expiry handling, private OAuth activity/application manifest wiring, and v1.6.0-beta.3 release identity.

## Remaining Definition of Done gates

- [x] Provider-neutral authentication model implemented.
- [x] Android Keystore-backed OAuth credential vault implemented.
- [x] Gmail OAuth authorization code path implemented.
- [x] Gmail IMAP/SMTP XOAUTH2 transport implemented.
- [x] Gmail App Password fallback retained.
- [x] Microsoft OAuth2/PKCE flow implemented.
- [x] Microsoft IMAP/SMTP XOAUTH2 transport implemented.
- [x] Microsoft refresh-token logic implemented.
- [x] Existing standards-based provider paths preserved in code and automated regression.
- [x] Pull-request and main-branch automated QA passed.
- [ ] Google provider registration/consent activation completed.
- [ ] Gmail OAuth sign-in passes on physical Android device.
- [ ] Gmail receive/send/background sync pass on device.
- [ ] Microsoft Entra public-client registration activated in the build.
- [ ] Outlook.com/Microsoft 365 sign-in and mail transport pass on device.
- [ ] Token refresh/re-consent scenarios pass live-provider QA.
- [ ] Cross-provider physical-device regression passes.
- [ ] Product Owner UAT passes for MX-QA-030.

## Historical note

PR #17 added Gmail App Password validation, whitespace normalisation and safer diagnostics. Device testing on 3 Oct 2026 showed the tested Gmail account still could not connect through the App Password route. That hardening remains useful fallback behaviour, but App Password is no longer the target architecture for Gmail.
