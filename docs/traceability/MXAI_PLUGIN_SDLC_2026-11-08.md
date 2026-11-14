# MailXperts universal AI assistants — combined product backlog and SDLC extension

> **13 Nov 2026 planning addendum (draft; not released):** The near-P0 universal AI SDLC proposal supersedes this document **for prospective priorities and wave allocation only**. Current draft RTM = **160 IDs/44 columns** and detailed MXAI backlog = **94 proposed items / 683 provisional points** including six planned iOS companion requirements. The original 8 Nov figures of 88 proposed MXAI items, 633 points and 154 total IDs remain historical snapshots, not current planning totals. See [Near-P0 SDLC plan](MAILXPERTS_NEAR_P0_SDLC_PLAN_2026-11-13.md), [proposed HLD/LLD](../architecture/MAILXPERTS_UNIVERSAL_AI_HLD_LLD_2026-11-13.md) and [automated QA + beta UAT plan](../qa/MAILXPERTS_UNIVERSAL_AI_BETA_QA_2026-11-13.md). All new scope remains proposed, unbuilt, untested and unreleased; previous accepted MX-QA evidence is unchanged.

**As of:** 8 November 2026 (Australia/Melbourne) · **Initiative:** MX-AI-001 · **New requirements:** MXAI-001…MXAI-088 · **Repository:** T1Xperts/MailXperts-Android

## Authoritative baseline and provenance

- Original register: [master QA/development #3](https://github.com/T1Xperts/MailXperts-Android/issues/3), [end-user AI backlog #29](https://github.com/T1Xperts/MailXperts-Android/issues/29), [release control #11](https://github.com/T1Xperts/MailXperts-Android/issues/11), [cross-assistant AI Agent #34](https://github.com/T1Xperts/MailXperts-Android/issues/34).
- [RTM.csv](RTM.csv) retains the 66 pre-existing 44-column records and adds 88 unique MXAI records (154 requirement IDs). **154 is an identifier count, not 154 independent/fully distinct capabilities.** Overlapping scope is cross-referenced below and not claimed as additive feature delivery.
- [MXAI_PLUGIN_BACKLOG.csv](MXAI_PLUGIN_BACKLOG.csv) is the detailed backlog with proposed tests, dependencies, acceptance criteria, priority, waves, risk and crosswalk.
- Original MXAI planning source: MailXperts_AI_Plugins_GitHub_Import_Backlog.csv (8 Nov 2026; 88 lines). This GitHub copy incorporates existing-backlog cross-references. The earlier v1.4 source inspection is not considered the current GitHub implementation baseline.
- **Do not overwrite historical status:** MX-QA-001…028 remain PO accepted; MX-QA-029 partially built; MX-QA-030 device QA pending. AI-001…007 have candidate code in draft PRs #30/#33 and aggregate CI evidence; they are not released or device-UAT closed. The rest of AI-001…036 keep the evidence in #29 / #3.

## Scope boundary: avoid merging unlike systems

- **MX-QA-029**: developer/engineering Build Expert and controlled coding runner; not equivalent to a consumer email plugin.
- **AI-001…036**: end-user intelligence and actions in the MailXperts client; reuse their tested service contracts but do not reclassify them as implemented cross-platform MCP.
- **Issue #34**: existing Python local stdio MCP pilot plus planned Android/remote gateway. A local mock-tested agent is **not** a secure production HTTPS MCP gateway.
- **MXAI-001…088**: new integration-specific acceptance conditions for multi-provider, remotely authorisable MailXperts access from ChatGPT, Microsoft Copilot and Claude/Cowork, and later controlled actions.

## Planned MXAI waves (new workstream, distinct from older release waves 0–6)

| Wave | Added IDs | Story points (provisional) | Feature/HLD/LLD | Build/QA/UAT/release |
|---|---:|---:|---|---|
| W0 | 11 | 69 | Discovery / proposed | Not built or individually QA verified |
| W1 | 22 | 139 | Discovery / proposed | Not built or individually QA verified |
| W2 | 19 | 140 | Discovery / proposed | Not built or individually QA verified |
| W3 | 22 | 160 | Discovery / proposed | Not built or individually QA verified |
| W4 | 12 | 99 | Discovery / proposed | Not built or individually QA verified |
| W5 | 2 | 26 | Discovery / proposed | Not built or individually QA verified |
| **Total** | **88** | **633** | **Discovery, approvals pending** | **No MXAI completion evidenced** |

**Pre-existing 66-row baseline is preserved unchanged.** Existing wave RAG remains AMBER/NO-GO; this planning change does not introduce a beta-version bump, a release acceptance, a deployment or a device QA result. The MXAI wave labels are **namespaced** to avoid collision with pre-existing W0–W6 delivery plans.

## Proposed staged gates and deliverables

| Gate | Required artifact | Exit criteria | MXAI state as of 2026-11-08 |
|---|---|---|---|
| G0 Discovery | PRD, duplicate/crosswalk review, provider constraints | Product owner signs off scope and retains original AI IDs | Proposed |
| G1 HLD | Tenant/mailbox trust boundaries, gateway hosting and consent RFC | Architecture/security review approved | Not started |
| G2 LLD | OAuth flows, provider adapters, message schemas, MCP API contracts | Reviewable interface and threat model | Not started |
| G3 Build | Branch, code, configuration, synthetic fixture tests | Requirement-linked implementation commits | Not started |
| G4 CI | Unit, integration, security, isolation, tool-schema tests | Relevant checks green with run URLs | Not run |
| G5 QA | Cross-account, auth-revoke, prompt-injection, provider-specific cases | Each TC-MXAI-### result attached | Not run |
| G6 UAT | Consenting account owners, Android and assistant journeys | Explicit PO acceptance for each scope | Not started |
| G7 Release | Privacy/OAuth authorisations, product docs, rollback, signed gates | Approved deployment and verified smoke tests | NOT RELEASED |

**No email send, delete, calendar booking, invoice posting or payment without separate user approval.** Email content is untrusted and cannot elevate agent permissions. No Android Keystore material or OAuth refresh tokens may be published or silently exported. The repository is public: never commit secrets, private email, personal contacts or signing material.

## Existing overlap / duplicate-prevention crosswalk

The associations below are **potential reusable capabilities**, not assertions of exact duplicate requirements or acceptance evidence. Existing IDs must not be closed from this mapping.

| New MXAI item | Prior item(s)/epic | Rule |
|---|---|---|
| MXAI-003 | #34; MX-QA-029; AI-035 | Related scope only; no transfer of acceptance or QA status |
| MXAI-010 | MX-QA-030 | Related scope only; no transfer of acceptance or QA status |
| MXAI-012 | #34; AI-035 | Related scope only; no transfer of acceptance or QA status |
| MXAI-013 | #34; AI-035 | Related scope only; no transfer of acceptance or QA status |
| MXAI-014 | #34; AI-035 | Related scope only; no transfer of acceptance or QA status |
| MXAI-015 | MX-QA-001; MX-QA-020; MX-QA-021; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-017 | MX-QA-030; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-018 | MX-QA-030; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-023 | MX-QA-030 | Related scope only; no transfer of acceptance or QA status |
| MXAI-024 | MX-QA-030; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-028 | AI-004; AI-011; MX-QA-026 | Related scope only; no transfer of acceptance or QA status |
| MXAI-032 | AI-012 | Related scope only; no transfer of acceptance or QA status |
| MXAI-034 | #34; MX-QA-029 | Related scope only; no transfer of acceptance or QA status |
| MXAI-035 | #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-036 | #34; AI-004; AI-011 | Related scope only; no transfer of acceptance or QA status |
| MXAI-037 | AI-001; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-038 | AI-006; AI-016 | Related scope only; no transfer of acceptance or QA status |
| MXAI-041 | #34; AI-024 | Related scope only; no transfer of acceptance or QA status |
| MXAI-043 | AI-023; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-044 | AI-023; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-049 | #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-052 | MX-QA-029; AI-035 | Related scope only; no transfer of acceptance or QA status |
| MXAI-053 | AI-001 | Related scope only; no transfer of acceptance or QA status |
| MXAI-054 | AI-003; AI-004; AI-011 | Related scope only; no transfer of acceptance or QA status |
| MXAI-055 | AI-016; AI-036 | Related scope only; no transfer of acceptance or QA status |
| MXAI-056 | AI-005; AI-020; AI-021 | Related scope only; no transfer of acceptance or QA status |
| MXAI-057 | AI-010; AI-006 | Related scope only; no transfer of acceptance or QA status |
| MXAI-058 | AI-009 | Related scope only; no transfer of acceptance or QA status |
| MXAI-059 | AI-002; AI-014; AI-015; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-060 | AI-007; AI-025 | Related scope only; no transfer of acceptance or QA status |
| MXAI-061 | AI-012 | Related scope only; no transfer of acceptance or QA status |
| MXAI-062 | AI-002; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-063 | #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-064 | AI-022; #34 | Related scope only; no transfer of acceptance or QA status |
| MXAI-065 | AI-017 | Related scope only; no transfer of acceptance or QA status |
| MXAI-066 | AI-018 | Related scope only; no transfer of acceptance or QA status |
| MXAI-067 | AI-016; AI-034 | Related scope only; no transfer of acceptance or QA status |
| MXAI-068 | AI-010; AI-024 | Related scope only; no transfer of acceptance or QA status |
| MXAI-069 | AI-024 | Related scope only; no transfer of acceptance or QA status |
| MXAI-070 | AI-036; #31 | Related scope only; no transfer of acceptance or QA status |
| MXAI-071 | AI-034 | Related scope only; no transfer of acceptance or QA status |
| MXAI-072 | AI-003; AI-004 | Related scope only; no transfer of acceptance or QA status |
| MXAI-075 | AI-035 | Related scope only; no transfer of acceptance or QA status |
| MXAI-076 | AI-035 | Related scope only; no transfer of acceptance or QA status |
| MXAI-078 | MX-QA-012 | Related scope only; no transfer of acceptance or QA status |
| MXAI-079 | AI-035 | Related scope only; no transfer of acceptance or QA status |
| MXAI-080 | MX-QA-030 | Related scope only; no transfer of acceptance or QA status |
| MXAI-085 | #3; #11 | Related scope only; no transfer of acceptance or QA status |

## Next execution dependencies

1. **MXAI-002** reconcile v1.4-derived planning against current main, draft PR #27/#30/#32/#33 and #34's local MCP pilot.
2. **MXAI-003/007/009/079/080** decide secure consent, custody, tenancy, Google restricted-scope/privacy gates before any live mailbox ingestion.
3. **MXAI-010…014** scoped OAuth/OIDC and grant ledger; verify provider app registrations and permissions.
4. **MXAI-017/018/028/029/034…037** build authorised read-only gateway using synthetic fixtures, then real-account opt-in UAT.
5. **MXAI-043/044** private ChatGPT validation; **MXAI-046…051** Copilot + Claude parity in their supported environments.
6. Only after approval controls pass: **MXAI-041/059/062…071** writing, automation and financial workflows.

## Traceability rule

Requirement ID → #34 / linked specific issue → RFC/HLD/LLD → branch/PR/commit → CI run → individually executed test case → QA → UAT owner acceptance → release/rollback evidence. Until evidence exists, every MXAI row remains **PROPOSED, NOT BUILT, NOT TESTED, NOT RELEASED**. Related AI-001…007 candidate code never silently closes its MXAI cross-platform obligations.
