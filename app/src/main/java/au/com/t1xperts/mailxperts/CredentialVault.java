package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores OAuth tokens separately from ordinary account metadata. */
final class CredentialVault {
    private static final String PREFS = "mailxperts_oauth_credentials_v1";
    private static final String KEY_ALIAS = "mailxperts_oauth_tokens_aes";

    private final SharedPreferences prefs;

    CredentialVault(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized void save(String accountId, OAuthCredential credential) throws Exception {
        if (accountId == null || accountId.trim().isEmpty()) return;
        if (credential == null || (!credential.hasAccessToken() && !credential.hasRefreshToken())) {
            clear(accountId);
            return;
        }
        JSONObject json = new JSONObject();
        json.put("accessToken", credential.accessToken);
        json.put("refreshToken", credential.refreshToken);
        json.put("expiresAtMillis", credential.expiresAtMillis);
        prefs.edit().putString(accountId, encrypt(json.toString())).apply();
    }

    synchronized OAuthCredential load(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) return new OAuthCredential("", "", 0L);
        String encrypted = prefs.getString(accountId, "");
        if (encrypted == null || encrypted.isEmpty()) return new OAuthCredential("", "", 0L);
        try {
            JSONObject json = new JSONObject(decrypt(encrypted));
            return new OAuthCredential(
                    json.optString("accessToken", ""),
                    json.optString("refreshToken", ""),
                    json.optLong("expiresAtMillis", 0L));
        } catch (Exception ignored) {
            return new OAuthCredential("", "", 0L);
        }
    }

    synchronized void clear(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) return;
        prefs.edit().remove(accountId).apply();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), new SecureRandom());
        return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(cipher.doFinal(
                        value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    }

    private String decrypt(String value) throws Exception {
        String[] parts = value.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid encrypted OAuth credential");
        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] ciphertext = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
}
