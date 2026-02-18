package com.alok.resumebuilder.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String key = error instanceof FieldError
                    ? ((FieldError) error).getField()
                    : error.getObjectName();
            errors.put(key, error.getDefaultMessage());
        });

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("message", "Validation failed");
        responseBody.put("errors", errors);
        responseBody.put("status", 400);
        responseBody.put("timestamp", Instant.now());

        return ResponseEntity.badRequest().body(responseBody);
    }

    @ExceptionHandler(ResourceExistsException.class)
    public ResponseEntity<Map<String, Object>> handleResourceExistsException(
            ResourceExistsException ex) {

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Resource already exists");
        responseBody.put("errors", ex.getMessage());
        responseBody.put("status", HttpStatus.CONFLICT);
        responseBody.put("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleGenericException(Exception ex){
        Map<String,Object> responseBody=new HashMap<>();
        responseBody.put("message","An error occurred. Contact Administrator");
        responseBody.put("errors",ex.getMessage());
        responseBody.put("status",HttpStatus.INTERNAL_SERVER_ERROR);
        responseBody.put("timestamp",Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);

    }
}

