package com.symphonyai.tasktracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "User name is required")
        @Size(max = 255, message = "User name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 100, message = "Role must not exceed 100 characters")
        String role
) {
}
