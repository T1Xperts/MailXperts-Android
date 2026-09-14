# GitHub production signing secret entry checklist

This repository is already configured to consume four GitHub Actions secrets for Android production signing. Because GitHub secrets are intentionally write-only and their API is not exposed through the connected automation, an authorized repository administrator must enter them once in the GitHub UI.

Go to:

**Repository → Settings → Secrets and variables → Actions → New repository secret**

Create exactly these four secrets:

1. `MX_KEYSTORE_BASE64`
   - Value: a single-line Base64 encoding of the permanent MailXperts PKCS12 keystore.
2. `MX_KEYSTORE_PASSWORD`
   - Value: the store password from the private MailXperts signing pack.
3. `MX_KEY_ALIAS`
   - Value: `mailxperts-release`
4. `MX_KEY_PASSWORD`
   - Value: the private-key password from the private MailXperts signing pack.

Do not paste these values into issues, pull requests, commits, wiki pages or release notes.

After all four are present:

1. Open **Actions → Build MailXperts Android**.
2. Choose **Run workflow** on `main`, or push a release tag matching the app version, e.g. `v1.5.2`.
3. Confirm the **Restore and verify permanent production signing key** step passes.
4. Confirm the signed APK/AAB build passes.
5. Confirm the APK signing fingerprint check passes.
6. Confirm the GitHub Release contains APK, AAB, SHA256SUMS, signing-certificate evidence and build metadata.

The workflow is fail-closed: a missing secret or wrong certificate prevents a production release.
