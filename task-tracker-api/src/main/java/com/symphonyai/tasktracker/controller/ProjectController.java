package com.symphonyai.tasktracker.controller;

import com.symphonyai.tasktracker.dto.PageResponse;
import com.symphonyai.tasktracker.dto.ProjectRequest;
import com.symphonyai.tasktracker.dto.ProjectResponse;
import com.symphonyai.tasktracker.dto.TaskResponse;
import com.symphonyai.tasktracker.service.ProjectService;
import com.symphonyai.tasktracker.web.Pageables;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${pagination.default-page-size}") int size,
            Sort sort) {
        return projectService.list(Pageables.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/tasks")
    public PageResponse<TaskResponse> listTasks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${pagination.default-page-size}") int size,
            Sort sort) {
        return projectService.listTasks(id, Pageables.of(page, size, sort));
    }
}
