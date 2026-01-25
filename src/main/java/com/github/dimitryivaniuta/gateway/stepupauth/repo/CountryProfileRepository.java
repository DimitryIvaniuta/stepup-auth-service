package com.github.dimitryivaniuta.gateway.stepupauth.repo;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.CountryProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Country profile repository. */
public interface CountryProfileRepository extends JpaRepository<CountryProfileEntity, UUID> { }
