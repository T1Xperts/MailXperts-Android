# MailXperts 1.5.0

## Instant cache-first mail

- Paints up to 100 cached summaries before waiting for IMAP and fetches only 25 headers in the first network batch.
- Continues initial and older-mail backfill asynchronously in bounded batches, with a 200-header budget for periodic jobs.
- Enables SQLite write-ahead logging for concurrent cache reads and background updates.
- Keeps UID-based incremental refresh, 60-second watchdogs and a maximum of three simultaneous account connections.

## User-controlled synchronisation

- Adds per-account Manual, 15-minute, 30-minute, hourly, 2-hour, 6-hour, 12-hour and daily intervals.
- Replaces repeating alarms with persisted, network-aware Android JobScheduler work.
- Separates background cache refresh from notification preference.
- Adds explicit settings for server read-state updates, server deletion and server Drafts synchronisation.
- Defaults deletion to local-only and persists hidden-message tombstones across refreshes.
- Moves server deletions to Trash when supported and always saves a local Draft before optional server Draft sync.

## MailXperts 1.4.1

## Inbox loading and sync reliability

- Opens previously synced Inbox, Sent, Spam/Junk and Smart Priority summaries immediately from an on-device cache.
- Fetches a fast 50-message first page, followed by 100-message background batches with visible progress.
- Refreshes incrementally using IMAP UIDs and resumes interrupted backfills from the oldest cached UID.
- Keeps explicit 1,000-message older-mail paging with a 5,000-message per-account ceiling.
- Handles UIDVALIDITY changes, deleted cache anchors and empty server folders safely.
- Limits unified sync to three concurrent accounts and prevents duplicate refresh jobs.
- Adds bounded IMAP connection/read/write timeouts plus a 60-second per-account watchdog.
- Updates cached read state and removes messages moved between Inbox and Spam/Junk.

## MailXperts 1.4.0

## Unified multi-account mail

- Adds **All Accounts** to the top-right selector and keeps every individual mailbox selectable.
- Adds **Add new mail account** and **Manage accounts** directly to that selector.
- Unifies Inbox, Sent, Spam/Junk, Smart Priority and Inbox/Sent search across all connected providers.
- Unifies local Drafts, Outbox and Scheduled/recurring messages.
- Labels unified messages and local items with their source account.
- Opens, replies, marks spam, retries and schedules through the correct source account.
- Adds a visible **From account** selector to the composer.

## Provider and release updates

- Supports Gmail, Yahoo Mail, iCloud, Outlook/Hotmail/MSN/Live guided setup and secure custom IMAP/SMTP servers.
- Keeps Outlook-family password-only sign-in blocked until the T1Xperts Microsoft OAuth2 app registration is configured.
- Targets Android API 36 with version code 7.
- Adds a signed Android App Bundle release output alongside the signed APK.
- Retains the v1.2 production certificate for a normal update over v1.2 and v1.3.

## MailXperts 1.3.0

## New mail features

- Adds guided Gmail, Yahoo Mail, iCloud and secure custom IMAP/SMTP account presets.
- Shows Microsoft Outlook/Hotmail setup but safely blocks password-only connection until T1Xperts OAuth2 application credentials are configured.
- Adds provider-native **Mark spam** and **Not spam** actions using Junk/Spam folders and IMAP junk flags.
- Adds a separate, user-reviewed Cloudflare abuse-report draft with full email headers; nothing is reported automatically.
- Adds private on-device Smart Priority for bills/payments, security, travel, delivery and receipt messages.
- Detects likely due dates and offers a user-confirmed calendar reminder.
- Adds emoji and symbol palettes, HTML source editing and UTF-8 output for ASCII, Unicode and emoji.
- Adds selectable SMTP SSL/TLS and required STARTTLS.

## Interface and safety

- Protects every app screen from Android status-bar, camera-cutout and gesture-navigation overlap.
- Retains the premium black/ocean-teal and white/ocean-teal themes, account switcher, hamburger navigation and bounded 1,000-message loading.
- Preserves the production package name and v1.2 signing certificate for a normal in-place update.

## Known integration requirement

- Microsoft Outlook/Hotmail requires an application registration, redirect URI and OAuth2 client configuration before sign-in can be enabled. No fake or shared credentials are embedded in this release.

## MailXperts 1.2.2

## System-bar safe-area fix

- Keeps headers, menus, account controls and compose actions below the Android status bar.
- Keeps scrollable content and bottom controls above gesture/navigation bars.
- Applies display-cutout safe areas in portrait and landscape on Android 15 and newer.

## MailXperts 1.2.1

## Startup reliability fix

- Rebuilt against the real Android 15 framework API instead of compile-only framework stubs.
- Replaced the legacy DX packaging path with current D8 bytecode desugaring.
- Added defensive startup fallbacks so a non-critical theme or splash failure cannot terminate the app.

MailXperts 1.2 establishes the permanent production-signing baseline following the approved one-time reinstall from v1.1.

## Highlights

- Premium white, premium black and automatic themes.
- Branded light and dark splash screens.
- Account dropdown and hamburger navigation.
- Global Inbox and Sent search, including message bodies.
- Latest 1,000-message initial loading for Inbox and Sent.
- Device-font rich-text editor and HTML source mode.
- Account signatures and background new-mail notifications.
- Existing multi-account IMAP/SMTP, Drafts, Outbox, Sent copy and scheduling functions retained.

## Reinstall notice

The previous v1.1 debug signing key is unavailable. Back up or send any local Drafts, Outbox items and scheduled messages before uninstalling v1.1. Email stored on the IMAP server is not removed by uninstalling the app.
