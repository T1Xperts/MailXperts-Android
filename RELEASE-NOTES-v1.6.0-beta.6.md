# MailXperts v1.6.0-beta.6

## Gmail OAuth login hotfix

Device QA on v1.6.0-beta.5 exposed a regression in the Google OAuth connection flow. A mailbox validation error was converted into a safe provider message and then converted a second time, causing the useful diagnosis to be replaced by a generic connection-failed message.

### Fixed

- Provider/mail-transport errors are mapped exactly once and remain actionable.
- Gmail IMAP and SMTP OAuth failures retain the failing stage in the user-facing message.
- Google authorization requests the Gmail mail scope plus primary-account email identity scope so MailXperts can bind the token to the intended Gmail address.
- MailXperts blocks an OAuth token/account mismatch instead of silently testing one Google account token against another configured address.
- OAuth migration is transactional: account metadata is saved only after both IMAP and SMTP validation succeed.
- If validation fails, the previous OAuth credential state is restored and the existing account configuration is left unchanged.
- Google OAuth configuration/developer errors now produce a production-signing/OAuth-registration diagnostic instead of a generic mailbox error.

### Release identity

- versionName: `1.6.0-beta.6`
- versionCode: `20`
- package: `au.com.t1xperts.mailxperts`

Device QA/UAT is required before the authentication regression is closed again.
