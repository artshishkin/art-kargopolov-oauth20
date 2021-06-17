package net.shyshkin.study.oauth.ws.api.albums.services;

import net.shyshkin.study.oauth.ws.api.albums.dto.AlbumDto;

import java.util.List;

public interface AlbumsService {

    List<AlbumDto> getAllAlbums();
}
