package net.shyshkin.study.oauth.ws.api.users.security;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class WebSecurity extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.cors()
                .and()
                .authorizeRequests((requests) -> requests
                        .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .antMatchers(HttpMethod.GET, "/actuator/info").permitAll()
                        .antMatchers(HttpMethod.GET, "/users/status/check").authenticated()
                        .antMatchers(HttpMethod.GET, "/users/scope/**").hasAuthority("SCOPE_profile")
                        .antMatchers(HttpMethod.GET, "/users/role/developer/**").hasRole("developer") //.hasAnyRole("developer","user")
                        .antMatchers(HttpMethod.GET, "/users/role/admin/**").hasRole("admin")
                        .antMatchers(HttpMethod.GET, "/users/role/no_developer/**").not().hasRole("developer")
                        .anyRequest().authenticated());

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakAuthorityConverter());

        http.oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(List.of("*"));
        corsConfiguration.setAllowedMethods(List.of("*"));
//        corsConfiguration.setAllowedMethods(List.of("POST"));
        corsConfiguration.setAllowedHeaders(List.of("*"));

        var configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return configurationSource;
    }
}
