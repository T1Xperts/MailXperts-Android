import http from "node:http";
import { RequestAuthenticator } from "./auth.mjs";
import { ToolGateway } from "./tool-gateway.mjs";
import { createReadOnlyHandlers } from "./read-only-handlers.mjs";
import { BuildExpertOrchestrator } from "./orchestrator.mjs";
import { OpenAIProvider } from "./providers/openai.mjs";

const MAX_BODY_BYTES = 256000;

export function createServer({ orchestrator, gateway, authenticator } = {}) {
  const auth = authenticator || new RequestAuthenticator();
  const tools = gateway || new ToolGateway(createReadOnlyHandlers());
  const ai = orchestrator || new BuildExpertOrchestrator({
    providers: { openai: new OpenAIProvider() }
  });

  return http.createServer(async (req, res) => {
    if (req.method === "GET" && req.url === "/health") {
      return json(res, 200, { ok: true, service: "mailxperts-build-expert" });
    }
    if (req.method !== "POST" || (req.url !== "/v1/tasks" && req.url !== "/v1/tools/call")) {
      return json(res, 404, { error: "not_found" });
    }

    try {
      const body = await readBody(req);
      const verified = auth.verify({
        timestamp: req.headers["x-mx-timestamp"],
        nonce: req.headers["x-mx-nonce"],
        signature: req.headers["x-mx-signature"],
        method: req.method,
        path: req.url,
        body
      });
      if (!verified.ok) return json(res, 401, { error: "unauthorized", reason: verified.reason });
      const payload = JSON.parse(body || "{}");

      if (req.url === "/v1/tasks") {
        const result = await ai.execute(payload);
        return json(res, 200, result);
      }

      const result = await tools.call(payload.name, payload.input || {}, { approved: false });
      return json(res, 200, { name: payload.name, result });
    } catch (error) {
      const code = error?.code || "REQUEST_FAILED";
      const status = code === "INPUT_TOO_LARGE" ? 413 : code === "APPROVAL_REQUIRED" || code === "FORBIDDEN" ? 403 : 400;
      return json(res, status, { error: code, message: safe(error) });
    }
  });
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const port = Number(process.env.BUILD_EXPERT_PORT || 8787);
  const host = process.env.BUILD_EXPERT_HOST || "127.0.0.1";
  createServer().listen(port, host, () => {
    process.stdout.write(`MailXperts Build Expert listening on ${host}:${port}\n`);
  });
}

async function readBody(req) {
  const chunks = [];
  let bytes = 0;
  for await (const chunk of req) {
    bytes += chunk.length;
    if (bytes > MAX_BODY_BYTES) {
      const error = new Error("Request body exceeds policy limit");
      error.code = "INPUT_TOO_LARGE";
      throw error;
    }
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString("utf8");
}

function json(res, status, payload) {
  const text = JSON.stringify(payload);
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff"
  });
  res.end(text);
}

function safe(error) {
  const message = error?.message || "Request failed";
  return String(message).replace(/sk-[A-Za-z0-9_-]{12,}/g, "[REDACTED]").slice(0, 500);
}
