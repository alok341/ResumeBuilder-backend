package com.alok.resumebuilder.controller;

import com.alok.resumebuilder.Document.Resume;
import com.alok.resumebuilder.Dto.CreateResumeRequest;
import com.alok.resumebuilder.service.FileUploadService;
import com.alok.resumebuilder.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.alok.resumebuilder.util.AppConstants.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(RESUME_CONTROLLER)
public class ResumeController {

    private  final ResumeService resumeService;
    private final FileUploadService fileUploadService;


    @PostMapping
    public ResponseEntity<?> createResume(
            @Valid @RequestBody CreateResumeRequest request,
            Authentication authentication) {

        Resume newResume = resumeService.createResume(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(newResume);
    }

    @GetMapping
    public ResponseEntity<List<Resume>> getUserResumes(Authentication authentication) {
        String userId = authentication.getName(); // ✅ USER ID
        List<Resume> resumes = resumeService.getUserResumes(userId);
        return ResponseEntity.ok(resumes);
    }


    @GetMapping(ID)
    public ResponseEntity<?> getResumeById(@PathVariable String id,Authentication authentication) {

        Resume resume = resumeService.getResumeById(id,authentication.getName());
        return ResponseEntity.ok(resume);
    }

    @PutMapping(ID)
    public  ResponseEntity<?> updateResume(@PathVariable String id,  @RequestBody Resume updatedData, Authentication authentication) {
        Resume resume  = resumeService.updateResume(id, updatedData, authentication.getName());
        return ResponseEntity.ok(resume);
    }

    @PutMapping(UPLOAD_IMAGE)
    public ResponseEntity<?> uploadResumeImages(@PathVariable String id,
                                                @RequestPart(value = "thumbnail",required = false) MultipartFile thumbnail,
                                                @RequestPart(value = "profileImage", required = false)MultipartFile profileImage,
                                                Authentication authentication) throws IOException {
        Map<String,String > response = fileUploadService.uploadResumeImages(id,thumbnail,profileImage,authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(ID)
    public ResponseEntity<?> deleteResume(@PathVariable String id, Authentication authentication) {
        Resume response = resumeService.deleteResume(id, authentication.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);

    }

}
