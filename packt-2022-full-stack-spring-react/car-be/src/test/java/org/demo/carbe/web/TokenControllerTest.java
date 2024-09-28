package org.demo.carbe.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class TokenControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void shouldNotAllowAccessToUnauthenticatedUsers() throws Exception {
        TokenController.TokenRequest tokenRequest = TokenController.TokenRequest.builder()
                .username("user")
                .password("password")
                .build();
        String request = new ObjectMapper().writeValueAsString(tokenRequest);
        mvc
                .perform(post("/token/create")
                        .contentType("application/json")
                        .content(request)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldAllowAccessToAuthenticatedUsers() throws Exception {
        TokenController.TokenRequest tokenRequest = TokenController.TokenRequest.builder()
                .username("john")
                .password("12345")
                .build();
        String request = new ObjectMapper().writeValueAsString(tokenRequest);
        mvc
                .perform(post("/token/create")
                        .contentType("application/json")
                        .content(request)
                )
                .andExpect(status().isOk());
    }
}
