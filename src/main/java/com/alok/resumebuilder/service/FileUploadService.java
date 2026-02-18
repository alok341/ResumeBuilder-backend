package com.alok.resumebuilder.service;

import com.alok.resumebuilder.Document.Resume;
import com.alok.resumebuilder.Document.User;
import com.alok.resumebuilder.Dto.AuthResponse;
import com.alok.resumebuilder.repository.ResumeRepository;
import com.alok.resumebuilder.repository.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final Cloudinary cloudinary;
    private final AuthService authService;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    public Map<String,String> uploadSingleImage(MultipartFile file) throws IOException {
        Map<String,Object> imageUploadResult =  cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "image"));
        log.info("Inside FileUploadService : uploadSingleImage() with result {}", imageUploadResult);

        // Get the actual Cloudinary URL
        String imageUrl = imageUploadResult.get("secure_url").toString();


        return Map.of("imageUrl", imageUrl);
    }
    public Map<String,String> uploadSingleImage(MultipartFile file, String userId) throws IOException {
        Map<String,Object> imageUploadResult =  cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "image"));
        log.info("Inside FileUploadService : uploadSingleImage() with result {}", imageUploadResult);

        // Get the actual Cloudinary URL
        String imageUrl = imageUploadResult.get("secure_url").toString();

        // Find the user and update their profileImageUrl
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);

        return Map.of("imageUrl", imageUrl);
    }

    public Map<String, String> uploadResumeImages(String id, MultipartFile thumbnail, MultipartFile profileImage, String userId) throws IOException {
        AuthResponse response = authService.getProfile(userId);
        Resume existingResume = resumeRepository.findByIdAndUserId(id, response.getId())
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        Map<String, String> returnValue = new HashMap<>();

        // Upload thumbnail if provided
        if (Objects.nonNull(thumbnail)) {
            Map<String, String> uploadResult = uploadSingleImage(thumbnail);
            existingResume.setThumbnailLink(uploadResult.get("imageUrl"));
            returnValue.put("thumbnailLink", uploadResult.get("imageUrl"));
        }

        // Upload profile image if provided - FIXED: now checks profileImage
        if (Objects.nonNull(profileImage)) {
            Map<String, String> uploadResult = uploadSingleImage(profileImage);

            // Initialize profileInfo if it doesn't exist
            if (existingResume.getProfileInfo() == null) {
                existingResume.setProfileInfo(new Resume.ProfileInfo());
            }

            existingResume.getProfileInfo().setProfilePreviewUrl(uploadResult.get("imageUrl"));
            returnValue.put("profileImageLink", uploadResult.get("imageUrl"));
        }

        resumeRepository.save(existingResume);
        returnValue.put("message", "Images uploaded successfully");
        return returnValue;
    }
}
