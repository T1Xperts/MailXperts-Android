# MailXperts Build Expert

Server-side implementation foundation for **MX-QA-029**. This directory is deliberately separate from the Android application so AI-provider credentials never enter the APK.

## Implemented scope

- Provider-neutral orchestration through a provider registry.
- OpenAI first adapter using the **Responses API**.
- `store: false` on OpenAI requests from this service.
- Read-only task classes: planning, review, build-failure explanation, build summary and risk estimation.
- Repository/log context treated as untrusted data.
- Secret/private-key/bearer redaction before provider transmission and before API error responses.
- Per-task context limits and OpenAI output-token caps.
- Privacy-preserving audit JSONL with hashes and usage metadata, not raw prompt/context/output.
- Tool policy with read-only auto-allowed tools, human-gated low-risk writes and forbidden production/signing/secret operations.
- Signed HMAC service API with timestamp validation and nonce replay protection.
- Bounded read-only repo/log/build-status handlers with lexical and resolved-path traversal protection.
- CI security tests.

## Deliberate safety boundary

This phase does **not** autonomously:

- push to protected `main`;
- merge pull requests;
- publish releases or deploy production;
- access/export signing keys;
- read or modify secret stores;
- disable security controls.

Those actions are explicitly denied by `src/policy.mjs`.

## Runtime configuration

Set these on the trusted server/runner only:

- `OPENAI_API_KEY` — required for the OpenAI adapter.
- `OPENAI_MODEL` — optional, defaults to `gpt-6.1-sol`.
- `OPENAI_TIMEOUT_MS` — optional request timeout, capped at 180 seconds.
- `OPENAI_MAX_OUTPUT_TOKENS` — optional per-task response budget, capped at 12,000 tokens.
- `BUILD_EXPERT_MAX_CONTEXT_CHARS` — optional context cap, hard-capped at 500,000 characters.
- `BUILD_EXPERT_AUDIT_FILE` — optional path for append-only JSONL audit evidence.
- `BUILD_EXPERT_GATEWAY_SECRET` — required for the service API, at least 32 characters.
- `BUILD_EXPERT_REPO_ROOT` — allow-listed local root for `repo.read`.
- `BUILD_EXPERT_LOG_ROOT` — allow-listed local root for `logs.read`.
- `BUILD_EXPERT_STATUS_FILE` — optional build-status JSON file for `build.status`.
- `BUILD_EXPERT_HOST` / `BUILD_EXPERT_PORT` — service bind configuration; host defaults to `127.0.0.1`.

Do not place any of these values in Gradle, Android resources, BuildConfig, source control, issue text, build logs or the APK.

## CLI read-only invocation

```bash
cat task.json | OPENAI_API_KEY='runtime-secret' npm start
```

Example task:

```json
{
  "id": "MX-QA-029-review-001",
  "type": "review",
  "repository": "T1Xperts/MailXperts-Android",
  "ref": "feature/example",
  "prompt": "Review this change for correctness and security.",
  "context": "<approved, minimum-necessary repository context>"
}
```

## Signed service API

Run with `npm run serve`. All POST requests to `/v1/tasks` and `/v1/tools/call` require:

- `X-MX-Timestamp` — Unix seconds;
- `X-MX-Nonce` — unique request nonce;
- `X-MX-Signature` — HMAC-SHA256 of `timestamp + newline + nonce + newline + METHOD + newline + path + newline + raw body` using `BUILD_EXPERT_GATEWAY_SECRET`.

The service rejects expired timestamps, replayed nonces, invalid signatures, oversized bodies, lexical/symlink path traversal and any non-allow-listed tool. `/health` contains no secrets and is the only unsigned endpoint.

The built-in HTTP listener defaults to **localhost only**. If the service is exposed beyond the local/private runner boundary, place it behind authenticated private connectivity or a TLS-terminating reverse proxy. Do not expose the plain listener directly to the public Internet.

## Remaining MX-QA-029 phases

The foundation intentionally stops before autonomous mutation. Remaining work includes a standards-compliant remote MCP transport if desired, isolated ephemeral coding runners, implemented human-approved draft branch/patch/PR handlers, broader provider failover/circuit breaking, full spend alerts/accounting and operational UAT. Production release/signing must remain human gated.
