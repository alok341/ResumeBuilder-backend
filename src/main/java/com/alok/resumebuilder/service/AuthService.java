package com.alok.resumebuilder.service;

import com.alok.resumebuilder.Document.User;
import com.alok.resumebuilder.Dto.AuthResponse;
import com.alok.resumebuilder.Dto.LoginRequest;
import com.alok.resumebuilder.Dto.OAuth2LoginRequest;
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
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend.url}")
    private String frontendUrl;


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
            // Remove "/api/auth" from the path
            String link = frontendUrl + "/verify-email?token=" + newUser.getVerificationToken();
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
        log.info("Inside AuthService : login() {}", loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        // Check if user has password set
        if (!user.isHasPassword()) {
            throw new RuntimeException("Please login with Google");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new UsernameNotFoundException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email is not verified. Please verify your email before login.");
        }

        AuthResponse response = toResponse(user);
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

    public AuthResponse oauth2Login(OAuth2LoginRequest request) {
        log.info("Inside AuthService : oauth2Login() {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            // Create new user with OAuth provider
            user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .profileImageUrl(request.getProfileImageUrl())
                    .password(null) // No password for OAuth users
                    .subscriptionPlan("basic")
                    .emailVerified(true) // Google emails are verified
                    .hasPassword(false) // Password login not enabled
                    .oauthProviders(new ArrayList<>())
                    .build();

            // Add OAuth provider
            user.getOauthProviders().add(new User.OAuthProvider(
                    request.getProvider(),
                    request.getProviderId(),
                    request.getEmail(),
                    request.getName(),
                    request.getProfileImageUrl()
            ));

            userRepository.save(user);
            log.info("New OAuth user created: {}", user.getEmail());

        } else {
            // Check if this OAuth provider is already linked
            boolean hasProvider = user.getOauthProviders().stream()
                    .anyMatch(p -> p.getProvider().equals(request.getProvider())
                            && p.getProviderId().equals(request.getProviderId()));

            if (!hasProvider) {
                // Link new OAuth provider to existing account
                user.getOauthProviders().add(new User.OAuthProvider(
                        request.getProvider(),
                        request.getProviderId(),
                        request.getEmail(),
                        request.getName(),
                        request.getProfileImageUrl()
                ));

                // Update profile image if needed
                if (request.getProfileImageUrl() != null &&
                        (user.getProfileImageUrl() == null || !user.getProfileImageUrl().equals(request.getProfileImageUrl()))) {
                    user.setProfileImageUrl(request.getProfileImageUrl());
                }

                userRepository.save(user);
                log.info("OAuth provider linked to existing user: {}", user.getEmail());
            }

            // Update profile info if changed
            if (request.getProfileImageUrl() != null &&
                    (user.getProfileImageUrl() == null || !user.getProfileImageUrl().equals(request.getProfileImageUrl()))) {
                user.setProfileImageUrl(request.getProfileImageUrl());
                userRepository.save(user);
            }
        }

        AuthResponse response = toResponse(user);
        String token = jwtUtil.generateToken(user.getId());
        response.setToken(token);

        log.info("OAuth2 login successful for user: {}", user.getEmail());
        return response;
    }
    public void setPassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setHasPassword(true);
        userRepository.save(user);

        log.info("Password set for user: {}", user.getEmail());
    }
    public void initiatePasswordSetup(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate a special token for password setup
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationExpires(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        // Send email with password setup link
        String link = frontendUrl + "/set-password?token=" + token;
        emailService.sendHtmlEmail(email, "Set Your Password",
                "<p>Click the link to set a password for your account: <a href='" + link + "'>Set Password</a></p>");
    }


}
