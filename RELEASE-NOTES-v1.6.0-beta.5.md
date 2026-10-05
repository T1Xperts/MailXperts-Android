# MailXperts v1.6.0-beta.5

## Consolidated backlog completion

This beta consolidates MX-QA-006, 011, 012, 013, 014, 015, 017, 019, 021, 022, 023 and 026.

- HTML signatures can preview remote http/https logos/images in the secure rich editor while local file/content access remains blocked.
- Expandable sender/recipient/message metadata remains available from message view and is selectable/copyable.
- HTML mail uses a neutral light sender-content canvas so app dark/light chrome does not corrupt sender formatting.
- Full-height structured navigation drawer is covered by release contracts.
- Smart Priority presentation remains compact.
- Account/settings forms use grouped sections and collapsed advanced server settings.
- To/Cc/Bcc now use chip-style autocomplete tokens and cross-field duplicate prevention.
- Android SEND/SEND_MULTIPLE share intents open MailXperts compose with shared text/files.
- Inbox header provides direct multi-account switching with actual email identity.
- Provider authentication errors are mapped to safe, actionable messages instead of raw server text.
- Server-delete mode is explicit, safe-by-default and consistent between settings and message actions.
- Advanced search supports account/folder scope, all/any/exact matching, wildcard-style partial matching and origin-labelled results.

## Release identity

- versionName: `1.6.0-beta.5`
- versionCode: `19`
- package: `au.com.t1xperts.mailxperts`
- implementation PR: **#24**
- merged source commit: `7138d8d935c5684f01fe13cfd2939541c8bec806`

## Automated validation

- PR standard Android build/lint/unit validation: **PASS**
- PR P0 regression and backlog contract suite: **PASS**
- Main standard Android validation: **PASS**
- Main P0 regression / release identity verification: **PASS**
- GitHub-hosted production signing remains blocked at permanent-keystore restoration because the protected signing secret is not available to that workflow.
- The established offline production-signing path is used for the distributable beta.5 APK.

## Lifecycle disposition

MX-QA-006, 011, 012, 013, 014, 015, 017, 019, 021, 022, 023 and 026 are **code complete** in this build. Device QA/UAT remains required before Product Owner lifecycle closure.
