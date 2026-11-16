package com.interviewai.backend.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Component
public class AccountPasswordEncoder implements PasswordEncoder {

    private static final Pattern LEGACY_HASH = Pattern.compile("[0-9a-f]{64}");
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence password) {
        return bcrypt.encode(password);
    }

    @Override
    public boolean matches(CharSequence password, String hash) {
        if (!isLegacyHash(hash)) {
            return bcrypt.matches(password, hash);
        }

        byte[] stored = HexFormat.of().parseHex(hash);
        byte[] entered = sha256(password.toString());
        return MessageDigest.isEqual(stored, entered);
    }

    public boolean isLegacyHash(String hash) {
        return hash != null && LEGACY_HASH.matcher(hash).matches();
    }

    public boolean fitsBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private byte[] sha256(String password) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable.", error);
        }
    }
}
