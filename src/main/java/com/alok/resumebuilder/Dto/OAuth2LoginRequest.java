package com.alok.resumebuilder.Dto;

import lombok.Data;

@Data
public class OAuth2LoginRequest {
    private String email;
    private String name;
    private String profileImageUrl;
    private String provider; // "google", "github", etc.
    private String providerId;
}