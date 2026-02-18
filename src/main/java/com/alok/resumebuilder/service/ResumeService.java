package com.alok.resumebuilder.service;

import com.alok.resumebuilder.Document.Resume;
import com.alok.resumebuilder.Dto.AuthResponse;
import com.alok.resumebuilder.Dto.CreateResumeRequest;
import com.alok.resumebuilder.repository.ResumeRepository;
import com.alok.resumebuilder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public Resume createResume(
            CreateResumeRequest request,
            Authentication authentication) {

        String userId = authentication.getName(); // ✅ USER ID

        Resume newResume = new Resume();
        newResume.setUserId(userId);
        newResume.setTitle(request.getTitle());
        setDefaultResumeData(newResume);

        return resumeRepository.save(newResume);
    }


    private void setDefaultResumeData(Resume newResume) {
        newResume.setProfileInfo(new Resume.ProfileInfo());
        newResume.setContactInfo(new Resume.ContactInfo());
        newResume.setWorkExperiences(new ArrayList<>());
        newResume.setEducations(new ArrayList<>());
        newResume.setSkills(new ArrayList<>());
        newResume.setProjects(new ArrayList<>());
        newResume.setCertifications(new ArrayList<>());
        newResume.setLanguages(new ArrayList<>());
        newResume.setInterests(new ArrayList<>());
    }

    public List<Resume> getUserResumes(String userId) {
        AuthResponse response = authService.getProfile(userId);
        List<Resume> resumes =  resumeRepository.findByUserIdOrderByUpdatedAtDesc(response.getId());
        return resumes;
    }

    public Resume getResumeById(String id, String userId) {
        AuthResponse response = authService.getProfile(userId);
        Resume resume = resumeRepository.findByIdAndUserId(id, response.getId())
                .orElseThrow(() -> new RuntimeException("Resume not found "));

        return resume;
    }

    public Resume updateResume(String id, Resume updatedData, String userId) {
        AuthResponse response = authService.getProfile(userId);
        Resume existingResume = resumeRepository.findByIdAndUserId(id, response.getId())
                .orElseThrow(() -> new RuntimeException("Resume not found "));

        // Update fields
        existingResume.setTitle(updatedData.getTitle());
        existingResume.setThumbnailLink(updatedData.getThumbnailLink());
        existingResume.setTemplate(updatedData.getTemplate());
        existingResume.setProfileInfo(updatedData.getProfileInfo());
        existingResume.setContactInfo(updatedData.getContactInfo());
        existingResume.setWorkExperiences(updatedData.getWorkExperiences());
        existingResume.setEducations(updatedData.getEducations());
        existingResume.setSkills(updatedData.getSkills());
        existingResume.setProjects(updatedData.getProjects());
        existingResume.setCertifications(updatedData.getCertifications());
        existingResume.setLanguages(updatedData.getLanguages());
        existingResume.setInterests(updatedData.getInterests());

        resumeRepository.save(existingResume);
        return existingResume;
    }

    public Resume deleteResume(String id, String name) {
        AuthResponse response = authService.getProfile(name);
        Resume existingResume = resumeRepository.findByIdAndUserId(id, response.getId())
                .orElseThrow(() -> new RuntimeException("Resume not found "));

        resumeRepository.delete(existingResume);
        return existingResume;
    }
}
