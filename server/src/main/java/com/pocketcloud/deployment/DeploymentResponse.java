package com.pocketcloud.deployment;

import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
        UUID id,
        String project,
        DeploymentStatus status,
        String commitSha,
        AppType appType,
        String error,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
    public static DeploymentResponse from(Deployment d) {
        return new DeploymentResponse(d.getId(), d.getProject().getName(), d.getStatus(),
                d.getCommitSha(), d.getAppType(), d.getError(),
                d.getCreatedAt(), d.getStartedAt(), d.getFinishedAt());
    }
}
