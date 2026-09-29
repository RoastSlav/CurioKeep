package org.rostislav.curiokeep.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.entities.CollectionMemberEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionAccessServiceTest {

    private static final UUID COLLECTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    CollectionMemberRepository members;

    @ParameterizedTest
    @CsvSource({
            "OWNER,OWNER", "OWNER,ADMIN", "OWNER,EDITOR", "OWNER,VIEWER",
            "ADMIN,ADMIN", "ADMIN,EDITOR", "ADMIN,VIEWER",
            "EDITOR,EDITOR", "EDITOR,VIEWER",
            "VIEWER,VIEWER"
    })
    void memberIsAllowedWhenTheirRoleIsAtLeastTheRequiredOne(Role have, Role need) {
        CollectionMemberEntity member = memberWithRole(have);
        when(members.findByIdCollectionIdAndIdUserId(COLLECTION_ID, USER_ID)).thenReturn(Optional.of(member));

        CollectionMemberEntity result = new CollectionAccessService(members).requireRole(COLLECTION_ID, USER_ID, need);

        assertThat(result).isSameAs(member);
    }

    @ParameterizedTest
    @CsvSource({
            "ADMIN,OWNER",
            "EDITOR,OWNER", "EDITOR,ADMIN",
            "VIEWER,OWNER", "VIEWER,ADMIN", "VIEWER,EDITOR"
    })
    void memberIsForbiddenWhenTheirRoleIsBelowTheRequiredOne(Role have, Role need) {
        when(members.findByIdCollectionIdAndIdUserId(COLLECTION_ID, USER_ID)).thenReturn(Optional.of(memberWithRole(have)));

        assertForbidden(need);
    }

    @Test
    void nonMemberIsForbiddenEvenForViewerAccess() {
        when(members.findByIdCollectionIdAndIdUserId(COLLECTION_ID, USER_ID)).thenReturn(Optional.empty());

        assertForbidden(Role.VIEWER);
    }

    private void assertForbidden(Role need) {
        assertThatThrownBy(() -> new CollectionAccessService(members).requireRole(COLLECTION_ID, USER_ID, need))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private CollectionMemberEntity memberWithRole(Role role) {
        CollectionMemberEntity member = new CollectionMemberEntity();
        member.setRole(role);
        return member;
    }
}
