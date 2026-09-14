# MailXperts Android production signing

## Permanent signing identity

MailXperts production releases must continue to use the existing permanent PKCS12 signing key established with v1.2. Do **not** generate a new key for each version.

Expected production certificate SHA-256:

`74:8E:11:11:95:28:4B:12:C9:9D:C3:A0:11:F7:58:AD:C1:B2:0A:ED:FE:DF:DD:EC:83:68:0D:5D:13:74:6E:EC`

The private keystore and passwords must never be committed to this repository, attached to issues, or placed in release assets.

## Required GitHub Actions secrets

Configure these secrets in **Settings → Secrets and variables → Actions**:

- `MX_KEYSTORE_BASE64` — Base64 of the permanent PKCS12 keystore.
- `MX_KEYSTORE_PASSWORD` — PKCS12 store password.
- `MX_KEY_ALIAS` — permanent release-key alias.
- `MX_KEY_PASSWORD` — private-key password.

The workflow fails closed if any value is missing or if the restored keystore certificate fingerprint does not match the permanent MailXperts production certificate.

## Creating MX_KEYSTORE_BASE64 locally

Linux/macOS:

```bash
base64 < MailXperts-v1.2-release.p12 | tr -d '\n'
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("MailXperts-v1.2-release.p12"))
```

Paste the resulting single-line Base64 text into the GitHub secret. Do not save the Base64 value in the repository.

## Release behaviour

A production release may be started by:

1. pushing a `v*` tag, or
2. manually running **Build MailXperts Android** from GitHub Actions.

The workflow:

1. restores the PKCS12 file only inside the ephemeral GitHub runner;
2. validates alias/password and the permanent certificate SHA-256;
3. runs release lint and unit tests;
4. builds signed APK and AAB artifacts;
5. verifies the APK/AAB signatures, package, version and signing certificate;
6. generates SHA-256 checksums and signing-certificate evidence;
7. uploads the signed artifacts; and
8. creates or updates the matching GitHub Release.

If production signing is not configured, the production job must fail. It must never publish an unsigned APK as a production release.

## Key management

Keep at least two encrypted offline backups of the permanent signing pack under separate administrative control. Any future signing-key rotation must be an explicit Android key-rotation project, not a routine release action.
