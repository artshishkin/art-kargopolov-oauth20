package net.shyshkin.study.oauth.clients.pkce.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexPageController {

    @GetMapping({"/", "index.html"})
    public String index() {
        return "index";
    }
}
