package org.rostislav.curiokeep.api;

import org.rostislav.curiokeep.api.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> illegalState(IllegalStateException ex) {
        log.warn("Illegal state exception occurred", ex);
        return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500)
                .body(new ApiError("INTERNAL_ERROR", "Unexpected error"));
    }

}
