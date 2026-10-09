import { appendFile, mkdir } from "node:fs/promises";
import { dirname } from "node:path";
import { createHash } from "node:crypto";

export function sha256(value) {
  return createHash("sha256").update(String(value ?? ""), "utf8").digest("hex");
}

export async function appendAudit(event, file = process.env.BUILD_EXPERT_AUDIT_FILE) {
  if (!file) return;
  const safe = {
    timestamp: new Date().toISOString(),
    taskId: event.taskId ?? "",
    taskType: event.taskType ?? "",
    provider: event.provider ?? "",
    model: event.model ?? "",
    repository: event.repository ?? "",
    ref: event.ref ?? "",
    inputHash: event.inputHash ?? "",
    outputHash: event.outputHash ?? "",
    status: event.status ?? "",
    inputTokens: Number(event.inputTokens ?? 0),
    outputTokens: Number(event.outputTokens ?? 0),
    redactions: Number(event.redactions ?? 0),
    truncated: Boolean(event.truncated),
    errorCode: event.errorCode ?? ""
  };
  await mkdir(dirname(file), { recursive: true });
  await appendFile(file, JSON.stringify(safe) + "\n", { encoding: "utf8", mode: 0o600 });
}
