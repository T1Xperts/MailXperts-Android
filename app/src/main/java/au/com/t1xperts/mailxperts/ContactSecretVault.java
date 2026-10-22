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

/** Android Keystore-backed storage for CardDAV credentials. */
final class ContactSecretVault {
    static final class CardDavCredential {
        final String username;
        final String appPassword;
        final String endpoint;

        CardDavCredential(String username, String appPassword, String endpoint) {
            this.username = safe(username);
            this.appPassword = safe(appPassword);
            this.endpoint = safe(endpoint);
        }

        boolean configured() {
            return !username.isEmpty() && !appPassword.isEmpty() && !endpoint.isEmpty();
        }
    }

    private static final String PREFS = "mailxperts_contact_secrets_v1";
    private static final String KEY_ALIAS = "mailxperts_contact_secrets_aes";
    private static final String KEY_ICLOUD = "icloud_carddav";
    private final SharedPreferences prefs;

    ContactSecretVault(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized void saveICloud(String username, String appPassword, String endpoint) throws Exception {
        JSONObject json = new JSONObject();
        json.put("username", safe(username));
        json.put("appPassword", safe(appPassword));
        json.put("endpoint", normalizeEndpoint(endpoint));
        prefs.edit().putString(KEY_ICLOUD, encrypt(json.toString())).apply();
    }

    synchronized CardDavCredential loadICloud() {
        String encrypted = prefs.getString(KEY_ICLOUD, "");
        if (encrypted == null || encrypted.isEmpty()) return new CardDavCredential("", "", "");
        try {
            JSONObject json = new JSONObject(decrypt(encrypted));
            return new CardDavCredential(
                    json.optString("username", ""),
                    json.optString("appPassword", ""),
                    normalizeEndpoint(json.optString("endpoint", "")));
        } catch (Exception ignored) {
            return new CardDavCredential("", "", "");
        }
    }

    synchronized void clearICloud() {
        prefs.edit().remove(KEY_ICLOUD).apply();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        generator.init(spec);
        return generator.generateKey();
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
        if (parts.length != 2) throw new IllegalArgumentException("Invalid encrypted contact secret");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                StandardCharsets.UTF_8);
    }

    static String normalizeEndpoint(String endpoint) {
        String value = safe(endpoint);
        if (value.isEmpty()) value = "https://contacts.icloud.com/";
        if (!value.startsWith("https://")) {
            throw new IllegalArgumentException("CardDAV endpoint must use HTTPS");
        }
        return value.endsWith("/") ? value : value + "/";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
