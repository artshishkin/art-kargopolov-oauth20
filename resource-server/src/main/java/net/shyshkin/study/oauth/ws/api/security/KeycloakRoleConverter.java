package net.shyshkin.study.oauth.ws.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        Collection<GrantedAuthority> authorities = Optional
                .ofNullable(realmAccess)
                .map(rAccess -> rAccess.get("roles"))
                .filter(roles -> roles instanceof List)
                .map(roles -> (List<String>) roles)
                .stream()
                .flatMap(Collection::stream)
                .map("ROLE_"::concat)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
        log.debug("Convert to granted authorities: {}", authorities);
        return authorities;
    }
}
