package com.github.dimitryivaniuta.gateway.stepupauth.repo;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.RiskDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Risk decision repository.
 */
public interface RiskDecisionRepository extends JpaRepository<RiskDecisionEntity, UUID> {
    /**
     * Latest decisions for a user, newest first.
     */
    java.util.List<RiskDecisionEntity> findTop200ByUserIdOrderByCreatedAtDesc(java.util.UUID userId);

    /**
     * Latest decisions for admin view.
     */
    java.util.List<RiskDecisionEntity> findTop500ByOrderByCreatedAtDesc();
}
