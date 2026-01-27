package com.github.dimitryivaniuta.gateway.stepupauth.repo;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.StepUpChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Step-up challenge repository.
 */
public interface StepUpChallengeRepository extends JpaRepository<StepUpChallengeEntity, UUID> {
    Optional<StepUpChallengeEntity> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Latest challenges for admin view.
     */
    java.util.List<StepUpChallengeEntity> findTop500ByOrderByCreatedAtDesc();

    /**
     * Latest challenges for a user.
     */
    java.util.List<StepUpChallengeEntity> findTop200ByUserIdOrderByCreatedAtDesc(UUID userId);
}
