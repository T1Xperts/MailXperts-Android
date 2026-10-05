# v1.6.0-beta.6 OAuth login regression hotfix

Device QA on v1.6.0-beta.5 exposed a Gmail OAuth connection failure screen that replaced the real provider/mail-transport cause with a generic message.

## Confirmed defects

1. `OAuthConnectActivity.persistAndValidate()` converted the caught exception into a friendly provider error and then passed that text into another provider-error mapping path. The second mapping collapsed useful diagnostics into the generic fallback.
2. OAuth account metadata and the new token were persisted before IMAP/SMTP validation completed. A failed validation could therefore leave the account in a partially migrated OAuth state.
3. Google authorization requested only the Gmail mail scope. The authorized Google account email was therefore not reliably available for identity binding, increasing the risk of testing a token against a stale configured username when multiple Google accounts exist.

## Hotfix acceptance criteria

- A mailbox validation failure is mapped exactly once and preserves provider/stage guidance.
- The OAuth token is bound to the authorized Google account email where Google returns identity information.
- Google authorization also requests the primary-account email scope.
- A returned Google account that differs from the configured MailXperts Gmail address is blocked with an explicit mismatch message.
- If Google omits the email identity but MailXperts already has an explicitly configured Gmail address, the token is validated only against that configured identity and no silent account switch occurs.
- Failed validation restores the pre-login OAuth credential state and leaves existing account metadata unchanged.
- Successful validation persists the candidate account, schedules sync, and returns success exactly as before.
- Package/signing identity stays unchanged; version advances to v1.6.0-beta.6 / VC20.
