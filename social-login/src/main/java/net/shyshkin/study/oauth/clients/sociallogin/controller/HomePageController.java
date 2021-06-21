package net.shyshkin.study.oauth.clients.sociallogin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Slf4j
@Controller
public class HomePageController {

    @GetMapping("/home")
    public String getHomePage(Model model, @AuthenticationPrincipal OAuth2User principal) {

        log.debug("OAuth2User: {}", principal);

        Optional.ofNullable(principal)
                .map(OAuth2User::getName)
                .ifPresent(name -> model.addAttribute("name", name));

        Optional.ofNullable(principal)
                .map(oAuth2User -> oAuth2User.getAttribute("name"))
                .ifPresent(name -> model.addAttribute("nameAttribute", name));
        return "home";
    }

}
