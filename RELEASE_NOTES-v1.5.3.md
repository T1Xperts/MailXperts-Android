# MailXperts v1.5.3

Production update build for the existing MailXperts installation.

## Fixes
- Preserve the production package identity `au.com.t1xperts.mailxperts` so the APK upgrades the existing app rather than installing a separate debug application.
- Render externally hosted images in HTML email bodies.
- Keep the existing MailXperts production signing identity for update compatibility.
- GitHub Android SDK setup updated to use current SDK packages.

## Android version
- versionName: 1.5.3
- versionCode: 13

This release is intended to install over prior production builds signed with the permanent MailXperts release certificate.
