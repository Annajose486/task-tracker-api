package com.symphonyai.tasktracker.service;

import com.symphonyai.tasktracker.dto.PageResponse;
import com.symphonyai.tasktracker.dto.ProjectRequest;
import com.symphonyai.tasktracker.dto.ProjectResponse;
import com.symphonyai.tasktracker.dto.TaskResponse;
import com.symphonyai.tasktracker.entity.Project;
import com.symphonyai.tasktracker.exception.DuplicateResourceException;
import com.symphonyai.tasktracker.exception.ResourceNotFoundException;
import com.symphonyai.tasktracker.repository.ProjectRepository;
import com.symphonyai.tasktracker.repository.TaskRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        if (projectRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(
                    "Project already exists with name: " + request.name());
        }
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(Pageable pageable) {
        return PageResponse.from(projectRepository.findAll(pageable), ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        return ProjectResponse.from(findOrThrow(id));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findOrThrow(id);
        if (projectRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException(
                    "Project already exists with name: " + request.name());
        }
        project.setName(request.name());
        project.setDescription(request.description());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        Project project = findOrThrow(id);
        // Tasks belonging to the project are removed via cascade + orphanRemoval
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(Long projectId, Pageable pageable) {
        findOrThrow(projectId);
        return PageResponse.from(
                taskRepository.findByProjectId(projectId, pageable), TaskResponse::from);
    }

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
