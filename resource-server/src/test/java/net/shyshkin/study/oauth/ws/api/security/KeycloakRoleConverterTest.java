package net.shyshkin.study.oauth.ws.api.security;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class KeycloakRoleConverterTest {

    static KeycloakRoleConverter converter;

    @BeforeAll
    static void beforeAll() {
        converter = new KeycloakRoleConverter();
    }

    @Test
    void convert_presentRoles() {
        //given
        List<String> roles = List.of(
                "default-roles-katarinazart",
                "offline_access",
                "developer",
                "uma_authorization"
        );
        Jwt jwt = Jwt.withTokenValue("eyJhbG___SOME_HUGE_TOKEN___6o0DA")
                .header("foo", "buzz")
                .claim("realm_access", Map.of("roles", roles))
                .build();
        //when
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        //then
        assertThat(authorities)
                .hasSize(4)
                .allSatisfy(grantedAuthority -> assertThat(grantedAuthority.getAuthority()).startsWith("ROLE_"));

        log.debug("Authorities: {}", authorities);

    }

    @Test
    void convert_absentRealmAccessClaim() {
        //given
        List<String> roles = List.of(
                "default-roles-katarinazart",
                "offline_access",
                "developer",
                "uma_authorization"
        );
        Jwt jwt = Jwt.withTokenValue("eyJhbG___SOME_HUGE_TOKEN___6o0DA")
                .header("foo", "buzz")
                .claim("fake_realm_access", Map.of("roles", roles))
                .build();
        //when
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        //then
        assertThat(authorities)
                .isEmpty();

        log.debug("Authorities: {}", authorities);

    }

    @Test
    void convert_absentRoles() {
        //given
        List<String> roles = List.of(
                "default-roles-katarinazart",
                "offline_access",
                "developer",
                "uma_authorization"
        );
        Jwt jwt = Jwt.withTokenValue("eyJhbG___SOME_HUGE_TOKEN___6o0DA")
                .header("foo", "buzz")
                .claim("realm_access", Map.of("fake_roles", roles))
                .build();
        //when
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        //then
        assertThat(authorities)
                .isEmpty();

        log.debug("Authorities: {}", authorities);

    }

    @Test
    void convert_rolesEmpty() {
        //given
        List<String> roles = List.of();
        Jwt jwt = Jwt.withTokenValue("eyJhbG___SOME_HUGE_TOKEN___6o0DA")
                .header("foo", "buzz")
                .claim("realm_access", Map.of("roles", roles))
                .build();
        //when
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        //then
        assertThat(authorities)
                .isEmpty();

        log.debug("Authorities: {}", authorities);

    }
}