package com.pocketcloud.build;

import com.pocketcloud.deployment.AppType;
import com.pocketcloud.deployment.BuildJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs the real pipeline (real git, real processes) against a throwaway local repo. Needs git on PATH. */
class BuildPipelineTest {

    @TempDir
    Path tmp;

    ProcessRunner runner = new ProcessRunner();

    @Test
    void clonesAndBuildsStaticSite() throws IOException {
        Path repo = createRepo("<h1>hello</h1>");
        BuildPipeline pipeline = pipeline();
        UUID id = UUID.randomUUID();

        BuildPipeline.Result result = pipeline.run(new BuildJob(id, "hello", repo.toUri().toString(), "main"));

        assertThat(result.plan().appType()).isEqualTo(AppType.STATIC);
        assertThat(result.commitSha()).hasSize(40);
        assertThat(result.artifactPath().resolve("index.html")).exists();
        assertThat(Files.readString(pipeline.logFile(id))).contains("Detected: Static site", "Build succeeded");
    }

    @Test
    void rejectsShellInjectionInBranch() throws IOException {
        Path repo = createRepo("<h1>hello</h1>");

        assertThatThrownBy(() -> pipeline().run(
                new BuildJob(UUID.randomUUID(), "x", repo.toUri().toString(), "main$(whoami)")))
                .isInstanceOf(BuildException.class)
                .hasMessageContaining("unsupported characters");
    }

    private BuildPipeline pipeline() {
        var props = new BuildProperties(true, tmp.resolve("work"), Duration.ofSeconds(2), Duration.ofMinutes(1));
        return new BuildPipeline(props, runner, new FrameworkDetector(JsonMapper.builder().build()));
    }

    private Path createRepo(String indexHtml) throws IOException {
        Path repo = Files.createDirectories(tmp.resolve("repo"));
        Files.writeString(repo.resolve("index.html"), indexHtml);
        runner.capture("git init -b main", repo);
        runner.capture("git add .", repo);
        runner.capture("git -c user.name=test -c user.email=test@example.com commit -m init", repo);
        return repo;
    }
}
