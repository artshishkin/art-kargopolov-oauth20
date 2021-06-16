package net.shyshkin.study.oauth.ws.api.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("users")
public class UsersController {

    @GetMapping({"/status/check", "/scope/status/check", "/role/developer/status/check"})
    public String status() {
        return "Working...";
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

    @PreAuthorize("hasRole('developer') or principal.username == #name")
    @PutMapping("/regular/{name}")
    public String updateUserByUserName(@PathVariable String name) {
        log.debug("updateUserByUserName invoked (PreAuthorize)");
        return "Updated user with name: " + name;
    }

    @PreAuthorize("hasAuthority('ROLE_admin') or principal.username == #name")
    @PutMapping("/super/{name}")
    public String updateSuperUserByUserName(@PathVariable String name) {
        log.debug("updateSuperUserByUserName invoked (PreAuthorize)");
        return "Updated super user with name: " + name;
    }
}
