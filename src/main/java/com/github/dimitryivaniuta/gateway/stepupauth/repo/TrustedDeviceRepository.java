package com.github.dimitryivaniuta.gateway.stepupauth.repo;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.TrustedDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Trusted device repository. */
public interface TrustedDeviceRepository extends JpaRepository<TrustedDeviceEntity, Long> {
    Optional<TrustedDeviceEntity> findByUserIdAndDeviceHash(UUID userId, String deviceHash);
}
