import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { RequestAuthenticator, sign } from "../src/auth.mjs";
import { createReadOnlyHandlers } from "../src/read-only-handlers.mjs";
import { ToolGateway } from "../src/tool-gateway.mjs";

test("signed gateway authentication rejects tampering and replay", () => {
  const secret = "0123456789abcdef0123456789abcdef";
  const auth = new RequestAuthenticator({ secret, maxSkewSeconds: 300 });
  const timestamp = Math.floor(Date.now() / 1000);
  const request = { timestamp, nonce: "nonce-123456789", method: "POST", path: "/v1/tools/call", body: "{}" };
  const signature = sign(secret, request);
  assert.equal(auth.verify({ ...request, signature }).ok, true);
  assert.equal(auth.verify({ ...request, signature }).reason, "replay");

  const other = { ...request, nonce: "nonce-987654321", body: "{\"tampered\":true}" };
  assert.equal(auth.verify({ ...other, signature }).reason, "invalid_signature");
});

test("read-only repo handler blocks path traversal", async () => {
  const root = await mkdtemp(join(tmpdir(), "mx-repo-"));
  await writeFile(join(root, "README.md"), "safe", "utf8");
  const gateway = new ToolGateway(createReadOnlyHandlers({ repoRoot: root, logRoot: root }));
  const allowed = await gateway.call("repo.read", { path: "README.md" });
  assert.equal(allowed.content, "safe");
  await assert.rejects(() => gateway.call("repo.read", { path: "../escape.txt" }), error => error.code === "PATH_TRAVERSAL");
});

test("remote tool surface still denies any write or production action", async () => {
  const gateway = new ToolGateway({});
  await assert.rejects(() => gateway.call("branch.create", { name: "x" }), error => error.code === "APPROVAL_REQUIRED");
  await assert.rejects(() => gateway.call("release.publish", {}), error => error.code === "FORBIDDEN");
  await assert.rejects(() => gateway.call("signing.export_key", {}), error => error.code === "FORBIDDEN");
});
