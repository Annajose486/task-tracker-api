package com.symphonyai.tasktracker.dto;

import com.symphonyai.tasktracker.entity.Task;
import com.symphonyai.tasktracker.entity.TaskPriority;
import com.symphonyai.tasktracker.entity.TaskStatus;

import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        TaskStatus status,
        TaskPriority priority,
        Long projectId,
        Long assigneeId,
        LocalDate dueDate
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getProject().getId(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getDueDate()
        );
    }
}
