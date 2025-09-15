package com.translator.messagingapp.p2p;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Utility class for encrypting and decrypting P2P connection data.
 * Uses Android Keystore for secure key management.
 */
public class P2PEncryptionUtils {
    private static final String TAG = "P2PEncryptionUtils";
    private static final String KEY_ALIAS = "P2P_CONNECTION_KEY";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    /**
     * Encrypts connection data for P2P SMS trigger.
     *
     * @param context The application context
     * @param connectionData The connection data to encrypt (JSON format)
     * @return Base64 encoded encrypted data, or null if encryption fails
     */
    public static String encryptConnectionData(Context context, String connectionData) {
        try {
            SecretKey secretKey = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal(connectionData.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

            String result = Base64.encodeToString(combined, Base64.NO_WRAP);
            Log.d(TAG, "Successfully encrypted connection data");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt connection data", e);
            return null;
        }
    }

    /**
     * Decrypts connection data from P2P SMS trigger.
     *
     * @param context The application context
     * @param encryptedData Base64 encoded encrypted data
     * @return Decrypted connection data, or null if decryption fails
     */
    public static String decryptConnectionData(Context context, String encryptedData) {
        try {
            byte[] combined = Base64.decode(encryptedData, Base64.NO_WRAP);
            
            if (combined.length < GCM_IV_LENGTH) {
                Log.e(TAG, "Invalid encrypted data length");
                return null;
            }

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKey secretKey = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decryptedData = cipher.doFinal(encrypted);
            String result = new String(decryptedData, StandardCharsets.UTF_8);
            Log.d(TAG, "Successfully decrypted connection data");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt connection data", e);
            return null;
        }
    }

    /**
     * Gets or creates the secret key for P2P encryption.
     */
    private static SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            // Create new key
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
            Log.d(TAG, "Created new P2P encryption key");
        }

        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    /**
     * Validates if the given string is a valid P2P trigger format.
     *
     * @param message The SMS message to validate
     * @return true if it's a valid P2P trigger, false otherwise
     */
    public static boolean isValidP2PTrigger(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        // Check for P2P_CONNECT# prefix
        String trimmed = message.trim();
        if (!trimmed.startsWith("P2P_CONNECT#")) {
            return false;
        }

        // Extract the payload part
        int hashIndex = trimmed.indexOf('#');
        if (hashIndex == -1 || hashIndex == trimmed.length() - 1) {
            return false;
        }

        String payload = trimmed.substring(hashIndex + 1);
        
        // Check if payload has USER: prefix and encrypted data
        if (!payload.startsWith("USER:")) {
            return false;
        }

        String encryptedPart = payload.substring(5); // Remove "USER:" prefix
        if (encryptedPart.trim().isEmpty()) {
            return false;
        }

        // Validate Base64 format
        try {
            Base64.decode(encryptedPart, Base64.NO_WRAP);
            return true;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid Base64 in P2P trigger: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the encrypted payload from a P2P trigger SMS.
     *
     * @param message The P2P trigger SMS message
     * @return The encrypted payload, or null if extraction fails
     */
    public static String extractEncryptedPayload(String message) {
        if (!isValidP2PTrigger(message)) {
            return null;
        }

        String trimmed = message.trim();
        int hashIndex = trimmed.indexOf('#');
        String payload = trimmed.substring(hashIndex + 1);
        
        if (payload.startsWith("USER:")) {
            return payload.substring(5); // Remove "USER:" prefix
        }
        
        return null;
    }
}