package com.github.dimitryivaniuta.gateway.stepupauth.service.otp;

import com.github.dimitryivaniuta.gateway.stepupauth.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/** OTP service (Redis TTL + attempt counter). */
@Service
public class OtpService {
    private static final SecureRandom RNG = new SecureRandom();
    private final StringRedisTemplate redis;
    @Value("${app.otp.ttl}") private Duration ttl;
    @Value("${app.otp.max-attempts}") private int maxAttempts;

    public OtpService(StringRedisTemplate redis) { this.redis = redis; }

    private String otpKey(UUID id) { return "otp:" + id; }
    private String attemptsKey(UUID id) { return "otp_attempts:" + id; }

    public String generateAndStore(UUID challengeId) {
        String otp = String.format("%06d", RNG.nextInt(1_000_000));
        redis.opsForValue().set(otpKey(challengeId), otp, ttl);
        redis.opsForValue().set(attemptsKey(challengeId), "0", ttl);
        return otp;
    }

    public void verify(UUID challengeId, String otp) {
        String expected = redis.opsForValue().get(otpKey(challengeId));
        if (expected == null) throw new ApiException(HttpStatus.GONE, "OTP expired");

        Long attempts = redis.opsForValue().increment(attemptsKey(challengeId));
        if (attempts != null && attempts > maxAttempts) throw new ApiException(HttpStatus.LOCKED, "Too many attempts");

        if (!expected.equals(otp)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        redis.delete(otpKey(challengeId));
        redis.delete(attemptsKey(challengeId));
    }

    public String peek(UUID challengeId) { return redis.opsForValue().get(otpKey(challengeId)); }
}
