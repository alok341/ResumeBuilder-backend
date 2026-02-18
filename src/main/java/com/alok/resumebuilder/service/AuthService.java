package com.alok.resumebuilder.service;

import com.alok.resumebuilder.Document.User;
import com.alok.resumebuilder.Dto.AuthResponse;
import com.alok.resumebuilder.Dto.LoginRequest;
import com.alok.resumebuilder.Dto.RegisterRequest;
import com.alok.resumebuilder.exceptions.ResourceExistsException;
import com.alok.resumebuilder.repository.UserRepository;
import com.alok.resumebuilder.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.base.url:http://localhost:8080}")
    private String appUrl;

    private final EmailService emailService;

    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Inside AuthService : register() {}", registerRequest);

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.error("Email already exists: {}", registerRequest.getEmail());
            throw new ResourceExistsException("Email already exists");
        }
        User newUser = toDocument(registerRequest);
        userRepository.save(newUser);

        sendVerificationEmail(newUser);


        return toResponse(newUser);
    }

    private void sendVerificationEmail(User newUser) {
        log.info("Inside AuthService : sendVerificationEmail() to {}", newUser.getEmail());
        try {
            String link = appUrl + "/api/auth/verify-email?token=" + newUser.getVerificationToken();
            String htmlContent = """
                    <p>Dear %s,</p>
                    <p>Thank you for registering. Please click the link below to verify your email address:</p>
                    <p>
                        <a href="%s"
                           style="background-color:#4CAF50;color:white;padding:10px 15px;
                                  text-decoration:none;border-radius:5px;display:inline-block;">
                            Verify Email
                        </a>
                    </p>
                    <p>This link will expire in 24 hours.</p>
                    <p>Best regards,<br/>Resume Builder Team</p>
                    """.formatted(newUser.getName(), link);

            emailService.sendHtmlEmail(newUser.getEmail(), "Email Verification", htmlContent);
            log.info("Verification email sent to: {}", newUser.getEmail());


        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", newUser.getEmail(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }


    }

    private AuthResponse toResponse(User newUser) {
        return AuthResponse.builder()
                .id(newUser.getId())
                .name(newUser.getName())
                .email(newUser.getEmail())
                .profileImageUrl(newUser.getProfileImageUrl())
                .subscriptionPlan(newUser.getSubscriptionPlan())
                .emailVerified(newUser.isEmailVerified())
                .createdAt(newUser.getCreatedAt())
                .updatedAt(newUser.getUpdatedAt())
                .build();
    }

    private User toDocument(RegisterRequest registerRequest) {
        return User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword())) // In real application, password should be hashed
                .profileImageUrl(registerRequest.getProfileImageUrl())
                .subscriptionPlan("basic")
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .verificationExpires(LocalDateTime.now().plusHours(24))
                .build();
    }

    public void verifyEmail(String token) {
        log.info("Inside AuthService : verifyEmail() with token {}", token);
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (user.getVerificationExpires()!= null && user.getVerificationExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpires(null);
        userRepository.save(user);
        log.info("Email verified for user: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Inside AuthService : login() {}", loginRequest);
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new UsernameNotFoundException("Invalid email or password");
        }

        if(user.isEmailVerified()!=true){
            throw new RuntimeException("Email is not verified.Please verify your email before login.");
        }


        AuthResponse response =  toResponse(user);
        String token = jwtUtil.generateToken(user.getId());
        response.setToken(token);
        return response;

    }


    public void resendVerificationEmail(String email) {
        User emailNotFound = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
        if(emailNotFound.isEmailVerified()){
            throw new RuntimeException("Email is already verified");
        }
        emailNotFound.setVerificationToken(UUID.randomUUID().toString());
        emailNotFound.setVerificationExpires(LocalDateTime.now().plusHours(24));
        userRepository.save(emailNotFound);
        sendVerificationEmail(emailNotFound);


    }
    public AuthResponse getProfile(String userId) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(existingUser);
    }


}
