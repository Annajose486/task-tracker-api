package com.symphonyai.tasktracker.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    private TaskService taskService;

    private Project project;
    private User assignee;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, projectRepository, userRepository,
                TaskPriority.MEDIUM);

        project = new Project();
        project.setId(1L);
        project.setName("Website Revamp");

        assignee = new User();
        assignee.setId(2L);
        assignee.setName("Ada Lovelace");
        assignee.setEmail("ada@example.com");
    }

    @Test
    void create_appliesDefaultPriorityWhenNotSupplied() {
        TaskRequest request = new TaskRequest("Write docs", null, null, 1L, null, null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TaskResponse response = taskService.create(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.projectId()).isEqualTo(1L);
    }

    @Test
    void create_keepsExplicitPriorityAndStatus() {
        TaskRequest request = new TaskRequest("Fix login bug", TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH, 1L, 2L, LocalDate.of(2026, 11, 1));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(11L);
            return t;
        });

        TaskResponse response = taskService.create(request);

        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.assigneeId()).isEqualTo(2L);
    }

    @Test
    void create_throwsNotFoundWhenProjectMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(
                new TaskRequest("Orphan", null, null, 99L, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
    }

    @Test
    void create_throwsNotFoundWhenAssigneeMissing() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(
                new TaskRequest("Orphan", null, null, 1L, 77L, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void update_changesStatusAndDetails() {
        Task existing = new Task();
        existing.setId(5L);
        existing.setTitle("Old title");
        existing.setStatus(TaskStatus.TODO);
        existing.setPriority(TaskPriority.LOW);
        existing.setProject(project);

        when(taskRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.update(5L,
                new TaskRequest("New title", TaskStatus.DONE, null, 1L, null, null));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void getById_throwsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesExistingTask() {
        Task existing = new Task();
        existing.setId(5L);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(existing));

        taskService.delete(5L);

        verify(taskRepository).delete(existing);
    }
}
