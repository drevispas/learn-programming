package org.demo.carbe.web;

import lombok.*;
import org.demo.carbe.service.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/self")
    public ResponseEntity<TokenResponse> createSelfToken(@RequestBody TokenRequest tokenRequest) {
        var accessToken = tokenService.generateToken(tokenRequest.username, tokenRequest.password);
        var body = TokenResponse.builder()
                .accessToken(accessToken)
                .build();
        // Return the token in the header as well for the client to be able to read it
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.AUTHORIZATION)
                .body(body);
    }

    @GetMapping("/wrapper")
    public ResponseEntity<TokenResponse> createWrapperToken(@AuthenticationPrincipal OidcUser oidcUser) {
        var accessToken = tokenService.generateToken(oidcUser.getPreferredUsername());
        var body = TokenResponse.builder()
                .accessToken(accessToken)
                .build();
        // Return the token in the header as well for the client to be able to read it
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.AUTHORIZATION)
                .body(body);
    }

    @Data
    @Builder
    public static class TokenRequest {
        private String username;
        private String password;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class TokenResponse {
        private String accessToken;
    }
}
