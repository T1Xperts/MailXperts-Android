# MailXperts v1.6.0-beta.4

## MX-QA-020 — Account identity clarity

- The selected account in the mailbox header now shows the actual connected email address.
- The expanded account switcher shows the friendly account label together with the actual email address when they differ.
- The mailbox subtitle shows provider name, actual email address and secure transport context.
- Multiple Gmail accounts are now distinguishable at a glance.
- Existing OAuth tokens, passwords, account IDs, sync settings and local mail data are unchanged.

## Validation

- Android build/lint/unit validation passed before release packaging.
- P0 regression workflow passed.
- Production package remains `au.com.t1xperts.mailxperts`.
- This build uses versionCode 18 so it upgrades v1.6.0-beta.3/versionCode 17.
- Production release signing is invoked only through the protected release pipeline or the established offline signing baseline.
