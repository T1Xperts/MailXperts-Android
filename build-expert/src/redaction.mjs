const RULES = [
  [/-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----/g, "[REDACTED_PRIVATE_KEY]"],
  [/\b(authorization\s*:\s*bearer\s+)[^\s"']+/gi, "$1[REDACTED]"],
  [/\b(api[-_ ]?key|access[-_ ]?token|refresh[-_ ]?token|password|secret)\b\s*[:=]\s*["']?[^\s,"'}]+/gi, "$1=[REDACTED]"],
  [/\bsk-[A-Za-z0-9_-]{12,}\b/g, "[REDACTED_API_KEY]"]
];

export function redact(value) {
  let text = typeof value === "string" ? value : JSON.stringify(value ?? "");
  let redactions = 0;
  for (const [pattern, replacement] of RULES) {
    text = text.replace(pattern, match => {
      redactions += 1;
      if (typeof replacement === "function") return replacement(match);
      if (replacement.includes("$1")) {
        const prefix = match.match(pattern)?.[1] ?? "";
        return replacement.replace("$1", prefix);
      }
      return replacement;
    });
  }
  return { text, redactions };
}

export function limitContext(text, maxChars = 120000) {
  const safeLimit = Math.max(1000, Math.min(Number(maxChars) || 120000, 500000));
  if (text.length <= safeLimit) return { text, truncated: false };
  return {
    text: text.slice(0, safeLimit) + "\n[TRUNCATED_BY_BUILD_EXPERT_POLICY]",
    truncated: true
  };
}
