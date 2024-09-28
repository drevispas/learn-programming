package org.demo.carbe.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class CarControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testWithNoBearerToken() throws Exception {
        mvc
                .perform(get("/cars/owners"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testWithValidBearerToken() throws Exception {
        TokenController.TokenRequest tokenRequest = TokenController.TokenRequest.builder()
                .username("john")
                .password("12345")
                .build();
        MvcResult result = mvc
                .perform(post("/token/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tokenRequest))
                )
                .andExpect(status().isOk())
                .andReturn();
        TokenController.TokenResponse tokenResponse = new ObjectMapper().readValue(
                result.getResponse().getContentAsString(), TokenController.TokenResponse.class
        );
        mvc
                .perform(get("/cars/owners")
                        .header("Authorization", "Bearer " + tokenResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(mvcResult -> assertTrue(mvcResult.getResponse().getContentAsString().contains("John")));
    }
}
