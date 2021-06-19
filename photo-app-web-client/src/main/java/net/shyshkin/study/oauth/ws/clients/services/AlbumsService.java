package net.shyshkin.study.oauth.ws.clients.services;

import net.shyshkin.study.oauth.ws.clients.dto.AlbumDto;

import java.util.List;

public interface AlbumsService {

    List<AlbumDto> getAllAlbums();
}
