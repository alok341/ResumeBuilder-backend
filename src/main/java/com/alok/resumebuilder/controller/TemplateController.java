package com.alok.resumebuilder.controller;

import com.alok.resumebuilder.service.TemplatesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.alok.resumebuilder.util.AppConstants.TEMPLATE;

@RestController
@RequiredArgsConstructor
@RequestMapping(TEMPLATE)
@Slf4j
public class TemplateController {

    private  final TemplatesService templatesService;

    @GetMapping
    public ResponseEntity<?> getTemplates(Authentication authentication) {
        log.info("Inside TemplateController : getTemplates() ");
        Map<String, Object> response = templatesService.getTemplates(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
