# Play Data safety working notes

These notes are an engineering handoff, not a completed legal declaration. The Play Console owner must confirm them against the final privacy policy and production integrations.

## Current on-device behavior

- Email addresses, server names, usernames and encrypted credentials are stored on the user's device.
- Passwords are encrypted with an Android Keystore AES-GCM key.
- Message headers and bodies are retrieved directly from configured IMAP servers.
- Outgoing messages are sent directly to configured SMTP servers.
- Drafts, Outbox items and scheduled message content are stored in the app's private local database.
- Smart Priority and due-date detection run locally on the device.
- MailXperts does not currently include advertising, analytics or a T1Xperts message-content backend.
- An external abuse report is never sent silently; the app only opens a user-reviewable draft/share action.

## Items to verify before declaring Data safety

- Whether crash reporting, analytics, support SDKs or other telemetry will be added to the Play build.
- Whether T1Xperts support receives account identifiers, logs, message content or diagnostic exports.
- The retention/deletion process for any future server-side service.
- Whether production Google/Microsoft OAuth introduces token handling or provider-specific data-use obligations.
- The final encrypted-backup and device-transfer behavior.
