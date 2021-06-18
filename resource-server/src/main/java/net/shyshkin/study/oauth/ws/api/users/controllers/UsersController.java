package net.shyshkin.study.oauth.ws.api.users.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.api.users.dto.UserDto;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UsersController {

    private final Environment environment;

    @GetMapping({"/status/check", "/scope/status/check", "/role/developer/status/check"})
    public ResponseEntity<String> status() {

        String port = environment.getProperty("local.server.port");
        String hostAddress;
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            hostAddress = "UNKNOWN";
        }
        return ResponseEntity.ok()
                .header("SERVER_PORT", port)
                .header("SERVER_IP", hostAddress)
                .body("Working...");
    }

    @GetMapping({"/role/admin/status/check"})
    public String adminStatus() {
        return "Admin status check";
    }

    @GetMapping({"/role/no_developer/status/check"})
    public String noDevelopersStatus() {
        return "No Dev status check";
    }

    @Secured("ROLE_developer")
    @DeleteMapping("/regular/{id}")
    public String deleteUser(@PathVariable String id) {
        return "Deleted user with id: " + id;
    }

    @Secured("ROLE_admin")
    @DeleteMapping("/super/{id}")
    public String deleteSuperUser(@PathVariable String id) {
        return "Deleted Super user with id: " + id;
    }

    @PreAuthorize("hasRole('developer') or principal.getClaimAsString('preferred_username') == #name")
    @PutMapping("/regular/{name}")
    public String updateUserByUserName(@PathVariable String name) {
        log.debug("updateUserByUserName invoked (PreAuthorize)");
        return "Updated user with name: " + name;
    }

    @PreAuthorize("hasAuthority('ROLE_admin') or principal.getClaimAsString('preferred_username') == #name")
    @PutMapping("/super/{name}")
    public String updateSuperUserByUserName(@PathVariable String name) {
        log.debug("updateSuperUserByUserName invoked (PreAuthorize)");
        return "Updated super user with name: " + name;
    }

    @PreAuthorize("hasAuthority('ROLE_admin') or #jwt.subject == #id")
    @PutMapping("/byId/super/{id}")
    public String updateSuperUserById(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.debug("updateSuperUserById invoked (PreAuthorize)");
        return String.format("Updated super user with id: `%s` and JWT subject: `%s`", id, jwt.getSubject());
    }

    @PreAuthorize("hasAuthority('ROLE_admin') or principal.subject == #id")
    @PutMapping("/byId_principal/super/{id}")
    public String updateSuperUserById(@PathVariable String id) {
        log.debug("updateSuperUserById invoked (PreAuthorize)");
        return String.format("Updated super user with id: `%s` and same JWT subject", id);
    }

    @PostAuthorize("hasAuthority('ROLE_admin') or returnObject.id == #jwt.subject")
    @GetMapping("/byId/super/{id}")
    public UserDto getSuperUserById(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.debug("getSuperUserById invoked (PostAuthorize)");
        return findByIdFakeRepositoryCall(id);
    }

    private UserDto findByIdFakeRepositoryCall(String id) {
        return UserDto.builder()
                .id(id)
                .firstName("Mike")
                .lastName("Wazowski")
                .build();
    }
}
