package com.pocketcloud.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    @Column(nullable = false, length = 100)
    private String branch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
        // required by JPA
    }

    public Project(String name, String repoUrl, String branch) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.repoUrl = repoUrl;
        this.branch = branch;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(String repoUrl, String branch) {
        this.repoUrl = repoUrl;
        this.branch = branch;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getRepoUrl() { return repoUrl; }
    public String getBranch() { return branch; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
