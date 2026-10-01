# MailXperts 1.6.0 Beta 2

**Version:** 1.6.0-beta.2  
**versionCode:** 16  
**Package:** `au.com.t1xperts.mailxperts`  
**Channel:** Beta / MX-QA-027 validation build

## Purpose

This beta implements the on-device and Android-provider portions of **MX-QA-027 — Smart Contacts, To/CC/BCC autocomplete & cross-platform contact sync**. Automated unit, lint and build gates must pass before the APK is distributed. Physical-device UAT remains required before MX-QA-027 can be closed.

## Smart Contacts delivered in this beta

- Dedicated **Smart Contacts** screen available from the navigation drawer.
- Learns legitimate participants from sent and received mail.
- Learns From, Reply-To, To, Cc and Bcc addresses when those headers are available from the provider.
- Excludes the configured mailbox owner's own address from learned contacts.
- Filters common automated addresses such as no-reply and mailer-daemon.
- De-duplicates email addresses case-insensitively.
- Persists first/last interaction, incoming/outgoing counts and source metadata.
- Uses IMAP account/folder/UID learning markers so repeated refreshes do not inflate frequency.
- To/Cc/Bcc autocomplete combines MailXperts learned contacts with permissioned Android contacts.
- Optional Android Contacts access is permission-gated and Smart Contacts continues to work if permission is denied.
- Android Contacts already synchronised from Google, Exchange or CardDAV/iCloud can participate in Smart Contacts/autocomplete.
- Users can explicitly import phone contacts into the local MailXperts Smart Contacts index.
- Users can compose from a contact, remove a learned copy, block future learning, and clear learned history.
- “Add / update in phone contacts” opens the Android system contact editor, keeping the destination account and final write under user control.
- Scheduled sends also contribute to learned outgoing-contact history.
- Contact lookup remains local-first and does not upload the address book to MailXperts infrastructure.

## Provider integration boundary

This Android beta deliberately does **not** collect Google or iCloud credentials and does not silently push every learned address into a cloud contact service. Google/iCloud/CardDAV contacts that are synchronised into Android are supported through the Android Contacts provider.

A future direct Google People API or direct iCloud/CardDAV cloud connector requires provider registration, OAuth/credential architecture, token-storage review and separate security/UAT evidence. Until that provider-specific work is completed, MX-QA-027 should be treated as **partially implemented**, not Closed.

## QA coverage added

- Smart-contact automated/system-address filtering.
- Cross-source contact de-duplication and source merge.
- Manifest contract for explicit `READ_CONTACTS` permission.
- Manifest contract confirming `SmartContactsActivity` is non-exported.
- Release identity contract updated for versionCode 16 / 1.6.0-beta.2.
- Existing recipient parsing/autocomplete regression tests remain active.


## Release signing path

The main-branch automated code-quality and P0 regression gates passed for this release. The GitHub-hosted signed-release lane reached the permanent-key restoration gate but did not complete signing, so no unsigned artifact is being represented as a production release.

The repository's offline-signing workflow is used as the controlled fallback: it builds and verifies the production package identity, bundles Android Build Tools `apksigner`, and the resulting APK is signed offline with the established MailXperts production certificate before distribution.

## Beta exit criteria

- Standard Android CI: lint, JVM tests and debug APK build pass.
- Dedicated P0 regression workflow remains green.
- Signed release workflow verifies the established MailXperts production signing certificate if repository signing secrets are configured.
- Physical-device QA validates permission grant/deny, local learning, Inbox/Sent learning, autocomplete, import, remove/block, and system contact-editor flow.
- Upgrade from a compatible production build preserves account/settings/app data.
