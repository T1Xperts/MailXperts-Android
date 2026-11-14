export const SAFE_TASKS = new Set([
  "plan",
  "review",
  "explain_failure",
  "summarize_build",
  "estimate_risk"
]);

export const READ_ONLY_TOOLS = new Set([
  "repo.read",
  "issue.read",
  "logs.read",
  "build.status"
]);

export const HUMAN_APPROVAL_TOOLS = new Set([
  "build.start",
  "test.run",
  "branch.create",
  "patch.propose",
  "pr.create_draft"
]);

export const FORBIDDEN_TOOLS = new Set([
  "main.push",
  "pr.merge",
  "release.publish",
  "deploy.production",
  "signing.export_key",
  "secret.read",
  "secret.write",
  "security.disable"
]);

export function validateTask(task) {
  if (!task || typeof task !== "object") throw new Error("Task must be an object");
  if (!SAFE_TASKS.has(task.type)) throw new Error(`Task type is not enabled: ${task.type ?? "missing"}`);
  if (!task.id || typeof task.id !== "string") throw new Error("Task id is required");
  if (!task.prompt || typeof task.prompt !== "string") throw new Error("Task prompt is required");
  return task;
}

export function authorizeTool(name, { approved = false } = {}) {
  if (FORBIDDEN_TOOLS.has(name)) return { allowed: false, reason: "forbidden" };
  if (READ_ONLY_TOOLS.has(name)) return { allowed: true, reason: "read_only" };
  if (HUMAN_APPROVAL_TOOLS.has(name)) {
    return approved
      ? { allowed: true, reason: "human_approved" }
      : { allowed: false, reason: "approval_required" };
  }
  return { allowed: false, reason: "not_allowlisted" };
}
