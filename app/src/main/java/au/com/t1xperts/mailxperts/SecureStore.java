package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureStore {
    private static final String PREFS = "mailxperts_accounts_v2";
    private static final String LEGACY_PREFS = "mailxperts_account";
    private static final String KEY_ALIAS = "mailxperts_credentials_aes";
    private final Context context;
    private final SharedPreferences prefs;

    SecureStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateLegacyIfNeeded();
    }

    synchronized List<AccountConfig> loadAll() {
        ArrayList<AccountConfig> out = new ArrayList<>();
        String raw = prefs.getString("accounts", "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                AccountConfig c = fromJson(a.getJSONObject(i));
                if (c != null) out.add(c);
            }
        } catch (Exception ignored) {}
        return out;
    }

    synchronized AccountConfig load(String id) {
        if (id == null || id.isEmpty()) id = getSelectedId();
        for (AccountConfig a : loadAll()) if (a.id.equals(id)) return a;
        List<AccountConfig> all = loadAll();
        return all.isEmpty() ? new AccountConfig() : all.get(0);
    }

    synchronized void save(AccountConfig account) throws Exception {
        List<AccountConfig> all = loadAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(account.id)) {
                all.set(i, account);
                replaced = true;
                break;
            }
        }
        if (!replaced) all.add(account);
        JSONArray a = new JSONArray();
        for (AccountConfig c : all) a.put(toJson(c));
        prefs.edit().putString("accounts", a.toString()).putString("selected", account.id).apply();
    }

    synchronized void delete(String id) {
        List<AccountConfig> all = loadAll();
        JSONArray a = new JSONArray();
        String next = "";
        for (AccountConfig c : all) {
            if (!c.id.equals(id)) {
                try { a.put(toJson(c)); } catch (Exception ignored) {}
                if (next.isEmpty()) next = c.id;
            }
        }
        prefs.edit().putString("accounts", a.toString()).putString("selected", next).apply();
    }

    boolean hasAccounts() { return !loadAll().isEmpty(); }
    String getSelectedId() { return prefs.getString("selected", ""); }
    void setSelectedId(String id) { prefs.edit().putString("selected", id == null ? "" : id).apply(); }

    synchronized void clearPassword(String id) {
        AccountConfig a = load(id);
        if (a.id.equals(id)) {
            a.password = "";
            try { save(a); } catch (Exception ignored) {}
        }
    }

    private JSONObject toJson(AccountConfig a) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", a.id);
        o.put("provider", a.provider == null ? ProviderPreset.CUSTOM : a.provider);
        o.put("label", a.label == null ? "" : a.label);
        o.put("email", a.email);
        o.put("username", a.username);
        o.put("imapHost", a.imapHost);
        o.put("imapPort", a.imapPort);
        o.put("smtpHost", a.smtpHost);
        o.put("smtpPort", a.smtpPort);
        o.put("smtpSecurity", a.smtpSecurity == null ? AccountConfig.SMTP_SSL : a.smtpSecurity);
        o.put("syncEnabled", a.syncEnabled);
        o.put("notificationsEnabled", a.notificationsEnabled);
        o.put("signatureEnabled", a.signatureEnabled);
        o.put("signatureHtml", a.signatureHtml == null ? "" : a.signatureHtml);
        o.put("password", a.password == null || a.password.isEmpty() ? "" : encrypt(a.password));
        return o;
    }

    private AccountConfig fromJson(JSONObject o) {
        try {
            AccountConfig a = new AccountConfig();
            a.id = o.optString("id", a.id);
            a.provider = o.optString("provider", "");
            a.label = o.optString("label", "");
            a.email = o.optString("email", a.email);
            a.username = o.optString("username", a.username);
            a.imapHost = o.optString("imapHost", a.imapHost);
            a.imapPort = o.optInt("imapPort", a.imapPort);
            a.smtpHost = o.optString("smtpHost", a.smtpHost);
            a.smtpPort = o.optInt("smtpPort", a.smtpPort);
            a.smtpSecurity = o.optString("smtpSecurity", AccountConfig.SMTP_SSL);
            a.syncEnabled = o.optBoolean("syncEnabled", true);
            a.notificationsEnabled = o.optBoolean("notificationsEnabled", true);
            a.signatureEnabled = o.optBoolean("signatureEnabled", false);
            a.signatureHtml = o.optString("signatureHtml", "");
            String enc = o.optString("password", "");
            a.password = enc.isEmpty() ? "" : decrypt(enc);
            if (a.provider == null || a.provider.isEmpty()) {
                a.provider = ProviderPreset.infer(a.email, a.imapHost);
            }
            return a;
        } catch (Exception e) { return null; }
    }

    private void migrateLegacyIfNeeded() {
        if (!prefs.getString("accounts", "").isEmpty()) return;
        SharedPreferences legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
        String legacyPassword = legacy.getString("password", "");
        if (legacyPassword.isEmpty() && legacy.getString("email", "").isEmpty()) return;
        AccountConfig a = new AccountConfig();
        a.label = "T1Xperts Customer Care";
        a.email = legacy.getString("email", a.email);
        a.username = legacy.getString("username", a.username);
        a.imapHost = legacy.getString("imapHost", a.imapHost);
        a.imapPort = legacy.getInt("imapPort", a.imapPort);
        a.smtpHost = legacy.getString("smtpHost", a.smtpHost);
        a.smtpPort = legacy.getInt("smtpPort", a.smtpPort);
        if (!legacyPassword.isEmpty()) {
            try { a.password = decrypt(legacyPassword); } catch (Exception ignored) {}
        }
        try { save(a); } catch (Exception ignored) {}
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), new SecureRandom());
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ciphertext, Base64.NO_WRAP);
    }

    private String decrypt(String value) throws Exception {
        String[] parts = value.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid encrypted credential");
        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] ciphertext = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
}
