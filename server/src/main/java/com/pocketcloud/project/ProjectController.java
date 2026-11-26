package com.pocketcloud.project;

import com.pocketcloud.project.ProjectDtos.CreateProjectRequest;
import com.pocketcloud.project.ProjectDtos.ProjectResponse;
import com.pocketcloud.project.ProjectDtos.UpdateProjectRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        Project project = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{name}").buildAndExpand(project.getName()).toUri();
        return ResponseEntity.created(location).body(ProjectResponse.from(project));
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return service.findAll().stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{name}")
    public ProjectResponse get(@PathVariable String name) {
        return ProjectResponse.from(service.getByName(name));
    }

    @PutMapping("/{name}")
    public ProjectResponse update(@PathVariable String name, @Valid @RequestBody UpdateProjectRequest request) {
        return ProjectResponse.from(service.update(name, request));
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        service.delete(name);
    }
}
