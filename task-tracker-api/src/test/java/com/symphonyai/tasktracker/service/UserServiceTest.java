package com.symphonyai.tasktracker.service;

import com.symphonyai.tasktracker.dto.UserRequest;
import com.symphonyai.tasktracker.dto.UserResponse;
import com.symphonyai.tasktracker.entity.User;
import com.symphonyai.tasktracker.exception.DuplicateResourceException;
import com.symphonyai.tasktracker.exception.ResourceNotFoundException;
import com.symphonyai.tasktracker.repository.TaskRepository;
import com.symphonyai.tasktracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Ada Lovelace");
        user.setEmail("ada@example.com");
        user.setRole("DEVELOPER");
        return user;
    }

    @Test
    void create_savesAndReturnsUser() {
        UserRequest request = new UserRequest("Ada Lovelace", "ada@example.com", "DEVELOPER");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser());

        UserResponse response = userService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.role()).isEqualTo("DEVELOPER");
    }

    @Test
    void create_throwsConflictWhenEmailExists() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(
                new UserRequest("Other", "ada@example.com", "QA")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFound() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_throwsConflictWhenEmailTakenByAnotherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser()));
        when(userRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L,
                new UserRequest("Ada", "taken@example.com", "DEVELOPER")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void delete_unassignsTasksBeforeDeletingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser()));

        userService.delete(1L);

        verify(taskRepository).unassignTasksFromUser(1L);
        verify(userRepository).delete(any(User.class));
    }
}
