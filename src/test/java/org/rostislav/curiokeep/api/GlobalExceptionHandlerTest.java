package org.rostislav.curiokeep.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    record Payload(@NotBlank String name, @Size(max = 5) String note) {
    }

    @RestController
    static class Probe {
        @PostMapping("/probe/body")
        String body(@Valid @RequestBody Payload payload) {
            return "ok";
        }

        @GetMapping("/probe/number")
        String number(@RequestParam int n) {
            return "ok";
        }

        @GetMapping("/probe/denied")
        String denied() {
            throw new AccessDeniedException("internal detail about roles");
        }

        @GetMapping("/probe/integrity")
        String integrity() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"secret_idx\"");
        }

        @GetMapping("/probe/upload")
        String upload() {
            throw new MaxUploadSizeExceededException(1);
        }

        @GetMapping("/probe/reason")
        String reason(@RequestParam String code) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, code);
        }

        @GetMapping("/probe/boom")
        String boom() {
            throw new IllegalArgumentException("secret internal message /var/lib/data");
        }
    }

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void invalidBodyAnswersBadRequestNamingTheFields() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"note\":\"too long\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("name")))
                .andExpect(jsonPath("$.message").value(containsString("note")));
    }

    @Test
    void malformedJsonAnswersBadRequestWithoutParserDetails() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value(not(containsString("Unexpected"))));
    }

    @Test
    void aMissingBodyAndABadParameterTypeAreBadRequests() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        mvc.perform(get("/probe/number").param("n", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/probe/number")).andExpect(status().isBadRequest());
    }

    @Test
    void accessDeniedIsForbiddenAndDoesNotEchoTheReason() throws Exception {
        mvc.perform(get("/probe/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(not(containsString("roles"))));
    }

    @Test
    void aConstraintViolationIsAConflictAndDoesNotLeakSql() throws Exception {
        mvc.perform(get("/probe/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(not(containsString("secret_idx"))));
    }

    @Test
    void anOversizeUploadIsPayloadTooLarge() throws Exception {
        mvc.perform(get("/probe/upload")).andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.error").value("PAYLOAD_TOO_LARGE"));
    }

    @Test
    void wrongMethodAndWrongContentTypeAreClientErrors() throws Exception {
        mvc.perform(delete("/probe/number")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/probe/body").contentType(MediaType.TEXT_PLAIN).content("x")).andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void aReasonCodeKeepsItsStatusAndIsPutIntoWordsForTheMessage() throws Exception {
        mvc.perform(get("/probe/reason").param("code", "DUPLICATE_FIELD_isbn13"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_FIELD_isbn13"))
                .andExpect(jsonPath("$.message").value("Another item in this collection already has this isbn13"));
        mvc.perform(get("/probe/reason").param("code", "MISSING_REQUIRED_FIELD_title"))
                .andExpect(jsonPath("$.message").value("title is required"));
        mvc.perform(get("/probe/reason").param("code", "INVALID_FIELD_pages"))
                .andExpect(jsonPath("$.message").value("pages has an invalid value"));
    }

    @Test
    void anyOtherReasonIsTheMessageWithoutTheStatusPrefix() throws Exception {
        mvc.perform(get("/probe/reason").param("code", "MODULE_NOT_ENABLED"))
                .andExpect(jsonPath("$.message").value("MODULE_NOT_ENABLED"));
    }

    @Test
    void anUnexpectedFailureIsAGenericServerErrorThatNeverShowsItsMessage() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}
