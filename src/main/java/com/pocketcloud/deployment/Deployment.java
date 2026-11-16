package com.pocketcloud.deployment;

import com.pocketcloud.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deployments")
public class Deployment {

    @Id
    private UUID id;

    // LAZY: loading a deployment should not pull the project unless we ask for it.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeploymentStatus status;

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    private String error;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_type", length = 20)
    private AppType appType;

    @Column(name = "start_command")
    private String startCommand;

    @Column(name = "artifact_path")
    private String artifactPath;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Deployment() {
        // required by JPA
    }

    public Deployment(Project project) {
        this.id = UUID.randomUUID();
        this.project = project;
        this.status = DeploymentStatus.QUEUED;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markReady(String commitSha, AppType appType, String startCommand, String artifactPath) {
        this.status = DeploymentStatus.READY;
        this.commitSha = commitSha;
        this.appType = appType;
        this.startCommand = startCommand;
        this.artifactPath = artifactPath;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String commitSha, String error) {
        this.status = DeploymentStatus.FAILED;
        this.commitSha = commitSha;
        this.error = error;
        this.finishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Project getProject() { return project; }
    public DeploymentStatus getStatus() { return status; }
    public String getCommitSha() { return commitSha; }
    public String getError() { return error; }
    public AppType getAppType() { return appType; }
    public String getStartCommand() { return startCommand; }
    public String getArtifactPath() { return artifactPath; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
