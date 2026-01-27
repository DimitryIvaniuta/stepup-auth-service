package com.github.dimitryivaniuta.gateway.stepupauth.repo;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.OutboxEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Outbox repository.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Admin list: newest first, limited.
     */
    List<OutboxEventEntity> findTop500ByOrderByCreatedAtDescIdDesc();

    /**
     * Admin list: newest first for a specific event type, limited.
     */
    List<OutboxEventEntity> findTop500ByEventTypeOrderByCreatedAtDescIdDesc(String eventType);

    /**
     * Admin list: newest first (paged).
     */
    @Query("""
            select e
            from OutboxEventEntity e
            order by e.createdAt desc, e.id desc
            """)
    Page<OutboxEventEntity> findRecent(Pageable pageable);

    /**
     * Admin list filtered by event type (paged).
     */
    @Query("""
            select e
            from OutboxEventEntity e
            where e.eventType = :eventType
            order by e.createdAt desc, e.id desc
            """)
    Page<OutboxEventEntity> findRecentByEventType(@Param("eventType") String eventType, Pageable pageable);

    /**
     * Publisher worker: fetch a batch of due events with SKIP LOCKED (requires active TX).
     * Fetches a batch of "due" outbox events.
     *
     * <p>Uses {@code FOR UPDATE SKIP LOCKED} to support multiple publisher workers without double-publishing.</p>
     */
    @Query(value = """
            select *
            from outbox_event
            where status in (:statuses)
              and next_attempt_at <= :now
            order by next_attempt_at asc, id asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> findDueBatch(@Param("now") Instant now,
                                         @Param("statuses") List<String> statuses,
                                         @Param("limit") int limit);


    java.util.List<OutboxEventEntity> findTop500ByEventTypeOrderByCreatedAtDesc(String eventType);
}

