package com.pocketcloud.deployment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DeploymentController {

    private final DeploymentService service;

    public DeploymentController(DeploymentService service) {
        this.service = service;
    }

    /** 202 Accepted: the work is queued, not finished. Poll the Location URL for status. */
    @PostMapping("/projects/{name}/deployments")
    public ResponseEntity<DeploymentResponse> trigger(@PathVariable String name) {
        DeploymentResponse deployment = DeploymentResponse.from(service.trigger(name));
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/deployments/{id}").buildAndExpand(deployment.id()).toUri();
        return ResponseEntity.accepted().location(location).body(deployment);
    }

    @GetMapping("/projects/{name}/deployments")
    public List<DeploymentResponse> list(@PathVariable String name) {
        return service.listForProject(name);
    }

    @GetMapping("/deployments/{id}")
    public DeploymentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
