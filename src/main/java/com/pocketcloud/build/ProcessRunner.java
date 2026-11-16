package com.pocketcloud.build;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs shell commands for builds, appending all output to a log file. */
@Component
public class ProcessRunner {

    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    /** Runs {@code command} in {@code dir}; throws {@link BuildException} on non-zero exit or timeout. */
    public void run(String command, Path dir, Path logFile, Duration timeout) {
        appendLog(logFile, "$ " + command + System.lineSeparator());

        Process process = start(command, dir, logFile);
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                killTree(process);
                throw new BuildException("'%s' timed out after %s".formatted(command, timeout));
            }
            if (process.exitValue() != 0) {
                throw new BuildException("'%s' failed with exit code %d".formatted(command, process.exitValue()));
            }
        } catch (InterruptedException e) {
            killTree(process);
            Thread.currentThread().interrupt();
            throw new BuildException("'%s' was interrupted".formatted(command));
        }
    }

    /** Runs a quick command and returns its trimmed stdout (e.g. {@code git rev-parse HEAD}). */
    public String capture(String command, Path dir) {
        try {
            Process process = shell(command).directory(dir.toFile()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.waitFor() != 0) {
                throw new BuildException("'%s' failed: %s".formatted(command, output));
            }
            return output;
        } catch (IOException e) {
            throw new BuildException("Could not run '%s': %s".formatted(command, e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildException("'%s' was interrupted".formatted(command));
        }
    }

    public void appendLog(Path logFile, String text) {
        try {
            Files.writeString(logFile, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new BuildException("Could not write build log: " + e.getMessage());
        }
    }

    private Process start(String command, Path dir, Path logFile) {
        try {
            return shell(command)
                    .directory(dir.toFile())
                    .redirectErrorStream(true)                                   // stderr -> same stream
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()))
                    .start();
        } catch (IOException e) {
            throw new BuildException("Could not run '%s': %s".formatted(command, e.getMessage()));
        }
    }

    // Going through a shell resolves npm.cmd on Windows and PATH lookups everywhere.
    private static ProcessBuilder shell(String command) {
        return new ProcessBuilder(WINDOWS ? List.of("cmd", "/c", command) : List.of("sh", "-c", command));
    }

    // npm spawns child processes; killing only the parent would leave them running.
    private static void killTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}
