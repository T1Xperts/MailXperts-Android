# MX-QA-020 — Current account identity must be obvious

## Problem
The account switcher can show only a friendly label such as `Google Gmail`. That does not tell the user which actual Gmail account is connected, especially when more than one Gmail account is configured.

## User story
As a MailXperts user, I want the account switcher and mailbox header to show the actual connected email address so that I always know which mailbox I am viewing or switching to.

## Acceptance criteria
- The collapsed account switcher shows the actual email address for a specific account.
- The expanded account switcher shows both the friendly account label and actual email address when they differ.
- The mailbox subtitle shows provider name and actual email address.
- The All Accounts option remains unchanged.
- Add Account and Manage Accounts actions remain unchanged.
- Existing account IDs, authentication, sync settings and local data are not modified.
- Unit tests cover compact, expanded and subtitle identity formatting.
