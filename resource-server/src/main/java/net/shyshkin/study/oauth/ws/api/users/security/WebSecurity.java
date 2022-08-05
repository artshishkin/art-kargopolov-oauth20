package net.shyshkin.study.oauth.ws.api.users.security;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class WebSecurity {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests((authz) -> authz
                        .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .antMatchers(HttpMethod.GET, "/actuator/info").permitAll()
                        .antMatchers(HttpMethod.GET, "/users/status/check").authenticated()
                        .antMatchers(HttpMethod.GET, "/users/scope/**").hasAuthority("SCOPE_profile")
                        .antMatchers(HttpMethod.GET, "/users/role/developer/**").hasRole("developer") //.hasAnyRole("developer","user")
                        .antMatchers(HttpMethod.GET, "/users/role/admin/**").hasRole("admin")
                        .antMatchers(HttpMethod.GET, "/users/role/no_developer/**").not().hasRole("developer")
                        .anyRequest().authenticated()
                );

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakAuthorityConverter());

        http.oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter);

        return http.build();
    }

}
