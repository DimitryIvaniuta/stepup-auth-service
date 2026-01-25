package com.github.dimitryivaniuta.gateway.stepupauth.service.signal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Hashes sensitive signals like device ids (do not store raw). */
public final class SignalHasher {
    private SignalHasher() { }
    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash", e);
        }
    }
}
