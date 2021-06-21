package net.shyshkin.study.oauth.clients.sociallogin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class HomePageController {

    @GetMapping
    public String getHomePage(Model model, @AuthenticationPrincipal OAuth2User principal) {

        Optional.ofNullable(principal)
                .map(OAuth2User::getName)
                .ifPresent(name -> model.addAttribute("name", name));
        return "home";
    }

}
