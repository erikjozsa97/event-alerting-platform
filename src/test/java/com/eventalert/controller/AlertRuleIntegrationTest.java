package com.eventalert.controller;

import com.eventalert.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlertRuleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createValidNewsRuleSucceeds() throws Exception {
        String token = registerAndLogin();
        String rule = """
                {"category":"NEWS","name":"Test rule","criteria":{"keywords":["earthquake"],"match":"any"},"channelIds":[]}
                """;

        mockMvc.perform(post("/api/alert-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rule))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("NEWS"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void newsRuleWithoutKeywordsIsRejected() throws Exception {
        String token = registerAndLogin();
        String rule = """
                {"category":"NEWS","name":"Bad rule","criteria":{},"channelIds":[]}
                """;

        mockMvc.perform(post("/api/alert-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rule))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disasterRuleWithMagnitudeOutOfRangeIsRejected() throws Exception {
        String token = registerAndLogin();
        String rule = """
                {"category":"DISASTER","name":"Bad rule","criteria":{"minMagnitude":15},"channelIds":[]}
                """;

        mockMvc.perform(post("/api/alert-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rule))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marketRuleWithoutThresholdIsRejected() throws Exception {
        String token = registerAndLogin();
        String rule = """
                {"category":"MARKET","name":"Bad rule","criteria":{"symbol":"AAPL"},"channelIds":[]}
                """;

        mockMvc.perform(post("/api/alert-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rule))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listingAlertRulesWithoutTokenIsRejected() throws Exception {
        // Not pinning 401 vs 403 here — Spring Security's default entry point for an
        // unauthenticated request on a stateless config can return either depending
        // on configuration, and this test cares that access is denied, not which code.
        mockMvc.perform(get("/api/alert-rules"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void userCannotSeeAnotherUsersAlertRule() throws Exception {
        String ownerToken = registerAndLogin();
        String rule = """
                {"category":"NEWS","name":"Owner's rule","criteria":{"keywords":["test"]},"channelIds":[]}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/alert-rules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rule))
                .andExpect(status().isCreated())
                .andReturn();

        String ruleId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        String otherToken = registerAndLogin();
        mockMvc.perform(get("/api/alert-rules/" + ruleId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    private String registerAndLogin() throws Exception {
        String email = "rules-" + System.nanoTime() + "@example.com";
        String body = """
                {"email": "%s", "password": "correct-horse-battery"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
