package org.rostislav.curiokeep.items.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.items.ItemImageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock
    ItemImageService images;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AssetController(images)).build();
    }

    @Test
    void servesAnImageWithItsRealTypeAndHeadersThatStopScriptExecution() throws Exception {
        when(images.load("0123456789abcdef0123456789abcdef.png")).thenReturn(Optional.of(
                new ItemImageService.StoredImage(new ByteArrayResource(new byte[]{1, 2, 3}), MediaType.IMAGE_PNG)));

        mvc.perform(get("/api/assets/0123456789abcdef0123456789abcdef.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; sandbox"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    void answersNotFoundForAnUnknownFile() throws Exception {
        when(images.load("missing.png")).thenReturn(Optional.empty());

        mvc.perform(get("/api/assets/missing.png")).andExpect(status().isNotFound());
    }
}
