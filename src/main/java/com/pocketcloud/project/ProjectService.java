package com.pocketcloud.project;

import com.pocketcloud.common.ConflictException;
import com.pocketcloud.common.NotFoundException;
import com.pocketcloud.project.ProjectDtos.CreateProjectRequest;
import com.pocketcloud.project.ProjectDtos.UpdateProjectRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private static final String DEFAULT_BRANCH = "main";

    private final ProjectRepository projects;

    public ProjectService(ProjectRepository projects) {
        this.projects = projects;
    }

    @Transactional
    public Project create(CreateProjectRequest request) {
        if (projects.existsByName(request.name())) {
            throw new ConflictException("Project '%s' already exists".formatted(request.name()));
        }
        String branch = request.branch() == null || request.branch().isBlank()
                ? DEFAULT_BRANCH
                : request.branch();
        return projects.save(new Project(request.name(), request.repoUrl(), branch));
    }

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projects.findAll();
    }

    @Transactional(readOnly = true)
    public Project getByName(String name) {
        return projects.findByName(name)
                .orElseThrow(() -> new NotFoundException("Project '%s' not found".formatted(name)));
    }

    @Transactional
    public Project update(String name, UpdateProjectRequest request) {
        Project project = getByName(name);
        project.update(request.repoUrl(), request.branch());
        return project; // dirty checking flushes the change on commit
    }

    @Transactional
    public void delete(String name) {
        projects.delete(getByName(name));
    }
}
