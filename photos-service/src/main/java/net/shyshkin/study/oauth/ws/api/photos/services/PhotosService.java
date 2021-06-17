package net.shyshkin.study.oauth.ws.api.photos.services;

import net.shyshkin.study.oauth.ws.api.photos.dto.PhotoDto;

import java.util.List;

public interface PhotosService {

    List<PhotoDto> getAllPhotos();
}
