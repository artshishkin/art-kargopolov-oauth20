package net.shyshkin.study.oauth.ws.api.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("users")
public class UsersController {

    @GetMapping({"/status/check","/scope/status/check","/role/developer/status/check"})
    public String status(){
        return "Working...";
    }

    @GetMapping({"/role/admin/status/check"})
    public String adminStatus(){
        return "Admin status check";
    }

    @GetMapping({"/role/no_developer/status/check"})
    public String noDevelopersStatus(){
        return "No Dev status check";
    }
}
