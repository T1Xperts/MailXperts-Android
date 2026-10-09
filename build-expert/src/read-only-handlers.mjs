import { readFile, realpath } from "node:fs/promises";
import { resolve, sep } from "node:path";

const MAX_READ_CHARS = 200000;

export function createReadOnlyHandlers({
  repoRoot = process.env.BUILD_EXPERT_REPO_ROOT,
  logRoot = process.env.BUILD_EXPERT_LOG_ROOT,
  statusFile = process.env.BUILD_EXPERT_STATUS_FILE
} = {}) {
  return {
    "repo.read": async ({ path }) => readBounded(repoRoot, path),
    "logs.read": async ({ path }) => readBounded(logRoot, path),
    "build.status": async () => {
      if (!statusFile) throw coded("BUILD_EXPERT_STATUS_FILE is not configured", "NOT_CONFIGURED");
      const text = await readFile(resolve(statusFile), "utf8");
      if (text.length > MAX_READ_CHARS) throw coded("Status file exceeds policy limit", "OUTPUT_TOO_LARGE");
      return JSON.parse(text);
    }
  };
}

async function readBounded(root, requestedPath) {
  if (!root) throw coded("Read root is not configured", "NOT_CONFIGURED");
  const lexicalBase = resolve(root);
  const lexicalTarget = resolve(lexicalBase, String(requestedPath || ""));
  if (lexicalTarget !== lexicalBase && !lexicalTarget.startsWith(lexicalBase + sep)) {
    throw coded("Path escapes configured read root", "PATH_TRAVERSAL");
  }
  const base = await realpath(lexicalBase);
  const target = await realpath(lexicalTarget);
  if (target !== base && !target.startsWith(base + sep)) {
    throw coded("Resolved path escapes configured read root", "PATH_TRAVERSAL");
  }
  const text = await readFile(target, "utf8");
  if (text.length > MAX_READ_CHARS) throw coded("File exceeds policy limit", "OUTPUT_TOO_LARGE");
  return { path: requestedPath, content: text };
}

function coded(message, code) {
  const error = new Error(message);
  error.code = code;
  return error;
}
