import { readFile } from "node:fs/promises";
import { stdin as input } from "node:process";
import { BuildExpertOrchestrator } from "./orchestrator.mjs";
import { OpenAIProvider } from "./providers/openai.mjs";

async function readTask() {
  const file = process.argv[2];
  if (file) return JSON.parse(await readFile(file, "utf8"));
  const chunks = [];
  for await (const chunk of input) chunks.push(chunk);
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

try {
  const task = await readTask();
  const provider = new OpenAIProvider();
  const orchestrator = new BuildExpertOrchestrator({ providers: { openai: provider } });
  const result = await orchestrator.execute(task);
  process.stdout.write(JSON.stringify(result, null, 2) + "\n");
} catch (error) {
  process.stderr.write(JSON.stringify({
    error: error?.message || "Build Expert failed",
    code: error?.code || error?.name || "ERROR"
  }) + "\n");
  process.exitCode = 1;
}
