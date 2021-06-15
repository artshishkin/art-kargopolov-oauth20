package net.shyshkin.study.oauth.ws.api.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class WebSecurity extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests((requests) -> requests
                .antMatchers(HttpMethod.GET, "/users/**").hasAuthority("SCOPE_profile")
                .anyRequest().authenticated());
        http.oauth2ResourceServer().jwt();
    }
}
