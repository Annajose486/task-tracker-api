package com.symphonyai.tasktracker.controller;

import com.symphonyai.tasktracker.dto.PageResponse;
import com.symphonyai.tasktracker.dto.TaskRequest;
import com.symphonyai.tasktracker.dto.TaskResponse;
import com.symphonyai.tasktracker.entity.TaskStatus;
import com.symphonyai.tasktracker.service.TaskService;
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
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @GetMapping
    public PageResponse<TaskResponse> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${pagination.default-page-size}") int size,
            Sort sort) {
        return taskService.list(status, projectId, Pageables.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
