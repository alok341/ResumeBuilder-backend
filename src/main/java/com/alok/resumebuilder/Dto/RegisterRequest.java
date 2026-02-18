package com.alok.resumebuilder.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "name is required")
    @Size(min=2, max=20, message = "name must be between 2 and 20 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email should be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min=6, message = "password must be at least 6 characters")
    private String password;

    private String profileImageUrl;
}
