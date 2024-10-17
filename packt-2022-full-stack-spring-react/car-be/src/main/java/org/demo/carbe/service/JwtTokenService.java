package org.demo.carbe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.carbe.security.jwt.JwtHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtTokenService implements TokenService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtHelper jwtHelper;

    @Override
    public String generateToken(String username, String password) {
        // Authenticate the user by selecting the user from the database
        log.info("Generating token for user: {}", username);
        // This will throw an exception if the user is not found
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // This check is not necessary actually because the userDetailsService.loadUserByUsername(username) will throw an exception if the user is not found
//        if (userDetails == null || !userDetails.getUsername().equals(username)) {
//            throw new BadCredentialsException("User not found");
//        }
        return jwtHelper.createToken(Collections.emptyMap(), userDetails.getUsername());
    }

    @Override
    public String generateToken(String username) {
        return jwtHelper.createToken(Collections.emptyMap(), username);
    }
}
