package com.github.dimitryivaniuta.gateway.stepupauth.repo;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.RiskDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Risk decision repository. */
public interface RiskDecisionRepository extends JpaRepository<RiskDecisionEntity, UUID> { }
