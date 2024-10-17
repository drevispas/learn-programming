package org.demo.carbe.security;

import lombok.RequiredArgsConstructor;
import org.demo.carbe.security.filter.JdbcJwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@RequiredArgsConstructor
@Configuration
public class WebAuthorizationConfig {

    private final Customizer<CorsConfigurer<HttpSecurity>> corsCustomizer;
    private final JdbcJwtFilter jdbcJwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    //    @Bean
//    @Order(1)
//    public SecurityFilterChain formLoginSecurityFilterChain(HttpSecurity http) throws Exception {
//        http.cors(corsCustomizer);
//        http.csrf(AbstractHttpConfigurer::disable);
//        // the authorization rules at the endpoints
//        http
//                .securityMatcher("/home", "/login")
//                .authorizeHttpRequests(authorizeHttpRequests ->
//                        authorizeHttpRequests
//                                .requestMatchers("/login").permitAll()
//                                .anyRequest().authenticated()
//                )
//                .formLogin(formLogin -> formLogin
//                        .defaultSuccessUrl("/home", true)
//                )
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));
//        return http.build();
//    }

//        @Bean
//    @Order(2)
//    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
//        http.cors(corsCustomizer);
//        http.csrf(AbstractHttpConfigurer::disable);
//        // the authorization rules at the endpoints
//        http
//                .authorizeHttpRequests(authorizeRequests ->
//                        authorizeRequests
//                                .requestMatchers("/auth/**").permitAll()
//                                .requestMatchers("/swagger-ui/**").permitAll()
//                                .requestMatchers("/v3/api-docs/**").permitAll()
//                                .anyRequest().authenticated()
//                )
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .addFilterBefore(jdbcJwtFilter, UsernamePasswordAuthenticationFilter.class)
//                .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint));
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain keycloackSecurityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        return http
                .cors(corsCustomizer)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)))
                .build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/home");
        return oidcLogoutSuccessHandler;
    }
}
