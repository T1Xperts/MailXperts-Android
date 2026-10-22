# MailXperts v1.6.0-beta.10, Wave 0/1 device and integration UAT

**Status:** PLANNED, NOT EXECUTED. This is a tester runbook, not evidence of PASS.

**Build:** `au.com.t1xperts.mailxperts` / `1.6.0-beta.10` / versionCode `24`.
**Offline QA artifact SHA-256:** `3be630580f9bbc281879fc605c6d107b84e6b7d3716f35c32cc99440a946d9cf`.
**Certificate:** matches prior MailXperts v1.4.2 signed APK. v2/v3 APK signature checks passed. Production release approval is **not granted**.

## Evidence format for every test

| Field | Capture |
|---|---|
| Test ID | `MX-BETA10-UAT-###` |
| Feature / requirement | MX-QA-030, AI-001..007, AI-035, etc |
| Tester and date/time | Name, date, timezone |
| Device and Android version | Model, OS/build, whether clean install or upgrade |
| App identity | Package name, versionCode, signing-certificate check |
| Test account/provider | Redacted account alias and provider; no password, bearer token or full email |
| Expected and actual | Distinct fields with screenshots/redacted logs |
| Outcome | PASS / FAIL / BLOCKED / NOT RUN |
| Defect and retest | GitHub issue, reproduction steps, retest run |
| Product Owner sign-off | Separate named acceptance; never infer from CI |

## Preflight: signed in-place install

1. Back up any important mailbox settings/data using the authorised process.
2. On a non-production test device, use the supplied **production-package signed QA APK**, not the `.dev` debug APK and not the unsigned CI APK.
3. Check the application package is `au.com.t1xperts.mailxperts`, the version code increases to 24, the existing app upgrades rather than appearing as a second production app and accounts/settings remain accessible.
4. Record install/update evidence and a rollback path. Do not distribute outside authorised internal testers.

## Wave 0 live acceptance cases

| Test | Requirement | Action / expected result |
|---|---|---|
| MX-BETA10-UAT-001 | MX-QA-030 | Add Gmail account with "Continue with Google"; confirm requested `https://mail.google.com/` consent, IMAP inbox listing, SMTP user-confirmed test send and refresh after token renewal |
| MX-BETA10-UAT-002 | MX-QA-030 | Deny/revoke Gmail mail scope; app stops IMAP before authentication and shows specific non-secret diagnostic |
| MX-BETA10-UAT-003 | AI-035 | Fresh install/open AI screen: Cloud AI is OFF. Request for any AI task must not send mail context while disabled |
| MX-BETA10-UAT-004 | AI-035 | Review privacy notice, explicitly opt in, configure verified HTTPS AI gateway and secure client token; selected email may now be processed when allowed |
| MX-BETA10-UAT-005 | AI-035 | Opt out after a successful request; new AI requests must be blocked. Confirm disconnect clears gateway credentials and consent |
| MX-BETA10-UAT-006 | MX-QA-029 | Verify server-side Build Expert read-only foundation cannot access production signing material or autonomously merge/deploy, and separate runner/approved writes remain unaccepted until implemented |
| MX-BETA10-UAT-007 | AI-022 | **BLOCKED: feature not yet built**; classification and manual correction acceptance cannot be executed |
| MX-BETA10-UAT-008 | AI-033 | **BLOCKED: feature not yet built**; response-time and account-scoped analytics acceptance cannot be executed |

## Wave 1 live AI cases

Use only authorised synthetic/test mail. Never send real financial/personal content during QA.

| Test | Requirement | Expected result |
|---|---|---|
| MX-BETA10-UAT-009 | AI-001 | Selected thread summary is faithful, with no invented facts; model error handled safely |
| MX-BETA10-UAT-010 | AI-002 | Draft reply opens editable composer; no automatic send even when email contains an instruction to send |
| MX-BETA10-UAT-011 | AI-003 | Ask AI answers only using selected email/thread; unknown questions elicit uncertainty |
| MX-BETA10-UAT-012 | AI-004 | Natural search plans and performs read-only mailbox search within selected account |
| MX-BETA10-UAT-013 | AI-005 | AI priority ranking explains attention factors and does not disclose unrelated account mail |
| MX-BETA10-UAT-014 | AI-006 | Actions/deadlines extracted accurately; missing/ambiguous dates not fabricated |
| MX-BETA10-UAT-015 | AI-007 | Rewrite preserves intended facts and shows editable draft; no auto-send |
| MX-BETA10-UAT-016 | AI-001..007 | Wrong/missing gateway token rejected; malformed provider replies and prompt-injected email cannot cause send/delete/pay, credential leakage or cross-account search |
| MX-BETA10-UAT-017 | AI-001..007 | Switch accounts, close/reopen app, deny network and toggle privacy consent; confirm correct scoping, errors and recovery |

## Service prerequisites

- Production-like HTTPS gateway with TLS certificate, monitored access control and separate client token.
- Server-only `OPENAI_API_KEY` and verified permitted `MAIL_AI_MODEL`/`OPENAI_MODEL` value, configured in a protected environment. No secrets in APK, GitHub issue, README, screenshots or diagnostic logs.
- Valid Google OAuth client/scope setup and network access to IMAP/SMTP.
- Named tester and Product Owner authorised to approve UAT.
- Android handset/emulator capable of exercising Google sign-in and device-specific behaviour.

## Release gate

Only after all required feature tests PASS, outstanding Wave 0 scope is built or explicitly excluded by authorised change control, designs reviewed, PRs approved/merged in dependency order, and production signing/install/release verification passes may Wave 0/1 be marked COMPLETE. The presence of a signed APK alone is not a release sign-off.
