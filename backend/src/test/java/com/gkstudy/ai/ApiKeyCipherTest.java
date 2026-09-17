package com.gkstudy.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyCipherTest {

    private ApiKeyCipher cipherWithKey(String masterKey) {
        return new ApiKeyCipher() {
            @Override protected String masterKey() { return masterKey; }
        };
    }

    @Test
    void encryptThenDecryptRestoresPlaintext() {
        ApiKeyCipher cipher = cipherWithKey("test-master-key-1");
        String plaintext = "sk-abcdefgh12345678";
        String encrypted = cipher.encrypt(plaintext);
        assertNotEquals(plaintext, encrypted);
        assertFalse(encrypted.contains(plaintext));
        assertEquals(plaintext, cipher.decrypt(encrypted));
    }

    @Test
    void encryptProducesDifferentCipherEachTime() {
        ApiKeyCipher cipher = cipherWithKey("test-master-key-1");
        assertNotEquals(cipher.encrypt("sk-same"), cipher.encrypt("sk-same"));
    }

    @Test
    void decryptWithWrongMasterKeyFails() {
        String encrypted = cipherWithKey("key-A").encrypt("sk-secret-key-000111");
        assertThrows(IllegalStateException.class, () -> cipherWithKey("key-B").decrypt(encrypted));
    }

    @Test
    void encryptWithoutMasterKeyThrows() {
        assertThrows(IllegalStateException.class, () -> cipherWithKey(null).encrypt("sk-abc"));
    }

    @Test
    void maskKeepsPrefixAndLastFour() {
        ApiKeyCipher cipher = cipherWithKey("k");
        assertEquals("sk-****0111", cipher.mask("sk-abcdefgh12340111"));
        assertEquals("", cipher.mask(null));
        assertEquals("", cipher.mask("  "));
        assertFalse(cipher.mask("sk-abcdefgh12340111").contains("bcdefgh"));
    }

    @Test
    void masterKeyConfiguredReflectsEnvPresence() {
        assertTrue(cipherWithKey("k").masterKeyConfigured());
        assertFalse(cipherWithKey(null).masterKeyConfigured());
    }
}
