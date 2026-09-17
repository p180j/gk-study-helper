package com.gkstudy.ai;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AI Provider API Key 加密器：AES-256-GCM，主密钥来自服务器环境变量 AI_CONFIG_MASTER_KEY。
 * 数据库只保存密文（base64(iv + ciphertext)），任何接口不返回明文。
 */
@Component
public class ApiKeyCipher {
    public static final String MASTER_KEY_ENV = "AI_CONFIG_MASTER_KEY";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecureRandom random = new SecureRandom();

    public boolean masterKeyConfigured() {
        return masterKey() != null;
    }

    public String encrypt(String plaintext) {
        String masterKey = requireMasterKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(masterKey), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        String masterKey = requireMasterKey();
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length <= IV_LENGTH) throw new IllegalArgumentException("密文格式不合法");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(masterKey),
                    new GCMParameterSpec(TAG_LENGTH_BITS, combined, 0, IV_LENGTH));
            byte[] decrypted = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 解密失败", e);
        }
    }

    /** maskedKey：保留前缀与末4位，例如 sk-****abcd */
    public String mask(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) return "";
        String value = apiKey.trim();
        String prefix = value.length() <= 8 ? value.substring(0, Math.min(3, value.length())) : value.substring(0, 3);
        String suffix = value.length() >= 4 ? value.substring(value.length() - 4) : "";
        return prefix + "****" + suffix;
    }

    private SecretKeySpec deriveKey(String masterKey) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(masterKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }

    private String requireMasterKey() {
        String masterKey = masterKey();
        if (masterKey == null) {
            throw new IllegalStateException("服务器未配置环境变量 " + MASTER_KEY_ENV + "，无法保存或读取 AI API Key");
        }
        return masterKey;
    }

    protected String masterKey() {
        String value = System.getenv(MASTER_KEY_ENV);
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
