package com.pocketcloud.deployment;

import com.pocketcloud.common.NotFoundException;
import com.pocketcloud.project.Project;
import com.pocketcloud.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeploymentService {

    private final DeploymentRepository deployments;
    private final ProjectService projects;

    public DeploymentService(DeploymentRepository deployments, ProjectService projects) {
        this.deployments = deployments;
        this.projects = projects;
    }

    /** Queues a deployment. A build worker (next milestone) will pick it up. */
    @Transactional
    public Deployment trigger(String projectName) {
        Project project = projects.getByName(projectName);
        return deployments.save(new Deployment(project));
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> listForProject(String projectName) {
        projects.getByName(projectName); // 404 if the project does not exist
        // Map inside the transaction so the lazy project reference can still load.
        return deployments.findByProjectNameOrderByCreatedAtDesc(projectName).stream()
                .map(DeploymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeploymentResponse get(UUID id) {
        return DeploymentResponse.from(find(id));
    }

    /** Snapshot of what the build worker needs, read in a short transaction before the slow build starts. */
    @Transactional(readOnly = true)
    public BuildJob getBuildJob(UUID id) {
        Deployment d = find(id);
        return new BuildJob(d.getId(), d.getProject().getName(), d.getProject().getRepoUrl(),
                d.getProject().getBranch());
    }

    @Transactional
    public void markReady(UUID id, String commitSha, AppType appType, String startCommand, String artifactPath) {
        find(id).markReady(commitSha, appType, startCommand, artifactPath);
    }

    @Transactional
    public void markFailed(UUID id, String commitSha, String error) {
        find(id).markFailed(commitSha, error);
    }

    private Deployment find(UUID id) {
        return deployments.findById(id)
                .orElseThrow(() -> new NotFoundException("Deployment '%s' not found".formatted(id)));
    }
}
