package net.shyshkin.study.oauth.ws.clients.orders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@EnableGlobalMethodSecurity(securedEnabled = true)
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests((authz) -> authz
                        .antMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated());

        http.oauth2Login(Customizer.withDefaults());
//        http.oauth2Login().userInfoEndpoint().userAuthoritiesMapper(userAuthoritiesMapper());

        return http.build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            var authority = authorities.iterator().next();

            boolean isOidc = authority instanceof OidcUserAuthority;

            if (isOidc) {
                var oidcUserAuthority = (OidcUserAuthority) authority;
                var userInfo = oidcUserAuthority.getUserInfo();

                if (userInfo != null) {
                    //Keycloak
                    if (userInfo.hasClaim("realm_access")) {
                        var realmAccess = userInfo.getClaimAsMap("realm_access");
                        var roles = (Collection<String>) realmAccess.get("roles");
                        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                    }

                    //My Spring auth server
                    if (userInfo.hasClaim("authorities")) {
                        var roles = userInfo.getClaimAsStringList("authorities");
                        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                    }
                }

            } else {
                var oauth2UserAuthority = (OAuth2UserAuthority) authority;
                Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

                if (userAttributes != null) {
                    if (userAttributes.containsKey("realm_access")) {
                        var realmAccess = (Map<String, Object>) userAttributes.get("realm_access");
                        var roles = (Collection<String>) realmAccess.get("roles");
                        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                    }

                    if (userAttributes.containsKey("authorities")) {
                        var roles = (Collection<String>) userAttributes.get("authorities");
                        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                    }
                }
            }

            return mappedAuthorities;
        };
    }

    Collection<GrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

}
