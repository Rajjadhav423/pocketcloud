package com.pocketcloud.build;

import com.pocketcloud.deployment.BuildJob;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

/** clone → detect → build. Knows nothing about the database; the worker records the outcome. */
@Component
public class BuildPipeline {

    private final BuildProperties props;
    private final ProcessRunner runner;
    private final FrameworkDetector detector;

    public BuildPipeline(BuildProperties props, ProcessRunner runner, FrameworkDetector detector) {
        this.props = props;
        this.runner = runner;
        this.detector = detector;
    }

    public record Result(String commitSha, BuildPlan plan, Path artifactPath) {
    }

    public Path logFile(UUID deploymentId) {
        return deploymentDir(deploymentId).resolve("build.log");
    }

    public Result run(BuildJob job) {
        Path dir = deploymentDir(job.deploymentId());
        Path sourceDir = dir.resolve("src");
        Path log = dir.resolve("build.log");
        createDirectories(dir);

        runner.appendLog(log, "==> Deploying %s (branch %s)%n".formatted(job.repoUrl(), job.branch()));

        // --depth 1: we only need the latest commit, not the whole history. Much faster on a phone.
        // "--" ends git's options, so a URL can never be mistaken for a flag.
        runner.run("git clone --depth 1 --branch %s -- %s src".formatted(quote(job.branch()), quote(job.repoUrl())),
                dir, log, props.timeout());
        String commitSha = runner.capture("git rev-parse HEAD", sourceDir);
        runner.appendLog(log, "==> Commit %s%n".formatted(commitSha));

        BuildPlan plan = detector.detect(sourceDir);
        runner.appendLog(log, "==> Detected: %s%n".formatted(plan.framework()));

        for (String command : plan.buildCommands()) {
            runner.run(command, sourceDir, log, props.timeout());
        }

        Path artifact = resolveArtifact(plan, sourceDir);
        runner.appendLog(log, "==> Build succeeded, artifact: %s%n".formatted(artifact));
        return new Result(commitSha, plan, artifact);
    }

    private Path deploymentDir(UUID deploymentId) {
        return props.workDir().resolve(deploymentId.toString()).toAbsolutePath();
    }

    private static Path resolveArtifact(BuildPlan plan, Path sourceDir) {
        if (plan.outputDirs().isEmpty()) {
            return sourceDir; // NODE: the server runs from the source folder
        }
        return plan.outputDirs().stream()
                .map(sourceDir::resolve)
                .filter(p -> Files.isRegularFile(p.resolve("index.html")))
                .findFirst()
                .map(Path::normalize)
                .orElseThrow(() -> new BuildException(
                        "Build finished but no index.html found in any of " + plan.outputDirs()));
    }

    private static void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BuildException("Could not create work directory: " + e.getMessage());
        }
    }

    // Repo URL and branch come from users and end up in a shell command. Allow only characters that
    // are safe in both sh and cmd, so values like "$(rm -rf ~)" can never run as commands.
    private static final Pattern SAFE_ARG = Pattern.compile("[A-Za-z0-9@:/._~+=-]+");

    private static String quote(String value) {
        if (!SAFE_ARG.matcher(value).matches()) {
            throw new BuildException("Repo URL or branch contains unsupported characters: " + value);
        }
        return "\"" + value + "\"";
    }
}
