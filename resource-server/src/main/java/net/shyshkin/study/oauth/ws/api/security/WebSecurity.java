package net.shyshkin.study.oauth.ws.api.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@EnableWebSecurity
public class WebSecurity extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests((requests) -> requests
                .antMatchers(HttpMethod.GET, "/users/status/check").permitAll()
                .antMatchers(HttpMethod.GET, "/users/scope/**").hasAuthority("SCOPE_profile")
                .antMatchers(HttpMethod.GET, "/users/role/developer/**").hasRole("developer") //.hasAnyRole("developer","user")
                .antMatchers(HttpMethod.GET, "/users/role/admin/**").hasRole("admin")
                .antMatchers(HttpMethod.GET, "/users/role/no_developer/**").not().hasRole("developer")
                .anyRequest().authenticated());

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

        http.oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter);
    }
}
