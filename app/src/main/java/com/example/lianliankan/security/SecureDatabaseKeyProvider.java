package com.example.lianliankan.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.security.SecureRandom;

public final class SecureDatabaseKeyProvider {

    private static final String PREFS_NAME = "secure_lianliankan_keys";
    private static final String KEY_DATABASE = "database_key";

    private SecureDatabaseKeyProvider() {
    }

    public static String getDatabasePassphrase(Context context) {
        SharedPreferences prefs = openPrefs(context.getApplicationContext());
        String key = prefs.getString(KEY_DATABASE, null);
        if (key == null || key.length() < 32) {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            key = Base64.encodeToString(bytes, Base64.NO_WRAP);
            prefs.edit().putString(KEY_DATABASE, key).apply();
        }
        return key;
    }

    private static SharedPreferences openPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }
}
