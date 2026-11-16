package com.pocketcloud.build;

import com.pocketcloud.deployment.AppType;

import java.util.List;

/**
 * What to do with a cloned repo.
 *
 * @param framework        human-readable name for logs ("Static site", "Node.js")
 * @param appType          how the runtime will run the result
 * @param buildCommands    shell commands run in order inside the source folder
 * @param startCommand     command that starts the server (NODE only)
 * @param outputDirs       for STATIC: candidate folders holding the built site, first existing one wins
 */
public record BuildPlan(
        String framework,
        AppType appType,
        List<String> buildCommands,
        String startCommand,
        List<String> outputDirs
) {
}
