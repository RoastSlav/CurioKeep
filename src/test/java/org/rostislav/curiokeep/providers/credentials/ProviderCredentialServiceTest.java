package org.rostislav.curiokeep.providers.credentials;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderCredentialServiceTest {

    private static final String SALT = "5f4dcc3b5aa765d6";

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private ProviderCredentialRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(ProviderCredentialRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ProviderCredentialService service(String password, String salt) {
        return new ProviderCredentialService(repository, objectMapper, password, salt);
    }

    private ProviderCredentialEntity savedEntity() {
        ArgumentCaptor<ProviderCredentialEntity> captor = ArgumentCaptor.forClass(ProviderCredentialEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void storesTheCredentialsEncryptedAndReadsThemBack() {
        service("s3cret", SALT).saveCredentials("rawg", Map.of("apiKey", "top-secret-key"));
        ProviderCredentialEntity stored = savedEntity();
        when(repository.findById("rawg")).thenReturn(Optional.of(stored));

        assertThat(stored.getEncryptedPayload()).doesNotContain("top-secret-key").matches("[0-9a-f]+");
        assertThat(service("s3cret", SALT).getCredentials("rawg"))
                .get()
                .satisfies(credential -> assertThat(credential.values()).containsEntry("apiKey", "top-secret-key"));
    }

    @Test
    void encryptsTheSameValuesDifferentlyEachTime() {
        ProviderCredentialService service = service("s3cret", SALT);

        service.saveCredentials("a", Map.of("apiKey", "same"));
        service.saveCredentials("b", Map.of("apiKey", "same"));

        ArgumentCaptor<ProviderCredentialEntity> captor = ArgumentCaptor.forClass(ProviderCredentialEntity.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getEncryptedPayload())
                .isNotEqualTo(captor.getAllValues().get(1).getEncryptedPayload());
    }

    @Test
    void cannotReadCredentialsEncryptedWithAnotherPassword() {
        service("s3cret", SALT).saveCredentials("rawg", Map.of("apiKey", "top-secret-key"));
        ProviderCredentialEntity stored = savedEntity();
        when(repository.findById("rawg")).thenReturn(Optional.of(stored));

        assertThat(service("different", SALT).getCredentials("rawg")).isEmpty();
    }

    @Test
    void refusesToStartWithASaltThatIsNotEvenLengthHex() {
        assertThatThrownBy(() -> service("s3cret", "not-hex-at-all!!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("salt");
        assertThatThrownBy(() -> service("s3cret", "abc")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("s3cret", "5f4dcc3b5aa765d")).isInstanceOf(IllegalStateException.class);
    }
}
