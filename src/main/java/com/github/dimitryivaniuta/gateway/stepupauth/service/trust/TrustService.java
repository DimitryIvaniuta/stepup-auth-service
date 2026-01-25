package com.github.dimitryivaniuta.gateway.stepupauth.service.trust;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.CountryProfileEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.domain.TrustedDeviceEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.CountryProfileRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.TrustedDeviceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Maintains trusted device+country baseline (after safe/verified actions). */
@Service
public class TrustService {
    private final TrustedDeviceRepository devices;
    private final CountryProfileRepository countries;

    public TrustService(TrustedDeviceRepository devices, CountryProfileRepository countries) {
        this.devices = devices; this.countries = countries;
    }

    public boolean isDeviceTrusted(UUID userId, String deviceHash) {
        return devices.findByUserIdAndDeviceHash(userId, deviceHash).isPresent();
    }

    public boolean isNewCountry(UUID userId, String country) {
        var p = countries.findById(userId);
        if (p.isEmpty() || p.get().getLastCountry() == null) return false;
        return !p.get().getLastCountry().equalsIgnoreCase(country);
    }

    @Transactional
    public void trust(UUID userId, String deviceHash, String country) {
        var d = devices.findByUserIdAndDeviceHash(userId, deviceHash).orElseGet(() -> {
            TrustedDeviceEntity nd = new TrustedDeviceEntity();
            nd.setUserId(userId);
            nd.setDeviceHash(deviceHash);
            nd.setFirstSeenAt(Instant.now());
            nd.setLastSeenAt(Instant.now());
            return nd;
        });
        d.setLastSeenAt(Instant.now());
        devices.save(d);

        var cp = countries.findById(userId).orElseGet(() -> {
            CountryProfileEntity n = new CountryProfileEntity();
            n.setUserId(userId);
            return n;
        });
        cp.setLastCountry(country);
        cp.setUpdatedAt(Instant.now());
        countries.save(cp);
    }
}
