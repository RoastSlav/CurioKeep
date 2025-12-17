package org.rostislav.curiokeep.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.api.dto.LoginRequest;
import org.rostislav.curiokeep.user.api.dto.MeResponse;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;

@Tag(name = "Auth", description = "Session authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authManager;
    private final AppUserRepository users;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthenticationManager authManager, AppUserRepository users, SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
        this.authManager = authManager;
        this.users = users;
    }

    @Operation(summary = "Login", description = "Creates an authenticated session (JSESSIONID cookie).", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in",
                    content = @Content(schema = @Schema(implementation = OkResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth;
        try {
            auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    req.email().trim().toLowerCase(),
                    req.password()
                )
            );
        } catch (BadCredentialsException e) {
            log.warn("Login failed: bad credentials for email={}", req.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("UNAUTHORIZED", "Invalid email or password"));
        } catch (Exception e) {
            log.error("Login failed: unexpected error for email={}", req.email(), e);
            throw new AuthenticationServiceException("Login failed", e);
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        request.getSession(true);

        users.findByEmailIgnoreCase(req.email().trim().toLowerCase()).ifPresent(u -> {
            u.setLastLoginAt(OffsetDateTime.now());
            users.save(u);
        });

        return ResponseEntity.ok(new OkResponse(true));
    }

    @Operation(summary = "Logout", description = "Invalidates the current session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out",
                    content = @Content(schema = @Schema(implementation = OkResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication, HttpServletRequest request) {
        new SecurityContextLogoutHandler().logout(request, null, authentication);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @Operation(summary = "Current user", description = "Returns the currently authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User info returned",
                    content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        if (authentication == null) {
            log.info("Unauthorized /me access attempt");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        AppUserEntity u = users.findByEmailIgnoreCase(email).orElseThrow();
        return new MeResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.isAdmin());
    }
}