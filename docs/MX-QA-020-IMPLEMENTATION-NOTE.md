Implementation branch: `fix/mx-qa-020-account-identity`.

The fix is intentionally UI-only. It does not alter OAuth tokens, passwords, server configuration, account IDs, or message data. The account switcher will use the actual email address as the compact identity and show the friendly label plus email address in the expanded list.
