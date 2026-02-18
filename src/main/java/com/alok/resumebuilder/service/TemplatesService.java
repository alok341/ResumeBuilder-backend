package com.alok.resumebuilder.service;

import com.alok.resumebuilder.Dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alok.resumebuilder.util.AppConstants.PREMIUM;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplatesService {

    private final AuthService authService;

    public Map<String,Object> getTemplates(String userId) {
        log.info("Inside TemplatesService : getTemplates() {}", userId);
        AuthResponse response = authService.getProfile(userId);;
        List<String> availableTemplates ;
        Boolean isPremium = PREMIUM.equalsIgnoreCase(response.getSubscriptionPlan());
        if(isPremium) {
            availableTemplates = List.of("01","02","03");
        } else {
            availableTemplates = List.of("01");
        }
        log.info("Available templates for user {} : {}", userId, availableTemplates);
        Map<String,Object> result = new HashMap<>();
        result.put("availavleTemplates",availableTemplates);
        result.put("allTemplates", List.of("01","02","03"));
        result.put("subscriptionPlan", List.of(response.getSubscriptionPlan()));
        result.put("isPremium",isPremium);
        return result;

    }
}
