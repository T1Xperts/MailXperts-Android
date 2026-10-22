const PRIVATE_KEY = /-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----/g;
const AUTH_BEARER = /\b(authorization\s*:\s*bearer\s+)[^\s"']+/gi;
const NAMED_SECRET = /\b(api[-_ ]?key|access[-_ ]?token|refresh[-_ ]?token|password|secret)\b["']?\s*[:=]\s*["']?[^\s,"'}]+/gi;
const OPENAI_STYLE_KEY = /\bsk-[A-Za-z0-9_-]{12,}\b/g;

export function redact(value) {
  let text = typeof value === "string" ? value : JSON.stringify(value ?? "");
  let redactions = 0;
  text = text.replace(PRIVATE_KEY, () => {
    redactions += 1;
    return "[REDACTED_PRIVATE_KEY]";
  });
  text = text.replace(AUTH_BEARER, (_match, prefix) => {
    redactions += 1;
    return `${prefix}[REDACTED]`;
  });
  text = text.replace(NAMED_SECRET, (_match, name) => {
    redactions += 1;
    return `${name}=[REDACTED]`;
  });
  text = text.replace(OPENAI_STYLE_KEY, () => {
    redactions += 1;
    return "[REDACTED_API_KEY]";
  });
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
