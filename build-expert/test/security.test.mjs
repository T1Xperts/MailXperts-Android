import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, readFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { authorizeTool } from "../src/policy.mjs";
import { redact, limitContext } from "../src/redaction.mjs";
import { ToolGateway } from "../src/tool-gateway.mjs";
import { BuildExpertOrchestrator } from "../src/orchestrator.mjs";
import { OpenAIProvider } from "../src/providers/openai.mjs";

test("secret redaction removes bearer, named secrets, API keys and private keys", () => {
  const source = [
    "Authorization: Bearer super-secret-token",
    "password=NeverLogMe",
    "sk-abcdefghijklmnopqrstuvwxyz123456",
    "-----BEGIN PRIVATE KEY-----\nabc123\n-----END PRIVATE KEY-----"
  ].join("\n");
  const result = redact(source);
  assert.ok(result.redactions >= 4);
  assert.ok(!result.text.includes("super-secret-token"));
  assert.ok(!result.text.includes("NeverLogMe"));
  assert.ok(!result.text.includes("abcdefghijklmnopqrstuvwxyz123456"));
  assert.ok(!result.text.includes("abc123"));
});

test("context cap truncates oversized untrusted repository input", () => {
  const result = limitContext("x".repeat(5000), 1000);
  assert.equal(result.truncated, true);
  assert.ok(result.text.includes("TRUNCATED_BY_BUILD_EXPERT_POLICY"));
});

test("tool policy blocks production and secret operations", async () => {
  assert.equal(authorizeTool("repo.read").allowed, true);
  assert.equal(authorizeTool("pr.merge").allowed, false);
  assert.equal(authorizeTool("secret.read").allowed, false);
  assert.equal(authorizeTool("branch.create").reason, "approval_required");
  const gateway = new ToolGateway({ "repo.read": async () => ({ ok: true }) });
  assert.deepEqual(await gateway.call("repo.read", { path: "README.md" }), { ok: true });
  await assert.rejects(() => gateway.call("pr.merge", {}), /Tool denied/);
});

test("orchestrator redacts before provider call and writes hash-only audit evidence", async () => {
  const folder = await mkdtemp(join(tmpdir(), "mx-build-expert-"));
  const auditFile = join(folder, "audit.jsonl");
  const previous = process.env.BUILD_EXPERT_AUDIT_FILE;
  process.env.BUILD_EXPERT_AUDIT_FILE = auditFile;
  let received = "";
  const fake = {
    model: "fake-model",
    async execute(task, context) {
      received = `${task.prompt}\n${context}`;
      return { provider: "fake", model: "fake-model", text: "safe result", usage: { inputTokens: 1, outputTokens: 2 } };
    }
  };
  try {
    const orchestrator = new BuildExpertOrchestrator({ providers: { fake }, defaultProvider: "fake" });
    const result = await orchestrator.execute({
      id: "qa-1",
      type: "review",
      prompt: "Review password=top-secret",
      context: "Authorization: Bearer token-123",
      repository: "T1Xperts/MailXperts-Android",
      ref: "feature/test"
    });
    assert.ok(!received.includes("top-secret"));
    assert.ok(!received.includes("token-123"));
    assert.equal(result.text, "safe result");
    const audit = await readFile(auditFile, "utf8");
    assert.ok(audit.includes("inputHash"));
    assert.ok(!audit.includes("top-secret"));
    assert.ok(!audit.includes("token-123"));
  } finally {
    if (previous === undefined) delete process.env.BUILD_EXPERT_AUDIT_FILE;
    else process.env.BUILD_EXPERT_AUDIT_FILE = previous;
  }
});

test("OpenAI adapter uses Responses API, store false, and server-only bearer", async () => {
  let request;
  const fetchImpl = async (url, options) => {
    request = { url, options };
    return new Response(JSON.stringify({
      id: "resp_test",
      model: "gpt-test",
      output: [{ type: "message", content: [{ type: "output_text", text: "reviewed" }] }],
      usage: { input_tokens: 7, output_tokens: 3 }
    }), { status: 200, headers: { "content-type": "application/json" } });
  };
  const provider = new OpenAIProvider({ apiKey: "server-key-only", model: "gpt-test", fetchImpl });
  const result = await provider.execute({ type: "review", prompt: "Review" }, "context");
  assert.equal(request.url, "https://api.openai.com/v1/responses");
  const payload = JSON.parse(request.options.body);
  assert.equal(payload.store, false);
  assert.equal(payload.model, "gpt-test");
  assert.equal(request.options.headers.Authorization, "Bearer server-key-only");
  assert.equal(result.text, "reviewed");
});
