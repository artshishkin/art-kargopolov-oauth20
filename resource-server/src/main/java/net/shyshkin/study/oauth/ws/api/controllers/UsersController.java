package net.shyshkin.study.oauth.ws.api.controllers;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

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
}
