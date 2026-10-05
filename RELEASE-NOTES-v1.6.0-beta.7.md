# MailXperts v1.6.0-beta.7

## Focus
Gmail OAuth IMAP/SMTP transport hardening after device QA reproduced an IMAP OAuth rejection in beta.6.

## Fixes
- Requests only the Gmail protocol scope required for IMAP/SMTP: `https://mail.google.com/`.
- Stops rolling unrelated previously granted Google scopes into the Gmail mailbox token.
- Verifies that Google actually granted the Gmail mail scope before MailXperts attempts IMAP authentication.
- Gives a specific consent error when Gmail mailbox permission was not granted instead of treating any Google access token as sufficient.
- Explicitly enables JavaMail XOAUTH2 for both IMAPS and SMTP while keeping LOGIN/PLAIN disabled for OAuth accounts.
- Retains the beta.6 transactional account migration and provider-safe diagnostics.

## Rationale
Google's Android Authorization API can return a token after the user grants only a subset of requested scopes. Gmail's IMAP/SMTP XOAUTH2 service requires `https://mail.google.com/`. MailXperts must therefore validate the granted scopes before using the token as a SASL credential.

## Release identity
- versionName: `1.6.0-beta.7`
- versionCode: `21`
- package: `au.com.t1xperts.mailxperts`

## QA gate
Automated Android/P0 regression tests include Gmail scope validation and JavaMail XOAUTH2 transport contracts. Physical-device Gmail OAuth login remains the final acceptance gate for reopened MX-QA-030.
