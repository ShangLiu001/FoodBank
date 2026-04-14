package com.primaryfeed.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Translates DataIntegrityViolationException (including DB trigger violations from
 * SQLSTATE '45000') into a clean 400 JSON response so the frontend receives a readable message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException ex) {
        // Walk the cause chain — the deepest SQLException carries the trigger MESSAGE_TEXT
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg == null || msg.isBlank()) msg = ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }
}
