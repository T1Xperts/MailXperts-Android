# MailXperts Product Backlog | 19 November 2026

**Owner:** T1Xperts Product Owner | **Status:** Controlled addendum, subject to approval | **Release:** NOT RELEASED

## Backlog baseline and scope
- Historical MX-QA-001–030 acceptance/status preserved; 28 Product Owner accepted, MX-QA-029 partial, MX-QA-030 device UAT pending. Do not reopen accepted scope without regression evidence.
- AI-001–036 retain their separate [end-user AI epic](https://github.com/T1Xperts/MailXperts-Android/issues/29).
- MXAI-001–094 retain their [proposed multi-assistant + iOS backlog](../traceability/MXAI_PLUGIN_BACKLOG.csv); no implication of shipped features.
- This increment adds **MX-QA-031** (confirmed user-observed defect) and **MX-QA-032** (new diagnostic feature). Combined requirement IDs in the RTM increase **160 → 162** with 44 original fields retained. MX-QA+AI subset now totals 68 (32+36); add 94 MXAI proposed records for complete RTM count.

## New or active top-priority items

| ID | Type | Priority | User outcome | Implementation | QA / UAT | Next action |
| --- | --- | --- | --- | --- | --- | --- |
| [MX-QA-031](https://github.com/T1Xperts/MailXperts-Android/issues/38) | Defect | P1, escalate P0 if broadly reproduced | Reply/Reply All must load composer for complex HTML threads without blank screen | Not fixed / root cause unconfirmed | User reproduction reported; Android logs not captured; retest pending | Capture sanitized logcat from physical device, fix, regression test |
| [MX-QA-032](https://github.com/T1Xperts/MailXperts-Android/issues/39) | Feature | P1 | View on-device crash/error history, export safe reports and optionally sync issues to GitHub | **Partial code on [draft PR #40](https://github.com/T1Xperts/MailXperts-Android/pull/40)**: local logger, diagnostics UI, navigation breadcrumbs; server sync unbuilt | JUnit/source contract tests and Android/P0 CI **PASS (aggregate)**; physical-device UAT **not run** | Review code/privacy; build, test, approve secure gateway+consent, then UAT |

## MX-QA-032 deliverable slices

| Slice | Deliverable | State | Dependency |
| --- | --- | --- | --- |
| A1 | Uncaught exception metadata, supported Android process exits, local bounded history | Candidate code in draft PR #40 | Review, Android CI, physical QA |
| A2 | Reply/Reply All and composer-ready/timeout event breadcrumbs | Candidate code in draft PR #40 | Regression test MX-QA-031 |
| A3 | Diagnostics settings/navigation UI and sanitized explicit export/delete | Candidate code in draft PR #40 | UI/device testing, accessibility |
| B1 | Consent, scopes, revocation, retention and privacy review | Design only | Product Owner + security approval |
| B2 | Authenticated private telemetry gateway, anti-abuse controls, offline queue | Not implemented | Approved identity, hosting, backend |
| C1 | GitHub App server-side installation, private issue creation/update and fingerprint dedupe | Not implemented | Backend, permissions, production credentials |
| D1 | End-to-end gateway/device UAT, release signing and GO/NO-GO | Not executed | All earlier slices |

## Standard SDLC gates
Backlog / triage → approved HLD → reviewed LLD → PR/code → automated QA → device UAT → release approval → production signing/deployment → monitoring.

**Non-negotiable controls:** no full device logcat in production; never ship GitHub tokens/keys or LLM credentials in APK; no collection of mail bodies, subject, participants, patient details, OAuth tokens or attachments; opt-in remote reporting and permissioned private GitHub issues; human approval for fixes, email sends and operational changes.

**Traceability:** [44-field RTM](../traceability/RTM.csv) · [Detailed SDLC/HLD/LLD](../traceability/MX_QA_031_032_SDLC_2026-11-19.md) · [QA case plan](../qa/MX_QA_031_032_DEVICE_QA_2026-11-19.md) · [Master QA #3](https://github.com/T1Xperts/MailXperts-Android/issues/3) · [Release #11](https://github.com/T1Xperts/MailXperts-Android/issues/11).

## Verified CI evidence, 19 Nov 2026

- [Android workflow #44484269468](https://github.com/T1Xperts/MailXperts-Android/actions/runs/44484269468): **SUCCESS** — clean, lintDebug, testDebugUnitTest, assembleDebug; development APK artifact `MailXperts-v1.6.0-beta.7-debug-apk`.
- [P0 QA workflow #44484269483](https://github.com/T1Xperts/MailXperts-Android/actions/runs/44484269483): **SUCCESS** — P0 regression and debug/unsigned QA artifacts.
- **Not evidence:** no device UAT, no signed production APK, no private GitHub gateway deploy, no bug #38 fix.
