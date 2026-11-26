package com.pocketcloud.deployment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Uses the deployments table itself as a job queue.
 *
 * <p>{@code FOR UPDATE SKIP LOCKED} lets several workers poll at once: each locks a different
 * QUEUED row and skips rows another worker already holds, so a job is never picked twice.
 * The claim (select + status change) is a single statement, so it is atomic.
 */
@Component
public class DeploymentQueue {

    private static final String CLAIM_NEXT = """
            UPDATE deployments
               SET status = 'BUILDING', started_at = NOW(), updated_at = NOW()
             WHERE id = (SELECT id
                           FROM deployments
                          WHERE status = 'QUEUED'
                          ORDER BY created_at
                          LIMIT 1
                          FOR UPDATE SKIP LOCKED)
            RETURNING id
            """;

    // A build cannot survive a restart of this app, so anything still BUILDING was interrupted.
    private static final String FAIL_INTERRUPTED = """
            UPDATE deployments
               SET status = 'FAILED', error = 'Interrupted by server restart',
                   finished_at = NOW(), updated_at = NOW()
             WHERE status = 'BUILDING'
            """;

    private final JdbcTemplate jdbc;

    public DeploymentQueue(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UUID> claimNext() {
        return jdbc.query(CLAIM_NEXT, (rs, row) -> rs.getObject("id", UUID.class))
                .stream()
                .findFirst();
    }

    public int failInterruptedBuilds() {
        return jdbc.update(FAIL_INTERRUPTED);
    }
}
