package com.learningplatform.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeControllerTest {

    private static final String USER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String ORG_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void me_missingHeaders_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_validInstructor_returnsPermissions() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", ORG_ID)
                        .header("X-Role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.organizationId").value(ORG_ID))
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.permissions", hasItem("COURSE_CREATE")))
                .andExpect(jsonPath("$.permissions", hasItem("CONTENT_GENERATE")));
    }

    @Test
    void me_invalidRole_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Org-Id", UUID.randomUUID().toString())
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }
}
