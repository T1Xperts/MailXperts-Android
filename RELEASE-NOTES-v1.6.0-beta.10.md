# MailXperts v1.6.0-beta.10 (Wave 0/1 QA candidate)

**Status:** QA-only preview from unmerged draft PR #33; not a signed-off Alpha, Play Store publication or completed Wave 0/1 UAT.

## Changes on top of v1.6.0-beta.9

- Explicit opt-in for sending selected mailbox data to the configured MailXperts Cloud AI gateway. The default is off.
- Visible privacy notice in gateway settings clarifies selected mail content reaches an external server/model provider, not an on-device model.
- Users can revoke Cloud AI consent; the Android gateway client blocks new AI network requests when disabled.
- Hardened HTTPS gateway URL policy rejects query, fragment, path traversal and encoded separator input.
- AI server requires an explicitly configured approved model via `MAIL_AI_MODEL` or `OPENAI_MODEL`; no speculative built-in model default.
- JUnit security contracts cover consent, revocation and request enforcement.
- Android production identity remains `au.com.t1xperts.mailxperts`; versionName `1.6.0-beta.10`, versionCode `24`.

## Required verification

Android build, lint, JUnit, P0 QA/APK, Build Expert Node security must PASS on the beta.10 branch head. Code-level tests are not live HTTPS gateway or physical-device UAT.

## Remaining acceptance gates

- MX-QA-029: isolated Build Expert runner, human-approved writes, provider resilience, cost controls and operational security UAT.
- MX-QA-030: Google/Gmail OAuth2→IMAP/SMTP production-identity device UAT.
- AI-001–007: deployed authenticated HTTPS gateway, real model responses, physical-device UAT and Product Owner acceptance.
- AI-035: basic global Cloud AI opt-in/revocation implemented; comprehensive per-account privacy controls and genuine on-device model support still pending.
- HLD/LLD reviews, PR review/merge and release sign-off pending.

## Distribution

The QA APK may be supplied as an **internal signed preview** only if signed offline using the existing matching production certificate and verified with v2/v3 APK signatures. No signing material in source control. A `.dev` debug APK installs as a separate app and will not upgrade production. Unsigned CI release APK must not be distributed for installation.
