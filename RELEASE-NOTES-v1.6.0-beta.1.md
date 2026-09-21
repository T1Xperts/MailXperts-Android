# MailXperts 1.6.0 Beta 1

**Version:** 1.6.0-beta.1  
**versionCode:** 15  
**Package:** `au.com.t1xperts.mailxperts`  
**Channel:** Public/closed beta candidate

## Purpose
This beta consolidates the current MailXperts QA backlog into a field-testable Android build. Automated CI, unit and regression tests must pass before distribution. Device/UAT results remain required before affected QA items are closed.

## Included improvements
- Gmail App Password normalisation, validation and safer staged IMAP/SMTP diagnostics.
- Rich/HTML signature inline images converted to CID MIME parts for broader recipient compatibility.
- Neutral sender-safe HTML message canvas across light/dark app themes.
- Expandable message metadata/details.
- More compact Smart Priority display.
- Full-height left navigation drawer.
- Quick account switching directly from server mailbox screens.
- Clearer account-settings hierarchy and server-delete behaviour.
- Android Share-to-MailXperts for text, files and images.
- Local learned-recipient autocomplete for To/Cc/Bcc with a privacy clear-history control.
- Advanced search modes: All words, Any word and Exact phrase.
- Search scopes across account(s), Inbox, Sent, Spam/Junk, Drafts, Outbox, Scheduled and combined scopes.
- Practical wildcard/partial-term matching and local attachment-filename matching.
- Search entry points from server and local mailbox screens.

## Known beta limitations
- Google OAuth2 / Sign in with Google is not yet implemented. Gmail currently uses a Google App Password flow where permitted.
- Server-side attachment filenames are not separately indexed by generic IMAP search. Local Draft/Outbox/Scheduled attachment filenames are searchable.
- Android device contacts are not imported automatically. Learned-recipient suggestions are local to MailXperts.
- Build Expert / AI provider orchestration (MX-QA-029) is a separate architecture/security workstream and is not part of the Android beta runtime.
- Device-specific rendering, provider interoperability, upgrade continuity and physical intent handling require beta-tester validation.

## Beta exit criteria
- GitHub standard Android CI passes.
- Dedicated P0 regression workflow passes.
- Release APK uses the established package identity and expected signing certificate.
- Physical upgrade from a compatible existing production build succeeds.
- Core P0 device test matrix passes.
- No unresolved beta-stopping security, data-loss or crash defect remains.
