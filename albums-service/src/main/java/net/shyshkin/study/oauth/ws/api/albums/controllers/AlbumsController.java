package net.shyshkin.study.oauth.ws.api.albums.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.api.albums.dto.AlbumDto;
import net.shyshkin.study.oauth.ws.api.albums.services.AlbumsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
public class AlbumsController {

    private final AlbumsService albumsService;

    @GetMapping
    public List<AlbumDto> getAlbums() {
        return albumsService.getAllAlbums();
    }

    @PreAuthorize("hasAuthority('ROLE_admin')")
    @GetMapping("/super-secret")
    public List<AlbumDto> getSecretAlbums() {
        return albumsService.getAllAlbums();
    }

}
