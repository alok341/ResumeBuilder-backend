package com.alok.resumebuilder.controller;

import com.alok.resumebuilder.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.alok.resumebuilder.util.AppConstants.EMAIL;

@RestController
@RequiredArgsConstructor
@RequestMapping(EMAIL)
@Slf4j
public class EmailController {
    private final EmailService emailService;

    @PostMapping(value = "/send-resume",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,Object>> sendResumeByEmail(
            @RequestPart("recipientEmail") String recipientEmail,
            @RequestPart("subject") String subject,
            @RequestPart("message") String message,
            @RequestPart("pdfFile") MultipartFile pdffile,
            Authentication authentication
    ) throws IOException {
        Map<String,Object> response = new HashMap<>();
        if(Objects.isNull(recipientEmail) || Objects.isNull(pdffile)) {
            response.put("success", false);
            response.put("message", "Missing Required fields");
            return ResponseEntity.badRequest().body(response);
        }
        byte[] pdfBytes = pdffile.getBytes();
        String originalFileName = pdffile.getOriginalFilename();
        String filename = Objects.nonNull(originalFileName) ? originalFileName : "resume.pdf";

        String emailSubject = Objects.nonNull(subject) ? subject : "Resume Application";
        String emailBody = Objects.nonNull(message) ? message : "Please find my resume attached.\n\n Best Regards.";

        emailService.sendEmailWithAttachment(recipientEmail,emailSubject,emailBody,pdfBytes,filename);
        response.put("success",true);
        response.put("message","Resume send successfully to " + recipientEmail);
        return ResponseEntity.ok(response);


    }
}
