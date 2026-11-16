package com.pocketcloud.build;

import com.pocketcloud.deployment.BuildJob;
import com.pocketcloud.deployment.DeploymentQueue;
import com.pocketcloud.deployment.DeploymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Polls for QUEUED deployments and builds them one at a time.
 *
 * <p>One build at a time is deliberate: npm installs are RAM-hungry and the target is a phone.
 * To build in parallel, run more workers; the SKIP LOCKED queue keeps them from colliding.
 */
@Component
@ConditionalOnProperty(prefix = "pocketcloud.build", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BuildWorker {

    private static final Logger log = LoggerFactory.getLogger(BuildWorker.class);

    private final DeploymentQueue queue;
    private final DeploymentService deployments;
    private final BuildPipeline pipeline;

    public BuildWorker(DeploymentQueue queue, DeploymentService deployments, BuildPipeline pipeline) {
        this.queue = queue;
        this.deployments = deployments;
        this.pipeline = pipeline;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedBuilds() {
        int count = queue.failInterruptedBuilds();
        if (count > 0) {
            log.warn("Marked {} interrupted build(s) as FAILED", count);
        }
    }

    // fixedDelay (not fixedRate): the next poll starts only after this one finishes, so builds never overlap.
    @Scheduled(fixedDelayString = "${pocketcloud.build.poll-interval}")
    public void poll() {
        Optional<UUID> claimed = queue.claimNext();
        claimed.ifPresent(this::build);
    }

    void build(UUID deploymentId) {
        BuildJob job = deployments.getBuildJob(deploymentId);
        log.info("Building deployment {} for project {}", deploymentId, job.projectName());
        try {
            BuildPipeline.Result result = pipeline.run(job);
            deployments.markReady(deploymentId, result.commitSha(), result.plan().appType(),
                    result.plan().startCommand(), result.artifactPath().toString());
            log.info("Deployment {} is READY", deploymentId);
        } catch (BuildException e) {
            fail(deploymentId, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error building {}", deploymentId, e);
            fail(deploymentId, "Internal error: " + e.getMessage());
        }
    }

    private void fail(UUID deploymentId, String error) {
        log.warn("Deployment {} FAILED: {}", deploymentId, error);
        deployments.markFailed(deploymentId, null, error);
    }
}
