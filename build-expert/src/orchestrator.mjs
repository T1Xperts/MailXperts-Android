import { appendAudit, sha256 } from "./audit.mjs";
import { validateTask } from "./policy.mjs";
import { limitContext, redact } from "./redaction.mjs";

export class BuildExpertOrchestrator {
  constructor({ providers = {}, defaultProvider = "openai", maxContextChars } = {}) {
    this.providers = new Map(Object.entries(providers));
    this.defaultProvider = defaultProvider;
    this.maxContextChars = Number(maxContextChars || process.env.BUILD_EXPERT_MAX_CONTEXT_CHARS || 120000);
  }

  async execute(rawTask) {
    const task = validateTask(rawTask);
    const providerName = task.provider || this.defaultProvider;
    const provider = this.providers.get(providerName);
    if (!provider || typeof provider.execute !== "function") {
      throw new Error(`Provider is not configured: ${providerName}`);
    }

    const contextRedaction = redact(task.context ?? "");
    const promptRedaction = redact(task.prompt);
    const limited = limitContext(contextRedaction.text, this.maxContextChars);
    const safeTask = {
      ...task,
      prompt: promptRedaction.text,
      context: undefined
    };
    const inputFingerprint = sha256(`${safeTask.prompt}\n${limited.text}`);

    try {
      const result = await provider.execute(safeTask, limited.text);
      const safeOutput = redact(result.text ?? "");
      await appendAudit({
        taskId: task.id,
        taskType: task.type,
        provider: result.provider || providerName,
        model: result.model || "",
        repository: task.repository || "",
        ref: task.ref || "",
        inputHash: inputFingerprint,
        outputHash: sha256(safeOutput.text),
        status: "success",
        inputTokens: result.usage?.inputTokens || 0,
        outputTokens: result.usage?.outputTokens || 0,
        redactions: contextRedaction.redactions + promptRedaction.redactions + safeOutput.redactions,
        truncated: limited.truncated
      });
      return {
        taskId: task.id,
        provider: result.provider || providerName,
        model: result.model || "",
        text: safeOutput.text,
        usage: result.usage || { inputTokens: 0, outputTokens: 0 },
        contextTruncated: limited.truncated,
        redactions: contextRedaction.redactions + promptRedaction.redactions + safeOutput.redactions
      };
    } catch (error) {
      await appendAudit({
        taskId: task.id,
        taskType: task.type,
        provider: providerName,
        model: provider.model || "",
        repository: task.repository || "",
        ref: task.ref || "",
        inputHash: inputFingerprint,
        status: "failure",
        redactions: contextRedaction.redactions + promptRedaction.redactions,
        truncated: limited.truncated,
        errorCode: error?.code || error?.name || "ERROR"
      });
      throw error;
    }
  }
}
