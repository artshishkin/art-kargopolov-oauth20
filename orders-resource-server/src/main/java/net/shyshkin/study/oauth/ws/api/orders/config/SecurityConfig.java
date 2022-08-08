package net.shyshkin.study.oauth.ws.api.orders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Make communication STATELESS
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // Configure antMatchers if needed
        http.authorizeHttpRequests(authz -> authz
                .antMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
        );

        http.oauth2ResourceServer()
                .jwt();

        return http.build();
    }
}
