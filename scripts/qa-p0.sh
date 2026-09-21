#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "== MailXperts P0 QA remediation suite =="
echo "Issues: MX-QA-001,002,003,024,004,005,016,018,028"

export MX_ALLOW_UNSIGNED_RELEASE=true
./gradlew --no-daemon --stacktrace clean lintDebug testDebugUnitTest assembleDebug assembleRelease

DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

test -s "$DEBUG_APK"
test -s "$RELEASE_APK"

VERSION_NAME="$(sed -n "s/^[[:space:]]*versionName[[:space:]]*'\([^']*\)'.*/\1/p" app/build.gradle | head -1)"
VERSION_CODE="$(sed -n "s/^[[:space:]]*versionCode[[:space:]]*\([0-9][0-9]*\).*/\1/p" app/build.gradle | head -1)"
test -n "$VERSION_NAME"
test -n "$VERSION_CODE"

mkdir -p build/p0-qa
cat > build/p0-qa/P0-QA-RESULTS.md <<EOF
# MailXperts P0 automated QA evidence

Automated suite completed successfully.

| QA ID | Automated evidence |
|---|---|
| MX-QA-001 | AccountConfigTest verifies blank identity/password for a new account |
| MX-QA-002 | MimeMessageRendererTest + MessageWebViewContractTest verify remote HTTP/HTTPS image preservation and WebView network policy |
| MX-QA-003 | MimeMessageRendererTest + MailAttachmentRepositoryTest verify CID inline image resolution and classification |
| MX-QA-024 | MimeMessageRendererTest verifies multipart/alternative, multipart/related and multipart/mixed nesting |
| MX-QA-004 | ExternalLinkPolicyTest + MessageWebViewContractTest verify safe-scheme link handling |
| MX-QA-005 | MailAttachmentRepositoryTest verifies true incoming attachments remain discoverable and inline CID images are excluded |
| MX-QA-016 | RecipientNormalizerTest + OutgoingAttachmentMimeTest verify comma/semicolon multi-recipient handling |
| MX-QA-018 | OutgoingAttachmentMimeTest verifies multipart/mixed outgoing attachment construction |
| MX-QA-028 | ReleaseIdentityContractTest verifies production applicationId and current upgrade identity: ${VERSION_NAME} / versionCode ${VERSION_CODE} |

## Beta regression evidence

| QA area | Automated evidence |
|---|---|
| MX-QA-006 | InlineImageCidTest + SignatureHtmlTest verify safe signature HTML and conversion of editor data images to CID-ready MIME parts |
| MX-QA-012 | EmailHtmlPolicyTest verifies sender formatting is retained on a neutral light message canvas |
| MX-QA-017/027 | RecipientDirectoryTest + RecipientNormalizerTest verify recipient learning/ranking and robust address parsing |
| MX-QA-022/030 | GmailAppPasswordTest verifies Gmail App Password normalisation and validation |
| MX-QA-026 | MailSearchSpecTest verifies account/folder search modes, order-independent tokens and practical wildcard matching |

Full lintDebug, testDebugUnitTest, assembleDebug and assembleRelease also passed.

Device-only acceptance remains required for UX/network/provider-specific behaviour and in-place upgrade verification.
EOF

echo "P0 QA completed successfully."
