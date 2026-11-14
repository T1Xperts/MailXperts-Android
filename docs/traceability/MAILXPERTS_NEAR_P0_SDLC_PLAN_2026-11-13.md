# MailXperts Universal AI: Near-P0 SDLC Master Plan

**Document:** MX-SDLC-UNIVERSAL-AI-2026-11-13 | **Status:** PLANNING DRAFT, NOT APPROVED | **Product priority:** Near-P0 (tracked as P0 for mandatory release-gating work) | **Owner:** T1Xperts / MailXperts Product Owner

**Baseline:** GitHub main is Android v1.6.0-beta.7/versionCode 21; v1.6.0-beta.10/versionCode 24 is an offline-signed QA candidate in unmerged PR #33, not a release. Existing MX-QA-001..028 are product-owner accepted; MX-QA-029 is partial; MX-QA-030 needs live device OAuth QA. AI-001..007 have unmerged candidate code and aggregate CI, not live UAT. PR #35 merged the historic 88 MXAI proposed requirements. This plan changes no earlier acceptance or code status.

## 1. Product charter and fixed decisions

**Vision:** MailXperts becomes a secure, AI-native mail and communications platform on Android, future iOS, and an optional web consent console. Users may link selected Gmail / Microsoft mailboxes and later selected hosted/IMAP accounts, then use MailXperts AI in-app or connect authorised external assistants through one standards-based integration.

**Two-way integration means:** (A) assistant -> MailXperts authorised read-only tools, and (B) MailXperts mobile -> consented server-side LLM analysis. It does **not** mean autonomous send, delete, archive, payments, accounting posting or workflow mutation in MVP. Any later write capability needs a distinct contract, review surface, scoped grant and separate explicit human approval.

**Prioritisation:** The universal AI read-only product lane is **Near-P0 / release-critical next MVP**. Existing Priority enums in the backlog/RTM stay P0/P1/P2/P3; essential Near-P0 items are promoted to P0, avoiding a nonstandard P0.5 CSV value. Keep critical existing stability/auth defects in the release gate. ChatGPT, Claude/Cowork, Copilot Studio and generic compatible remote MCP clients are targeted for the read-only beta, subject to individual platform entitlement and onboarding approval. Full Microsoft 365 declarative agents and public directories are an expansion, not a beta blocker.

**Multi-platform strategy:** One MailXperts identity/authorisation service, shared account brokers and AI backend, a remote MCP resource server for assistants, REST/JSON BFF for mobile clients. Android uses existing Java client and may add an AI panel; iOS is a new native SwiftUI beta shell and read-only AI experience using the same backend, not an unapproved full parity rewrite of Android mail transport.

**Privacy strategy:** Existing Android-stored passwords and OAuth material never leave Android Keystore implicitly. A user explicitly links a **new** provider account to the server-side vault through provider-native OAuth where available. Each assistant requires a separate named grant; no implicit All Accounts access; no cloud AI request without consent. Australia-region hosting preferred subject to provider availability and documented cross-border privacy review.

## 2. MVP boundaries

### In scope for first closed beta (P0)
- User sign-in, distinct tenant/user identity and organisation boundaries; consent, privacy notice, revoke, grant audit.
- Link one or more selected Google Gmail / Google Workspace and Microsoft Outlook.com / M365 accounts with the minimum verified read scopes; clear external provider authorisation prerequisites.
- List allowed accounts; search across only explicitly granted accounts; fetch message and thread with source/citation anchors, pagination, redaction, rate limits and refusal when not authorised.
- Remote HTTPS Streamable HTTP MCP endpoint with OAuth 2.1-compatible discovery, audience-bound short-lived tokens, PKCE and scope validation; compatible tool schemas and version negotiation.
- Private smoke-tested connections for ChatGPT, Claude, Cowork, Copilot Studio and one independent MCP client; adapters or platform-specific packaging only where necessary.
- Android AI view for authorised summarisation, Ask AI, priority/actions (reuse draft AI-001..007 only after review); explicit cloud AI opt-in and account selector.
- iOS/TestFlight **read-only AI companion** beta with login, account selection, AI summary/search, consent/revocation and source drill-through. Native full IMAP/SMTP composition/sync parity is outside this MVP.
- Staging deployment with synthetic fixtures, reproducible security/contract testing, redacted audit/metrics, kill switch and rollback.
- Manual UAT on Android and iOS plus four assistant environments, using only consented test mailboxes.

### Explicitly deferred (no silent scope creep)
- Agent-initiated sending, draft persistence on the mail provider, contacts writes, server message mutation, autonomous monitors/webhooks, archive/delete, billing/payments, Odoo posting, and admin-wide unattended mailbox access.
- Full native iOS mailbox transport engine, local-only LLM inference on all devices, advanced document/OCR analytics, publication in public connector directories, unrestricted generic IMAP onboarding, Yahoo/iCloud as default beta providers.
- Mail content storage/semantic indexing unless a separate privacy-reviewed opt-in is approved.

## 3. User journeys and acceptance outcomes

| Journey | Acceptance requirement |
| --- | --- |
| J1 Link | Consenting user explicitly selects provider, mailbox and read scope; only encrypted server vault receives provider credentials; Android local vault is untouched |
| J2 Authorise | User authorises a named assistant and exact account IDs; grant can be revoked immediately |
| J3 Search | Assistant returns only source-citable matching results from explicitly granted accounts; never mixes sender identities/tenants |
| J4 Fetch | User can drill from answer to the correct original message/thread; access denied after revocation |
| J5 In-app AI | Android/iOS summarise, ask and rank authorised mail with consent on; disabled/revoked mode issues no outbound AI request |
| J6 Safe refusal | Untrusted mail instructions are not executed as tool commands; a prompt injection cannot expand scopes |
| J7 Operational resilience | Expired token, provider throttling and partial outages yield non-leaky, actionable errors without pretending data is complete |
| J8 Beta safety | No writable email tool exists on beta server; no covert background send/delete/billpay; complete account and assistant isolation tested |

## 4. SDLC stages, work packages and gates

| Stage | Deliverables | Exit gate (evidence) |
| --- | --- | --- |
| G0 Strategy & discovery | PRD, current-branch inventory, 94-MXAI backlog/160-ID RTM extension, scope and cross-platform matrix, dependency register | Product Owner signs off MVP/no-write boundary and provider/platform constraints |
| G1 Security architecture & HLD | Trust-boundary HLD, data flows, DPIA, retention/cross-border decisions, threat model, OAuth provider feasibility | Security/architecture reviewer approves; high risks have mitigations |
| G2 LLD & test design | API/MCP contracts, auth-grant schema, adapter interfaces, error taxonomy, iOS + Android UX contracts, test fixture plan | Contracts reviewed against latest client platform and MCP supported version(s) |
| G3 Foundations | Separate gateway/identity/token-vault CI project and mock provider clients; no real customer mail | Contract, SAST, SCA, secret and auth-matrix tests pass |
| G4 Provider read | Gmail and Microsoft read-only, account-scoped search/fetch/thread/pagination/citations | Synthetic and consented live mailbox tests; forbidden access denied |
| G5 AI + assistant parity | Shared LLM orchestration, Android AI panel, ChatGPT + Claude/Cowork + Copilot Studio/generic MCP smoke paths | Model/provider, privacy and tool-schema QA passes; no write tools |
| G6 iOS + beta readiness | iOS SwiftUI minimal app, TestFlight lane, Android internal testing, version/signature continuity, privacy documentation | Android/iOS device matrix and assistant-specific manual UAT accepted |
| G7 Security/UAT release | Pentest, cross-tenant abuse testing, privacy/legal review, rollback drill, release notes, incident response runbook | Product Owner + security + release engineer explicit beta GO |
| G8 Controlled beta learning | Opt-in cohort, performance/security telemetry without message contents, defect triage, feedback | Beta exit report determines next production backlog; no automatic public launch |

**Recommended dependency order:** G0 -> G1 -> G2 -> G3 -> G4 -> G5 -> G6 -> G7 -> G8. iOS UX scaffolding can occur after G2 in parallel with gateway work, but real-data access cannot bypass G1/G4 authorisation. No sprint/date commitment until staffing and provider registration/verification effort are confirmed.

## 5. Requirements reprioritisation and RTM policy

- Promote AI-025 (external AI bridge) and AI-035 (privacy/consent) from P1 to P0 in planning RTM, without changing code/UAT statuses.
- Advance essential MXAI cross-platform and mobile stories to P0 planning priority: **MXAI-047, 049, 051, 052, 053, 072, 075 and 076**. Six W3 integration items move to W2 for read-only assistant parity, privacy and Android AI access. **MXAI-046** (Microsoft 365 declarative-agent packaging) and **MXAI-032** (attachment processing) remain P1 because they are not beta-critical; essential read-only consent/redaction stays within P0 gateway and AI security controls. Existing P0 foundations remain P0.
- Add six **proposed** iOS requirements MXAI-089..094. They are wholly new, not delivered, and do not overwrite the historical 88-item 8 Nov planning snapshot.
- The 44-column RTM must retain each original accepted record and each unmerged candidate status, plus links to this plan/HLD/LLD/QA. Existing 154 IDs + six planned iOS IDs = **160 IDs**, while the detailed plugin backlog is **94 proposed items** (88 historic + six iOS), **683 provisional story points** (633 historic + 50 new). Revised MXAI wave distribution: W0=11, W1=22, W2=29, W3=18, W4=12, W5=2. **These counts are not distinct completed features or a release claim**.
- Preserve the existing MXAI wave IDs W0-W5 as a provenance grouping; introduce a near-P0 **execution lane** in this document, not a disruptive renumbering of historic waves.
- Every requirement flows: backlog -> HLD/LLD -> branch/PR -> CI artifact -> per-case test -> security evidence -> manual UAT -> beta release decision. Documentation PRs cannot satisfy build/test/release gates.

## 6. Ownership and external dependencies

| Role | Accountability |
| --- | --- |
| Product Owner | Sign scope, priority trade-offs, privacy consent journeys and UAT criteria |
| Solution architect / technical lead | HLD, LLD, trust boundaries, provider/client capability map |
| Security/privacy | DPIA, data classification, OAuth review, cross-border/regulator constraints, pen test |
| Backend/MCP engineer | Identity/grant ledger, provider broker, tool contracts, hosting, observability |
| Android engineer | Existing v1.6 branch reconciliation, consent screens, AI integration, regression |
| iOS engineer | SwiftUI read-only companion, Keychain auth, accessibility, TestFlight |
| QA lead | Cross-provider automated pyramid, assistant/device UAT, defect severity, CI gates |
| Release/DevOps | Secret and signing custody, deployment, rollback, Play internal / TestFlight tracks |

External prerequisites requiring human ownership: domain/DNS + TLS; secure hosting and region; MailXperts identity service/provider registrations; Google restricted Gmail-scope feasibility and verification/security assessment where required; Microsoft Entra permissions; approved LLM provider tenancy/data handling; Apple Developer/TestFlight and Google Play internal test accounts; production-signing lineage; pilot users' consent. Avoid embedding any confidential secret in public GitHub source or APK.

## 7. Security risk register (hypotheses, not confirmed vulnerabilities)

| Risk | Severity | Mandatory control / validation |
| --- | --- | --- |
| Cross-user, cross-tenant or cross-mailbox leakage | Critical | Deny-by-default account IDs; subject/tenant/account validation on **each tool call**, negative test matrix and independent penetration test |
| Provider token exposure/forwarding | Critical | Server-only envelope-encrypted provider vault; bound audience; never forward provider tokens to MCP clients, loggers or models |
| Prompt injection through mail and attachments | High | Treat all mailbox content as inert data; deterministic tool allowlist, structured outputs, no privileged tool chain from retrieved content |
| OAuth mix-up, login CSRF, token replay/redirect | High | PKCE/state/nonce; strict redirect, token issuer/audience/resource and client validation; refresh rotation/revoke |
| Unrestricted MCP tools or tool-definition drift | High | Versioned immutable schema, read-only beta tool registry, response validation, reapproval for any new write tool |
| SSRF/malicious URLs and attachment parsing | High | Provider API allowlists; no arbitrary URL fetch in beta; size limits, antivirus/sandbox for later attachments |
| Sensitive prompts, telemetry or retention | High | Redacted logs, per-user deletion and retention controls, no raw mail in analytics/CI, cross-border review |
| Account enumeration, DoS and provider throttling | Medium/High | Opaque IDs, quotas, backoff, circuit breakers, abuse alerts and global kill switch |
| Android beta upgrade/signing mismatch | High | Retain au.com.t1xperts.mailxperts and matching certificate, monotonic versionCode, install-over-history device test |
| App Store privacy/login rejection | Medium/High | Consent disclosure, correct app-privacy answers, entitlements, TestFlight review and signed iOS binary |

**No code-level vulnerability is declared confirmed by this planning document.** The code review and automated scanners are G3/G7 tasks and any actual findings require a linked issue, severity, proof and remediation evidence.

## 8. Definition of ready, definition of done, and release authority

**Ready to implement:** signed scope + explicit excluded actions; architecture/privacy approval; provider/scope feasibility; reviewed API/schema; synthetic test data; case IDs; implementer/tester; rollback design.

**Done for beta:** PR merged after human code review; affected unit/integration/security tests pass; per-requirement tests executed (not merely inherited aggregate CI); no critical/high unresolved vulnerabilities; provider consent/revocation verified live; no cross-account leaks; mobile and assistant manual UAT passes; signature and platform distribution checks done; documentation/rollback tested; Product Owner approves.

**Beta release candidate naming (planned only):** Android **v1.7.0-beta.1** with versionCode strictly >24, subject to final version reconciliation; future iOS **0.1.0 TestFlight**. These versions are **not built, signed, deployed, uploaded or approved by this planning change**. Signed v1.6.0-beta.10 remains a separate earlier internal QA preview.

## 9. Cross-references and official standards

- Historical issues: #3 master QA, #11 release tracker, #29 end-user AI, #34 MCP agent, #31 draft-only Odoo.
- Historical PRs: #27 platform foundation, #30 AI MVP, #32 traceability HLD/LLD, #33 AI consent hardening, #35 merged documentation.
- Companion documents: ../architecture/MAILXPERTS_UNIVERSAL_AI_HLD_LLD_2026-11-13.md ; ../qa/MAILXPERTS_UNIVERSAL_AI_BETA_QA_2026-11-13.md ; RTM.csv ; MXAI_PLUGIN_BACKLOG.csv.
- Platform standards/guidance consulted (reconfirm during implementation): https://developers.openai.com/plugins/build/auth ; https://developers.openai.com/api/docs/guides/custom-mcp-server ; https://support.claude.com/en/articles/11175166-get-started-with-custom-connectors-using-remote-mcp ; https://learn.microsoft.com/en-us/microsoft-copilot-studio/mcp-add-existing-server-to-agent ; https://modelcontextprotocol.io/specification ; https://owasp.github.io/DevSecOpsGuideline/2-Process/2-2-Develop/2-2-5-AI-Agent-and-MCP-Security/

**Planning-only safeguard:** No implementation, merge, account connection, third-party request, hosted deployment, production signing, or email operation is authorised by this document.
