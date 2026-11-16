package com.pocketcloud.build;

import com.pocketcloud.deployment.AppType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Looks at the files in a repo and decides how to build it, like Vercel's zero-config detection. */
@Component
public class FrameworkDetector {

    // Common output folders: Vite -> dist, Create React App -> build, Next export -> out.
    private static final List<String> STATIC_OUTPUT_DIRS = List.of("dist", "build", "out", "public");

    private final ObjectMapper json;

    public FrameworkDetector(ObjectMapper json) {
        this.json = json;
    }

    public BuildPlan detect(Path sourceDir) {
        Path packageJson = sourceDir.resolve("package.json");
        if (Files.isRegularFile(packageJson)) {
            return nodePlan(sourceDir, packageJson);
        }
        if (Files.isRegularFile(sourceDir.resolve("index.html"))) {
            return new BuildPlan("Static site", AppType.STATIC, List.of(), null, List.of("."));
        }
        throw new BuildException("Could not detect a framework: expected package.json or index.html in the repo root");
    }

    private BuildPlan nodePlan(Path sourceDir, Path packageJson) {
        JsonNode scripts = readJson(packageJson).path("scripts");

        List<String> commands = new ArrayList<>();
        // npm ci is faster and reproducible, but only works when a lockfile exists.
        commands.add(Files.exists(sourceDir.resolve("package-lock.json")) ? "npm ci" : "npm install");
        if (scripts.has("build")) {
            commands.add("npm run build");
        }

        if (scripts.has("start")) {
            return new BuildPlan("Node.js server", AppType.NODE, commands, "npm start", List.of());
        }
        if (scripts.has("build")) {
            return new BuildPlan("Node.js static build", AppType.STATIC, commands, null, STATIC_OUTPUT_DIRS);
        }
        throw new BuildException("package.json needs a \"start\" script (server) or a \"build\" script (static site)");
    }

    private JsonNode readJson(Path file) {
        try {
            return json.readTree(Files.readString(file));
        } catch (IOException | RuntimeException e) {
            throw new BuildException("Could not read package.json: " + e.getMessage());
        }
    }
}
