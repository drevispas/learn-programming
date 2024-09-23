package org.demo.carbe.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    // Set a static password instead of the default random password
    @Bean
    public UserDetailsService userDetailsService() {
        var user = User.withUsername("user")
                .password("{noop}user")
                .authorities("read")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                        .requestMatchers("/**").permitAll()
                        .anyRequest().authenticated()
        )
//                .formLogin(withDefaults())
//                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}
