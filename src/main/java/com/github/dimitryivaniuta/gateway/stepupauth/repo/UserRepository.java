package com.github.dimitryivaniuta.gateway.stepupauth.repo;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** User repository. */
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);
}
