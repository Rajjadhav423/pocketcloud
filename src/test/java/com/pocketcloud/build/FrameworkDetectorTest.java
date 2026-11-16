package com.pocketcloud.build;

import com.pocketcloud.deployment.AppType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrameworkDetectorTest {

    @TempDir
    Path repo;

    FrameworkDetector detector = new FrameworkDetector(JsonMapper.builder().build());

    @Test
    void plainIndexHtmlIsStaticWithNoBuild() throws IOException {
        Files.writeString(repo.resolve("index.html"), "<h1>hi</h1>");

        BuildPlan plan = detector.detect(repo);

        assertThat(plan.appType()).isEqualTo(AppType.STATIC);
        assertThat(plan.buildCommands()).isEmpty();
    }

    @Test
    void nodeWithStartScriptIsServer() throws IOException {
        Files.writeString(repo.resolve("package.json"), """
                {"scripts": {"start": "node server.js"}}
                """);

        BuildPlan plan = detector.detect(repo);

        assertThat(plan.appType()).isEqualTo(AppType.NODE);
        assertThat(plan.buildCommands()).containsExactly("npm install");
        assertThat(plan.startCommand()).isEqualTo("npm start");
    }

    @Test
    void nodeWithOnlyBuildScriptIsStaticAndUsesLockfile() throws IOException {
        Files.writeString(repo.resolve("package.json"), """
                {"scripts": {"build": "vite build"}}
                """);
        Files.writeString(repo.resolve("package-lock.json"), "{}");

        BuildPlan plan = detector.detect(repo);

        assertThat(plan.appType()).isEqualTo(AppType.STATIC);
        assertThat(plan.buildCommands()).containsExactly("npm ci", "npm run build");
        assertThat(plan.outputDirs()).contains("dist", "build");
    }

    @Test
    void unknownRepoFails() {
        assertThatThrownBy(() -> detector.detect(repo))
                .isInstanceOf(BuildException.class)
                .hasMessageContaining("Could not detect");
    }
}
