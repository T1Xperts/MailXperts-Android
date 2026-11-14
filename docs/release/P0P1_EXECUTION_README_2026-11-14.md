# MailXperts all-P0/P1 production execution baseline

**Date:** 14 November 2026 · **Status:** DRAFT FOR PRODUCT OWNER / ARCHITECT / SECURITY / QA REVIEW · **Requested outcome:** signed Android APK for direct QA + signed Google Play AAB + hosted gateway/assistant integrations + separately accepted iOS TestFlight as required by 131 P0/P1 RTM IDs.

**NO-GO: This PR makes only planning, dependency and evidence changes. It does not develop MailXperts functionality, sign builds, store keys, deploy environments, connect accounts or submit a release.**

## Review navigation and controls

| Review document | Decision question |
| --- | --- |
| [P0/P1 release scope register](P0P1_RELEASE_SCOPE_2026-11-14.csv) | Are all **131** priority-ID rows accounted for, gaps/overlap/owners correct? |
| [Task-level 164-item execution schedule](P0P1_PRODUCTION_EXECUTION_SCHEDULE_2026-11-14.csv) | Which implementation and gate tasks must be assigned and executed? |
| [16-package Gantt](P0P1_PRODUCTION_GANTT_2026-11-14.md) | Are the overlapping workstreams and milestones acceptable? |
| [572 FS/SS/FF/SF dependencies](P0P1_EXECUTION_DEPENDENCIES_2026-11-14.csv) | Are all dependency gates logically correct and safe? |
| [9 corrected source backlog links](P0P1_DEPENDENCY_REPAIRS_2026-11-14.csv) | Should the architects approve source dependency repairs? |
| [16 work packages, roles and AI copilots](P0P1_WORK_PACKAGES_2026-11-14.csv) | Who provides actual named people and capacity? |
| [Historical main RTM](../traceability/RTM.csv) | Which evidence/status must remain unchanged? |
| [Reprioritised MXAI backlog](../traceability/MXAI_PLUGIN_BACKLOG.csv) | Does source backlog reflect revised dependency sequence? |

## 1. Scope reconciliation and state, not inflated by duplicates

- The latest **160-ID** RTM consists of **70 P0, 61 P1, 27 P2, 2 P3** IDs. This plan traces **all 131 P0/P1 identifiers**. It does **not** claim 131 distinct new features or 131 completed features.
- **21** P0/P1 accepted MX-QA Android stories need cross-version production-signed regression only; previous Product Owner acceptance is retained.
- **7** P0 AI MVP stories `AI-001..007` have unmerged candidate code in PR #30/#33. Existing old aggregate CI is not individual production-gate acceptance.
- **3** stories are **partial / pending device QA**: `MX-QA-029` developer Build Expert, `MX-QA-030` provider OAuth, `AI-035` cloud privacy consent.
- **100** others have no release-ready implementation evidenced, including 80 P0/P1 MXAI integration IDs. Their implementation, automated QA and live UAT statuses remain **not evidenced**. The register identifies each individually.
- **57 P0/P1 rows** have **proposed overlapping-capability crosswalks**, including some expanded cross-references beyond the historic 48 relationships. **These are proposed mappings**; the BA/QA lead must test whether implementation and acceptance really can be shared. One accepted source item must never automatically close a dependent MXAI requirement.

### P1 features that challenge the original read-only MVP

- `AI-018` calendar writes and `AI-019` Find Time, `AI-020` user-approved mailbox rules, `AI-026` approved workflow designer, and `AI-036` **Odoo vendor bill DRAFT only** are mandatory P1 *requirement-level* implementation gates, although related broader MXAI stories (`MXAI-066/069/070`) sit in P2/P3. Implement the **minimum acceptance-criteria P1 capability** without silently promoting full P2/P3 scope.
- `MXAI-041` is a human approval coordinator, not permission for autonomous operations. `MXAI-062` is a controlled provider **draft** write; `MXAI-063` actual sending is P2 and **out of this release**. `MXAI-067` may display verified payment links, but never initiate payment. Odoo bills must not post or pay without a new approved scope.
- `MX-QA-029` developer Build Expert and `MXAI-034` consumer MailXperts MCP gateway must remain security isolated. Cross-assistant read permissions never imply access to coding or production automation tools.
- `MXAI-089..094` include a **separate iOS TestFlight deliverable** which cannot be packed into an Android APK. A single full-P0/P1 production gate includes approved evidence for that work unless the Product Owner formally scopes it into a separate release.

## 2. Dependencies repaired and hard release ordering

Nine MXAI source-dependency links were revised in `MXAI_PLUGIN_BACKLOG.csv` and recorded old-vs-new for review:
`MXAI-020`, `027`, `028`, `044`, `047`, `051`, `053`, `072`, `076`.

They remove unnecessary coupling to deferred analysis, documentation, full declarative agents, nonessential natural-language Q&A and digests while preserving downstream integration/acceptance testing.

The execution network has **164 tasks (131 requirements + 33 release tasks), 572 edges (566 FS / 3 SS / 2 FF / 1 SF), zero unknown predecessor IDs and no dependency cycles**.

**Critical release logic:** 131 requirement implementation tasks -> `PLAY-005` signed APK+AAB candidate -> `QA-003` full scope/device UAT -> `QA-004` final security/regression -> `PLAY-007` Play internal/closed track tests -> `PLAY-008` Play access/policy check -> `REL-001` all-131 acceptance evidence -> `REL-003` **explicit human GO/NO-GO** -> `REL-004` manual Play Console submission -> `REL-005` production watch/rollback.

**Controlled monitoring handover:** one SF relation `OPS-LIVE SF -> OPS-INTERIM` keeps interim supervision until live monitoring has begun. SS/FF links are used for constrained overlapping activities; the hard production chain is FS.

## 3. Mandatory release work packages missing from the previous beta schedule

- **GOV-001..005:** signed release-scope definition, BA overlap reconciliation, real capacity/staffing, separate safe write-action contract.
- **ENG-001..002:** reconcile unmerged PRs #27/#30/#32/#33, repair theme/OAuth regressions, preserve existing package/signing/cache-first inbox.
- **INF-001..005:** hosted regional gateway, identity/Google restricted-scope verification, Microsoft Entra, audited vault and Apple developer access. None assumed configured.
- **SEC-001..002:** DPIA/DPA, threat model, isolated provider tokens, authorisation boundaries, independent gateway penetration test and retest.
- **QA-001..004:** CI/SAST/SCA/secret scans, 131 individually evidenced cases, consent/revocation and unauthorised account tests, live provider/device/assistant UAT **on the signed candidate**.
- **PLAY-001..008:** Play Console eligibility, Play App Signing custody, privacy/Data safety declarations, store listing, signed APK+AAB, SHA-256/SBOM/provenance, internal/closed testing and production access checks.
- **REL-001..005 / OPS:** human acceptance, security go/no-go, incident runbook/rollback, **manual** approved Play production submission and operational monitoring.

**Signing:** use existing production certificate lineage and private signing custody; no key or password may be added to repository, app resource, connector logs, test fixtures, issue descriptions or assistant prompt. The CI must be unable to push to Play production without a separate authorised human-controlled action.

## 4. Ownership and AI-human separation

All listed names in the plan are **human role assignments proposed, not confirmed specific people**. A person must be formally named and capacity allocated for each: Product Owner, Solution Architect, Android/iOS/Backend/OAuth/Provider Integration/Workflow/Odoo engineers, Security/Privacy, QA Automation, Signing Custodian, DevOps/Operations and Play Release Manager.

AI resources are **supporting tools** (coding, test generation, document drafting, security triage, release checklist). They cannot independently approve requirements, view/store production signing keys, grant email access, accept penetration-test risk or publish to production.

### Critical resource-leveling blocker

Calendar-date estimates use 5-day Victorian business weeks and nominal task durations, without individual worker calendars or staffing. There are **154 overloaded human role-days** if each named role is staffed by one FTE. Peaks: 22 concurrent Android Engineer tasks (2 December 2026) and 10 concurrent LLM/AI Engineer tasks (18–19 May 2027). **The provisional 1 September 2027 human GO / 2 September manual submission is not a commitment.** GOV-003 must resolve capacity, staffing, holidays, vendor reviews and cost/budget before management baseline approval.

## 5. Product Owner execution and approval order

1. Approve `GOV-001`: **all 131** priority-ID acceptance requirements, controlled P1 operations, iOS artifact separation, and whether an internal beta is allowed before full production acceptance.
2. Approve `GOV-002`: crosswalks/overlaps, nine dependency repairs and independent acceptance proof for every ID. Do not treat a 'mapped feature' as accepted by default.
3. Approve `GOV-003`: nominate actual team members/FTE, budget, deadlines and task owners. Re-run a resource-levelled critical path.
4. `SEC-001` + `INF-002/003/004`: verify Google restricted Gmail scopes, Microsoft Entra grants, data protection and hosted provider controls.
5. `ENG-001`: reconcile legacy draft PRs before coding implementation or bumping Android version.
6. `QA-001` gate: implement actual automated test, privacy, security and cross-account validation. No source-only contract test is enough.
7. `PLAY-005` gate: sign test candidate APK+AAB with verified existing identity and package `au.com.t1xperts.mailxperts`, then `QA-003/004` live/device/UAT.
8. `PLAY-001..008`: Play Console publisher account eligibility, privacy declarations, closed testing if required, pre-launch/policy reviews.
9. `REL-001..003`: **zero outstanding P0/P1 acceptance defects**, no unresolved critical/high security findings, individual 131-ID evidence, signed artifact identity and rollback, named owner **written GO**.
10. **Only then** manually carry out `REL-004` Play production submission, and `REL-005` measured, reversible rollout.

## 6. Exit criteria and evidence ledger

No requirement may be marked production-released merely because a document was merged, a branch exists or aggregate CI passed.

For each P0/P1 ID, record **feature/HLD/LLD approval; merged commit SHA; exact test cases; test execution command + CI URL; security findings/resolution; relevant account/client/device UAT; defect status; Product Owner acceptance; release version/AAB/APK SHA-256; deployment and rollback proof**. Maintain the 44-column original RTM history; the companion 131-row release register does not rewrite accepted evidence.

**Decision status:** proposed execution baseline is ready for review, **not approved for implementation, real mailbox access, spending, production signing, release automation or publishing**. 
