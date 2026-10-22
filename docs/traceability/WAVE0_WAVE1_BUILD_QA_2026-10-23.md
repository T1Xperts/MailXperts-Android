# MailXperts Wave 0 + Wave 1: Build and acceptance status

**Verified:** 23 October 2026 (Australia/Melbourne). **Scope:** optimised seven-wave WBS; Wave 0 (5 requirements) and Wave 1 (7 requirements). **Decision:** QA APK available, **full Wave 0 / Wave 1 closure is NO-GO**.

## Build deliverable

- **QA candidate:** MailXperts **v1.6.0-beta.10**, versionCode **24**, `applicationId=au.com.t1xperts.mailxperts`.
- **Code:** [PR #30 AI MVP](https://github.com/T1Xperts/MailXperts-Android/pull/30) stacked on [PR #27 platform](https://github.com/T1Xperts/MailXperts-Android/pull/27), with [PR #33 security + opt-in privacy](https://github.com/T1Xperts/MailXperts-Android/pull/33). All remain **draft/unmerged**. Repository `main` still uses older app version.
- **Production-identity QA signing (offline, private):** user-provided historical PKCS12 certificate, SHA-256 fingerprint `748E111195284B12C99DC3A011F758ADC1B20AEDFEDFDDEC83680D5D13746EEC`, verified identical to permanent MailXperts lineage; modern APK signatures v2 and v3 verified.
- **Signed QA APK SHA-256:** `3be630580f9bbc281879fc605c6d107b84e6b7d3716f35c32cc99440a946d9cf`. Size 3,198,976 bytes.
- **No release publication:** Do not treat the signed QA preview as released/approved. No private signing key or password was committed to GitHub.

## Verified automated QA evidence

| Test/evidence | GitHub run | Result |
|---|---|---|
| Android `clean lintDebug testDebugUnitTest assembleDebug` | [39790646040](https://github.com/T1Xperts/MailXperts-Android/actions/runs/39790646040) | PASS |
| P0 regression, debug + unsigned production-identity APK | [39790645770](https://github.com/T1Xperts/MailXperts-Android/actions/runs/39790645770) | PASS |
| Node Build Expert/security tests | [39790645815](https://github.com/T1Xperts/MailXperts-Android/actions/runs/39790645815) | PASS |
| CI P0 artifact | [Artifact 12431106914](https://github.com/T1Xperts/MailXperts-Android/actions/runs/39790645770) | Available while retained |
| JUnit XML evidence from artifact | Run 39790645770 | **112 tests, 0 failures, 0 errors, 0 skipped**, 32 test suites |
| Cloud AI opt-in, revoke and request-block source contracts | `AiConsentContractTest` in run 39790645770 | **3 included JUnit tests PASS** |

**Important:** The 114 previously proposed functional/security/UAT cases in [QA plan](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mailxperts-wave0-sdlc-rtm-20261021/docs/traceability/QA_CASES_WAVES.json) are **NOT individually executed**. Their status remains planned, and generic green CI is not evidence of their case-level acceptance.

## Wave 0: Readiness and independent foundations (5 requirements)

| ID | Feature | HLD / LLD | Build | Automated QA | Live/device UAT |
|---|---|---|---|---|---|
| MX-QA-029 | Build Expert platform | Draft, unapproved | **Partial** read-only/gateway foundation on PR #27; runner, approved writes, provider resilience/spend controls remain | Security CI PASS for foundation only | Pending |
| MX-QA-030 | Gmail / Microsoft OAuth2/XOAUTH2 | Existing auth architecture + draft wave outline | Code complete/reopened Gmail regression | Contract CI PASS | **Google/Gmail physical-device test pending** |
| AI-035 | Privacy / consent / on-device options | Draft, unapproved | **Partial** Cloud AI explicit opt-in (default off), revoke, client-side no-send gate on PR #33; per-account controls and on-device model not complete | 3 static contract JUnit tests PASS | **Privacy disclosure/consent device UAT pending** |
| AI-022 | Mail classification | Draft, unapproved | **NOT STARTED** | NOT RUN | NOT STARTED |
| AI-033 | Mailbox analytics | Draft, unapproved | **NOT STARTED** | NOT RUN | NOT STARTED |

**Wave 0 gate:** Not complete (two independent product features remain unbuilt, platform scope incomplete and OAuth/device consent acceptance missing).

## Wave 1: AI MVP (7 requirements)

| IDs | HLD / LLD | Build | Automated QA | Live/device UAT | Release |
|---|---|---|---|---|---|
| AI-001..AI-007: summary, contextual reply, Ask AI, natural search, priority, action extraction, rewrite | Draft, unapproved | **Code complete** candidate in PR #30 + PR #33 | **Aggregate Android/P0/Node CI PASS** | **Hosted AI gateway, provider credentials/model, real responses and on-device acceptance pending** | **Signed QA preview only; PRs unmerged** |

**Wave 1 gate:** Not complete. The code exists, but there is no evidence of live gateway/API account integration or physical-device Product Owner UAT for the seven new user-facing features.

## Exact external prerequisites / remaining work

1. Configure a secure HTTPS MailXperts AI gateway with a selected/available model, **server-only** `OPENAI_API_KEY`, and `MAILXPERTS_AI_CLIENT_TOKEN`. Do not paste secrets into GitHub or the APK.
2. Validate selected-account mail AI calls with a controlled test mailbox, consent disabled/enabled/revoked and genuine provider responses.
3. Validate Google/Gmail `https://mail.google.com/` OAuth scope, IMAP folder listing, SMTP send and token refresh on an Android device using an authorized account.
4. Run AI-001–007 user-level test cases, including contextual reply that never sends on its own and prompt-injected mail that cannot change trusted instructions.
5. Complete AI-022 classification and AI-033 analytics code, component tests and real-device acceptance before Wave 0 closure.
6. Complete MX-QA-029 remaining safe Build Expert phases and AI-035 full per-account privacy/on-device alternatives or explicitly approve reduced Wave 0 scope.
7. Obtain technical HLD/LLD review, independent human PR review, merge-chain validation and Product Owner UAT sign-off before any production release.

## Cross-reference

- [44-column master RTM](RTM.csv)
- [Seven-wave WBS/dependency plan](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mailxperts-wave0-sdlc-rtm-20261021/docs/traceability/WORK_BREAKDOWN_DEPENDENCIES.json)
- [Architecture HLD/LLD drafts](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mailxperts-wave0-sdlc-rtm-20261021/docs/architecture/MAILXPERTS_WAVES_HLD_LLD.md)
- [Release notes beta.10](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mailxperts-wave1-ai-gateway-hardening-20261021/RELEASE-NOTES-v1.6.0-beta.10.md)
- [Master QA register](https://github.com/T1Xperts/MailXperts-Android/issues/3), [AI epic](https://github.com/T1Xperts/MailXperts-Android/issues/29), [release register](https://github.com/T1Xperts/MailXperts-Android/issues/11)
