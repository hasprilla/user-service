package com.sonifoy.user.infrastructure.security.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Arrays;

@Service
public class CryptoService {

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    public CryptoService() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public String decrypt(String encryptedData, byte[] keyBytes) {
        try {
            SecretKey key = new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
            String[] parts = encryptedData.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed for data length: {}", encryptedData != null ? encryptedData.length() : "null",
                    e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public String encrypt(String plainText, byte[] keyBytes) {
        try {
            SecretKey key = new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            new java.security.SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public byte[] deriveKey(String secret) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        return sha.digest(secret.getBytes(StandardCharsets.UTF_8));
    }
}
