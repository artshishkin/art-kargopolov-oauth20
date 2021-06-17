package net.shyshkin.study.oauth.ws.api.albums.services;

import net.shyshkin.study.oauth.ws.api.albums.dto.AlbumDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class HardcodedAlbumsService implements AlbumsService {

    @Override
    public List<AlbumDto> getAllAlbums() {
        return IntStream
                .rangeClosed(1, 2)
                .mapToObj(this::fakeAlbum)
                .collect(Collectors.toList());
    }

    private AlbumDto fakeAlbum(int index) {
        return AlbumDto.builder()
                .id("AlbumId" + index)
                .title("AlbumTitle" + index)
                .description("AlbumDescription" + index)
                .url("AlbumUrl" + index)
                .userId("userId" + index)
                .build();
    }
}
