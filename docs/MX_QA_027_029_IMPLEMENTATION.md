# MX-QA-027 + MX-QA-029 implementation record

## Scope

This branch implements the remaining direct cloud-contact foundation for **MX-QA-027** and a production-safe Build Expert foundation for **MX-QA-029**. Android candidate identity is **v1.6.0-beta.8 / versionCode 22**. Production release/signing remains outside autonomous AI capabilities.

## MX-QA-027 — Smart Contacts direct provider integration

Implemented:

- Direct Google Contacts OAuth consent separated from Gmail mail OAuth.
- Google People API pull with pagination and incremental sync token handling.
- Full-sync fallback when an incremental sync token expires.
- Google contact create/update support only for contacts the user explicitly selects for cloud sync.
- Direct standards-based CardDAV connector suitable for Apple/iCloud and other compatible providers.
- CardDAV principal/home/address-book discovery, contact REPORT, vCard parsing and conditional PUT/DELETE primitives.
- iCloud/CardDAV username, app-specific password and endpoint stored with Android Keystore-backed AES/GCM encryption.
- Sync modes: Disconnected, Read only, Selected push, and Two-way selected.
- Pull-first reconciliation into the existing local Smart Contacts index.
- Provider remote-ID/ETag mapping and Google sync-token persistence.
- Smart Contacts long-press controls to select/remove an individual contact for Google or iCloud push.
- Dedicated Cloud Contacts settings/sync screen.
- Disconnect preserves the local Smart Contacts history.
- Automated mode/scope/security/privacy contracts.

Privacy boundary:

- Learned MailXperts contacts remain local by default.
- Enabling a cloud provider does not bulk-upload the learned address book.
- Provider writes require a write-capable mode **and** explicit per-contact selection.
- Google access credentials are short-lived Google Identity credentials and are not persisted by the new contact connector.
- CardDAV secrets are encrypted using Android Keystore-backed AES/GCM.

External/device acceptance still required:

- Google Cloud project must have the People API enabled and the Android OAuth registration/consent configuration must allow the requested Contacts scopes.
- Device UAT must confirm read-only and selected-write Google flows.
- iCloud UAT requires a real Apple account/app-specific password and must verify server discovery and conflict behaviour.
- Multi-account/multi-source reconciliation should be device-tested before MX-QA-027 is finally closed.

## MX-QA-029 — Build Expert / AI orchestration

Implemented production-safe foundation:

- Separate `build-expert/` server-side service, not Android code.
- Provider-neutral orchestrator with provider registry.
- OpenAI first adapter using the Responses API.
- Provider credential supplied only by server runtime environment.
- `store: false` on OpenAI requests.
- Read-only task classes for planning, review, build-failure explanation, build summaries and risk estimation.
- Repository/issues/logs treated as untrusted data and labelled as such in provider instructions.
- Secret, private-key and bearer-token redaction before provider transmission.
- Per-task context cap and output-token cap.
- Privacy-preserving JSONL audit evidence using hashes and usage metadata rather than raw prompt/context/output.
- Tool policy with:
  - auto-allowed read-only tools;
  - human-approval-required low-risk write tools;
  - hard-forbidden merge, production deploy/release, signing-key and secret operations.
- Tool gateway enforcing policy and payload limits.
- Signed HMAC service API with timestamp checks and nonce replay protection for `/v1/tasks` and `/v1/tools/call`.
- Read-only repository/log/build-status handlers with configured roots, size caps and path-traversal protection.
- Automated tests for redaction, context limits, tool denial, audit privacy, Responses API request shape, request signing/replay and path traversal.
- Dedicated GitHub Actions security workflow.

Still deliberately staged within MX-QA-029:

- standards-compliant remote MCP transport if required in addition to the signed service API;
- ephemeral isolated coding runner;
- implemented human-approved draft branch/patch/PR write handlers;
- provider failover/circuit breaker beyond bounded timeout;
- full operational cost accounting and spend alerts;
- controlled release-preparation orchestration;
- security review and operational UAT.

Those later phases must preserve the existing rule that production merge, release, deployment, signing-key operations and secret changes remain human-controlled.

## Change control

The combined PR remains **draft** until beta.8 Android CI, P0 regression and Build Expert security CI pass. Because MX-QA-029 is P1 and introduces an external AI integration surface, it must receive human review before merge to `main`.
