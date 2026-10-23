package com.symphonyai.tasktracker.service;

import com.symphonyai.tasktracker.dto.PageResponse;
import com.symphonyai.tasktracker.dto.TaskRequest;
import com.symphonyai.tasktracker.dto.TaskResponse;
import com.symphonyai.tasktracker.entity.Project;
import com.symphonyai.tasktracker.entity.Task;
import com.symphonyai.tasktracker.entity.TaskPriority;
import com.symphonyai.tasktracker.entity.TaskStatus;
import com.symphonyai.tasktracker.entity.User;
import com.symphonyai.tasktracker.exception.ResourceNotFoundException;
import com.symphonyai.tasktracker.repository.ProjectRepository;
import com.symphonyai.tasktracker.repository.TaskRepository;
import com.symphonyai.tasktracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskPriority defaultPriority;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       @Value("${task.default-priority}") TaskPriority defaultPriority) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.defaultPriority = defaultPriority;
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        applyRequest(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(TaskStatus status, Long projectId, Pageable pageable) {
        Page<Task> page;
        if (status != null && projectId != null) {
            page = taskRepository.findByStatusAndProjectId(status, projectId, pageable);
        } else if (status != null) {
            page = taskRepository.findByStatus(status, pageable);
        } else if (projectId != null) {
            page = taskRepository.findByProjectId(projectId, pageable);
        } else {
            page = taskRepository.findAll(pageable);
        }
        return PageResponse.from(page, TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        return TaskResponse.from(findOrThrow(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findOrThrow(id);
        applyRequest(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = findOrThrow(id);
        taskRepository.delete(task);
    }

    private void applyRequest(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setPriority(request.priority() != null ? request.priority() : defaultPriority);
        task.setProject(findProject(request.projectId()));
        task.setAssignee(request.assigneeId() != null
                ? findUser(request.assigneeId()) : null);
        task.setDueDate(request.dueDate());
    }

    private Task findOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
