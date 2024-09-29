package org.demo.carbe.web;

import lombok.*;
import org.demo.carbe.service.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/token")
@RestController
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/create")
    public ResponseEntity<TokenResponse> createToken(@RequestBody TokenRequest tokenRequest) {
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
