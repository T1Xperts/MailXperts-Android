import http from "node:http";
import { timingSafeEqual } from "node:crypto";
import { RequestAuthenticator } from "./auth.mjs";
import { ToolGateway } from "./tool-gateway.mjs";
import { createReadOnlyHandlers } from "./read-only-handlers.mjs";
import { BuildExpertOrchestrator } from "./orchestrator.mjs";
import { OpenAIProvider } from "./providers/openai.mjs";
import { MailAssistant } from "./mail-assistant.mjs";
import { redact } from "./redaction.mjs";

const MAX_BODY_BYTES = 256000;

export function createServer({ orchestrator, gateway, authenticator, mailAssistant, mailToken } = {}) {
  const auth = authenticator || new RequestAuthenticator();
  const tools = gateway || new ToolGateway(createReadOnlyHandlers());
  const ai = orchestrator || new BuildExpertOrchestrator({
    providers: { openai: new OpenAIProvider() }
  });
  let mail = mailAssistant || null;
  const expectedMailToken = String(mailToken || process.env.MAILXPERTS_AI_CLIENT_TOKEN || "");

  return http.createServer(async (req, res) => {
    if (req.method === "GET" && req.url === "/health") {
      return json(res, 200, {
        ok: true,
        service: "mailxperts-build-expert",
        mail_ai_configured: expectedMailToken.length >= 16
      });
    }

    if (req.method === "POST" && req.url === "/v1/mail/tasks") {
      try {
        const body = await readBody(req);
        if (expectedMailToken.length < 16) {
          return json(res, 503, { error: "ai_gateway_not_configured", message: "MailXperts AI gateway is not configured." });
        }
        if (!verifyMailBearer(req.headers.authorization, expectedMailToken)) {
          return json(res, 401, { error: "unauthorized", message: "MailXperts AI gateway authorization failed." });
        }
        const payload = JSON.parse(body || "{}");
        if (!mail) mail = new MailAssistant();
        const result = await mail.execute(payload);
        return json(res, 200, result);
      } catch (error) {
        const code = error?.code || "AI_REQUEST_FAILED";
        const status = code === "INPUT_TOO_LARGE" ? 413
          : code === "INVALID_AI_TASK" ? 400
          : code.startsWith("HTTP_") || code === "TIMEOUT" ? 502 : 400;
        return json(res, status, { error: code, message: safe(error) });
      }
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

export function verifyMailBearer(header, expectedToken) {
  const raw = String(header || "");
  if (!raw.startsWith("Bearer ")) return false;
  const supplied = raw.slice(7);
  const expected = String(expectedToken || "");
  if (supplied.length !== expected.length || expected.length < 16) return false;
  return timingSafeEqual(Buffer.from(supplied, "utf8"), Buffer.from(expected, "utf8"));
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
  return redact(String(message)).text.slice(0, 500);
}
