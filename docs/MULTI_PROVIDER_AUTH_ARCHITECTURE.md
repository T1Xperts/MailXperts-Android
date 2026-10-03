# MailXperts Multi-Provider Authentication Architecture

Status: **Implementation merged in v1.6.0-beta.3; provider activation and live device UAT pending**
Date: 3 Oct 2026
Release notes: `RELEASE-NOTES-v1.6.0-beta.3.md`

## Objective
Replace provider-specific password assumptions with a provider-neutral authentication layer suitable for a modern multi-provider email client. Gmail and Microsoft accounts use OAuth 2.0 as the primary sign-in path. Providers that still support or require app-specific passwords remain supported through a controlled credential fallback. Custom IMAP/SMTP remains available for hosted and business mail systems.

## Implemented baseline
- `AuthType`: OAuth2, app password and mailbox password modes.
- OAuth-aware `AccountConfig` without mandatory password semantics.
- Provider capability metadata in `ProviderPreset`.
- Android Keystore-backed AES-GCM `CredentialVault` for OAuth material.
- `MailAuth` XOAUTH2 bridge for IMAP and SMTP.
- Google Identity Gmail authorization using the Gmail mail scope.
- Microsoft Authorization Code + PKCE, delegated IMAP/SMTP scopes and refresh-token renewal.
- `Continue with Google` and `Continue with Microsoft` account UX with post-auth mailbox validation.
- Gmail App Password fallback retained where provider policy permits.
- Existing T1Xperts/custom IMAP and Yahoo/iCloud app-password paths retained.
- Existing account IDs, settings, signatures and local cache ownership preserved during authentication migration.

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
- Register package `au.com.t1xperts.mailxperts` with the production signing identity in Google Cloud/Android OAuth configuration.
- Configure consent and Gmail mail scope `https://mail.google.com/` as required.
- Perform physical-device sign-in, receive, send, background sync and re-consent QA.

### Microsoft
- Register a Microsoft Entra public-client application.
- Supply `MX_MICROSOFT_CLIENT_ID` and matching `MX_MICROSOFT_REDIRECT_URI` at build time.
- Configure delegated IMAP/SMTP permissions and consent subject to tenant policy.
- Perform physical-device Outlook.com/Microsoft 365 sign-in, receive, send, token refresh and background-sync QA.

No confidential OAuth client secret is embedded in the Android application.

## QA evidence
- PR #21 merged as `16153dc02ee14a62e9a613bf412a5f47267bbc45`.
- PR Build MailXperts Android #53 / run `37122256291`: PASS.
- PR P0 QA and APK Build #33 / run `37122256332`: PASS.
- Main Build MailXperts Android #54 / run `37122426566`: `validate-debug` PASS.
- Main P0 QA and APK Build #34 / run `37122426634`: PASS, including APK identity verification and artifact upload.
- Protected signed-release failed closed because repository secret `MX_KEYSTORE_BASE64` is absent. No incorrectly signed production artifact was published.

## Remaining Definition of Done gates
- [x] Provider-neutral auth model.
- [x] Keystore-backed OAuth vault.
- [x] Gmail OAuth/XOAUTH2 code path.
- [x] Gmail App Password fallback.
- [x] Microsoft OAuth2/PKCE/XOAUTH2 code path.
- [x] Microsoft refresh-token logic.
- [x] Existing standards-based provider paths preserved in code/regression.
- [x] Pull-request and main automated QA passed.
- [ ] Google provider registration/consent activation.
- [ ] Gmail OAuth physical-device receive/send/background-sync QA.
- [ ] Microsoft Entra public-client activation in the build.
- [ ] Outlook.com/Microsoft 365 physical-device receive/send/background-sync QA.
- [ ] Token refresh/re-consent live-provider QA.
- [ ] Cross-provider physical-device regression.
- [ ] Product Owner UAT.

## Historical note
PR #17 added Gmail App Password validation, whitespace normalisation and safer diagnostics. Device testing on 3 Oct 2026 showed the tested Gmail account still could not connect through that fallback route, so App Password is no longer the target Gmail architecture.
