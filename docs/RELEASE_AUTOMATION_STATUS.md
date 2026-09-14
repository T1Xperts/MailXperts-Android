# Production release automation status

The MailXperts GitHub Actions production path is configured to:

- derive versionName/versionCode from `app/build.gradle`;
- require all four signing secrets;
- restore the permanent PKCS12 keystore only inside the ephemeral GitHub runner;
- verify the keystore alias/password and permanent certificate SHA-256 before building;
- run release lint and unit tests;
- build signed APK and AAB artifacts;
- verify the APK certificate fingerprint after signing;
- verify the AAB signature;
- verify package/version metadata;
- generate APK/AAB SHA-256 checksums, signing-certificate evidence and build metadata;
- upload the signed release artifact; and
- create/update the matching GitHub Release.

The workflow now fails closed if secrets are missing or if the wrong signing certificate is supplied. No unsigned production fallback is permitted in the production release job.

The only remaining administrative prerequisite is one-time entry of the four GitHub Actions secrets by an authorized repository administrator. See `docs/GITHUB_SECRET_ENTRY_CHECKLIST.md`.
