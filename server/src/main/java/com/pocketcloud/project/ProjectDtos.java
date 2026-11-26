package com.pocketcloud.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** Request/response shapes for the Projects API, kept separate from the JPA entity. */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            // Lowercase DNS label, because the name becomes a subdomain.
            @NotBlank
            @Size(min = 2, max = 40)
            @Pattern(regexp = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$",
                    message = "must be lowercase letters, digits or '-', and not start/end with '-'")
            String name,

            @NotBlank
            @Pattern(regexp = "^(https://|git@).+", message = "must be an https:// or git@ repository URL")
            String repoUrl,

            @Size(max = 100)
            String branch
    ) {
    }

    public record UpdateProjectRequest(
            @NotBlank
            @Pattern(regexp = "^(https://|git@).+", message = "must be an https:// or git@ repository URL")
            String repoUrl,

            @NotBlank
            @Size(max = 100)
            String branch
    ) {
    }

    public record ProjectResponse(
            UUID id,
            String name,
            String repoUrl,
            String branch,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ProjectResponse from(Project p) {
            return new ProjectResponse(p.getId(), p.getName(), p.getRepoUrl(), p.getBranch(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
