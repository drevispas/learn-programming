package org.demo.footballresource.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
//                        .requestMatchers(HttpMethod.GET, "/football/teams/**").hasAuthority("SCOPE_football:read")
                        .requestMatchers(HttpMethod.GET, "/football/teams/**").permitAll()
                        // POST should have admin scope, or it will be forbidden with 403
//                        .requestMatchers(HttpMethod.POST, "/football/teams/**").hasAuthority("SCOPE_football:admin")
                        .requestMatchers(HttpMethod.POST, "/football/teams/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/football/ranking/**").permitAll()
                        .requestMatchers("/football/stats/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/football/bid/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Configure OAuth2 resource server to accept JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // todo: disable csrf for now. apply csrf token later
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}
