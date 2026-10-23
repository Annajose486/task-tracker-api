package com.symphonyai.tasktracker.dto;

import com.symphonyai.tasktracker.entity.TaskPriority;
import com.symphonyai.tasktracker.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "Task title is required")
        @Size(max = 255, message = "Task title must not exceed 255 characters")
        String title,

        TaskStatus status,

        TaskPriority priority,

        @NotNull(message = "projectId is required")
        Long projectId,

        Long assigneeId,

        LocalDate dueDate
) {
}
