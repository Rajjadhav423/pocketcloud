package com.pocketcloud.build;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

/** Bound from the {@code pocketcloud.build.*} section of application.yml. */
@ConfigurationProperties(prefix = "pocketcloud.build")
public record BuildProperties(
        boolean enabled,
        Path workDir,           // each deployment gets workDir/<deploymentId>/
        Duration pollInterval,
        Duration timeout        // per command (clone, install, build)
) {
}
