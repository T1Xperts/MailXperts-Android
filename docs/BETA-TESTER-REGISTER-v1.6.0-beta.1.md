# MailXperts 1.6.0 Beta 1 — Tester Register & QA Plan

Use one row per tester/device. Do not record mailbox passwords, App Passwords, OAuth tokens or private email content in this register.

## Tester register

| Tester ID | Name / Alias | Device | Android | Mail providers tested | Install type | Build | Start date | Result | Notes / defect IDs |
|---|---|---|---|---|---|---|---|---|---|
| BETA-001 |  |  |  |  | Fresh / Upgrade | 1.6.0-beta.1 (15) |  | Pending |  |

## Core beta test matrix

| Test | Related QA | Expected result | Tester result |
|---|---|---|---|
| Upgrade existing MailXperts | MX-QA-028 | Android offers Update; accounts/settings remain | Pending |
| Add new account | MX-QA-001 | Identity/credential fields start blank | Pending |
| Gmail connection | MX-QA-022/030 | App Password flow is clear; IMAP/SMTP failures are staged and safe | Pending |
| Inbox initial load/sync | Core sync | Cached mail appears quickly; background sync completes | Pending |
| Remote images | MX-QA-002 | HTTPS/allowed images render in real HTML mail | Pending |
| CID images | MX-QA-003/024 | Embedded inline images render correctly | Pending |
| Hyperlinks | MX-QA-004 | Web links open externally without ERR_CACHE_MISS | Pending |
| Incoming attachment | MX-QA-005 | File is listed; Open/Save/Share works | Pending |
| Signature image | MX-QA-006 | Sent signature logo/image displays in receiving client | Pending |
| Message actions | MX-QA-007/008/010 | Compact actions, overflow, Reply All and Forward work | Pending |
| Print/PDF | MX-QA-009 | Android print flow opens and PDF can be saved | Pending |
| Message details | MX-QA-011 | Expanded metadata can be shown/hidden | Pending |
| Light/dark rendering | MX-QA-012 | Sender formatting remains readable and uncorrupted | Pending |
| Navigation drawer | MX-QA-013 | Full-height drawer opens and destinations work | Pending |
| Smart Priority | MX-QA-014 | Priority cue is compact and usable | Pending |
| Settings hierarchy | MX-QA-015 | Normal settings are clear; advanced servers can be expanded | Pending |
| Multiple recipients | MX-QA-016/017 | To/Cc/Bcc accepts comma/semicolon lists and autocomplete | Pending |
| Outgoing attachment | MX-QA-018 | Recipient receives intact file attachment | Pending |
| Android share target | MX-QA-019 | Share sheet offers MailXperts and imports content/file | Pending |
| Account identity/switching | MX-QA-020/021 | Active account is obvious and switcher works | Pending |
| Delete policy | MX-QA-023 | Local-only vs server Trash behaviour matches setting | Pending |
| MIME corpus | MX-QA-024 | Alternative/related/mixed messages render without loss/crash | Pending |
| Reply/forward chain | MX-QA-025 | Previous thread content is preserved safely | Pending |
| Advanced search | MX-QA-026 | account/folder scopes and matching modes return expected mail | Pending |
| Learned recipients | MX-QA-027 | suggestions persist locally and can be cleared | Pending |

## Defect reporting
For a failure, record: tester ID, build/versionCode, device/Android version, mail provider, QA ID or feature area, exact steps, expected result, actual result, screenshot/screen recording if appropriate, and whether it reproduces after restarting MailXperts.

Never include passwords, App Passwords, authentication tokens or private signing material in a defect report.
