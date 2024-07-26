package org.demo.footballresource.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // SecurityFilterChain bean intercepts incoming HTTP requests and applies security configuration
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                        // Spring default JwtAuthenticationConverter will be used to convert JWT to Authentication
                        // "SCOPE_" prefix is added to the scope value by JwtAuthenticationConverter
                        .requestMatchers(HttpMethod.GET, "/football/teams/**").hasAuthority("SCOPE_football:read")
                        // POST should have admin scope, or it will be forbidden with 403
                        .requestMatchers(HttpMethod.POST, "/football/teams/**").hasAuthority("SCOPE_football:admin")
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasAuthority("SCOPE_football:admin")
                        .anyRequest().authenticated()
                )
                // Configure OAuth2 resource server to accept JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
