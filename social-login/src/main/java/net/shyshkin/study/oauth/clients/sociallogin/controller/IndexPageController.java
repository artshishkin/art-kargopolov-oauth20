package net.shyshkin.study.oauth.clients.sociallogin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexPageController {

    @GetMapping({"/","/index","index.html"})
    public String displayIndexPage(){
        return "index";
    }
}
