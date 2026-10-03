from pathlib import Path


def replace(path, old, new, count=-1):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"Expected text not found in {path}: {old[:120]!r}")
    text2 = text.replace(old, new, count)
    p.write_text(text2)

# SettingsActivity: OAuth entry point and auth-aware validation.
settings = "app/src/main/java/au/com/t1xperts/mailxperts/SettingsActivity.java"
replace(settings,
    '    private static final int EDIT_SIGNATURE = 5201;\n',
    '    private static final int EDIT_SIGNATURE = 5201;\n    private static final int OAUTH_CONNECT = 5202;\n')
replace(settings,
    '    private Button save;\n',
    '    private Button save;\n    private Button oauthConnect;\n')
replace(settings,
    '        identity.addView(password);\n        root.addView(identity);\n',
    '        identity.addView(password);\n        root.addView(identity);\n        oauthConnect = Ui.secondaryButton(this, "Secure provider sign-in", v -> startOAuthConnect());\n        root.addView(oauthConnect);\n        updateAuthUi(ProviderPreset.find(current.provider));\n')
replace(settings,
    '                current.provider = definition.id;\n                if (providerReady) applyPreset(definition);\n',
    '                current.provider = definition.id;\n                updateAuthUi(definition);\n                if (providerReady) applyPreset(definition);\n')
replace(settings,
    '    private void applyPreset(ProviderPreset.Definition definition) {\n        updatePasswordHint(definition);\n',
    '    private void applyPreset(ProviderPreset.Definition definition) {\n        updatePasswordHint(definition);\n        current.authType = definition.preferredAuthType;\n        updateAuthUi(definition);\n')
replace(settings,
    '''    private void updatePasswordHint(ProviderPreset.Definition definition) {\n        boolean gmail = definition != null && ProviderPreset.GMAIL.equals(definition.id);\n        password.setHint(gmail\n                ? "Google 16-character App Password"\n                : "Password or provider app-specific password");\n    }\n''',
    '''    private void updatePasswordHint(ProviderPreset.Definition definition) {\n        boolean gmail = definition != null && ProviderPreset.GMAIL.equals(definition.id);\n        password.setHint(gmail\n                ? "Google App Password (fallback only)"\n                : "Password or provider app-specific password");\n    }\n\n    private void updateAuthUi(ProviderPreset.Definition definition) {\n        if (oauthConnect == null || password == null || definition == null) return;\n        boolean oauthProvider = definition.supportsOAuth2;\n        oauthConnect.setVisibility(oauthProvider ? View.VISIBLE : View.GONE);\n        if (ProviderPreset.GMAIL.equals(definition.id)) {\n            oauthConnect.setText("Continue with Google (recommended)");\n            password.setVisibility(View.VISIBLE);\n        } else if (ProviderPreset.OUTLOOK.equals(definition.id)) {\n            oauthConnect.setText("Continue with Microsoft");\n            password.setVisibility(View.GONE);\n        } else {\n            password.setVisibility(View.VISIBLE);\n        }\n    }\n\n    private void startOAuthConnect() {\n        AccountConfig account = read();\n        if (account.email == null || account.email.trim().isEmpty()) {\n            status.setTextColor(Ui.error(this));\n            status.setText("Enter the email address first, then continue with the provider.");\n            return;\n        }\n        account.username = account.email.trim();\n        account.authType = AuthType.OAUTH2;\n        account.password = "";\n        try {\n            store.save(account);\n        } catch (Exception error) {\n            showSaveError("Could not prepare secure provider sign-in: ", error);\n            return;\n        }\n        current = account;\n        Intent intent = new Intent(this, OAuthConnectActivity.class);\n        intent.putExtra(OAuthConnectActivity.EXTRA_ACCOUNT_ID, account.id);\n        startActivityForResult(intent, OAUTH_CONNECT);\n    }\n''')
replace(settings,
    '''        if (definition.oauthRequired) {\n            status.setTextColor(Ui.error(this));\n            status.setText("Outlook.com requires OAuth2/Modern Auth. The T1Xperts Microsoft app registration and redirect URI must be configured before Outlook sign-in can be enabled.");\n            return;\n        }\n        if (ProviderPreset.GMAIL.equals(account.provider)\n                && !GmailAppPassword.isValid(account.password)) {\n''',
    '''        if (AuthType.isOAuth(account.authType)) {\n            status.setTextColor(Ui.muted(this));\n            status.setText(ProviderPreset.GMAIL.equals(account.provider)\n                    ? "Use Continue with Google to authorize this Gmail account."\n                    : "Use Continue with Microsoft to authorize this account.");\n            return;\n        }\n        if (ProviderPreset.GMAIL.equals(account.provider)\n                && !GmailAppPassword.isValid(account.password)) {\n''')
replace(settings,
    '''    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {\n        super.onActivityResult(requestCode, resultCode, data);\n        if (requestCode != EDIT_SIGNATURE || resultCode != RESULT_OK || data == null) return;\n        signatureHtmlDraft = SignatureHtml.normaliseStored(\n                data.getStringExtra(SignatureEditorActivity.EXTRA_SIGNATURE_HTML));\n        updateSignatureState();\n    }\n''',
    '''    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {\n        super.onActivityResult(requestCode, resultCode, data);\n        if (requestCode == OAUTH_CONNECT) {\n            if (resultCode == RESULT_OK && current != null) {\n                current = store.load(current.id);\n                Toast.makeText(this, "Secure provider sign-in completed", Toast.LENGTH_SHORT).show();\n                boolean backgroundReady = NotificationScheduler.update(getApplicationContext(), current);\n                openSavedAccount(current, backgroundReady);\n            }\n            return;\n        }\n        if (requestCode != EDIT_SIGNATURE || resultCode != RESULT_OK || data == null) return;\n        signatureHtmlDraft = SignatureHtml.normaliseStored(\n                data.getStringExtra(SignatureEditorActivity.EXTRA_SIGNATURE_HTML));\n        updateSignatureState();\n    }\n''')

# MailRepository: use provider-aware secret and enable XOAUTH2 SASL mechanisms.
repo = "app/src/main/java/au/com/t1xperts/mailxperts/MailRepository.java"
p = Path(repo)
text = p.read_text()
text = text.replace(
    'store.connect(account.imapHost, account.imapPort, account.username, account.password);',
    'store.connect(account.imapHost, account.imapPort, account.username, MailAuth.secret(account));')
text = text.replace(
    'transport.connect(account.smtpHost, account.smtpPort, account.username, account.password);',
    'transport.connect(account.smtpHost, account.smtpPort, account.username, MailAuth.secret(account));')
old = '''        properties.put("mail.smtp.writetimeout", "30000");\n        return Session.getInstance(properties, new Authenticator() {\n'''
new = '''        properties.put("mail.smtp.writetimeout", "30000");\n        MailAuth.configureSmtp(properties, account);\n        return Session.getInstance(properties, new Authenticator() {\n'''
if old not in text:
    raise SystemExit("SMTP session insertion point not found")
text = text.replace(old, new, 1)
old = '''        properties.put("mail.imaps.connectionpoolsize", "1");\n        return Session.getInstance(properties);\n'''
new = '''        properties.put("mail.imaps.connectionpoolsize", "1");\n        MailAuth.configureImap(properties, account);\n        return Session.getInstance(properties);\n'''
if old not in text:
    raise SystemExit("IMAP session insertion point not found")
text = text.replace(old, new, 1)
old = '''        if (ProviderPreset.GMAIL.equals(account.provider)) {\n            if (authentication) {\n                return new MessagingException(stage + " authentication failed. Gmail rejected the "\n                        + "App Password. Use a current 16-character Google App Password generated "\n                        + "for this Google account; do not use the normal Google account password.",\n                        error);\n            }\n'''
new = '''        if (ProviderPreset.GMAIL.equals(account.provider)) {\n            if (authentication && AuthType.isOAuth(account.authType)) {\n                return new MessagingException(stage + " OAuth authentication failed. Reconnect "\n                        + "this Gmail account using Continue with Google.", error);\n            }\n            if (authentication) {\n                return new MessagingException(stage + " authentication failed. Gmail rejected the "\n                        + "App Password. Use Continue with Google (recommended), or a current "\n                        + "16-character App Password where Google permits it.", error);\n            }\n'''
if old not in text:
    raise SystemExit("Gmail diagnostic replacement point not found")
text = text.replace(old, new, 1)
p.write_text(text)

print("MX-QA-030 integration patch applied")
