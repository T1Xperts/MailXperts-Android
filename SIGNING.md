# Permanent signing baseline

The production package is `au.com.t1xperts.mailxperts`. The release build deliberately fails unless all four signing environment variables are present.

Required GitHub Actions secrets:

| Secret | Value |
| --- | --- |
| `MX_KEYSTORE_BASE64` | Base64 encoding of `MailXperts-v1.2-release.p12` as one line |
| `MX_KEYSTORE_PASSWORD` | Password contained in the private signing pack |
| `MX_KEY_ALIAS` | `mailxperts-release` |
| `MX_KEY_PASSWORD` | Password contained in the private signing pack |

Linux/macOS base64 command:

```bash
base64 < MailXperts-v1.2-release.p12 | tr -d '\n'
```

The keystore is not part of the source repository. Keep at least two encrypted backups under separate control. Losing it would force another uninstall/reinstall; exposing it would allow an attacker to sign a malicious update.

The expected production certificate SHA-256 fingerprint is:

`74:8E:11:11:95:28:4B:12:C9:9D:C3:A0:11:F7:58:AD:C1:B2:0A:ED:FE:DF:DD:EC:83:68:0D:5D:13:74:6E:EC`

For every future release, preserve the application ID, use this same keystore and increase `versionCode`.
