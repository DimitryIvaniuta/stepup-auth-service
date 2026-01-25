package com.github.dimitryivaniuta.gateway.stepupauth.repo;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/** Outbox repository. */
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query(value = "select * from outbox_event " +
            "where status in (:statuses) and next_attempt_at <= :now " +
            "order by id limit :limit",
            nativeQuery = true)
    List<OutboxEventEntity> findDueBatch(@Param("now") Instant now,
                                        @Param("statuses") List<String> statuses,
                                        @Param("limit") int limit);
}
