package org.rostislav.curiokeep.user.api;

import org.rostislav.curiokeep.user.InviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/{token}/validate")
    public Map<String, Object> validate(@PathVariable String token) {
        return Map.of("valid", inviteService.isInviteValid(token));
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(@RequestBody AcceptInviteRequest req) {
        inviteService.acceptInvite(req.token(), req.password(), req.displayName());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    public record AcceptInviteRequest(String token, String password, String displayName) {}
}
