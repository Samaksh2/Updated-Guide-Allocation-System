package com.college.guide;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Password hashing without putting plain-text passwords in MongoDB. */
final class Passwords {
  private static final int ITERATIONS = 120_000;
  private static final int BITS = 256;

  static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(derive(password, salt));
  }

  static boolean matches(String password, String stored) {
    String[] parts = stored.split(":", 2);
    return parts.length == 2 && MessageDigest.isEqual(derive(password, Base64.getDecoder().decode(parts[0])), Base64.getDecoder().decode(parts[1]));
  }

  private static byte[] derive(String password, byte[] salt) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, BITS);
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (Exception e) { throw new IllegalStateException("Unable to hash password", e); }
  }
}
