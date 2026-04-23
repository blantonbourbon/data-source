package com.data.service.core.security;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalSecurityDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void localLoginRedirectsDirectlyToRequestedReturnUrl() throws Exception {
        mockMvc.perform(get("/api/auth/login").param("returnUrl", "/workspace"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/workspace"));
    }

    @Test
    void localProfileReturnsConfiguredDevUserWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("local-dev-user"))
                .andExpect(jsonPath("$.username").value("local-dev"))
                .andExpect(jsonPath("$.displayName").value("Local Developer"))
                .andExpect(jsonPath("$.email").value("local.dev@example.test"))
                .andExpect(jsonPath("$.permissions[0]").value("module:xms:read"))
                .andExpect(jsonPath("$.permissions[1]").value("module:xms:export"))
                .andExpect(jsonPath("$.claims.environment").value("local"));
    }

    @Test
    void localProfileAllowsUserApiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/user/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(8)));
    }

    @Test
    void localProfileAllowsExportEmailRequestsWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/user/trades/export/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validExportEmailPayload()))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    private String validExportEmailPayload() {
        return """
                {
                  "to": ["alice@example.com"],
                  "cc": [],
                  "attachments": [
                    {
                      "fileName": "trades-export.csv",
                      "contentType": "text/csv;charset=utf-8;",
                      "fileBase64": "aWQsdHJhZGVUeXBlCjEsU3BvdAo="
                    }
                  ]
                }
                """;
    }
}
