# MailXperts v1.6.0-beta.7-hotfix.1 — QA Candidate Release Notes (19 Nov 2026)

**Build identity:** versionCode 25; debug test application `au.com.t1xperts.mailxperts.dev`.
**Base:** GitHub `main` v1.6.0-beta.7 plus diagnostics candidate PR #40; the separate beta.10 code candidate is not included by inference.

## Fix: MX-QA-031 reply / reply all blank screen
- Move the quoted HTML conversation out of Android Intent extras into a bounded temporary private file and pass a token instead.
- Prepare long quoted threads on a worker thread; display a recoverable prompt if quote cannot be carried safely.
- Filter global Outlook/Word stylesheet blocks from quoted history while preserving inline message formatting and the chain.
- Include app-owned diagnostics breadcrumbs and local report export from MX-QA-032.

## Verification
- Android build, lint and JUnit: [PASS](https://github.com/T1Xperts/MailXperts-Android/actions/runs/44487973125)
- P0 QA and APK workflow: [PASS](https://github.com/T1Xperts/MailXperts-Android/actions/runs/44487973593)
- APK SHA-256: `8388d711b876db70c99261ec4906321a09aabf45db043a60ea4773d7bc92e525`
- Product Owner physical-device Reply/Reply All UAT: NOT RUN
- Production signing: NOT AVAILABLE/NOT RUN; existing production app is unaffected
- Automatic private GitHub diagnostic synchronization: NOT IMPLEMENTED; local logging/export only

## Test installation
- The signed debug APK installs as `au.com.t1xperts.mailxperts.dev`, separate from production. Existing production signing identity is not used.
- Keep existing production app and accounts intact; install debug app alongside and configure a test email account. Reopen the original long email and test Reply/Reply All and Forward; verify composer, recipients and quoted history.
- If the problem recurs, export diagnostics from the test app and capture ADB Logcat if available; review/redact sensitive content before submitting it to the private development team.

**Release decision:** NO-GO for production until physical-device UAT, relevant privacy review and appropriate production signing/approval. This source-code candidate does not prove the original device failure is fixed.
