# MailXperts Build Expert

Server-side implementation foundation for **MX-QA-029**. This directory is deliberately separate from the Android application so AI-provider credentials never enter the APK.

## Implemented scope

- Provider-neutral orchestration through a provider registry.
- OpenAI first adapter using the **Responses API**.
- `store: false` on OpenAI requests from this service.
- Read-only task classes: planning, review, build-failure explanation, build summary and risk estimation.
- Repository/log context treated as untrusted data.
- Secret/private-key/bearer redaction before provider transmission.
- Per-task context size limits.
- Privacy-preserving audit JSONL with hashes and usage metadata, not raw prompt/context/output.
- Tool policy with read-only auto-allowed tools, human-gated low-risk writes and forbidden production/signing/secret operations.
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
- `BUILD_EXPERT_MAX_CONTEXT_CHARS` — optional context cap, hard-capped at 500,000 characters.
- `BUILD_EXPERT_AUDIT_FILE` — optional path for append-only JSONL audit evidence.

Do not place any of these values in Gradle, Android resources, BuildConfig, source control, issue text, build logs or the APK.

## Example read-only invocation

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

## Remaining MX-QA-029 phases

The foundation intentionally stops before autonomous mutation. Remaining work includes authenticated remote MCP/signed-service transport, isolated ephemeral coding runners, controlled draft branch/PR tooling, stronger usage/cost accounting and operational UAT. Production release/signing must remain human gated.
