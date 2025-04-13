package com.zervice.kbase.database;

import com.zervice.common.utils.Base36;
import com.zervice.common.utils.IdGenerator;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

@Log4j2
public final class SecurityUtils {
    private static final int PASSWORD_HASH_ITERATIONS = 16;
    private static final int PASSWORD_SALT_LEN = 32;
    private static final int PASSWORD_KEY_LEN = 128;

    public static String encodePassword(String pass) {
        return encodePassword(pass, 0);
    }

    public static String encodePassword(String password, int plainCount) {
        return encodePassword(password, plainCount, plainCount);
    }

    public static String encodePassword(String password, int leadingPlainCount, int endingPlainCount) {
        int mask = password.length() - leadingPlainCount - endingPlainCount;
        if (mask <= 0) {
            return password;
        } else {
            StringBuilder sb = new StringBuilder();
            int idx = 0;

            while(idx < leadingPlainCount) {
                sb.append(password.charAt(idx++));
            }

            while(mask-- > 0) {
                sb.append("*");
                ++idx;
            }

            while(idx < password.length()) {
                sb.append(password.charAt(idx++));
            }

            return sb.toString();
        }
    }

    public static String generateTemporaryPassword() {
        return "hello";
    }

    private static String _generateKey() {
        return Base36.encode(IdGenerator.generateId());
    }

    public static String generateApiKey() {
        return _generateKey();
    }

    public static String generateCollectorAccessKey() {
        return _generateAccessKey();
    }

    private static String _generateAccessKey() {
        try {
            UUID uuid = UUID.randomUUID();
            return Base64.encodeBase64URLSafeString(uuid.toString().getBytes("utf-8"));
        } catch (UnsupportedEncodingException var1) {
            return _generateKey();
        }
    }

    public static String generateInstallationCode(String accountId, String random) {
        String code = accountId + " " + random;
        return encodeBase64(code);
    }

    public static String encodeBase64(String clearText) {
        try {
            return Base64.encodeBase64URLSafeString(clearText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            LOG.warn("Encounter encoding error", ex);
            return null;
        }
    }

    public static String decodeBase64(String encoded) {
        try {
            return new String(Base64.decodeBase64(encoded), "UTF-8");
        } catch (Exception ex) {
            LOG.warn("Encounting decoding error", ex);
            return null;
        }
    }

    public static SecretKey generateAESSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    public static String encrypt(String clearText, String secret) {
        String encryptedText = null;

        try {
            new SecretKeySpec(secret.getBytes(), "AES/CBC/PKCS5Padding");
            SecretKey secretKey = generateAESSecretKey();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, secretKey);
            byte[] encrypted = cipher.doFinal(clearText.getBytes());
            encryptedText = new String(encrypted);
        } catch (Exception ex) {
            LOG.warn("Encounter encrypting error", ex);
        }

        return encryptedText;
    }

    public static String decrypt(String encryptedText, SecretKey secretKey) {
        String clearText = null;

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(2, secretKey);
            byte[] decrypted = cipher.doFinal(encryptedText.getBytes());
            clearText = new String(decrypted);
        } catch (Exception ex) {
            LOG.warn("Encounter decrypting error", ex);
        }

        return clearText;
    }

    public static String getSaltedHash(String password) {
        try {
            byte[] salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(32);
            return Base64.encodeBase64URLSafeString(salt) + "$" + hash(password, salt);
        } catch (NoSuchAlgorithmException ex) {
            LOG.warn("Get salted hash for password encounters exception", ex);
        } catch (InvalidKeySpecException ex2) {
            LOG.warn("Get salted hash for password encounters exception", ex2);
        }

        return null;
    }

    public static String randomPlainPass() {
        return UUID.randomUUID().toString();
    }

    public static boolean verifyPassword(String password, String stored) {
        try {
            String[] saltAndPass = stored.split("\\$");
            if (saltAndPass.length != 2) {
                throw new IllegalStateException("The stored password have the form 'salt$hash'");
            }

            String hashOfInput = hash(password, Base64.decodeBase64(saltAndPass[0]));
            return hashOfInput.equals(saltAndPass[1]);
        } catch (NoSuchAlgorithmException ex) {
            LOG.warn("Verify password encounters exception", ex);
        } catch (InvalidKeySpecException ex2) {
            LOG.warn("Verify password encounters exception", ex2);
        }

        return false;
    }

    private static String hash(String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
        if (password != null && password.length() != 0) {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey key = f.generateSecret(new PBEKeySpec(password.toCharArray(), salt, 16, 128));
            return Base64.encodeBase64URLSafeString(key.getEncoded());
        } else {
            throw new IllegalArgumentException("Empty passwords are not supported.");
        }
    }

    private SecurityUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void main(String [] args) throws Exception {
        String pass = getSaltedHash("admin");
        System.out.println("Password is - " + pass);
    }
}
