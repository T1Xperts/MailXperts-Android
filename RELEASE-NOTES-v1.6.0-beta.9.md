# MailXperts v1.6.0-beta.9

**versionCode:** 23  
**Package:** `au.com.t1xperts.mailxperts`

## End-user AI P0 MVP — AI-001 to AI-007

This beta adds the first user-facing MailXperts AI experience as a dedicated workspace available from the navigation drawer.

### AI-001 — Summarise email / thread
- Select a recent message and generate a concise AI summary.
- Quoted conversation content present in the message body is included as thread context.

### AI-002 — Contextual reply drafting
- Generate a reply from the selected message context.
- The AI draft opens in the normal MailXperts rich-text composer for review/editing.
- AI never sends automatically.

### AI-003 — Ask AI about the current email/thread
- Ask free-form questions about the selected message.
- Answers are constrained to supplied mailbox context and must state uncertainty when evidence is insufficient.

### AI-004 — Natural-language mailbox search
- Describe what you want to find in normal language.
- AI converts the request to bounded mailbox search keywords and MailXperts executes the search using its existing mailbox search engine.

### AI-005 — AI Priority Inbox
- AI ranks recent message metadata by likely need for human attention.
- Ranking considers urgency, explicit requests, deadlines, consequences and unread state.
- No email is deleted, moved or sent by this feature.

### AI-006 — Actions, deadlines, commitments and decisions
- Extracts explicit next actions, owners where stated, dates/deadlines, commitments, decisions and unresolved questions.
- The prompt policy forbids invented deadlines.

### AI-007 — Rewrite drafts
- Rewrite text with instructions such as clearer, shorter, formal or friendly while preserving meaning and factual commitments.

## Security and privacy
- OpenAI/API-provider credentials are never embedded in the Android APK.
- Android connects only to a user/organisation-configured **HTTPS MailXperts AI gateway**.
- The gateway access token is encrypted with Android Keystore AES/GCM.
- The server-side OpenAI request uses `store: false`.
- Email content is explicitly treated as untrusted data to reduce prompt-injection risk.
- Potential secrets in context are redacted before provider transmission.
- End-user AI task types are allow-listed; send/delete/payment actions are not exposed by the P0 AI endpoint.
- Generated replies are editable and require the normal explicit Send action.

## Server deployment requirement
The APK contains the AI client and UX, while OpenAI access remains server-side. Operators must deploy the included `build-expert` service (or compatible gateway) behind HTTPS and configure:
- `OPENAI_API_KEY`
- `MAILXPERTS_AI_CLIENT_TOKEN` (minimum 16 characters; use a strong randomly generated value)
- optionally `MAIL_AI_MODEL`, default `gpt-6-luna`

The gateway endpoint and client token are configured in **MailXperts AI → AI gateway settings** on the device.

## Inherited beta.8 scope
This build is based on the v1.6.0-beta.8 branch and retains its direct cloud-contact foundation, Build Expert secure read-only foundation, OAuth/mail transport fixes, Smart Contacts, advanced search and prior accepted MailXperts functionality.
