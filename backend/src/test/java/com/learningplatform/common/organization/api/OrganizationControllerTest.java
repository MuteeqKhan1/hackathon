package com.learningplatform.common.organization.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerTest {

    private static final String USER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndGet_sameOrg_succeeds() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Physics Academy"));
        // bootstrap org id header can be any UUID; create does not check it against resource
        String bootstrapOrg = UUID.randomUUID().toString();

        MvcResult created = mockMvc.perform(post("/api/v1/organizations")
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", bootstrapOrg)
                        .header("X-Role", "CONTENT_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Physics Academy"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.slug").value("physics-academy"))
                .andReturn();

        String orgId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/organizations/" + orgId)
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", orgId)
                        .header("X-Role", "CONTENT_OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orgId));
    }

    @Test
    void get_otherOrg_returns403() throws Exception {
        String bootstrapOrg = UUID.randomUUID().toString();
        MvcResult created = mockMvc.perform(post("/api/v1/organizations")
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", bootstrapOrg)
                        .header("X-Role", "CONTENT_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Org Isolation Test\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String orgId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        String otherOrg = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/organizations/" + orgId)
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", otherOrg)
                        .header("X-Role", "CONTENT_OWNER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/organizations")
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", UUID.randomUUID().toString())
                        .header("X-Role", "INSTRUCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void addUser_duplicate_returns409() throws Exception {
        String bootstrapOrg = UUID.randomUUID().toString();
        MvcResult created = mockMvc.perform(post("/api/v1/organizations")
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", bootstrapOrg)
                        .header("X-Role", "CONTENT_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dup Membership Org\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String orgId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        String memberBody = objectMapper.writeValueAsString(Map.of(
                "userId", USER_ID,
                "role", "INSTRUCTOR"
        ));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/users")
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", orgId)
                        .header("X-Role", "CONTENT_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_EXISTS"));
    }

    @Test
    void listMine_returnsCreatedOrg() throws Exception {
        String bootstrapOrg = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/organizations")
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", bootstrapOrg)
                        .header("X-Role", "CONTENT_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Listable Org\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/organizations")
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", bootstrapOrg)
                        .header("X-Role", "CONTENT_OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }
}
