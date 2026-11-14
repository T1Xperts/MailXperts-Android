# Play Data safety working notes

These notes are an engineering handoff, not a completed legal declaration. The Play Console owner must confirm them against the final privacy policy and production integrations.

## Current on-device behavior

- Email addresses, server names, usernames and encrypted credentials are stored on the user's device.
- Passwords are encrypted with an Android Keystore AES-GCM key.
- Message headers and bodies are retrieved directly from configured IMAP servers.
- Outgoing messages are sent directly to configured SMTP servers.
- Drafts, Outbox items and scheduled message content are stored in the app's private local database.
- Smart Priority and due-date detection run locally on the device.
- Smart Contacts learns email participants locally on the device and stores the learned contact index in private app preferences.
- The optional Android `READ_CONTACTS` permission is requested only when the user enables phone-contact access. MailXperts reads email/name data from the Android Contacts provider for local search/autocomplete.
- Google, Exchange and CardDAV/iCloud contacts already synchronised into Android may appear through Android Contacts.
- MailXperts also supports optional **direct** Google Contacts and iCloud/CardDAV connections. These are disabled until the user explicitly connects the provider and selects a synchronization mode.
- Direct Google Contacts access uses separate Google Contacts OAuth scopes from Gmail mailbox OAuth. The contact connector does not persist its short-lived Google access credential.
- Direct iCloud/CardDAV uses an endpoint, username and app-specific password supplied by the user. These secrets are encrypted on-device using Android Keystore-backed AES/GCM storage.
- Cloud contact synchronization can operate read-only. Provider writes are available only in a write-capable mode and only for individual Smart Contacts the user explicitly marks for that provider; learned contacts are **not** bulk-uploaded automatically.
- Disconnecting Google Contacts or iCloud/CardDAV removes provider connection/sync state but does not silently erase local Smart Contacts history.
- Promoting a Smart Contact to phone contacts opens the Android system contact editor so the user controls the destination account and final save action.
- MailXperts does not upload the Android device address book or learned Smart Contacts to a T1Xperts backend as part of MX-QA-027.
- The separate Build Expert service introduced for MX-QA-029 is an engineering/release service, not an Android customer-mail backend. No OpenAI/API credential is stored in the APK.
- MailXperts does not currently include advertising or a T1Xperts message-content backend.
- An external abuse report is never sent silently; the app only opens a user-reviewable draft/share action.

## Items to verify before declaring Data safety

- Whether crash reporting, analytics, support SDKs or other telemetry will be added to the Play build.
- Whether T1Xperts support receives account identifiers, logs, message content or diagnostic exports.
- The retention/deletion process for any future customer-facing server-side service.
- Final Google People API production consent-screen configuration, privacy-policy disclosures and any Google API Services User Data Policy obligations.
- Final iCloud/CardDAV privacy wording and support guidance for app-specific passwords.
- Whether production Google/Microsoft mailbox OAuth introduces additional provider-specific data-use obligations.
- The final encrypted-backup and device-transfer behavior.
