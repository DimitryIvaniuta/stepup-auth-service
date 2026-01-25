package com.github.dimitryivaniuta.gateway.stepupauth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Last trusted country per user. */
@Getter @Setter @NoArgsConstructor
@Entity @Table(name="country_profile")
public class CountryProfileEntity {
    @Id private UUID userId;
    @Column(name="last_country", length=2) private String lastCountry;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
}
