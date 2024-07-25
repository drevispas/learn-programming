package org.demo.footballui.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

// Use @Controller for Thymeleaf instead of @RestController for JSON
@Controller
@Slf4j
public class FootballController {

    // home.html contains a link to /myself
    @GetMapping("/")
    public String home() {
        return "home";
    }

    // "/myself" need to be authenticated
    @GetMapping("/myself")
    public String myself(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        model.addAttribute("userName", oidcUser.getName());
        model.addAttribute("audience", oidcUser.getAudience());
        model.addAttribute("expiresAt", oidcUser.getExpiresAt());
        model.addAttribute("claims", oidcUser.getClaims());
        return "myself";
    }

    @GetMapping("/teams")
    public String teams(@RegisteredOAuth2AuthorizedClient("football-ui")OAuth2AuthorizedClient authorizedClient, Model model) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        log.info("Access token: {}", authorizedClient.getAccessToken().getTokenValue());
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + authorizedClient.getAccessToken().getTokenValue());
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:8000/football/teams",
                HttpMethod.GET, entity, String.class
        );
        model.addAttribute("teams", response.getBody());
        return "teams";
    }
}
