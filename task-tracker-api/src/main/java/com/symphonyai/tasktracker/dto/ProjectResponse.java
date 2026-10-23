package com.symphonyai.tasktracker.dto;

import com.symphonyai.tasktracker.entity.Project;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt()
        );
    }
}
