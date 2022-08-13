package net.shyshkin.study.oauth.ws.api.orders.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class MySpringAuthorityConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(getRoleBasedAuthorities(jwt));
        authorities.addAll(getScopeBasedAuthorities(jwt));
        log.debug("Convert to granted authorities: {}", authorities);
        return authorities;
    }

    private Set<GrantedAuthority> getRoleBasedAuthorities(Jwt jwt) {
        var authorities = jwt.getClaimAsStringList("authorities");
        return Optional.ofNullable(authorities)
                .stream()
                .flatMap(Collection::stream)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<GrantedAuthority> getScopeBasedAuthorities(Jwt jwt) {
        var scopes = jwt.getClaimAsStringList("scope");
        return Optional
                .ofNullable(scopes)
                .stream()
                .flatMap(Collection::stream)
                .filter(Strings::isNotBlank)
                .map("SCOPE_"::concat)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }
}
