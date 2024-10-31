package org.demo.todo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorizeHttpRequests ->
                    authorizeHttpRequests.requestMatchers("/api/todos/**").permitAll()
                    .anyRequest().authenticated()
            );
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(c -> {
            CorsConfigurationSource corsConfigurationSource = request -> {
                CorsConfiguration corsConfiguration = new CorsConfiguration();
                corsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
                corsConfiguration.setAllowedMethods(List.of("GET","POST","PUT","DELETE"));
                corsConfiguration.setAllowedHeaders(List.of("*"));
                return corsConfiguration;
            };
            c.configurationSource(corsConfigurationSource);
        });
        return http.build();
    }
}
