package com.alok.resumebuilder.controller;

import com.alok.resumebuilder.Dto.AuthResponse;
import com.alok.resumebuilder.Dto.LoginRequest;
import com.alok.resumebuilder.Dto.RegisterRequest;
import com.alok.resumebuilder.service.AuthService;
import com.alok.resumebuilder.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.alok.resumebuilder.util.AppConstants.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(AUTH_CONTROLLER)
public class AuthController {

    private final AuthService authService;
    private final FileUploadService fileUploadService;

    @PostMapping(REGISTER_URL)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Inside AuthController : register() {}", registerRequest);
        AuthResponse response = authService.register(registerRequest);
        log.info("Response from register: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping(VERIFY_EMAIL_URL)
    public ResponseEntity<?> verifyEmail(@Valid @RequestParam String token) {
        log.info("Inside AuthController : verifyEmail() with token {}", token);
        authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Email verified successfully"));
    }

    @PostMapping(UPLOAD_PROFILE)
    public ResponseEntity<?> uploadProfileImage(
            @RequestPart("image") MultipartFile image,
            Authentication authentication
    ) throws IOException {
        log.info("Inside AuthController : uploadProfileImage() with image {}", image.getOriginalFilename());
        String userId = authentication.getName();

        // Make sure you're calling the method WITH userId
        Map<String,String> response = fileUploadService.uploadSingleImage(image, userId); // THIS ONE

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping(LOGIN)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping(RESEND_VERIFICATION_EMAIL)
    public ResponseEntity<?> resendVerificationEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if(Objects.isNull(email)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Email is required"));
        }
        log.info("Inside AuthController : resendVerificationEmail() to {}", email);
        authService.resendVerificationEmail(email);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Verification email sent successfully"));
    }
    @GetMapping(PROFILE_URL)
    public ResponseEntity<AuthResponse> getProfile(Authentication authentication) {
        String userId = authentication.getName();
        AuthResponse response = authService.getProfile(userId);
        return ResponseEntity.ok(response);
    }


}
