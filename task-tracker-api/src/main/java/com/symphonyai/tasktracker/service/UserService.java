package com.symphonyai.tasktracker.service;

import com.symphonyai.tasktracker.dto.PageResponse;
import com.symphonyai.tasktracker.dto.UserRequest;
import com.symphonyai.tasktracker.dto.UserResponse;
import com.symphonyai.tasktracker.entity.User;
import com.symphonyai.tasktracker.exception.DuplicateResourceException;
import com.symphonyai.tasktracker.exception.ResourceNotFoundException;
import com.symphonyai.tasktracker.repository.TaskRepository;
import com.symphonyai.tasktracker.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public UserService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "User already exists with email: " + request.email());
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = findOrThrow(id);
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException(
                    "User already exists with email: " + request.email());
        }
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = findOrThrow(id);
        // Tasks keep their project but lose the deleted assignee
        taskRepository.unassignTasksFromUser(id);
        userRepository.delete(user);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
