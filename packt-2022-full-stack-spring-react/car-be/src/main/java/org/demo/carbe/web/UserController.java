package org.demo.carbe.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public User me(@AuthenticationPrincipal OidcUser oidcUser) {
        return new User(
                oidcUser.getPreferredUsername(),
                oidcUser.getGivenName(),
                oidcUser.getFamilyName(),
                oidcUser.getEmail(),
                List.of("USER")
        );
    }

    public record User(String username, String firstName, String lastName, String email, List<String> roles) {
    }
}
