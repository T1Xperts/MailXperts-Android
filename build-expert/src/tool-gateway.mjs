import { authorizeTool } from "./policy.mjs";

const MAX_TOOL_INPUT_CHARS = 50000;
const MAX_TOOL_OUTPUT_CHARS = 200000;

/**
 * In-process policy gateway. Transport (MCP/HTTP) can be layered over this contract later.
 * No handler exists for forbidden production/signing/secret operations.
 */
export class ToolGateway {
  constructor(handlers = {}) {
    this.handlers = new Map(Object.entries(handlers));
  }

  async call(name, input = {}, { approved = false } = {}) {
    const decision = authorizeTool(name, { approved });
    if (!decision.allowed) {
      const error = new Error(`Tool denied: ${name} (${decision.reason})`);
      error.code = decision.reason.toUpperCase();
      throw error;
    }
    const handler = this.handlers.get(name);
    if (typeof handler !== "function") {
      const error = new Error(`No implementation registered for tool: ${name}`);
      error.code = "TOOL_NOT_IMPLEMENTED";
      throw error;
    }
    const serialized = JSON.stringify(input ?? {});
    if (serialized.length > MAX_TOOL_INPUT_CHARS) {
      const error = new Error("Tool input exceeds policy limit");
      error.code = "INPUT_TOO_LARGE";
      throw error;
    }
    const result = await handler(Object.freeze({ ...(input ?? {}) }));
    const output = typeof result === "string" ? result : JSON.stringify(result ?? null);
    if (output.length > MAX_TOOL_OUTPUT_CHARS) {
      const error = new Error("Tool output exceeds policy limit");
      error.code = "OUTPUT_TOO_LARGE";
      throw error;
    }
    return result;
  }
}
