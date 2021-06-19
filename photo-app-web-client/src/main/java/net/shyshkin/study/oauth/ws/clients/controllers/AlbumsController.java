package net.shyshkin.study.oauth.ws.clients.controllers;

import lombok.RequiredArgsConstructor;
import net.shyshkin.study.oauth.ws.clients.services.AlbumsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AlbumsController {

    private final AlbumsService albumsService;

    @GetMapping("/albums")
    public String getAllAlbums(Model model) {

        model.addAttribute(
                "albums",
                albumsService.getAllAlbums()
        );

        return "albums";
    }
}
