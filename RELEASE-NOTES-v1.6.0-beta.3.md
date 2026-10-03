# MailXperts v1.6.0-beta.3

## Focus
MX-QA-030 provider authentication foundation for modern multi-provider email access.

## Implemented
- Provider-neutral account authentication type: OAuth2, app password, or mailbox password.
- Gmail now prefers **Continue with Google** and uses OAuth2 access tokens with JavaMail SASL XOAUTH2 for IMAP/SMTP.
- Gmail App Password support remains available as a compatibility fallback where Google policy permits it.
- Outlook.com / Microsoft 365 now uses a **Continue with Microsoft** OAuth2/PKCE flow and XOAUTH2 IMAP/SMTP transport.
- Microsoft OAuth registration values are supplied at build time using `MX_MICROSOFT_CLIENT_ID` and `MX_MICROSOFT_REDIRECT_URI`; no client secret is embedded in the APK.
- OAuth tokens are stored separately from account metadata in an Android Keystore-backed AES-GCM credential vault.
- Existing T1Xperts/custom IMAP password accounts and Yahoo/iCloud app-password paths are retained.
- Provider capability metadata replaces password-only assumptions in the account model.
- OAuth accounts no longer require a stored mailbox password to be considered usable.
- Safe OAuth-specific authentication errors instruct users to reconnect rather than exposing provider responses or credentials.
- Existing account IDs, local settings, signatures and mail caches remain attached to the same account during authentication migration.

## Security
- Normal Google and Microsoft passwords are not collected for OAuth accounts.
- OAuth bearer tokens and refresh tokens are never stored in ordinary account JSON.
- Long-lived OAuth material is encrypted with an Android Keystore-managed AES key.
- No confidential OAuth client secret is committed to source or embedded in the mobile application.
- Microsoft authorization uses Authorization Code + PKCE through AppAuth.

## Automated QA evidence
- PR #21 merged to `main` as `16153dc02ee14a62e9a613bf412a5f47267bbc45`.
- PR Build MailXperts Android #53 / run `37122256291`: PASS.
- PR P0 QA and APK Build #33 / run `37122256332`: PASS.
- Main Build MailXperts Android #54 / run `37122426566`: `validate-debug` PASS.
- Main P0 QA and APK Build #34 / run `37122426634`: PASS, including APK identity verification and artifact upload.
- Protected signed-release job failed closed because repository secret `MX_KEYSTORE_BASE64` is not configured. No incorrectly signed production artifact was published.

## External activation prerequisites
The implementation compiles and degrades safely without provider registrations, but live provider authorization requires provider-side configuration:

### Google
- Google Cloud / Android OAuth application registration for the MailXperts Android package and production signing identity.
- Gmail mail scope (`https://mail.google.com/`) enabled/approved as required by Google's consent and verification rules.

### Microsoft
- Microsoft Entra public-client application registration.
- Build-time `MX_MICROSOFT_CLIENT_ID` and matching `MX_MICROSOFT_REDIRECT_URI`.
- Delegated IMAP and SMTP permissions/consent subject to tenant policy.

## Remaining QA / UAT
Live Gmail and Microsoft authorization, receive/send/background-sync, token refresh/re-consent and cross-provider physical-device regression remain UAT gates because they require real provider registrations and user consent.

**Lifecycle status:** implementation merged and automated QA passed. MX-QA-030 remains open until provider activation and live device UAT pass.
