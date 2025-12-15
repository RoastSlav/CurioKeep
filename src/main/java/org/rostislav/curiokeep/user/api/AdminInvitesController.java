package org.rostislav.curiokeep.user.api;

import org.rostislav.curiokeep.user.InviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/invites")
public class AdminInvitesController {

    private final InviteService inviteService;

    public AdminInvitesController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PreAuthorize("hasAuthority('APP_ADMIN')")
    @PostMapping
    public ResponseEntity<?> createInvite(@RequestBody CreateInviteRequest req) {
        String token = inviteService.createInvite(req.email());
        return ResponseEntity.ok(Map.of(
                "token", token
        ));
    }

    public record CreateInviteRequest(String email) {}
}
