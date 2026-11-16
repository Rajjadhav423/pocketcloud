package com.pocketcloud.deployment;

import java.util.UUID;

public record BuildJob(UUID deploymentId, String projectName, String repoUrl, String branch) {
}
