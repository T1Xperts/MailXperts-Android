# MailXperts Google Play release checklist

## Build identity

- Application ID: `au.com.t1xperts.mailxperts`
- Version: `1.4.1` / version code `8`
- Minimum Android: API 26
- Target Android: API 36
- Upload signing certificate SHA-256: `74:8E:11:11:95:28:4B:12:C9:9D:C3:A0:11:F7:58:AD:C1:B2:0A:ED:FE:DF:DD:EC:83:68:0D:5D:13:74:6E:EC`

## Before production submission

- Upload the signed `MailXperts-v1.4.1.aab` to an internal testing track first.
- Install Play-generated APKs on Android 8, 13, 15 and 16 test devices.
- Test adding at least two accounts, All Accounts Inbox/Sent, search, Drafts, Outbox, Scheduled, notifications and upgrade from v1.3.
- Publish a T1Xperts privacy-policy URL that explains email content, credentials, local storage, background sync and external abuse-report drafts.
- Complete the Play Console Data safety form from the final production behavior.
- Supply reviewer account-access instructions because core mail features require an email account.
- Complete content rating, app access, ads, target audience, contact details, store listing graphics and support URL.
- Enrol in Play App Signing and securely retain the permanent upload key outside GitHub.

## OAuth launch requirements

- Register T1Xperts-owned Google and Microsoft OAuth clients with the final package name and signing certificate.
- Configure approved redirect URIs and provider consent screens.
- Complete any required sensitive/restricted-scope verification and privacy-domain verification.
- Do not ship placeholder, shared or personal OAuth client credentials.
- Until Microsoft OAuth is configured, Outlook/Hotmail/MSN/Live setup remains visibly unavailable instead of accepting a password that the provider will reject.

## Repository release

- Add the keystore only as encrypted GitHub Actions secrets; never commit it.
- Run the signed-release workflow from a version tag.
- Verify both the APK certificate and AAB JAR signature before publishing.
