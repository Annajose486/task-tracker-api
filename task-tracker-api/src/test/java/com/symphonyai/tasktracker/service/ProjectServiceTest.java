package com.symphonyai.tasktracker.service;

import com.symphonyai.tasktracker.dto.PageResponse;
import com.symphonyai.tasktracker.dto.ProjectRequest;
import com.symphonyai.tasktracker.dto.ProjectResponse;
import com.symphonyai.tasktracker.entity.Project;
import com.symphonyai.tasktracker.exception.DuplicateResourceException;
import com.symphonyai.tasktracker.exception.ResourceNotFoundException;
import com.symphonyai.tasktracker.repository.ProjectRepository;
import com.symphonyai.tasktracker.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project sampleProject() {
        Project project = new Project();
        project.setId(1L);
        project.setName("Website Revamp");
        project.setDescription("Marketing site rebuild");
        project.setCreatedAt(LocalDateTime.now());
        return project;
    }

    @Test
    void create_savesAndReturnsProject() {
        ProjectRequest request = new ProjectRequest("Website Revamp", "Marketing site rebuild");
        when(projectRepository.existsByName("Website Revamp")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject());

        ProjectResponse response = projectService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Website Revamp");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void create_throwsConflictWhenNameExists() {
        when(projectRepository.existsByName("Website Revamp")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(new ProjectRequest("Website Revamp", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Website Revamp");

        verify(projectRepository, never()).save(any());
    }

    @Test
    void getById_returnsProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));

        ProjectResponse response = projectService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void getById_throwsNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_throwsConflictWhenNameTakenByAnotherProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(projectRepository.existsByNameAndIdNot("Taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> projectService.update(1L, new ProjectRequest("Taken", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void delete_removesExistingProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject()));

        projectService.delete(1L);

        verify(projectRepository).delete(any(Project.class));
    }

    @Test
    void delete_throwsNotFoundForMissingProject() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_returnsPagedProjects() {
        when(projectRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleProject())));

        PageResponse<ProjectResponse> page = projectService.list(Pageable.unpaged());

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).name()).isEqualTo("Website Revamp");
    }
}
