package net.shyshkin.study.oauth.ws.api.photos.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.api.photos.dto.PhotoDto;
import net.shyshkin.study.oauth.ws.api.photos.services.PhotosService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotosController {

    private final PhotosService photosService;

    @GetMapping
    public List<PhotoDto> getPhotos() {
        return photosService.getAllPhotos();
    }

    @PreAuthorize("hasAuthority('ROLE_admin')")
    @GetMapping("/super-secret")
    public List<PhotoDto> getSecretPhotos() {
        return photosService.getAllPhotos();
    }

}
