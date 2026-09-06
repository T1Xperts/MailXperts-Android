# MailXperts Android 1.5.1

MailXperts is the T1Xperts smart Android email client for secure IMAP and SMTP mail. Version 1.5.1 keeps mailbox startup cache-first, repairs background scheduling and restores safe rich HTML signatures.

## Version identity

- Application ID: `au.com.t1xperts.mailxperts`
- Release version: `1.5.1`
- Release version code: `10`
- Minimum Android: 8.0 / API 26
- Compile and target Android: API 36
- Development builds use `au.com.t1xperts.mailxperts.dev` so they cannot accidentally replace the signed production app.

## v1.5.1 reliability and signature fixes

- Declares Android network-state access so network-constrained 15-minute (or user-selected) background jobs can be scheduled.
- Keeps a successful IMAP/SMTP account save successful even if an OEM job scheduler rejects background work; manual refresh remains available.
- Separates connection, persistence and scheduler outcomes so a scheduler error is never shown as an authentication failure.
- Replaces the plain signature box with a dedicated visual/HTML editor that preserves existing markup.
- Migrates signatures accidentally escaped by v1.5.0 back to formatted HTML.
- Adds bounded local/shared PNG, JPEG, GIF and WebP insertion plus HTTP/HTTPS image links to signature and message editors.
- Sanitises active HTML and unsafe URL protocols while retaining normal email-table formatting, links, fonts and inline styles.

## v1.5.0 instant loading and sync controls

- Paints up to 100 cached summaries before any network result and then expands the list asynchronously.
- Fetches only the newest 25 headers in the first network batch, followed by bounded 100-message batches.
- Uses SQLite write-ahead logging so cache reads remain responsive while background batches are committed.
- Uses IMAP UID checkpoints for incremental refresh and interrupted-sync resume.
- Loads older mail in explicit 1,000-message pages, capped at 5,000 per account.
- Allows Manual, 15-minute, 30-minute, hourly, 2-hour, 6-hour, 12-hour or daily refresh per account.
- Uses Android JobScheduler with a network constraint and incremental 200-header periodic backfill budget.
- Adds separate settings for server read-state updates, server deletion and server Drafts synchronisation.
- Defaults deletion to local-only, with persistent local tombstones so hidden messages do not reappear.
- Moves server-deleted messages to Trash when supported and saves Drafts locally before optional server sync.
- Invalidates stale cache safely when IMAP UIDVALIDITY changes.
- Runs unified-account sync with at most three concurrent connections.
- Adds connection, read, write and whole-sync timeouts so a server cannot leave the screen loading forever.
- Preserves cached flags when a message is read and removes a cached row after a Spam/Junk move.
- Migrates the local database without deleting Drafts, Outbox or Scheduled mail.

## v1.4 features

- A top-right **All Accounts** scope plus every connected account.
- **Add new mail account** and **Manage accounts** actions inside the top-right account menu.
- Unified Inbox, Sent, Spam/Junk and Smart Priority views across all connected IMAP providers.
- Unified Inbox-and-Sent search with account badges on every result.
- Unified local Drafts, Outbox and Scheduled/recurring folders.
- Account-aware message opening, replies, spam actions, retries and scheduled sending.
- A **From account** selector in the composer, including when composing from All Accounts.
- Guided Gmail, Yahoo Mail, iCloud and Outlook/Hotmail/MSN/Live setup plus unrestricted secure custom IMAP/SMTP settings.
- Android API 36 target and a signed Android App Bundle release path for Google Play.

## v1.3 features retained

- Android status-bar, display-cutout and gesture-navigation safe areas on every app screen.
- Gmail, Yahoo Mail, iCloud and custom IMAP/SMTP guided presets using provider app passwords where supported.
- Microsoft Outlook/Hotmail preset with an explicit OAuth2 requirement; password-only sign-in is intentionally blocked until the T1Xperts Microsoft application registration is configured.
- Provider-native **Mark spam** and **Not spam** actions using Junk/Spam folders and IMAP junk flags.
- Optional, user-reviewed Cloudflare abuse-report draft with full headers; no report is transmitted silently.
- Private on-device Smart Priority categories for bills/payments, finance/security notices, travel, deliveries and receipts.
- Due-date detection with a user-confirmed Android calendar reminder action.
- Emoji and symbol palettes, HTML source mode and UTF-8 MIME output covering ASCII, international Unicode text and emoji.
- Secure SMTP SSL/TLS or required STARTTLS selection.

## Retained features

- Premium black night, premium white day and automatic system themes.
- Theme-aware MailXperts opening splash screens and branded icon.
- Hamburger navigation for Inbox, Sent, Drafts, Outbox, Scheduled, Accounts, Appearance and settings.
- Top-right account selector on the mailbox home screen.
- Secure server-side search across Inbox and Sent subject, sender, recipient and message body.
- Initial Inbox and Sent sync limited to the latest 1,000 messages, with explicit 1,000-message older-mail loading.
- Rich-text and HTML-source composer with device font discovery on Android 10 and newer, plus safe generic fallbacks.
- Automatic per-account signatures.
- Secure background cache refresh and new-mail notifications at the interval selected for each account.
- Multi-account encrypted credentials using Android Keystore.
- Local Drafts, manual-retry Outbox, one-time and recurring scheduled mail.
- TLS server-identity checking for IMAP and SMTP; cleartext traffic remains disabled.

## Build

The project includes a Gradle 8.13 wrapper, Android Gradle Plugin 8.13.2 and a GitHub Actions workflow.

Development validation:

```bash
./gradlew clean lintDebug testDebugUnitTest assembleDebug
```

The development APK has the `.dev` application ID and is not the production signing baseline. GitHub-hosted runners create temporary debug certificates, so a debug APK can require one uninstall/reinstall when its certificate differs from a previously downloaded debug build.

Signed release build:

```bash
export MX_KEYSTORE_FILE=/secure/path/MailXperts-v1.2-release.p12
export MX_KEYSTORE_PASSWORD='<stored securely>'
export MX_KEY_ALIAS='mailxperts-release'
export MX_KEY_PASSWORD='<stored securely>'
./gradlew clean lintRelease assembleRelease bundleRelease
```

Never commit the keystore or credentials. See [SIGNING.md](SIGNING.md).

## Signing and upgrades

The supplied v1.1 APK was signed with an unavailable temporary debug key. Android therefore could not accept v1.2 as an in-place update over that APK.

Version 1.5.1 is configured to use the same permanent production certificate as v1.2.0 through v1.5.0 and installs as a normal in-place update when built with that keystore.

From v1.2 onward, keep using the v1.2 release keystore and increasing `versionCode`; future APKs will install as normal upgrades.

## Google Play release boundary

The source and release workflow produce a signed APK and Android App Bundle. A public store launch still requires the T1Xperts Play Console owner to complete the listing, privacy-policy URL, Data safety declaration, testing track and any account-access instructions.

Gmail app-password setup is available for compatible accounts. Consumer Outlook/Hotmail/MSN/Live password-only setup remains intentionally blocked because Microsoft requires OAuth2. Production one-tap Google or Microsoft sign-in requires T1Xperts-owned OAuth client registrations, redirect URIs and provider verification; no shared or placeholder OAuth credentials are embedded.
