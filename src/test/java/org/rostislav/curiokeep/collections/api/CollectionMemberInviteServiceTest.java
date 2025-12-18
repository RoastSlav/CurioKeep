package org.rostislav.curiokeep.collections.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.CollectionMemberManagementService;
import org.rostislav.curiokeep.collections.CollectionMemberRepository;
import org.rostislav.curiokeep.collections.CollectionRepository;
import org.rostislav.curiokeep.collections.api.dto.*;
import org.rostislav.curiokeep.collections.entities.CollectionEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberId;
import org.rostislav.curiokeep.collections.invites.CollectionInviteEntity;
import org.rostislav.curiokeep.collections.invites.CollectionInviteRepository;
import org.rostislav.curiokeep.collections.invites.CollectionInviteService;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionMemberInviteServiceTest {

    @Mock
    CollectionMemberRepository members;
    @Mock
    CollectionRepository collections;
    @Mock
    AppUserRepository users;
    @Mock
    CurrentUserService currentUser;
    @Mock
    CollectionAccessService access;
    @Mock
    CollectionInviteRepository invites;

    CollectionMemberManagementService memberService;
    CollectionInviteService inviteService;

    @BeforeEach
    void setup() {
        memberService = new CollectionMemberManagementService(members, collections, users, currentUser, access);
        inviteService = new CollectionInviteService(invites, members, collections, currentUser, access, memberService);
    }

    @Test
    void listMembersForbiddenForViewer() {
        UUID collectionId = UUID.randomUUID();
        AppUserEntity viewer = user("viewer@example.com", false);
        when(currentUser.requireCurrentUser()).thenReturn(viewer);
        when(access.requireRole(eq(collectionId), eq(viewer.getId()), eq(Role.ADMIN)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> memberService.list(collectionId));
    }

    @Test
    void cannotRemoveOwner() {
        UUID collectionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        AppUserEntity admin = user("admin@example.com", false);
        CollectionMemberEntity adminMembership = membership(collectionId, admin.getId(), Role.ADMIN);

        CollectionEntity collection = new CollectionEntity();
        collection.setId(collectionId);
        collection.setOwnerUserId(ownerId);

        CollectionMemberEntity ownerMembership = membership(collectionId, ownerId, Role.OWNER);

        when(currentUser.requireCurrentUser()).thenReturn(admin);
        when(access.requireRole(eq(collectionId), eq(admin.getId()), eq(Role.ADMIN))).thenReturn(adminMembership);
        when(collections.findById(collectionId)).thenReturn(Optional.of(collection));
        when(members.findByIdCollectionIdAndIdUserId(collectionId, ownerId)).thenReturn(Optional.of(ownerMembership));

        assertThrows(ResponseStatusException.class, () -> memberService.remove(collectionId, ownerId));
    }

    @Test
    void expiredInviteCannotBeAccepted() {
        AppUserEntity user = user("user@example.com", false);
        CollectionInviteEntity invite = new CollectionInviteEntity();
        invite.setToken("tok");
        invite.setCollectionId(UUID.randomUUID());
        invite.setRole(Role.EDITOR);
        invite.setExpiresAt(OffsetDateTime.now().minusDays(1));

        when(currentUser.requireCurrentUser()).thenReturn(user);
        when(invites.findByToken("tok")).thenReturn(Optional.of(invite));

        assertThrows(ResponseStatusException.class, () -> inviteService.accept(new AcceptCollectionInviteRequest("tok")));
    }

    @Test
    void adminCanCreateAndAcceptInvite() {
        UUID collectionId = UUID.randomUUID();
        AppUserEntity admin = user("admin@example.com", true);
        AppUserEntity target = user("member@example.com", false);
        CollectionMemberEntity adminMembership = membership(collectionId, admin.getId(), Role.ADMIN);

        CollectionEntity collection = new CollectionEntity();
        collection.setId(collectionId);
        collection.setOwnerUserId(admin.getId());

        when(currentUser.requireCurrentUser()).thenReturn(admin, target);
        when(access.requireRole(eq(collectionId), eq(admin.getId()), eq(Role.ADMIN))).thenReturn(adminMembership);
        when(collections.findById(collectionId)).thenReturn(Optional.of(collection));
        when(invites.save(any(CollectionInviteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateCollectionInviteResponse created = inviteService.createInvite(collectionId, new CreateCollectionInviteRequest(Role.EDITOR, 3));
        assertThat(created.token()).isNotBlank();
        assertThat(created.role()).isEqualTo(Role.EDITOR);

        CollectionInviteEntity invite = new CollectionInviteEntity();
        invite.setToken(created.token());
        invite.setCollectionId(collectionId);
        invite.setRole(Role.EDITOR);

        when(invites.findByToken(created.token())).thenReturn(Optional.of(invite));
        when(members.findByIdCollectionIdAndIdUserId(collectionId, target.getId())).thenReturn(Optional.empty());
        when(members.save(any(CollectionMemberEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CollectionMemberResponse accepted = inviteService.accept(new AcceptCollectionInviteRequest(created.token()));
        assertThat(accepted.userId()).isEqualTo(target.getId());
        assertThat(accepted.role()).isEqualTo(Role.EDITOR);

        when(currentUser.requireCurrentUser()).thenReturn(admin);
        when(access.requireRole(eq(collectionId), eq(admin.getId()), eq(Role.ADMIN))).thenReturn(adminMembership);
        when(members.findAllByIdCollectionId(collectionId)).thenReturn(List.of(
                membership(collectionId, target.getId(), Role.EDITOR)
        ));
        when(users.findAllById(any(Iterable.class))).thenReturn(List.of(target));

        List<CollectionMemberResponse> membersOut = memberService.list(collectionId);
        assertThat(membersOut).hasSize(1);
        assertThat(membersOut.get(0).email()).isEqualTo(target.getEmail());
    }

    private AppUserEntity user(String email, boolean admin) {
        AppUserEntity u = new AppUserEntity();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setDisplayName(email);
        u.setAdmin(admin);
        return u;
    }

    private CollectionMemberEntity membership(UUID collectionId, UUID userId, Role role) {
        CollectionMemberEntity m = new CollectionMemberEntity();
        m.setId(new CollectionMemberId(collectionId, userId));
        m.setRole(role);
        m.setCreatedAt(OffsetDateTime.now());
        return m;
    }
}
