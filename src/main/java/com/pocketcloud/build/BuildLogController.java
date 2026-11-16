package com.pocketcloud.build;

import com.pocketcloud.deployment.DeploymentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
public class BuildLogController {

    private final DeploymentService deployments;
    private final BuildPipeline pipeline;

    public BuildLogController(DeploymentService deployments, BuildPipeline pipeline) {
        this.deployments = deployments;
        this.pipeline = pipeline;
    }

    /** Plain-text build log. Call it repeatedly while BUILDING to follow progress. */
    @GetMapping(value = "/api/v1/deployments/{id}/logs", produces = MediaType.TEXT_PLAIN_VALUE)
    public String logs(@PathVariable UUID id) {
        deployments.get(id); // 404 for unknown deployments
        Path log = pipeline.logFile(id);
        if (!Files.exists(log)) {
            return "";       // still QUEUED: nothing logged yet
        }
        try {
            return Files.readString(log);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
