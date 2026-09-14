#!/usr/bin/env bash
set -euo pipefail

EXPECTED="748E111195284B12C99DC3A011F758ADC1B20AEDFEDFDDEC83680D5D13746EEC"
KEYSTORE="${1:-MailXperts-v1.2-release.p12}"
ALIAS="${MX_KEY_ALIAS:-mailxperts-release}"

if [[ -z "${MX_KEYSTORE_PASSWORD:-}" ]]; then
  echo "Set MX_KEYSTORE_PASSWORD in your local shell before running this helper." >&2
  exit 2
fi

actual=$(LC_ALL=C keytool -list -v \
  -storetype PKCS12 \
  -keystore "$KEYSTORE" \
  -storepass "$MX_KEYSTORE_PASSWORD" \
  -alias "$ALIAS" \
  | awk -F': ' '/SHA256:/{gsub(":", "", $2); print toupper($2); exit}')

if [[ -z "$actual" ]]; then
  echo "Could not read a SHA-256 certificate fingerprint." >&2
  exit 3
fi

if [[ "$actual" != "$EXPECTED" ]]; then
  echo "ERROR: This is not the permanent MailXperts production signing certificate." >&2
  echo "Expected: $EXPECTED" >&2
  echo "Actual:   $actual" >&2
  exit 4
fi

echo "MailXperts production signing certificate verified."
