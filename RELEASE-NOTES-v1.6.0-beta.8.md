# MailXperts v1.6.0-beta.8

## Focus
MX-QA-027 direct cloud-contact integration and MX-QA-029 server-side Build Expert security foundation.

## Smart Contacts / MX-QA-027
- Adds explicit Google Contacts OAuth separate from Gmail mailbox OAuth.
- Adds Google People API pull, pagination and incremental-sync support with expired-token full-sync fallback.
- Adds direct iCloud/CardDAV discovery and contact synchronization primitives.
- Adds Cloud Contacts settings with Disconnected, Read only, Selected push and Two-way selected modes.
- Adds per-contact Google/iCloud sync selection from Smart Contacts.
- Never bulk-uploads learned contacts automatically; cloud writes require an explicit write mode plus per-contact selection.
- Stores CardDAV credentials using Android Keystore-backed AES/GCM encryption.
- Disconnecting a provider preserves local Smart Contacts.

## Build Expert / MX-QA-029
- Adds a separate server-side `build-expert/` service. AI provider secrets are not embedded in the Android APK.
- Adds a provider-neutral orchestrator with OpenAI first through the Responses API.
- Adds `store:false`, input/output budgets, secret redaction and hash-only audit evidence.
- Adds signed HMAC API authentication with timestamp and nonce replay protection.
- Adds bounded read-only repo/log/build-status tools with path-traversal protection.
- Hard-blocks autonomous merge, production release/deployment, signing-key and secret operations.
- Adds dedicated Build Expert security CI.

## Release identity
- versionName: `1.6.0-beta.8`
- versionCode: `22`
- package: `au.com.t1xperts.mailxperts`

## QA gates
- Android P0 regression and APK identity/build validation must pass for beta.8.
- Build Expert security tests must pass.
- Google Contacts and iCloud/CardDAV still require physical-device/provider UAT before MX-QA-027 closure.
- MX-QA-029 remains a staged platform implementation: autonomous coding/release remains deliberately disabled and later runner/PR automation requires separate security/UAT gates.
