import { createHmac, timingSafeEqual } from "node:crypto";

const DEFAULT_SKEW_SECONDS = 300;

export class RequestAuthenticator {
  constructor({ secret = process.env.BUILD_EXPERT_GATEWAY_SECRET, maxSkewSeconds = DEFAULT_SKEW_SECONDS } = {}) {
    if (!secret || secret.length < 32) throw new Error("BUILD_EXPERT_GATEWAY_SECRET must be at least 32 characters");
    this.secret = secret;
    this.maxSkewSeconds = Math.max(30, Math.min(Number(maxSkewSeconds) || DEFAULT_SKEW_SECONDS, 900));
    this.nonces = new Map();
  }

  verify({ timestamp, nonce, signature, method, path, body }) {
    const ts = Number(timestamp);
    if (!Number.isFinite(ts)) return { ok: false, reason: "invalid_timestamp" };
    const now = Math.floor(Date.now() / 1000);
    if (Math.abs(now - ts) > this.maxSkewSeconds) return { ok: false, reason: "expired_timestamp" };
    if (!nonce || String(nonce).length < 12 || String(nonce).length > 128) return { ok: false, reason: "invalid_nonce" };
    this.prune(now);
    if (this.nonces.has(String(nonce))) return { ok: false, reason: "replay" };
    const expected = sign(this.secret, { timestamp: ts, nonce, method, path, body });
    const supplied = String(signature || "");
    if (!safeEqual(expected, supplied)) return { ok: false, reason: "invalid_signature" };
    this.nonces.set(String(nonce), now + this.maxSkewSeconds);
    return { ok: true, reason: "verified" };
  }

  prune(now = Math.floor(Date.now() / 1000)) {
    for (const [nonce, expiresAt] of this.nonces.entries()) {
      if (expiresAt < now) this.nonces.delete(nonce);
    }
  }
}

export function sign(secret, { timestamp, nonce, method, path, body }) {
  const canonical = [String(timestamp), String(nonce), String(method || "POST").toUpperCase(), String(path || "/"), String(body || "")].join("\n");
  return createHmac("sha256", secret).update(canonical, "utf8").digest("hex");
}

function safeEqual(a, b) {
  try {
    const left = Buffer.from(String(a), "hex");
    const right = Buffer.from(String(b), "hex");
    return left.length > 0 && left.length === right.length && timingSafeEqual(left, right);
  } catch {
    return false;
  }
}
