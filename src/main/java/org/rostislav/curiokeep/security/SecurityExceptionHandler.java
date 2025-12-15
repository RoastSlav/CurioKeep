package org.rostislav.curiokeep.security;

import org.rostislav.curiokeep.api.dto.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> badCreds() {
        return ResponseEntity.status(401)
                .body(new ApiError("INVALID_CREDENTIALS", "Email or password is incorrect"));
    }
}