# MailXperts HLD / LLD / Test Catalogue

**Snapshot:** 20 October 2026 · **Coverage:** 66 requirement IDs (MX-QA-001–030 and AI-001–036).

> **Evidence policy:** The HLD entries below are traceability outlines **authored for this report**, not assertions that previously approved HLD documents exist. Each LLD description is an as-built component pointer (when code exists) or a **proposed implementation design**. Likewise each `TC-...-01` is a **proposed test case** and is not evidence of execution. GitHub CI/UAT claims are cited from existing issue/PR records and are kept separate from these proposed test cases.

## High-level design catalogue (RTM outlines, not formal design approvals)

### HLD-CORE-ACCOUNT
Per-account configuration, identity isolation and provider selection.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-RENDER
Sanitised HTML rendering, CID/remote image and safe external-link boundary.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-MIME
Multipart MIME parsing, inline and downloaded attachment repository.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-COMPOSE
Composer, recipients, rich signatures and reply/forward formatting.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-UX
Navigation, message actions, themes and user configuration.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-INTELLIGENCE
Local non-generative message prioritisation and account-aware presentation.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-ANDROID
Android intent/provider permission and lifecycle integration.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-SYNC
Cache-first IMAP synchronisation, tombstones and server-action policy.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-SEARCH
Mailbox query parsing and server/local search integration.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-CONTACTS
Local Smart Contacts plus optional permissioned remote contact connectors.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-RELEASE
Version identity, production signing lineage and release gate automation.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-BUILD-EXPERT
Provider-neutral, server-side AI orchestrator with read-only tool gateway and human approvals.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-CORE-AUTH
OAuth2/XOAUTH2 with Keystore-secured tokens and provider fallbacks.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-AI-P0
Server-side privacy-protected AI gateway and editable Android AI UX; no automated mail send/payment.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-AI-WORKFLOWS
Permissioned agent intent → proposal → verification → explicit action → audit.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-AI-INTELLIGENCE
Source-scoped AI extraction, classification, synthesis, recommendations and corrections.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-AI-PAYMENTS
Financial document handling, control segregation and reviewed payment initiation.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

### HLD-AI-PRIVACY
Opt-in privacy and local-first processing controls.

**Status:** RTM-authored HLD outline only. Not equivalent to a separately reviewed/approved formal HLD. For as-built evidence refer to linked source files and PRs.

## Low-level design trace and planned test cases (all 66 features)

### LLD-MX-QA-001

**Feature:** Add Account identity / credential isolation
- **Requirement:** [MX-QA-001](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-ACCOUNT](#hld-core-account); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-001):** Component-level implementation trace: AccountConfig.java, AccountsActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [AccountConfig.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/AccountConfig.java), [AccountsActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/AccountsActivity.java).
- **Test case to formalise:** `TC-MX-QA-001-01` — Verify add Account identity / credential isolation across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AccountConfigTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/AccountConfigTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.4; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-002

**Feature:** Remote HTTP/HTTPS images
- **Requirement:** [MX-QA-002](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-RENDER](#hld-core-render); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-002):** Component-level implementation trace: EmailHtmlPolicy.java, MessageActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [EmailHtmlPolicy.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/EmailHtmlPolicy.java), [MessageActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java).
- **Test case to formalise:** `TC-MX-QA-002-01` — Verify remote HTTP/HTTPS images across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [EmailHtmlPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/EmailHtmlPolicyTest.java), [MessageWebViewContractTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/MessageWebViewContractTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.4; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-003

**Feature:** CID / embedded inline images
- **Requirement:** [MX-QA-003](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-MIME](#hld-core-mime); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-003):** Component-level implementation trace: InlineImageCid.java, MimeMessageRenderer.java. Review branch/commit for exact behaviour.
- **Code evidence:** [InlineImageCid.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/InlineImageCid.java), [MimeMessageRenderer.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MimeMessageRenderer.java).
- **Test case to formalise:** `TC-MX-QA-003-01` — Verify cID / embedded inline images across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [InlineImageCidTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/InlineImageCidTest.java), [MimeMessageRendererTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/MimeMessageRendererTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.4; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-004

**Feature:** Email hyperlinks / external navigation
- **Requirement:** [MX-QA-004](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-RENDER](#hld-core-render); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-004):** Component-level implementation trace: ExternalLinkPolicy.java, MessageActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [ExternalLinkPolicy.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ExternalLinkPolicy.java), [MessageActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java).
- **Test case to formalise:** `TC-MX-QA-004-01` — Verify email hyperlinks / external navigation across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [ExternalLinkPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/ExternalLinkPolicyTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.4; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-005

**Feature:** Received email attachments
- **Requirement:** [MX-QA-005](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-MIME](#hld-core-mime); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-005):** Component-level implementation trace: MailAttachmentRepository.java, MessageActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MailAttachmentRepository.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MailAttachmentRepository.java), [MessageActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java).
- **Test case to formalise:** `TC-MX-QA-005-01` — Verify received email attachments across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [MailAttachmentRepositoryTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/MailAttachmentRepositoryTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.1; v1.5.4; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-006

**Feature:** Signature logos/images render correctly
- **Requirement:** [MX-QA-006](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-COMPOSE](#hld-core-compose); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-006):** Component-level implementation trace: SignatureHtml.java, SignatureEditorActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [SignatureHtml.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/SignatureHtml.java), [SignatureEditorActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/SignatureEditorActivity.java).
- **Test case to formalise:** `TC-MX-QA-006-01` — Verify signature logos/images render correctly across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [SignatureHtmlTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/SignatureHtmlTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-007

**Feature:** Compact message actions
- **Requirement:** [MX-QA-007](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-UX](#hld-core-ux); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-007):** Component-level implementation trace: MessageActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MessageActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java).
- **Test case to formalise:** `TC-MX-QA-007-01` — Verify compact message actions across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [MessageWebViewContractTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/MessageWebViewContractTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.1; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-008

**Feature:** Per-message overflow menu
- **Requirement:** [MX-QA-008](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-UX](#hld-core-ux); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-008):** Component-level implementation trace: MessageActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MessageActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java).
- **Test case to formalise:** `TC-MX-QA-008-01` — Verify per-message overflow menu across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.1; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-009

**Feature:** Native Print / Save as PDF
- **Requirement:** [MX-QA-009](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-UX](#hld-core-ux); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-009):** Component-level implementation trace: MessageActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MessageActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java).
- **Test case to formalise:** `TC-MX-QA-009-01` — Verify native Print / Save as PDF across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.1; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-010

**Feature:** Reply All / Forward
- **Requirement:** [MX-QA-010](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-COMPOSE](#hld-core-compose); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-010):** Component-level implementation trace: MessageActivity.java, ComposeActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MessageActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java), [ComposeActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ComposeActivity.java).
- **Test case to formalise:** `TC-MX-QA-010-01` — Verify reply All / Forward across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [ReplyForwardFormatterTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/ReplyForwardFormatterTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.1; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-011

**Feature:** Expandable sender / recipient metadata
- **Requirement:** [MX-QA-011](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-UX](#hld-core-ux); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-011):** Component-level implementation trace: MessageActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MessageActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MessageActivity.java).
- **Test case to formalise:** `TC-MX-QA-011-01` — Verify expandable sender / recipient metadata across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-012

**Feature:** HTML readability in dark/light themes
- **Requirement:** [MX-QA-012](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-UX](#hld-core-ux); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-012):** Component-level implementation trace: EmailHtmlPolicy.java, ThemeManager.java. Review branch/commit for exact behaviour.
- **Code evidence:** [EmailHtmlPolicy.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/EmailHtmlPolicy.java), [ThemeManager.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ThemeManager.java).
- **Test case to formalise:** `TC-MX-QA-012-01` — Verify hTML readability in dark/light themes across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [EmailHtmlPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/EmailHtmlPolicyTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.
- **Important note:** A separate day-theme defect is open at issue #12; acceptance concerns HTML readability and must not close that defect.

### LLD-MX-QA-013

**Feature:** Structured full-height navigation drawer
- **Requirement:** [MX-QA-013](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-UX](#hld-core-ux); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-013):** Component-level implementation trace: Navigation.java, MailboxActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [Navigation.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/Navigation.java), [MailboxActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MailboxActivity.java).
- **Test case to formalise:** `TC-MX-QA-013-01` — Verify structured full-height navigation drawer across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-014

**Feature:** Priority banner UX
- **Requirement:** [MX-QA-014](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-INTELLIGENCE](#hld-core-intelligence); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-014):** Component-level implementation trace: MailIntelligence.java, InboxActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MailIntelligence.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MailIntelligence.java), [InboxActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/InboxActivity.java).
- **Test case to formalise:** `TC-MX-QA-014-01` — Verify priority banner UX across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-015

**Feature:** Account/settings hierarchy UX
- **Requirement:** [MX-QA-015](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-UX](#hld-core-ux); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-015):** Component-level implementation trace: SettingsActivity.java, Navigation.java. Review branch/commit for exact behaviour.
- **Code evidence:** [SettingsActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/SettingsActivity.java), [Navigation.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/Navigation.java).
- **Test case to formalise:** `TC-MX-QA-015-01` — Verify account/settings hierarchy UX across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-016

**Feature:** Multiple-recipient parsing
- **Requirement:** [MX-QA-016](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-COMPOSE](#hld-core-compose); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-016):** Component-level implementation trace: RecipientNormalizer.java, RecipientSet.java, ComposeActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [RecipientNormalizer.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/RecipientNormalizer.java), [RecipientSet.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/RecipientSet.java), [ComposeActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ComposeActivity.java).
- **Test case to formalise:** `TC-MX-QA-016-01` — Verify multiple-recipient parsing across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [RecipientNormalizerTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/RecipientNormalizerTest.java), [RecipientSetTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/RecipientSetTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.4; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-017

**Feature:** To / CC / BCC chips and autocomplete
- **Requirement:** [MX-QA-017](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-COMPOSE](#hld-core-compose); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-017):** Component-level implementation trace: RecipientDirectory.java, RecipientSuggestionAdapter.java. Review branch/commit for exact behaviour.
- **Code evidence:** [RecipientDirectory.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/RecipientDirectory.java), [RecipientSuggestionAdapter.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/RecipientSuggestionAdapter.java).
- **Test case to formalise:** `TC-MX-QA-017-01` — Verify to / CC / BCC chips and autocomplete across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [RecipientDirectoryTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/RecipientDirectoryTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-018

**Feature:** Outgoing file attachments
- **Requirement:** [MX-QA-018](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-COMPOSE](#hld-core-compose); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-018):** Component-level implementation trace: ComposeActivity.java, AttachmentStorage.java. Review branch/commit for exact behaviour.
- **Code evidence:** [ComposeActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ComposeActivity.java), [AttachmentStorage.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/AttachmentStorage.java).
- **Test case to formalise:** `TC-MX-QA-018-01` — Verify outgoing file attachments across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [OutgoingAttachmentMimeTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/OutgoingAttachmentMimeTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.4; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-019

**Feature:** Share-to-MailXperts
- **Requirement:** [MX-QA-019](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-ANDROID](#hld-core-android); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-019):** Component-level implementation trace: AndroidManifest.xml, ComposeActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [AndroidManifest.xml](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/AndroidManifest.xml), [ComposeActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ComposeActivity.java).
- **Test case to formalise:** `TC-MX-QA-019-01` — Verify share-to-MailXperts across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [ManifestContractTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/ManifestContractTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-020

**Feature:** Current Inbox account identity
- **Requirement:** [MX-QA-020](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-ACCOUNT](#hld-core-account); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-020):** Component-level implementation trace: AccountIdentity.java, MailboxActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [AccountIdentity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/AccountIdentity.java), [MailboxActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MailboxActivity.java).
- **Test case to formalise:** `TC-MX-QA-020-01` — Verify current Inbox account identity across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AccountIdentityContractTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/AccountIdentityContractTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0 beta (exact patch unverified); Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-021

**Feature:** Quick account switcher
- **Requirement:** [MX-QA-021](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-ACCOUNT](#hld-core-account); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-021):** Component-level implementation trace: MailboxActivity.java, AccountIdentity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MailboxActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MailboxActivity.java), [AccountIdentity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/AccountIdentity.java).
- **Test case to formalise:** `TC-MX-QA-021-01` — Verify quick account switcher across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AccountIdentityTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/AccountIdentityTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-022

**Feature:** Safe/friendly provider errors
- **Requirement:** [MX-QA-022](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-ACCOUNT](#hld-core-account); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-022):** Component-level implementation trace: ProviderErrorMessage.java, AccountsActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [ProviderErrorMessage.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ProviderErrorMessage.java), [AccountsActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/AccountsActivity.java).
- **Test case to formalise:** `TC-MX-QA-022-01` — Verify safe/friendly provider errors across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [ProviderErrorMessageTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/ProviderErrorMessageTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-023

**Feature:** Server-delete controls
- **Requirement:** [MX-QA-023](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-SYNC](#hld-core-sync); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-023):** Component-level implementation trace: SyncPolicy.java, MailRepository.java. Review branch/commit for exact behaviour.
- **Code evidence:** [SyncPolicy.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/SyncPolicy.java), [MailRepository.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MailRepository.java).
- **Test case to formalise:** `TC-MX-QA-023-01` — Verify server-delete controls across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [SyncPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/SyncPolicyTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-024

**Feature:** Robust multipart parsing
- **Requirement:** [MX-QA-024](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-MIME](#hld-core-mime); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-024):** Component-level implementation trace: MimeMessageRenderer.java, MimePartClassifier.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MimeMessageRenderer.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MimeMessageRenderer.java), [MimePartClassifier.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MimePartClassifier.java).
- **Test case to formalise:** `TC-MX-QA-024-01` — Verify robust multipart parsing across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [MimeMessageRendererTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/MimeMessageRendererTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.4; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-025

**Feature:** Preserve previous conversation chain
- **Requirement:** [MX-QA-025](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-COMPOSE](#hld-core-compose); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-025):** Component-level implementation trace: ReplyForwardFormatter.java, ComposeActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [ReplyForwardFormatter.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ReplyForwardFormatter.java), [ComposeActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/ComposeActivity.java).
- **Test case to formalise:** `TC-MX-QA-025-01` — Verify preserve previous conversation chain across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [ReplyForwardFormatterTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/ReplyForwardFormatterTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.2; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-026

**Feature:** Advanced mailbox search
- **Requirement:** [MX-QA-026](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-SEARCH](#hld-core-search); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-026):** Component-level implementation trace: MailSearchSpec.java, SearchActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [MailSearchSpec.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MailSearchSpec.java), [SearchActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/SearchActivity.java).
- **Test case to formalise:** `TC-MX-QA-026-01` — Verify advanced mailbox search across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [MailSearchSpecTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/MailSearchSpecTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.5; Product Owner accepted; public production release not independently verified.

### LLD-MX-QA-027

**Feature:** Smart Contacts + Android/Google/iCloud contact integration
- **Requirement:** [MX-QA-027](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-CONTACTS](#hld-core-contacts); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-027):** Component-level implementation trace: SmartContactsActivity.java, DeviceContactDirectory.java, RecipientDirectory.java, CloudContactsActivity.java. Review branch/commit for exact behaviour.
- **Code evidence:** [SmartContactsActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/SmartContactsActivity.java), [DeviceContactDirectory.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/DeviceContactDirectory.java), [RecipientDirectory.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/RecipientDirectory.java), [CloudContactsActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mx-qa-027-029-work/app/src/main/java/au/com/t1xperts/mailxperts/CloudContactsActivity.java).
- **Test case to formalise:** `TC-MX-QA-027-01` — Verify smart Contacts + Android/Google/iCloud contact integration across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [RecipientDirectoryTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/RecipientDirectoryTest.java), [CloudContactsSecurityContractTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mx-qa-027-029-work/app/src/test/java/au/com/t1xperts/mailxperts/CloudContactsSecurityContractTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.6.0-beta.2; beta.8 candidate; Product Owner accepted; public production release not independently verified.
- **Important note:** Android scope Product Owner accepted; direct cloud providers implemented only on draft PR #27 and still require provider/device UAT.

### LLD-MX-QA-028

**Feature:** In-place production APK upgrade identity
- **Requirement:** [MX-QA-028](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-RELEASE](#hld-core-release); **stage:** UAT accepted (owner baseline 2026-10-12).
- **LLD outline (MX-QA-028):** Component-level implementation trace: build.gradle. Review branch/commit for exact behaviour.
- **Code evidence:** [build.gradle](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/build.gradle).
- **Test case to formalise:** `TC-MX-QA-028-01` — Verify in-place production APK upgrade identity across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [ReleaseIdentityContractTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/ReleaseIdentityContractTest.java).
- **Result evidence:** UAT accepted by Product Owner; individual test-case execution report not verified; **QA:** PASS (Product Owner acceptance, not per-case execution); **cycle:** PO-UAT-2026-10-12 (aggregate).
- **Release:** v1.5.4 / subsequent identities; Product Owner accepted; public production release not independently verified.
- **Important note:** Upgrade identity accepted; signed production APK and installation are separate release gates.

### LLD-MX-QA-029

**Feature:** Provider-neutral Build Expert AI framework, OpenAI first
- **Requirement:** [MX-QA-029](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-BUILD-EXPERT](#hld-build-expert); **stage:** Partially implemented, review pending.
- **LLD outline (MX-QA-029):** Component-level implementation trace: orchestrator.mjs, policy.mjs. Review branch/commit for exact behaviour.
- **Code evidence:** [orchestrator.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mx-qa-027-029-work/build-expert/src/orchestrator.mjs), [policy.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mx-qa-027-029-work/build-expert/src/policy.mjs).
- **Test case to formalise:** `TC-MX-QA-029-01` — Verify provider-neutral Build Expert AI framework, OpenAI first across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [security.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mx-qa-027-029-work/build-expert/test/security.test.mjs), [gateway.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/mx-qa-027-029-work/build-expert/test/gateway.test.mjs).
- **Result evidence:** Beta.8 automated CI PASS for read-only foundation; full feature QA pending; **QA:** PARTIAL / not fully QA accepted; **cycle:** CI-beta.8: 37999716093 / 37999716115 / 37999716127.
- **Release:** v1.6.0-beta.8 candidate; Draft PR / not merged or released.
- **Important note:** Secure read-only foundation only; human-controlled write handlers, runner, spend controls and operational QA pending.

### LLD-MX-QA-030

**Feature:** Google/Microsoft OAuth2, XOAUTH2 and secure fallbacks
- **Requirement:** [MX-QA-030](https://github.com/T1Xperts/MailXperts-Android/issues/3); **HLD:** [HLD-CORE-AUTH](#hld-core-auth); **stage:** Code complete; physical-device UAT pending.
- **LLD outline (MX-QA-030):** Component-level implementation trace: GoogleOAuthManager.java, MicrosoftOAuthManager.java, MailAuth.java. Review branch/commit for exact behaviour.
- **Code evidence:** [GoogleOAuthManager.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/GoogleOAuthManager.java), [MicrosoftOAuthManager.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MicrosoftOAuthManager.java), [MailAuth.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/main/java/au/com/t1xperts/mailxperts/MailAuth.java).
- **Test case to formalise:** `TC-MX-QA-030-01` — Verify google/Microsoft OAuth2, XOAUTH2 and secure fallbacks across valid, invalid, restart and permission-boundary cases. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [GmailOAuthTransportContractTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/GmailOAuthTransportContractTest.java), [OAuthLoginRegressionContractTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/OAuthLoginRegressionContractTest.java), [AuthArchitectureTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/main/app/src/test/java/au/com/t1xperts/mailxperts/AuthArchitectureTest.java).
- **Result evidence:** Automated OAuth contracts PASS; live provider/device retest pending; **QA:** AUTOMATED PASS / DEVICE UAT PENDING; **cycle:** CI-beta.7; device-cycle pending.
- **Release:** v1.6.0-beta.3; beta.7 fix; Code merged; device UAT pending; no closure.
- **Important note:** Automated OAuth contracts passed; real Gmail XOAUTH2 device/provider UAT still pending.

### LLD-AI-001

**Feature:** As a user, I want MailXperts AI to summarise an email or thread instantly, so I can understand long conversations quickly.
- **Requirement:** [AI-001](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-P0](#hld-ai-p0); **stage:** Draft PR #30: code complete, automated QA PASS, live gateway and device UAT pending.
- **LLD outline (AI-001):** Given a received email/thread, request concise source-grounded summary; show errors safely.
- **Code evidence:** [AiAssistantActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiAssistantActivity.java), [AiGatewayClient.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiGatewayClient.java), [mail-assistant.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/src/mail-assistant.mjs).
- **Test case to formalise:** `TC-AI-001-01` — Given a received email/thread, request concise source-grounded summary; show errors safely. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AiPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/test/java/au/com/t1xperts/mailxperts/AiPolicyTest.java), [mail-assistant.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/test/mail-assistant.test.mjs).
- **Result evidence:** PR #30 automated suites PASS in aggregate; planned scenario not separately evidenced; **QA:** AUTOMATED PASS / UAT PENDING; **cycle:** CI-beta.9 run 38560840280; package correction CI #134; device UAT pending.
- **Release:** v1.6.0-beta.9 / versionCode 23 (draft candidate); Draft PR #30 / NOT MERGED / NOT RELEASED.
- **Important note:** Formal backlog table lags implementation comment; status reconciled against PR #30 and #29 comment.

### LLD-AI-002

**Feature:** As a user, I want AI to draft a contextual reply using the current email/thread, so I can respond faster without re-explaining context.
- **Requirement:** [AI-002](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-P0](#hld-ai-p0); **stage:** Draft PR #30: code complete, automated QA PASS, live gateway and device UAT pending.
- **LLD outline (AI-002):** Given a selected email, draft reply into editable composer; never auto-send.
- **Code evidence:** [AiAssistantActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiAssistantActivity.java), [AiGatewayClient.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiGatewayClient.java), [mail-assistant.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/src/mail-assistant.mjs).
- **Test case to formalise:** `TC-AI-002-01` — Given a selected email, draft reply into editable composer; never auto-send. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AiPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/test/java/au/com/t1xperts/mailxperts/AiPolicyTest.java), [mail-assistant.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/test/mail-assistant.test.mjs).
- **Result evidence:** PR #30 automated suites PASS in aggregate; planned scenario not separately evidenced; **QA:** AUTOMATED PASS / UAT PENDING; **cycle:** CI-beta.9 run 38560840280; package correction CI #134; device UAT pending.
- **Release:** v1.6.0-beta.9 / versionCode 23 (draft candidate); Draft PR #30 / NOT MERGED / NOT RELEASED.
- **Important note:** Formal backlog table lags implementation comment; status reconciled against PR #30 and #29 comment.

### LLD-AI-003

**Feature:** As a user, I want to Ask AI questions about the current email/thread, so I can extract facts, decisions, risks and next steps conversationally.
- **Requirement:** [AI-003](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-P0](#hld-ai-p0); **stage:** Draft PR #30: code complete, automated QA PASS, live gateway and device UAT pending.
- **LLD outline (AI-003):** Ask a question about current email; answer grounded in selected permitted content.
- **Code evidence:** [AiAssistantActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiAssistantActivity.java), [AiGatewayClient.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiGatewayClient.java), [mail-assistant.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/src/mail-assistant.mjs).
- **Test case to formalise:** `TC-AI-003-01` — Ask a question about current email; answer grounded in selected permitted content. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AiPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/test/java/au/com/t1xperts/mailxperts/AiPolicyTest.java), [mail-assistant.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/test/mail-assistant.test.mjs).
- **Result evidence:** PR #30 automated suites PASS in aggregate; planned scenario not separately evidenced; **QA:** AUTOMATED PASS / UAT PENDING; **cycle:** CI-beta.9 run 38560840280; package correction CI #134; device UAT pending.
- **Release:** v1.6.0-beta.9 / versionCode 23 (draft candidate); Draft PR #30 / NOT MERGED / NOT RELEASED.
- **Important note:** Formal backlog table lags implementation comment; status reconciled against PR #30 and #29 comment.

### LLD-AI-004

**Feature:** As a user, I want to search my mailbox using natural language, so I can find messages without knowing exact keywords or folders.
- **Requirement:** [AI-004](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-P0](#hld-ai-p0); **stage:** Draft PR #30: code complete, automated QA PASS, live gateway and device UAT pending.
- **LLD outline (AI-004):** Convert a natural-language query into supported mailbox filters; execute read-only search.
- **Code evidence:** [AiAssistantActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiAssistantActivity.java), [AiGatewayClient.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiGatewayClient.java), [mail-assistant.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/src/mail-assistant.mjs).
- **Test case to formalise:** `TC-AI-004-01` — Convert a natural-language query into supported mailbox filters; execute read-only search. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AiPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/test/java/au/com/t1xperts/mailxperts/AiPolicyTest.java), [mail-assistant.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/test/mail-assistant.test.mjs).
- **Result evidence:** PR #30 automated suites PASS in aggregate; planned scenario not separately evidenced; **QA:** AUTOMATED PASS / UAT PENDING; **cycle:** CI-beta.9 run 38560840280; package correction CI #134; device UAT pending.
- **Release:** v1.6.0-beta.9 / versionCode 23 (draft candidate); Draft PR #30 / NOT MERGED / NOT RELEASED.
- **Important note:** Formal backlog table lags implementation comment; status reconciled against PR #30 and #29 comment.

### LLD-AI-005

**Feature:** As a user, I want an AI Priority Inbox / What needs my attention? view, so urgent and important mail rises above noise.
- **Requirement:** [AI-005](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-P0](#hld-ai-p0); **stage:** Draft PR #30: code complete, automated QA PASS, live gateway and device UAT pending.
- **LLD outline (AI-005):** Rank recent mail with explanation; do not expose content from unselected accounts.
- **Code evidence:** [AiAssistantActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiAssistantActivity.java), [AiGatewayClient.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiGatewayClient.java), [mail-assistant.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/src/mail-assistant.mjs).
- **Test case to formalise:** `TC-AI-005-01` — Rank recent mail with explanation; do not expose content from unselected accounts. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AiPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/test/java/au/com/t1xperts/mailxperts/AiPolicyTest.java), [mail-assistant.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/test/mail-assistant.test.mjs).
- **Result evidence:** PR #30 automated suites PASS in aggregate; planned scenario not separately evidenced; **QA:** AUTOMATED PASS / UAT PENDING; **cycle:** CI-beta.9 run 38560840280; package correction CI #134; device UAT pending.
- **Release:** v1.6.0-beta.9 / versionCode 23 (draft candidate); Draft PR #30 / NOT MERGED / NOT RELEASED.
- **Important note:** Formal backlog table lags implementation comment; status reconciled against PR #30 and #29 comment.

### LLD-AI-006

**Feature:** As a user, I want AI to extract actions, deadlines, commitments and decisions, so important obligations do not disappear inside email text.
- **Requirement:** [AI-006](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-P0](#hld-ai-p0); **stage:** Draft PR #30: code complete, automated QA PASS, live gateway and device UAT pending.
- **LLD outline (AI-006):** Extract named actions, owners and dates; distinguish missing versus inferred fields.
- **Code evidence:** [AiAssistantActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiAssistantActivity.java), [AiGatewayClient.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiGatewayClient.java), [mail-assistant.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/src/mail-assistant.mjs).
- **Test case to formalise:** `TC-AI-006-01` — Extract named actions, owners and dates; distinguish missing versus inferred fields. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AiPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/test/java/au/com/t1xperts/mailxperts/AiPolicyTest.java), [mail-assistant.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/test/mail-assistant.test.mjs).
- **Result evidence:** PR #30 automated suites PASS in aggregate; planned scenario not separately evidenced; **QA:** AUTOMATED PASS / UAT PENDING; **cycle:** CI-beta.9 run 38560840280; package correction CI #134; device UAT pending.
- **Release:** v1.6.0-beta.9 / versionCode 23 (draft candidate); Draft PR #30 / NOT MERGED / NOT RELEASED.
- **Important note:** Formal backlog table lags implementation comment; status reconciled against PR #30 and #29 comment.

### LLD-AI-007

**Feature:** As a user, I want AI to rewrite my draft to be clearer, shorter, more formal, friendlier or otherwise fit my intent.
- **Requirement:** [AI-007](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-P0](#hld-ai-p0); **stage:** Draft PR #30: code complete, automated QA PASS, live gateway and device UAT pending.
- **LLD outline (AI-007):** Rewrite user draft to chosen tone without automatically sending.
- **Code evidence:** [AiAssistantActivity.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiAssistantActivity.java), [AiGatewayClient.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/main/java/au/com/t1xperts/mailxperts/AiGatewayClient.java), [mail-assistant.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/src/mail-assistant.mjs).
- **Test case to formalise:** `TC-AI-007-01` — Rewrite user draft to chosen tone without automatically sending. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** [AiPolicyTest.java](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/app/src/test/java/au/com/t1xperts/mailxperts/AiPolicyTest.java), [mail-assistant.test.mjs](https://github.com/T1Xperts/MailXperts-Android/blob/feature/ai-p0-mvp/build-expert/test/mail-assistant.test.mjs).
- **Result evidence:** PR #30 automated suites PASS in aggregate; planned scenario not separately evidenced; **QA:** AUTOMATED PASS / UAT PENDING; **cycle:** CI-beta.9 run 38560840280; package correction CI #134; device UAT pending.
- **Release:** v1.6.0-beta.9 / versionCode 23 (draft candidate); Draft PR #30 / NOT MERGED / NOT RELEASED.
- **Important note:** Formal backlog table lags implementation comment; status reconciled against PR #30 and #29 comment.

### LLD-AI-008

**Feature:** As a user, I want one-tap suggested replies for routine messages, so simple responses take seconds.
- **Requirement:** [AI-008](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-008):** Propose routine replies; require explicit review and Send.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-008-01` — Propose routine replies; require explicit review and Send. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-009

**Feature:** As a user, I want AI to detect unanswered or promised follow-ups and remind me, so conversations do not fall through the cracks.
- **Requirement:** [AI-009](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-009):** Detect unanswered obligations and suppress duplicate reminders.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-009-01` — Detect unanswered obligations and suppress duplicate reminders. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-010

**Feature:** As a user, I want a daily Inbox Briefing summarising priority messages, actions, deadlines and waiting-for items.
- **Requirement:** [AI-010](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-010):** Deliver permission-scoped daily priority/action/waiting summary.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-010-01` — Deliver permission-scoped daily priority/action/waiting summary. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-011

**Feature:** As a multi-account user, I want cross-account AI Q&A, so one query can reason across permitted mailboxes.
- **Requirement:** [AI-011](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-011):** Answer only across explicitly selected authorised accounts.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-011-01` — Answer only across explicitly selected authorised accounts. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-012

**Feature:** As a user, I want AI to summarise and answer questions about attachments such as PDF/Word documents without manually reading each file.
- **Requirement:** [AI-012](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-012):** Read supported attachment safely and cite source document.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-012-01` — Read supported attachment safely and cite source document. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-013

**Feature:** As a user, I want to select multiple emails and ask AI to compare/synthesise them, so I can understand a topic across many messages.
- **Requirement:** [AI-013](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-013):** Compare several selected emails and show provenance.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-013-01` — Compare several selected emails and show provenance. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-014

**Feature:** As a user, I want tone, sentiment and clarity coaching before Send, so messages better match my intention.
- **Requirement:** [AI-014](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-014):** Coach tone/clarity before Send without altering text silently.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-014-01` — Coach tone/clarity before Send without altering text silently. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-015

**Feature:** As a user, I want AI to learn my preferred writing style with permission, so drafts sound consistent with me.
- **Requirement:** [AI-015](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-015):** Apply learned writing style only with opt-in and deletion controls.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-015-01` — Apply learned writing style only with opt-in and deletion controls. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-016

**Feature:** As a user, I want AI to identify invoices, bills, amounts, due dates and payment references, so financial obligations are easier to track.
- **Requirement:** [AI-016](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-PAYMENTS](#hld-ai-payments); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-016):** Detect invoices and extract supplier, reference, amount, due date and tax with confidence.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-016-01` — Detect invoices and extract supplier, reference, amount, due date and tax with confidence. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-017

**Feature:** As a user, I want to turn an email or extracted action into a task/to-do, so execution follows directly from communication.
- **Requirement:** [AI-017](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-WORKFLOWS](#hld-ai-workflows); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-017):** Create editable task from a selected email with source trace.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-017-01` — Create editable task from a selected email with source trace. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-018

**Feature:** As a user, I want AI to create calendar events from email content, with details prefilled for review.
- **Requirement:** [AI-018](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-WORKFLOWS](#hld-ai-workflows); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-018):** Prefill a calendar event from email, only save after confirmation.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-018-01` — Prefill a calendar event from email, only save after confirmation. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-019

**Feature:** As a user, I want AI-assisted scheduling / Find Time, so meeting coordination is faster.
- **Requirement:** [AI-019](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-WORKFLOWS](#hld-ai-workflows); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-019):** Suggest free/busy-compatible meeting slots without sending invites automatically.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-019-01` — Suggest free/busy-compatible meeting slots without sending invites automatically. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-020

**Feature:** As a user, I want to create mailbox rules in plain English, so automation does not require learning complex rule builders.
- **Requirement:** [AI-020](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-WORKFLOWS](#hld-ai-workflows); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-020):** Translate rule request to previewable mailbox rule; require confirmation.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-020-01` — Translate rule request to previewable mailbox rule; require confirmation. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-021

**Feature:** As a user, I want AI Smart Notifications based on meaning and urgency, not only sender or folder.
- **Requirement:** [AI-021](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-021):** Notify only for user-approved semantic priority; offer mute/correction.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-021-01` — Notify only for user-approved semantic priority; offer mute/correction. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-022

**Feature:** As a user, I want AI to classify mail into useful categories/Smart Splits automatically, so the inbox remains organised.
- **Requirement:** [AI-022](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-022):** Classify messages with reversible user override.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-022-01` — Classify messages with reversible user override. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-023

**Feature:** As a user, I want AI to separate newsletters, bulk notifications and low-value noise, so human conversations and important work remain prominent.
- **Requirement:** [AI-023](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-023):** Identify marketing/bulk noise with reversible treatment.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-023-01` — Identify marketing/bulk noise with reversible treatment. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-024

**Feature:** As a user, I want AI to translate incoming mail and draft replies in another language, while preserving meaning and tone.
- **Requirement:** [AI-024](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-024):** Translate source and reply preserving meaning and original message.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-024-01` — Translate source and reply preserving meaning and original message. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-025

**Feature:** As a user, I want a ChatGPT ↔ MailXperts connector/plugin so, with permission, ChatGPT can search/read MailXperts mail and power summaries, workflows and actions.
- **Requirement:** [AI-025](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-WORKFLOWS](#hld-ai-workflows); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-025):** Expose permission-scoped, revocable MailXperts tool surface to ChatGPT.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-025-01` — Expose permission-scoped, revocable MailXperts tool surface to ChatGPT. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-026

**Feature:** As a user, I want to create AI workflows and automations in natural language, such as “if no reply after five days, remind me.”
- **Requirement:** [AI-026](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-WORKFLOWS](#hld-ai-workflows); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-026):** Construct workflow preview, risk classification, approval and audit before activation.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-026-01` — Construct workflow preview, risk classification, approval and audit before activation. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-027

**Feature:** As a user, I want MailXperts to explain why a message was prioritised, so AI ranking is understandable and correctable.
- **Requirement:** [AI-027](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-027):** Explain why message was prioritised with editable signal controls.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-027-01` — Explain why message was prioritised with editable signal controls. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-028

**Feature:** As a user, I want unsubscribe/block recommendations for unwanted senders and newsletters, with explicit confirmation.
- **Requirement:** [AI-028](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-028):** Suggest unsubscribe/block, never execute without explicit confirmation.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-028-01` — Suggest unsubscribe/block, never execute without explicit confirmation. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-029

**Feature:** As a user, I want AI to suggest strong subject lines from the message context.
- **Requirement:** [AI-029](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-029):** Suggest relevant subject lines editable by user.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-029-01` — Suggest relevant subject lines editable by user. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-030

**Feature:** As an organisation, I want shared AI prompts/templates and approved response patterns for consistent communication.
- **Requirement:** [AI-030](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-030):** Support role-scoped team prompt templates with versioning and approval.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-030-01` — Support role-scoped team prompt templates with versioning and approval. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-031

**Feature:** As a user, I want a voice inbox experience, for example “Tell me what I missed today,” with concise spoken results.
- **Requirement:** [AI-031](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-031):** Provide accessible spoken inbox briefing after permissioned query.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-031-01` — Provide accessible spoken inbox briefing after permissioned query. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-032

**Feature:** As a user, I want AI to build a customer/project/topic timeline from related emails, so history and decisions are easy to reconstruct.
- **Requirement:** [AI-032](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-032):** Construct time-ordered customer/project events with source citations.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-032-01` — Construct time-ordered customer/project events with source citations. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-033

**Feature:** As a business user, I want mailbox analytics and communication trends, such as response times, unresolved threads and recurring demand.
- **Requirement:** [AI-033](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-INTELLIGENCE](#hld-ai-intelligence); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-033):** Aggregate mailbox response trends without leaking messages across accounts.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-033-01` — Aggregate mailbox response trends without leaking messages across accounts. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-034

**Feature:** As a user, I want BillPay initiation from a recognised invoice, with identity verification, explicit confirmation and regulated payment integration.
- **Requirement:** [AI-034](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-PAYMENTS](#hld-ai-payments); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-034):** Prepare BillPay initiation only under strong authentication, payee checks and explicit confirmation.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-034-01` — Prepare BillPay initiation only under strong authentication, payee checks and explicit confirmation. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-035

**Feature:** As a privacy-conscious user, I want privacy controls and local/on-device AI where technically practical, with clear disclosure of what data leaves the device.
- **Requirement:** [AI-035](https://github.com/T1Xperts/MailXperts-Android/issues/29); **HLD:** [HLD-AI-PRIVACY](#hld-ai-privacy); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-035):** Expose AI privacy/retention controls, revocation and available on-device alternatives.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-035-01` — Expose AI privacy/retention controls, revocation and available on-device alternatives. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.

### LLD-AI-036

**Feature:** As a user, when an invoice is sent to my email, I want MailXperts to create the corresponding vendor bill in Odoo Accounting as Draft only, so I can review/correct it before I manually post and pay it. See #31.
- **Requirement:** [AI-036](https://github.com/T1Xperts/MailXperts-Android/issues/31); **HLD:** [HLD-AI-PAYMENTS](#hld-ai-payments); **stage:** Product backlog; no linked implementation or QA.
- **LLD outline (AI-036):** Detect supplier invoice → parse lines/tax → deduplicate → authenticate to correct Odoo company → create account.move vendor bill in DRAFT ONLY → attach source → human reviews/posts/pays; no automated posting or payment.
- **Code evidence:** Not implemented / not linked.
- **Test case to formalise:** `TC-AI-036-01` — Detect supplier invoice → parse lines/tax → deduplicate → authenticate to correct Odoo company → create account.move vendor bill in DRAFT ONLY → attach source → human reviews/posts/pays; no automated posting or payment. **This test case ID and scenario are proposed in this RTM; not a completed test execution record.**
- **Existing automated classes (where identifiable):** No story-specific automated test linked.
- **Result evidence:** NOT RUN (planned test); **QA:** NOT RUN; **cycle:** Not scheduled.
- **Release:** Not assigned; Not developed / NOT RELEASED.
- **Important note:** HARD CONTROL: create an Odoo draft vendor bill only; no automatic post/validate/approve/pay. Depends on AI-016.


## Mandatory security / QA design review gates

- Server-only secrets and per-account permission scoping; no OpenAI/Odoo credential bundled into the Android APK.
- All email content, attachments, PR text and external content treated as untrusted. Source content cannot escalate to a tool command.
- Draft invoices may be created in Odoo only after duplicate/vendor/company checks. **Never post, validate, approve or pay automatically** for AI-036.
- AI-generated replies may populate editable Compose but **never send automatically**.
- UAT and formal testcase results require execution records with owner, environment, datetime, actual result, build SHA, evidence, pass/fail and linked defect IDs before closure.
- Every new feature needs HLD sign-off, LLD review, unit/integration/security test, device/UAT record, and release gate evidence. Existing acceptance in issue #3 is not a substitute for per-case transcripts.
