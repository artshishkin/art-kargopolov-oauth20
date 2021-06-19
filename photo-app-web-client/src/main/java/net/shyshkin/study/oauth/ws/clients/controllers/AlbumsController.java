package net.shyshkin.study.oauth.ws.clients.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlbumsController {

    @GetMapping("/albums")
    public String getAllAlbums(Model model) {
        return "albums";
    }
}
